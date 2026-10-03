package app.simmer.importer

import app.simmer.data.Recipe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.jsoup.Jsoup
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/** A recipe found on the web but not yet imported. */
data class WebResult(
    val title: String,
    val url: String,
    val site: String,
    val snippet: String = "",
    val imageUrl: String = "",
    /** Set when the result came with full recipe data (TheMealDB), so no page fetch is needed. */
    val ready: Recipe? = null,
)

/**
 * Finds recipes on the web. Two free sources, no keys:
 *  - DuckDuckGo's HTML results for any query ("recipe" is appended)
 *  - TheMealDB's open database, which returns complete recipes with photos
 */
object WebSearch {
    private val client = OkHttpClient.Builder().connectTimeout(12, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS).build()
    private const val UA = "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36"

    private fun get(url: String): String = client.newCall(
        Request.Builder().url(url).header("User-Agent", UA).header("Accept-Language", "en-US,en;q=0.9").build(),
    ).execute().use { if (it.isSuccessful) it.body?.string().orEmpty() else "" }

    private val blocked = listOf("youtube.com", "pinterest.", "facebook.com", "instagram.com", "tiktok.com", "reddit.com", "quora.com", "amazon.")

    suspend fun search(query: String): List<WebResult> = withContext(Dispatchers.IO) {
        val q = query.trim()
        if (q.isEmpty()) return@withContext emptyList()
        val fromDb = runCatching { mealDbSearch(q) }.getOrDefault(emptyList())
        val fromWeb = runCatching { duckDuckGo(if (q.contains("recipe", true)) q else "$q recipe") }.getOrDefault(emptyList())
        // Interleave: a couple of complete database recipes first, then the web.
        (fromDb.take(3) + fromWeb + fromDb.drop(3)).distinctBy { it.url }
    }

    private fun duckDuckGo(q: String): List<WebResult> {
        val enc = URLEncoder.encode(q, "UTF-8")
        var html = get("https://html.duckduckgo.com/html/?q=$enc")
        if (html.isBlank() || !html.contains("result__a")) html = get("https://lite.duckduckgo.com/lite/?q=$enc")
        if (html.isBlank()) return emptyList()
        val doc = Jsoup.parse(html, "https://duckduckgo.com/")
        val anchors = doc.select("a.result__a").ifEmpty { doc.select("a.result-link") }
        return anchors.mapNotNull { a ->
            var href = a.absUrl("href").ifBlank { a.attr("href") }
            // DDG wraps links as //duckduckgo.com/l/?uddg=<encoded url>
            Regex("""[?&]uddg=([^&]+)""").find(href)?.let { href = URLDecoder.decode(it.groupValues[1], "UTF-8") }
            if (!href.startsWith("http")) return@mapNotNull null
            if (blocked.any { href.contains(it) }) return@mapNotNull null
            val site = runCatching { java.net.URI(href).host.removePrefix("www.") }.getOrDefault("")
            val snippet = a.parents().firstOrNull { it.hasClass("result") }?.selectFirst(".result__snippet")?.text()
                ?: a.closest("tr")?.nextElementSibling()?.selectFirst(".result-snippet")?.text()
            WebResult(title = a.text().trim(), url = href, site = site, snippet = snippet?.trim().orEmpty())
        }.distinctBy { it.url }.take(20)
    }

    private fun mealDbSearch(q: String): List<WebResult> {
        val json = get("https://www.themealdb.com/api/json/v1/1/search.php?s=" + URLEncoder.encode(q, "UTF-8"))
        return parseMeals(json)
    }

    /** A handful of random complete recipes for the empty Discover screen. */
    suspend fun inspiration(count: Int = 6): List<WebResult> = withContext(Dispatchers.IO) {
        (1..count).mapNotNull { runCatching { parseMeals(get("https://www.themealdb.com/api/json/v1/1/random.php")).firstOrNull() }.getOrNull() }
            .distinctBy { it.url }
    }

    /** Browse TheMealDB by category: Beef, Chicken, Dessert, Vegetarian, Seafood, Pasta, Breakfast... */
    suspend fun byCategory(category: String): List<WebResult> = withContext(Dispatchers.IO) {
        val json = runCatching { get("https://www.themealdb.com/api/json/v1/1/filter.php?c=" + URLEncoder.encode(category, "UTF-8")) }.getOrDefault("")
        val arr = runCatching { JSONObject(json).optJSONArray("meals") }.getOrNull() ?: return@withContext emptyList()
        (0 until minOf(arr.length(), 24)).map { i ->
            val m = arr.getJSONObject(i)
            val id = m.optString("idMeal")
            WebResult(title = m.optString("strMeal"), url = "https://www.themealdb.com/meal/$id", site = "themealdb.com", imageUrl = m.optString("strMealThumb"))
        }
    }

    /** Fetches the full recipe for a category result (which only has a title and photo). */
    suspend fun lookupMeal(url: String): Recipe? = withContext(Dispatchers.IO) {
        val id = url.substringAfterLast('/')
        runCatching { parseMeals(get("https://www.themealdb.com/api/json/v1/1/lookup.php?i=$id")).firstOrNull()?.ready }.getOrNull()
    }

    private fun parseMeals(json: String): List<WebResult> {
        if (json.isBlank()) return emptyList()
        val arr = JSONObject(json).optJSONArray("meals") ?: return emptyList()
        return (0 until arr.length()).map { i ->
            val m = arr.getJSONObject(i)
            val ingredients = (1..20).mapNotNull { n ->
                val ing = m.optString("strIngredient$n").trim()
                val meas = m.optString("strMeasure$n").trim()
                if (ing.isBlank() || ing == "null") null else listOf(meas, ing).filter { it.isNotBlank() && it != "null" }.joinToString(" ")
            }
            val steps = m.optString("strInstructions").split(Regex("""\r?\n+""")).map { it.trim().removePrefix("STEP").trim() }
                .filter { it.length > 3 }.map { it.replace(Regex("""^\d+[.)]\s*"""), "") }
            val id = m.optString("idMeal")
            val source = m.optString("strSource").takeIf { it.startsWith("http") } ?: "https://www.themealdb.com/meal/$id"
            val recipe = Recipe(
                title = m.optString("strMeal"),
                category = m.optString("strCategory").takeIf { it != "null" }.orEmpty(),
                ingredients = ingredients,
                steps = steps,
                source = source,
                imageUrl = m.optString("strMealThumb"),
                notes = m.optString("strArea").takeIf { it.isNotBlank() && it != "null" }?.let { "$it cuisine" }.orEmpty(),
            )
            WebResult(title = recipe.title, url = source, site = "themealdb.com", imageUrl = recipe.imageUrl, snippet = recipe.notes, ready = recipe)
        }
    }
}
