package app.simmer.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate

/** Everything about giving, streaks and levels, kept in one small on-device store. */
data class GiveState(
    val mealsFunded: Int = 0,
    val tipsCents: Int = 0,
    val adsWatchedTotal: Int = 0,
    val adsWatchedToday: Int = 0,
    val streakDays: Int = 0,
    val lastGiveDay: String = "",
    val recipesCooked: Int = 0,
    val bonusClaimed: Boolean = false,
) {
    val level: ChefLevel get() = ChefLevel.forMeals(mealsFunded)
    val nextLevel: ChefLevel? get() = ChefLevel.entries.firstOrNull { it.minMeals > mealsFunded }
    val adsLeftToday: Int get() = (app.simmer.Config.MAX_ADS_PER_DAY - adsWatchedToday).coerceAtLeast(0)
}

enum class ChefLevel(val title: String, val minMeals: Int, val dish: String) {
    HOME_COOK("Home Cook", 0, "A bowl of soup"),
    LINE_COOK("Line Cook", 10, "A loaf of bread"),
    SOUS_CHEF("Sous Chef", 50, "A family dinner"),
    HEAD_CHEF("Head Chef", 150, "A feast"),
    COMMUNITY_KITCHEN("Community Kitchen", 500, "A street party");

    companion object {
        fun forMeals(m: Int): ChefLevel = entries.last { m >= it.minMeals }
    }
}

class GiveStore(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("simmer.give", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(load())
    val state: StateFlow<GiveState> = _state

    private fun today() = LocalDate.now().toString()

    private fun load(): GiveState {
        val today = today()
        val adsDay = prefs.getString("adsDay", "") ?: ""
        return GiveState(
            mealsFunded = prefs.getInt("meals", 0),
            tipsCents = prefs.getInt("tipsCents", 0),
            adsWatchedTotal = prefs.getInt("adsTotal", 0),
            adsWatchedToday = if (adsDay == today) prefs.getInt("adsToday", 0) else 0,
            streakDays = prefs.getInt("streak", 0),
            lastGiveDay = prefs.getString("lastGiveDay", "") ?: "",
            recipesCooked = prefs.getInt("cooked", 0),
            bonusClaimed = prefs.getBoolean("bonus", false),
        )
    }

    private fun save(s: GiveState) {
        prefs.edit()
            .putInt("meals", s.mealsFunded)
            .putInt("tipsCents", s.tipsCents)
            .putInt("adsTotal", s.adsWatchedTotal)
            .putInt("adsToday", s.adsWatchedToday)
            .putString("adsDay", today())
            .putInt("streak", s.streakDays)
            .putString("lastGiveDay", s.lastGiveDay)
            .putInt("cooked", s.recipesCooked)
            .putBoolean("bonus", s.bonusClaimed)
            .apply()
        _state.value = s
    }

    /** Applies a gift of [meals] and advances the daily streak. */
    private fun credit(meals: Int, s: GiveState): GiveState {
        val today = today()
        val yesterday = LocalDate.now().minusDays(1).toString()
        val streak = when (s.lastGiveDay) {
            today -> s.streakDays
            yesterday -> s.streakDays + 1
            else -> 1
        }
        return s.copy(mealsFunded = s.mealsFunded + meals, streakDays = streak, lastGiveDay = today)
    }

    fun recordAdWatched() {
        val s = _state.value
        if (s.adsLeftToday <= 0) return
        save(credit(app.simmer.Config.MEALS_PER_AD, s).copy(adsWatchedTotal = s.adsWatchedTotal + 1, adsWatchedToday = s.adsWatchedToday + 1))
    }

    fun recordTip(cents: Int) {
        val s = _state.value
        val meals = (cents / 100.0 * app.simmer.Config.MEALS_PER_DOLLAR).toInt().coerceAtLeast(1)
        save(credit(meals, s).copy(tipsCents = s.tipsCents + cents))
    }

    fun recordCooked() {
        val s = _state.value
        save(s.copy(recipesCooked = s.recipesCooked + 1))
    }

    /** The one free meal "on the house" after a new cook's first import. */
    fun claimWelcomeBonus(): Boolean {
        val s = _state.value
        if (s.bonusClaimed) return false
        save(credit(1, s).copy(bonusClaimed = true))
        return true
    }
}
