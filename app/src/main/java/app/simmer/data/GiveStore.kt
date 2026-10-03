package app.simmer.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate

/**
 * Two separate numbers, on purpose:
 *  - mealsFunded: real money that reached the food bank (ads watched, tips). Shown on Impact.
 *  - embers: game points for using and spreading the app. Levels and unlocks run on these.
 */
data class GiveState(
    val mealsFunded: Int = 0,
    val embers: Int = 0,
    val tipsCents: Int = 0,
    val adsWatchedTotal: Int = 0,
    val adsWatchedToday: Int = 0,
    val streakDays: Int = 0,
    val lastGiveDay: String = "",
    val recipesCooked: Int = 0,
    val cookedToday: Int = 0,
    val savedToday: Int = 0,
    val sharedRecipesToday: Int = 0,
    val sharedAppToday: Boolean = false,
    val bonusClaimed: Boolean = false,
    // customizations
    val theme: String = "herb",
    val icon: String = "herb",
    val compactGrid: Boolean = false,
    val sansFont: Boolean = false,
    val confetti: Boolean = true,
    val chefTitleOnShares: Boolean = true,
) {
    val level: ChefLevel get() = ChefLevel.forEmbers(embers)
    val nextLevel: ChefLevel? get() = ChefLevel.entries.firstOrNull { it.minEmbers > embers }
    val adsLeftToday: Int get() = (app.simmer.Config.MAX_ADS_PER_DAY - adsWatchedToday).coerceAtLeast(0)
    fun has(unlock: Unlock) = embers >= unlock.level.minEmbers
}

enum class ChefLevel(val title: String, val minEmbers: Int, val dish: String) {
    HOME_COOK("Home Cook", 0, "a bowl of soup"),
    PREP_COOK("Prep Cook", 30, "a loaf of bread"),
    LINE_COOK("Line Cook", 80, "a pan of roast veg"),
    SOUS_CHEF("Sous Chef", 160, "a family dinner"),
    CHEF_DE_PARTIE("Chef de Partie", 300, "a Sunday roast"),
    HEAD_CHEF("Head Chef", 500, "a feast"),
    EXECUTIVE_CHEF("Executive Chef", 800, "a banquet"),
    COMMUNITY_KITCHEN("Community Kitchen", 1200, "a street party");

    companion object {
        fun forEmbers(e: Int): ChefLevel = entries.last { e >= it.minEmbers }
    }
}

/** Everything a level can unlock. Pure style: nothing useful is ever locked. */
enum class Unlock(val level: ChefLevel, val title: String, val blurb: String, val kind: Kind, val key: String = "") {
    THEME_HERB(ChefLevel.HOME_COOK, "Herb", "The classic green kitchen.", Kind.THEME, "herb"),
    THEME_PAPRIKA(ChefLevel.PREP_COOK, "Paprika", "Warm and smoky.", Kind.THEME, "paprika"),
    CONFETTI(ChefLevel.PREP_COOK, "Confetti", "A little celebration when you finish cooking.", Kind.TOGGLE, "confetti"),
    THEME_MIDNIGHT(ChefLevel.LINE_COOK, "Midnight", "Deep navy, always dark.", Kind.THEME, "midnight"),
    COMPACT_GRID(ChefLevel.LINE_COOK, "Compact grid", "Three recipes across instead of two.", Kind.TOGGLE, "compact"),
    THEME_HONEY(ChefLevel.SOUS_CHEF, "Honey", "Golden and sunlit.", Kind.THEME, "honey"),
    ICON_PAPRIKA(ChefLevel.SOUS_CHEF, "Paprika icon", "A warm red app icon.", Kind.ICON, "paprika"),
    THEME_OCEAN(ChefLevel.CHEF_DE_PARTIE, "Ocean", "Cool teal and sea glass.", Kind.THEME, "ocean"),
    CHEF_TITLE(ChefLevel.CHEF_DE_PARTIE, "Chef title on shares", "Sign shared recipes with your rank.", Kind.TOGGLE, "chefTitle"),
    SANS_FONT(ChefLevel.HEAD_CHEF, "Modern type", "Swap the serif headings for clean sans.", Kind.TOGGLE, "sans"),
    ICON_MIDNIGHT(ChefLevel.HEAD_CHEF, "Midnight icon", "A navy app icon.", Kind.ICON, "midnight"),
    THEME_PLUM(ChefLevel.EXECUTIVE_CHEF, "Plum", "Rich purple with brass.", Kind.THEME, "plum"),
    ICON_GOLD(ChefLevel.COMMUNITY_KITCHEN, "Gold icon", "For the ones who fed a crowd.", Kind.ICON, "gold"),
    THEME_GOLD(ChefLevel.COMMUNITY_KITCHEN, "Gilded", "Black and gold. You earned it.", Kind.THEME, "gold");

    enum class Kind { THEME, ICON, TOGGLE }
}

class GiveStore(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("simmer.give", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(load())
    val state: StateFlow<GiveState> = _state

    private fun today() = LocalDate.now().toString()

    private fun load(): GiveState {
        val today = today()
        val sameDay = prefs.getString("day", "") == today
        return GiveState(
            mealsFunded = prefs.getInt("meals", 0),
            embers = prefs.getInt("embers", 0),
            tipsCents = prefs.getInt("tipsCents", 0),
            adsWatchedTotal = prefs.getInt("adsTotal", 0),
            adsWatchedToday = if (sameDay) prefs.getInt("adsToday", 0) else 0,
            streakDays = prefs.getInt("streak", 0),
            lastGiveDay = prefs.getString("lastGiveDay", "") ?: "",
            recipesCooked = prefs.getInt("cooked", 0),
            cookedToday = if (sameDay) prefs.getInt("cookedToday", 0) else 0,
            savedToday = if (sameDay) prefs.getInt("savedToday", 0) else 0,
            sharedRecipesToday = if (sameDay) prefs.getInt("sharedToday", 0) else 0,
            sharedAppToday = if (sameDay) prefs.getBoolean("sharedApp", false) else false,
            bonusClaimed = prefs.getBoolean("bonus", false),
            theme = prefs.getString("theme", "herb") ?: "herb",
            icon = prefs.getString("icon", "herb") ?: "herb",
            compactGrid = prefs.getBoolean("compact", false),
            sansFont = prefs.getBoolean("sans", false),
            confetti = prefs.getBoolean("confetti", true),
            chefTitleOnShares = prefs.getBoolean("chefTitle", true),
        )
    }

    private fun save(s: GiveState) {
        prefs.edit()
            .putInt("meals", s.mealsFunded)
            .putInt("embers", s.embers)
            .putInt("tipsCents", s.tipsCents)
            .putInt("adsTotal", s.adsWatchedTotal)
            .putInt("adsToday", s.adsWatchedToday)
            .putString("day", today())
            .putInt("streak", s.streakDays)
            .putString("lastGiveDay", s.lastGiveDay)
            .putInt("cooked", s.recipesCooked)
            .putInt("cookedToday", s.cookedToday)
            .putInt("savedToday", s.savedToday)
            .putInt("sharedToday", s.sharedRecipesToday)
            .putBoolean("sharedApp", s.sharedAppToday)
            .putBoolean("bonus", s.bonusClaimed)
            .putString("theme", s.theme)
            .putString("icon", s.icon)
            .putBoolean("compact", s.compactGrid)
            .putBoolean("sans", s.sansFont)
            .putBoolean("confetti", s.confetti)
            .putBoolean("chefTitle", s.chefTitleOnShares)
            .apply()
        _state.value = s
    }

    /** A real gift: credits meals, embers, and advances the streak (with a bonus). */
    private fun gift(meals: Int, embers: Int, s: GiveState): GiveState {
        val today = today()
        val yesterday = LocalDate.now().minusDays(1).toString()
        val streak = when (s.lastGiveDay) {
            today -> s.streakDays
            yesterday -> s.streakDays + 1
            else -> 1
        }
        val streakBonus = if (s.lastGiveDay == yesterday) 5 else 0
        return s.copy(mealsFunded = s.mealsFunded + meals, embers = s.embers + embers + streakBonus, streakDays = streak, lastGiveDay = today)
    }

    /** Returns embers earned, or 0 if capped. */
    fun recordAdWatched(): Int {
        val s = _state.value
        if (s.adsLeftToday <= 0) return 0
        save(gift(app.simmer.Config.MEALS_PER_AD, 10, s).copy(adsWatchedTotal = s.adsWatchedTotal + 1, adsWatchedToday = s.adsWatchedToday + 1))
        return 10
    }

    fun recordTip(cents: Int): Int {
        val s = _state.value
        val meals = (cents / 100.0 * app.simmer.Config.MEALS_PER_DOLLAR).toInt().coerceAtLeast(1)
        val embers = (cents / 100.0 * 10).toInt().coerceAtLeast(5)
        save(gift(meals, embers, s).copy(tipsCents = s.tipsCents + cents))
        return embers
    }

    fun recordCooked(): Int {
        val s = _state.value
        val e = if (s.cookedToday < 3) 5 else 0
        save(s.copy(recipesCooked = s.recipesCooked + 1, cookedToday = s.cookedToday + 1, embers = s.embers + e))
        return e
    }

    fun recordSaved(): Int {
        val s = _state.value
        val e = if (s.savedToday < 5) 2 else 0
        save(s.copy(savedToday = s.savedToday + 1, embers = s.embers + e))
        return e
    }

    fun recordSharedRecipe(): Int {
        val s = _state.value
        val e = if (s.sharedRecipesToday < 3) 3 else 0
        save(s.copy(sharedRecipesToday = s.sharedRecipesToday + 1, embers = s.embers + e))
        return e
    }

    fun recordSharedApp(): Int {
        val s = _state.value
        val e = if (!s.sharedAppToday) 10 else 0
        save(s.copy(sharedAppToday = true, embers = s.embers + e))
        return e
    }

    /** The one free meal "on the house" after a new cook's first save. */
    fun claimWelcomeBonus(): Boolean {
        val s = _state.value
        if (s.bonusClaimed) return false
        save(gift(1, 5, s).copy(bonusClaimed = true))
        return true
    }

    fun setTheme(key: String) = save(_state.value.copy(theme = key))
    fun setIcon(key: String) = save(_state.value.copy(icon = key))
    fun setToggle(key: String, on: Boolean) {
        val s = _state.value
        save(
            when (key) {
                "compact" -> s.copy(compactGrid = on)
                "sans" -> s.copy(sansFont = on)
                "confetti" -> s.copy(confetti = on)
                "chefTitle" -> s.copy(chefTitleOnShares = on)
                else -> s
            }
        )
    }
}
