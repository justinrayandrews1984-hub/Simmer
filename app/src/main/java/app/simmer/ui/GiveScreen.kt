package app.simmer.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.simmer.Config
import app.simmer.ads.AdsManager
import app.simmer.data.ChefLevel
import app.simmer.data.GiveState

@Composable
fun GiveScreen(
    state: GiveState,
    onAdWatched: () -> Unit,
    onTip: (Int) -> Unit,
    onToast: (String) -> Unit,
) {
    val context = LocalContext.current
    val activity = context as? Activity

    fun openUrl(url: String) = runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }

    fun watchAd() {
        if (state.adsLeftToday <= 0) { onToast("You've hit today's limit. Come back tomorrow!"); return }
        if (activity == null) return
        val shown = AdsManager.show(activity, onRewarded = { onAdWatched(); onToast("Meal funded. Thank you!") })
        if (!shown) onToast("Loading an ad, try again in a few seconds")
    }

    fun tip(cents: Int) {
        // Until Play Billing is wired, the tip opens the charity's own donation page and we
        // credit the meals on trust. Swap for a real purchase flow when the app is on the store.
        openUrl(Config.DONATE_URL)
        onTip(cents)
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp)) {
        item {
            Text("Give", style = MaterialTheme.typography.displaySmall)
            Text("Cook at home, feed someone else.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(16.dp))
        }

        // Your table
        item {
            TableCard(state)
            Spacer(Modifier.height(14.dp))
        }

        // Watch to feed
        item {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surface).padding(16.dp),
            ) {
                Text("Watch to feed", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Watch a short ad and the ad money funds ${Config.MEALS_PER_AD} meal at ${Config.CHARITY_NAME}. Up to ${Config.MAX_ADS_PER_DAY} a day.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { watchAd() },
                    enabled = state.adsLeftToday > 0,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                ) {
                    Icon(Icons.Default.PlayCircle, contentDescription = null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (state.adsLeftToday > 0) "Watch an ad, fund a meal  (${state.adsLeftToday} left today)" else "Back tomorrow for more")
                }
            }
            Spacer(Modifier.height(14.dp))
        }

        // Tip jar
        item {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surface).padding(16.dp),
            ) {
                Text("Buy a meal", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Every dollar funds about ${Config.MEALS_PER_DOLLAR} meals. Opens ${Config.CHARITY_NAME}'s own donation page, so 100% goes to them.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(200, 500, 1000).forEach { cents ->
                        OutlinedButton(onClick = { tip(cents) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("$${cents / 100}", style = MaterialTheme.typography.titleMedium)
                                Text("${cents / 100 * Config.MEALS_PER_DOLLAR} meals", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
        }

        // Community pot
        item {
            val goal = Config.COMMUNITY_GOAL_MEALS
            val pot = Config.COMMUNITY_MEALS_SO_FAR + state.mealsFunded
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.primaryContainer).padding(16.dp),
            ) {
                Text("Community pot", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(
                    "Everyone's meals, together. This month for ${Config.CHARITY_NAME}.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(Modifier.height(12.dp))
                val progress by animateFloatAsState((pot.toFloat() / goal).coerceIn(0f, 1f), label = "pot")
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(999.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surface,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "${"%,d".format(pot)} / ${"%,d".format(goal)} meals",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Spacer(Modifier.height(14.dp))
        }

        // Impact + transparency
        item {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surface).padding(16.dp),
            ) {
                Text("Where the money goes", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Ad revenue and tips go to ${Config.CHARITY_NAME}, minus app-store and payment fees. " +
                        "A \"meal\" is counted at the charity's own rate of about ${Config.MEALS_PER_DOLLAR} meals per dollar, rounded down so the real total is never less than what's shown.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Text(Config.LAST_TRANSFER_NOTE, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                Row {
                    TextButton(onClick = { openUrl(Config.CHARITY_URL) }, contentPadding = PaddingValues(horizontal = 4.dp)) {
                        Icon(Icons.Outlined.OpenInNew, contentDescription = null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("About ${Config.CHARITY_NAME}")
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
        }

        // Share your impact
        item {
            OutlinedButton(
                onClick = {
                    val text = "I've funded ${state.mealsFunded} meal${if (state.mealsFunded == 1) "" else "s"} for ${Config.CHARITY_NAME} just by cooking at home with Simmer. " +
                        "Free recipe app, every recipe helps feed someone: ${Config.APP_URL}"
                    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
                    runCatching { context.startActivity(Intent.createChooser(send, "Share your impact")) }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(Icons.Default.Share, contentDescription = null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Share your impact")
            }
        }
    }
}

/** The illustrated "table" that fills as meals are funded, plus level and streak. */
@Composable
fun TableCard(state: GiveState) {
    val level = state.level
    val next = state.nextLevel
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.primary)
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Text("Meals you've funded", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.8f))
                Text("${state.mealsFunded}", style = MaterialTheme.typography.displaySmall, color = Color.White, fontWeight = FontWeight.Black)
            }
            Column(horizontalAlignment = Alignment.End) {
                Pill(level.title, container = Color.White.copy(alpha = 0.18f), content = Color.White)
                Spacer(Modifier.height(6.dp))
                Text(
                    if (state.streakDays > 1) "🔥 ${state.streakDays}-day streak" else "Give today to start a streak",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.9f),
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        TableIllustration(state.mealsFunded)
        Spacer(Modifier.height(12.dp))
        if (next != null) {
            val span = next.minMeals - level.minMeals
            val into = state.mealsFunded - level.minMeals
            LinearProgressIndicator(
                progress = { (into.toFloat() / span).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(999.dp)),
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.25f),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "${next.minMeals - state.mealsFunded} more to ${next.title}: ${next.dish.lowercase()} joins your table",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.9f),
            )
        } else {
            Text("You've set the whole table. Thank you.", style = MaterialTheme.typography.bodySmall, color = Color.White)
        }
    }
}

/** A row of dishes; each level unlocks one. Locked dishes are faint. */
@Composable
private fun TableIllustration(meals: Int) {
    val dishes = listOf("🍲" to ChefLevel.HOME_COOK, "🍞" to ChefLevel.LINE_COOK, "🍛" to ChefLevel.SOUS_CHEF, "🍗" to ChefLevel.HEAD_CHEF, "🎉" to ChefLevel.COMMUNITY_KITCHEN)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.14f))
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        dishes.forEach { (emoji, lvl) ->
            val unlocked = meals >= lvl.minMeals
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(emoji, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(2.dp).alpha(if (unlocked) 1f else 0.3f))
                Text(
                    if (unlocked) "✓" else "${lvl.minMeals}",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = if (unlocked) 1f else 0.5f),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
