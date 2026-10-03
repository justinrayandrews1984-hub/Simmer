package app.simmer.importer

import app.simmer.data.Recipe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.TextNode
import java.util.concurrent.TimeUnit

/**
 * Pulls a recipe out of a web page.
 *
 * Order of attempts:
 *  1. schema.org JSON-LD "Recipe" block (what nearly every recipe site publishes for Google)
 *  2. schema.org microdata (itemprop="recipeIngredient" etc.)
 *  3. A heading-based guess over the visible text ("Ingredients" ... "Instructions")
 */
object RecipeImporter {

    class ImportException(message: String) : Exception(message)

    private val client: OkHttpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private const val UA =
        "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36"

    /** Finds the first http(s) link in a blob of shared text. */
    fun extractUrl(text: String?): String? {
        if (text == null) return null
        val m = Regex("""https?://[^\s<>"']+""").find(text) ?: return null
        return m.value.trimEnd('.', ',', ')')
    }

    suspend fun fromUrl(rawUrl: String): Recipe = withContext(Dispatchers.IO) {
        val url = rawUrl.trim().let { if (it.startsWith("http")) it else "https://$it" }
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", UA)
            .header("Accept", "text/html,application/xhtml+xml")
            .header("Accept-Language", "en-US,en;q=0.9")
            .build()
        val html = try {
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) throw ImportException("The site answered with an error (${resp.code}).")
                resp.body?.string() ?: throw ImportException("The page was empty.")
            }
        } catch (e: ImportException) {
            throw e
        } catch (e: Exception) {
            throw ImportException("Couldn't reach that page. Check the link and your connection.")
        }
        parseHtml(html, url)
    }

    fun parseHtml(html: String, url: String): Recipe {
        val doc = Jsoup.parse(html, url)
        val pageImage = pageImage(doc)
        val fromLd = parseJsonLd(doc)
        if (fromLd != null && (fromLd.ingredients.isNotEmpty() || fromLd.steps.isNotEmpty())) {
            return fromLd.copy(source = url, imageUrl = fromLd.imageUrl.ifBlank { pageImage })
        }
        val fromMicro = parseMicrodata(doc)
        if (fromMicro != null && (fromMicro.ingredients.isNotEmpty() || fromMicro.steps.isNotEmpty())) {
            return fromMicro.copy(source = url, imageUrl = fromMicro.imageUrl.ifBlank { pageImage })
        }
        val guess = guessFromText(doc)
        if (guess.ingredients.isEmpty() && guess.steps.isEmpty()) {
            throw ImportException("No recipe found on that page. You can still add it by hand.")
        }
        return guess.copy(source = url, imageUrl = pageImage)
    }

    /** The page's share image (og:image), used when the recipe block has no picture of its own. */
    private fun pageImage(doc: Document): String {
        val og = doc.selectFirst("meta[property=og:image], meta[name=og:image], meta[name=twitter:image]")
            ?.attr("content")?.trim().orEmpty()
        if (og.startsWith("http")) return og
        val micro = doc.selectFirst("[itemprop=image]")
        val src = micro?.absUrl("src").orEmpty().ifBlank { micro?.absUrl("content").orEmpty() }
        return if (src.startsWith("http")) src else ""
    }

    /** schema.org `image` can be a URL string, a list of them, or an ImageObject. */
    private fun imageFrom(v: Any?): String = when (v) {
        null -> ""
        is String -> if (v.startsWith("http")) v else ""
        is JSONArray -> (0 until v.length()).asSequence().map { imageFrom(v.opt(it)) }.firstOrNull { it.isNotBlank() } ?: ""
        is JSONObject -> imageFrom(v.opt("url")).ifBlank { imageFrom(v.opt("contentUrl")) }
        else -> ""
    }

    // ---------- 1. JSON-LD ----------

    private fun parseJsonLd(doc: Document): Recipe? {
        for (script in doc.select("script[type=application/ld+json]")) {
            val raw = script.data().trim()
            if (raw.isEmpty()) continue
            val node: Any = try {
                if (raw.startsWith("[")) JSONArray(raw) else JSONObject(raw)
            } catch (e: Exception) {
                continue
            }
            val recipeNode = findRecipe(node) ?: continue
            return recipeFromJson(recipeNode)
        }
        return null
    }

    private fun isRecipeType(obj: JSONObject): Boolean {
        val t = obj.opt("@type") ?: return false
        return when (t) {
            is String -> t.equals("Recipe", ignoreCase = true)
            is JSONArray -> (0 until t.length()).any { t.optString(it).equals("Recipe", ignoreCase = true) }
            else -> false
        }
    }

    private fun findRecipe(node: Any?, depth: Int = 0): JSONObject? {
        if (depth > 6) return null
        when (node) {
            is JSONObject -> {
                if (isRecipeType(node)) return node
                node.optJSONArray("@graph")?.let { g -> findRecipe(g, depth + 1)?.let { return it } }
                node.opt("mainEntity")?.let { m -> findRecipe(m, depth + 1)?.let { return it } }
                for (key in node.keys()) {
                    val v = node.opt(key)
                    if (v is JSONObject || v is JSONArray) findRecipe(v, depth + 1)?.let { return it }
                }
            }
            is JSONArray -> for (i in 0 until node.length()) findRecipe(node.opt(i), depth + 1)?.let { return it }
        }
        return null
    }

    private fun recipeFromJson(r: JSONObject): Recipe {
        val ingredients = stringList(r.opt("recipeIngredient").let { it ?: r.opt("ingredients") })
            .map { clean(it) }.filter { it.isNotBlank() }
        val steps = steps(r.opt("recipeInstructions")).map { clean(it) }.filter { it.isNotBlank() }
        val total = r.optString("totalTime", "")
            .ifBlank { r.optString("cookTime", "") }
            .ifBlank { r.optString("prepTime", "") }
        return Recipe(
            title = clean(r.optString("name", "")),
            category = firstString(r.opt("recipeCategory")).let { clean(it) },
            servings = firstString(r.opt("recipeYield")).let { clean(it) },
            time = duration(total),
            ingredients = ingredients,
            steps = steps,
            notes = clean(r.optString("description", "")).take(600),
            imageUrl = imageFrom(r.opt("image")),
        )
    }

    private fun stringList(v: Any?): List<String> = when (v) {
        null -> emptyList()
        is String -> v.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        is JSONArray -> (0 until v.length()).flatMap { stringList(v.opt(it)) }
        is JSONObject -> listOf(v.optString("text", v.optString("name", ""))).filter { it.isNotBlank() }
        else -> listOf(v.toString())
    }

    private fun firstString(v: Any?): String = when (v) {
        null -> ""
        is String -> v
        is Number -> v.toString()
        is JSONArray -> if (v.length() > 0) firstString(v.opt(0)) else ""
        is JSONObject -> v.optString("name", v.optString("text", ""))
        else -> v.toString()
    }

    /** recipeInstructions can be a string, a list of strings, HowToStep objects, or HowToSections. */
    private fun steps(v: Any?): List<String> = when (v) {
        null -> emptyList()
        is String -> splitSteps(v)
        is JSONArray -> (0 until v.length()).flatMap { steps(v.opt(it)) }
        is JSONObject -> when {
            v.has("itemListElement") -> steps(v.opt("itemListElement"))
            else -> listOf(v.optString("text", v.optString("name", ""))).filter { it.isNotBlank() }
        }
        else -> emptyList()
    }

    private fun splitSteps(s: String): List<String> {
        val d = Jsoup.parse(s)
        d.select("br").forEach { it.replaceWith(TextNode("\n")) }
        d.select("p, li").forEach { it.appendText("\n") }
        val text = d.body().wholeText()
        val lines = text.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        return if (lines.size > 1) lines else text.split(Regex("""(?<=[.!?])\s+(?=[A-Z])""")).map { it.trim() }.filter { it.isNotEmpty() }
    }

    /** ISO 8601 durations like PT1H30M -> "1 h 30 min". Anything else is passed through. */
    fun duration(iso: String): String {
        if (iso.isBlank()) return ""
        val m = Regex("""^P(?:(\d+)D)?T?(?:(\d+)H)?(?:(\d+)M)?(?:(\d+)S)?$""", RegexOption.IGNORE_CASE).find(iso.trim())
            ?: return iso
        val days = m.groupValues[1].toIntOrNull() ?: 0
        val hours = m.groupValues[2].toIntOrNull() ?: 0
        val mins = m.groupValues[3].toIntOrNull() ?: 0
        val parts = mutableListOf<String>()
        if (days > 0) parts += "$days d"
        if (hours > 0) parts += "$hours h"
        if (mins > 0) parts += "$mins min"
        return parts.joinToString(" ")
    }

    // ---------- 2. Microdata ----------

    private fun parseMicrodata(doc: Document): Recipe? {
        val ing = doc.select("[itemprop=recipeIngredient], [itemprop=ingredients]").map { clean(it.text()) }.filter { it.isNotBlank() }
        val stepsEls = doc.select("[itemprop=recipeInstructions]")
        val steps = stepsEls.flatMap { el ->
            val lis = el.select("li, p")
            if (lis.isNotEmpty()) lis.map { it.text() } else splitSteps(el.html())
        }.map { clean(it) }.filter { it.isNotBlank() }
        if (ing.isEmpty() && steps.isEmpty()) return null
        return Recipe(
            title = clean(doc.selectFirst("[itemprop=name]")?.text() ?: doc.title()),
            servings = clean(doc.selectFirst("[itemprop=recipeYield]")?.text() ?: ""),
            time = duration(doc.selectFirst("[itemprop=totalTime]")?.attr("content") ?: ""),
            ingredients = ing,
            steps = steps,
        )
    }

    // ---------- 3. Heading-based guess ----------

    private val ingHeading = Regex("""^(ingredients?|what you need|you will need)\b""", RegexOption.IGNORE_CASE)
    private val stepHeading = Regex("""^(instructions?|directions?|method|steps?|preparation|how to make)\b""", RegexOption.IGNORE_CASE)
    private val stopHeading = Regex("""^(notes?|nutrition|equipment|video|comments?|reviews?|tips?|storage|more recipes|you might also like)\b""", RegexOption.IGNORE_CASE)

    private fun guessFromText(doc: Document): Recipe {
        doc.select("script, style, nav, footer, header, aside, form").remove()
        val lines = doc.body().select("h1, h2, h3, h4, h5, p, li, span.ingredient, div.step")
            .map { it.ownText().ifBlank { it.text() }.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
        fun isHeading(l: String, re: Regex) = l.length < 40 && re.containsMatchIn(l.replace(Regex("[:#*_]"), "").trim())
        val ingStart = lines.indexOfFirst { isHeading(it, ingHeading) }
        val stepStart = lines.indexOfFirst { isHeading(it, stepHeading) }
        fun slice(from: Int, until: Int): List<String> {
            val out = mutableListOf<String>()
            var i = from + 1
            while (i < lines.size && (until < 0 || i < until)) {
                val l = lines[i]
                if (isHeading(l, stopHeading) || isHeading(l, ingHeading) || isHeading(l, stepHeading)) break
                val c = cleanLine(l)
                if (c.isNotBlank()) out += c
                i++
            }
            return out
        }
        val ingredients = if (ingStart >= 0) slice(ingStart, if (stepStart > ingStart) stepStart else -1).filter { it.length < 160 } else emptyList()
        val steps = if (stepStart >= 0) slice(stepStart, if (ingStart > stepStart) ingStart else -1).filter { it.length > 8 } else emptyList()
        val title = doc.selectFirst("h1")?.text()?.trim().orEmpty().ifBlank { doc.title().substringBefore(" | ").substringBefore(" - ").trim() }
        return Recipe(title = clean(title), ingredients = ingredients, steps = steps)
    }

    private fun cleanLine(s: String): String =
        s.replace(Regex("""^[\s\-•\*●■–—>]+"""), "")
            .replace(Regex("""^(step\s*)?\d+[.):]\s*""", RegexOption.IGNORE_CASE), "")
            .trim()

    /** Strips stray HTML and entities that sites leave inside their JSON. */
    private fun clean(s: String): String =
        Jsoup.parse(s).text().replace(Regex("""\s+"""), " ").trim()
}
