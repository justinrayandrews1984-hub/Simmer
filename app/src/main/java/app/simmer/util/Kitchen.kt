package app.simmer.util

import kotlin.math.abs
import kotlin.math.roundToInt

/** Scaling of ingredient quantities, timer detection in steps, and pantry matching. */
object Kitchen {

    private val fractions = mapOf('½' to 0.5, '⅓' to 1.0 / 3, '⅔' to 2.0 / 3, '¼' to 0.25, '¾' to 0.75, '⅛' to 0.125)
    private val leadingQty = Regex("""^\s*(\d+\s+\d+/\d+|\d+/\d+|\d+(?:[.,]\d+)?|[½⅓⅔¼¾⅛]|\d+\s?[½⅓⅔¼¾⅛])(\s*(?:-|–|to)\s*(\d+(?:[.,]\d+)?|[½⅓⅔¼¾⅛]))?""")

    private fun parseNumber(s: String): Double? {
        val t = s.trim()
        if (t.length == 1 && fractions.containsKey(t[0])) return fractions[t[0]]
        if (t.isNotEmpty() && fractions.containsKey(t.last())) {
            val whole = t.dropLast(1).trim().toDoubleOrNull() ?: 0.0
            return whole + (fractions[t.last()] ?: 0.0)
        }
        if (t.contains('/')) {
            val parts = t.split(' ').filter { it.isNotBlank() }
            var total = 0.0
            for (p in parts) {
                if (p.contains('/')) {
                    val (a, b) = p.split('/')
                    val d = b.toDoubleOrNull() ?: return null
                    if (d == 0.0) return null
                    total += (a.toDoubleOrNull() ?: return null) / d
                } else total += p.toDoubleOrNull() ?: return null
            }
            return total
        }
        return t.replace(',', '.').toDoubleOrNull()
    }

    /** 1.5 -> "1½", 0.333 -> "⅓", 2.0 -> "2", 2.37 -> "2.4". */
    fun formatQty(v: Double): String {
        if (v <= 0) return "0"
        val whole = v.toInt()
        val frac = v - whole
        val nice = listOf(0.125 to "⅛", 0.25 to "¼", 1.0 / 3 to "⅓", 0.5 to "½", 2.0 / 3 to "⅔", 0.75 to "¾")
        val match = nice.firstOrNull { abs(it.first - frac) < 0.04 }
        return when {
            frac < 0.04 -> "$whole"
            match != null -> if (whole == 0) match.second else "$whole${match.second}"
            abs(frac - 1.0) < 0.04 -> "${whole + 1}"
            else -> String.format("%.1f", v).removeSuffix(".0")
        }
    }

    /** Multiplies the leading quantity of an ingredient line, leaving the rest alone. */
    fun scaleIngredient(line: String, factor: Double): String {
        if (factor == 1.0) return line
        val m = leadingQty.find(line) ?: return line
        val first = parseNumber(m.groupValues[1]) ?: return line
        val second = m.groupValues[3].takeIf { it.isNotBlank() }?.let { parseNumber(it) }
        val rest = line.substring(m.range.last + 1)
        val scaled = if (second != null) "${formatQty(first * factor)}–${formatQty(second * factor)}" else formatQty(first * factor)
        return scaled + rest
    }

    /** Pulls the servings count out of strings like "4", "4 servings", "Serves 6", "makes 12 cookies". */
    fun servingsNumber(s: String): Int? = Regex("""\d+""").find(s)?.value?.toIntOrNull()?.takeIf { it in 1..200 }

    data class TimerHint(val label: String, val seconds: Int)

    private val timerRe = Regex("""(\d+(?:[.,]\d+)?)(?:\s*(?:-|–|to)\s*(\d+(?:[.,]\d+)?))?\s*(hours?|hrs?|h\b|minutes?|mins?|min\b|seconds?|secs?)""", RegexOption.IGNORE_CASE)

    /** Finds "20 minutes", "1-2 hours", "45 sec" in a step; uses the upper bound of a range. */
    fun timersIn(step: String): List<TimerHint> = timerRe.findAll(step).mapNotNull { m ->
        val a = m.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return@mapNotNull null
        val b = m.groupValues[2].replace(',', '.').toDoubleOrNull()
        val n = b ?: a
        val unit = m.groupValues[3].lowercase()
        val secs = when {
            unit.startsWith("h") -> n * 3600
            unit.startsWith("m") -> n * 60
            else -> n
        }.roundToInt()
        if (secs < 5 || secs > 24 * 3600) null else TimerHint(m.value.trim(), secs)
    }.distinctBy { it.seconds }.toList()

    fun formatClock(totalSeconds: Int): String {
        val h = totalSeconds / 3600
        val m = (totalSeconds % 3600) / 60
        val s = totalSeconds % 60
        return if (h > 0) String.format("%d:%02d:%02d", h, m, s) else String.format("%d:%02d", m, s)
    }

    // ---------- Pantry matching ----------

    private val stopWords = setOf(
        "cup", "cups", "tbsp", "tablespoon", "tablespoons", "tsp", "teaspoon", "teaspoons", "oz", "ounce", "ounces",
        "lb", "lbs", "pound", "pounds", "g", "gram", "grams", "kg", "ml", "l", "litre", "liter", "clove", "cloves",
        "pinch", "dash", "can", "cans", "package", "large", "medium", "small", "fresh", "chopped", "diced", "minced",
        "sliced", "ground", "of", "to", "taste", "and", "or", "for", "the", "a", "optional", "finely", "roughly",
        "plus", "more", "divided", "about", "into", "cut", "peeled", "softened", "melted", "room", "temperature",
    )
    private val pantryStaples = setOf("salt", "pepper", "water", "oil", "olive", "sugar", "flour", "butter")

    /** The "main word" of an ingredient line, used for matching: "2 cups diced yellow onion" -> "onion". */
    fun keyword(line: String): String {
        val cleaned = line.lowercase()
            .replace(Regex("""\(.*?\)"""), " ")
            .replace(Regex("""[^a-z\s]"""), " ")
        val words = cleaned.split(Regex("\\s+")).filter { it.length > 2 && it !in stopWords }
        return words.lastOrNull()?.let { singular(it) } ?: ""
    }

    fun singular(w: String) = when {
        w.endsWith("ies") -> w.dropLast(3) + "y"
        w.endsWith("oes") -> w.dropLast(2)
        w.endsWith("s") && !w.endsWith("ss") -> w.dropLast(1)
        else -> w
    }

    fun isStaple(keyword: String) = keyword in pantryStaples

    data class Match(val missing: List<String>, val have: Int, val needed: Int) {
        val canMake get() = missing.isEmpty()
    }

    /** Compares a recipe's ingredients against what the cook has; staples are assumed on hand. */
    fun match(ingredients: List<String>, pantry: Set<String>): Match {
        val pantryWords = pantry.map { singular(it.trim().lowercase()) }.filter { it.isNotBlank() }.toSet()
        val needed = ingredients.map { keyword(it) }.filter { it.isNotBlank() && !isStaple(it) }.distinct()
        val missing = needed.filter { k -> pantryWords.none { p -> p == k || k.contains(p) || p.contains(k) } }
        return Match(missing, needed.size - missing.size, needed.size)
    }
}
