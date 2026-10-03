package app.simmer.ui

import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.simmer.data.Recipe
import app.simmer.util.Kitchen
import kotlinx.coroutines.delay

/**
 * One step at a time, big text, screen stays on, timers you tap straight from the step text.
 */
@Composable
fun CookModeScreen(
    recipe: Recipe,
    scaledIngredients: List<String>,
    onExit: () -> Unit,
    onFinished: () -> Unit,
    confetti: Boolean = true,
) {
    val context = LocalContext.current
    val steps = recipe.steps.ifEmpty { listOf("No steps written for this recipe yet.") }
    var index by remember { mutableIntStateOf(-1) } // -1 = ingredient overview
    var timerTotal by remember { mutableIntStateOf(0) }
    var timerLeft by remember { mutableIntStateOf(0) }
    var timerRunning by remember { mutableStateOf(false) }
    var timerDone by remember { mutableStateOf(false) }

    // Keep the screen awake while cooking.
    DisposableEffect(Unit) {
        val window = (context as? android.app.Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    LaunchedEffect(timerRunning, timerLeft) {
        if (timerRunning && timerLeft > 0) {
            delay(1000)
            timerLeft -= 1
            if (timerLeft == 0) { timerRunning = false; timerDone = true }
        }
    }
    LaunchedEffect(timerDone) {
        if (timerDone) {
            runCatching {
                val v = context.getSystemService(android.os.Vibrator::class.java)
                v?.vibrate(android.os.VibrationEffect.createWaveform(longArrayOf(0, 300, 200, 300, 200, 600), -1))
            }
        }
    }

    BackHandler { onExit() }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background, contentColor = MaterialTheme.colorScheme.onBackground) {
    Box(Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        // Top bar
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onExit) { Icon(Icons.Default.Close, contentDescription = "Exit cook mode") }
            Column(Modifier.weight(1f)) {
                Text(recipe.title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                Text(
                    if (index < 0) "Get everything ready" else "Step ${index + 1} of ${steps.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        LinearProgressIndicator(
            progress = { if (index < 0) 0f else (index + 1).toFloat() / steps.size },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(4.dp).clip(RoundedCornerShape(999.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )

        // Timer banner
        if (timerTotal > 0) {
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (timerDone) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Text(
                    if (timerDone) "Time's up!" else Kitchen.formatClock(timerLeft),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.weight(1f),
                )
                if (!timerDone) {
                    TextButton(onClick = { timerRunning = !timerRunning }) { Text(if (timerRunning) "Pause" else "Resume") }
                }
                TextButton(onClick = { timerTotal = 0; timerLeft = 0; timerRunning = false; timerDone = false }) { Text("Clear") }
            }
        }

        // Body
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 20.dp),
        ) {
            if (index < 0) {
                Text("Ingredients", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(10.dp))
                scaledIngredients.forEach { Text("•  $it", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(vertical = 4.dp)) }
            } else {
                val step = steps[index]
                Text(step, fontSize = 26.sp, lineHeight = 36.sp, fontWeight = FontWeight.Medium)
                val timers = remember(step) { Kitchen.timersIn(step) }
                if (timers.isNotEmpty()) {
                    Spacer(Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        timers.forEach { t ->
                            OutlinedButton(
                                onClick = { timerTotal = t.seconds; timerLeft = t.seconds; timerRunning = true; timerDone = false },
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Icon(Icons.Default.Timer, contentDescription = null, Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Start ${t.label}")
                            }
                        }
                    }
                }
            }
        }

        // Nav
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedButton(
                onClick = { if (index >= 0) index-- },
                enabled = index >= 0,
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(Icons.Outlined.ChevronLeft, contentDescription = null)
                Text("Back")
            }
            if (index < steps.lastIndex) {
                Button(
                    onClick = { index++ },
                    modifier = Modifier.weight(2f).height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(if (index < 0) "Start cooking" else "Next step", style = MaterialTheme.typography.titleMedium)
                    Icon(Icons.Outlined.ChevronRight, contentDescription = null)
                }
            } else {
                Button(
                    onClick = onFinished,
                    modifier = Modifier.weight(2f).height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                ) {
                    Text("Done. Dig in!", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
    if (confetti && index == steps.lastIndex && steps.size > 1) {
        Confetti(Modifier.fillMaxSize(), colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.primaryContainer))
    }
    }
    }
}

/** Shown after finishing a recipe: celebrate and offer to fund a meal. */
@Composable
fun AfterCookDialog(
    cookedCount: Int,
    onWatchAd: () -> Unit,
    onTip: () -> Unit,
    onDismiss: () -> Unit,
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("You cooked tonight. 🎉") },
        text = {
            Column {
                Text("That's ${cookedCount} home-cooked meal${if (cookedCount == 1) "" else "s"} with Simmer. Want to fund one for someone else while you're at it?")
                Spacer(Modifier.height(12.dp))
                Button(onClick = onWatchAd, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) { Text("Watch an ad, fund a meal") }
                OutlinedButton(onClick = onTip, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) { Text("Tip \$2  ·  6 meals") }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Not now") } },
    )
}
