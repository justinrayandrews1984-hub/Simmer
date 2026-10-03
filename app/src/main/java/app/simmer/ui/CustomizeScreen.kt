package app.simmer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.simmer.data.ChefLevel
import app.simmer.data.GiveState
import app.simmer.data.Unlock

/** Every unlockable, grouped by level, with what you have and what's next. */
@Composable
fun CustomizeScreen(
    state: GiveState,
    onBack: () -> Unit,
    onTheme: (String) -> Unit,
    onIcon: (String) -> Unit,
    onToggle: (String, Boolean) -> Unit,
) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 40.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, Modifier.offset(x = (-12).dp)) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                Text("Your kitchen style", style = MaterialTheme.typography.headlineMedium)
            }
            Text(
                "Nothing here costs money and nothing useful is ever locked. Earn embers ✦ by cooking, saving, sharing and giving, and the kitchen gets fancier.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(14.dp))
            EmberGuide()
            Spacer(Modifier.height(8.dp))
        }

        ChefLevel.entries.forEach { lvl ->
            val unlocks = Unlock.entries.filter { it.level == lvl }
            if (unlocks.isEmpty()) return@forEach
            val reached = state.embers >= lvl.minEmbers
            item {
                Row(Modifier.padding(top = 20.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(28.dp).clip(CircleShape).background(if (reached) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (reached) Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                        else Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(lvl.title, style = MaterialTheme.typography.titleLarge)
                        Text(
                            if (reached) "Unlocked" else "${lvl.minEmbers - state.embers} more embers",
                            style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            unlocks.forEach { u ->
                item {
                    when (u.kind) {
                        Unlock.Kind.THEME -> ThemeRow(u, reached, selected = state.theme == u.key) { onTheme(u.key) }
                        Unlock.Kind.ICON -> IconRow(u.title, u.blurb, u.key, reached, selected = state.icon == u.key) { onIcon(u.key) }
                        Unlock.Kind.TOGGLE -> ToggleRow(u, reached, on = when (u.key) {
                            "compact" -> state.compactGrid; "sans" -> state.sansFont; "confetti" -> state.confetti; "chefTitle" -> state.chefTitleOnShares; else -> false
                        }) { onToggle(u.key, it) }
                    }
                }
            }
            // The default icon lives at level 1 beside the Herb theme.
            if (lvl == ChefLevel.HOME_COOK) item { IconRow("Herb icon", "The classic green app icon.", "herb", true, selected = state.icon == "herb") { onIcon("herb") } }
        }
    }
}

@Composable
private fun EmberGuide() {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).padding(14.dp)) {
        Text("How to earn embers", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        listOf(
            "Watch an ad (funds a meal too)" to "10 ✦",
            "Tip \$1 (funds 3 meals too)" to "10 ✦",
            "Share the app, once a day" to "10 ✦",
            "Cook a recipe in cook mode" to "5 ✦",
            "Give two days in a row" to "+5 ✦",
            "Share a recipe" to "3 ✦",
            "Save a recipe" to "2 ✦",
        ).forEach { (what, pts) ->
            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                Text(what, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                Text(pts, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
        Text("Daily caps keep it fair: 5 ads, 3 cooks, 3 recipe shares, 5 saves.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
private fun ThemeRow(u: Unlock, unlocked: Boolean, selected: Boolean, onPick: () -> Unit) {
    val p = paletteFor(u.key)
    OptionRow(
        title = u.title, blurb = u.blurb, unlocked = unlocked, selected = selected, onPick = onPick,
        swatch = {
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(listOf(p.primary, p.secondary)))
                    .border(2.dp, if (p.alwaysDark) p.darkBg else p.lightBg, RoundedCornerShape(12.dp)),
            )
        },
    )
}

@Composable
private fun IconRow(title: String, blurb: String, key: String, unlocked: Boolean, selected: Boolean, onPick: () -> Unit) {
    val bg = when (key) { "paprika" -> Color(0xFFB7431F); "midnight" -> Color(0xFF1B2440); "gold" -> Color(0xFFC9A227); else -> Color(0xFF1F6B47) }
    OptionRow(
        title = title, blurb = blurb, unlocked = unlocked, selected = selected, onPick = onPick,
        swatch = {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(bg), contentAlignment = Alignment.Center) {
                Text("🍲", style = MaterialTheme.typography.titleLarge)
            }
        },
    )
}

@Composable
private fun ToggleRow(u: Unlock, unlocked: Boolean, on: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surface).padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(u.title, style = MaterialTheme.typography.titleMedium, color = if (unlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
            Text(u.blurb, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (unlocked) Switch(checked = on, onCheckedChange = onChange)
        else Icon(Icons.Default.Lock, contentDescription = "Locked", tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun OptionRow(title: String, blurb: String, unlocked: Boolean, selected: Boolean, onPick: () -> Unit, swatch: @Composable () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .then(if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp)) else Modifier)
            .clickable(enabled = unlocked, onClick = onPick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        swatch()
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = if (unlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
            Text(blurb, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        when {
            !unlocked -> Icon(Icons.Default.Lock, contentDescription = "Locked", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            selected -> Icon(Icons.Default.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
        }
    }
}
