package app.simmer.ui

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.simmer.data.GiveState
import app.simmer.data.Recipe
import java.time.LocalTime

/** The front page: a greeting, tonight's pick, quick actions, and rows of recipes. */
@Composable
fun HomeScreen(
    recipes: List<Recipe>,
    give: GiveState,
    groceryLeft: Int,
    onOpen: (Recipe) -> Unit,
    onLibrary: () -> Unit,
    onAdd: () -> Unit,
    onDiscover: () -> Unit,
    onPantry: () -> Unit,
    onGrocery: () -> Unit,
    onGive: () -> Unit,
) {
    val hour = LocalTime.now().hour
    val greeting = when (hour) { in 5..10 -> "Good morning"; in 11..16 -> "Good afternoon"; else -> "Good evening" }
    val mealWord = when (hour) { in 5..10 -> "breakfast"; in 11..15 -> "lunch"; else -> "dinner" }

    // Tonight's pick: a favourite if there is one, otherwise the most recent. Rotates daily.
    val pick = remember(recipes) {
        val pool = recipes.filter { it.favorite }.ifEmpty { recipes }
        if (pool.isEmpty()) null else pool[(java.time.LocalDate.now().toEpochDay() % pool.size).toInt()]
    }
    val recent = remember(recipes) { recipes.sortedByDescending { it.createdAt }.take(10) }
    val favourites = remember(recipes) { recipes.filter { it.favorite }.take(10) }
    val categories = remember(recipes) { recipes.groupBy { it.category }.filterKeys { it.isNotBlank() }.toList().sortedByDescending { it.second.size }.take(4) }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
        item {
            Row(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(greeting, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("What's for $mealWord?", style = MaterialTheme.typography.displaySmall)
                }
                Box(
                    Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surface).clickable(onClick = onLibrary),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Default.Search, contentDescription = "Search my recipes") }
            }
            Spacer(Modifier.height(16.dp))
        }

        // Hero
        item {
            if (pick != null) {
                Box(
                    Modifier
                        .padding(horizontal = 20.dp)
                        .fillMaxWidth()
                        .height(240.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .clickable { onOpen(pick) },
                ) {
                    RecipeImage(pick.imageUrl, Modifier.fillMaxSize())
                    PhotoScrim(Modifier.fillMaxSize())
                    Pill(if (pick.favorite) "Tonight's pick  ♥" else "Tonight's pick", container = Color.White.copy(alpha = 0.9f), content = Ink, modifier = Modifier.align(Alignment.TopStart).padding(14.dp))
                    Column(Modifier.align(Alignment.BottomStart).padding(18.dp)) {
                        Text(pick.title, style = MaterialTheme.typography.headlineSmall, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        val meta = listOf(pick.time, pick.servings.takeIf { it.isNotBlank() }?.let { "Serves $it" }.orEmpty(), pick.category).filter { it.isNotBlank() }
                        if (meta.isNotEmpty()) Text(meta.joinToString("  ·  "), style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.85f))
                    }
                }
            } else {
                Box(
                    Modifier
                        .padding(horizontal = 20.dp)
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable(onClick = onDiscover)
                        .padding(22.dp),
                ) {
                    Column(Modifier.align(Alignment.BottomStart)) {
                        Text("Your recipe book is empty", style = MaterialTheme.typography.headlineSmall, color = Color.White)
                        Spacer(Modifier.height(6.dp))
                        Text("Search the web for something good, or paste a link. Every recipe you save helps feed someone.", color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // Quick actions
        item {
            Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickAction(Icons.Default.TravelExplore, "Discover", Modifier.weight(1f), onDiscover)
                QuickAction(Icons.Outlined.AddCircleOutline, "Paste link", Modifier.weight(1f), onAdd)
                QuickAction(Icons.Default.Kitchen, "Pantry", Modifier.weight(1f), onPantry)
                QuickAction(Icons.Default.ShoppingCart, if (groceryLeft > 0) "$groceryLeft to buy" else "Groceries", Modifier.weight(1f), onGrocery)
            }
            Spacer(Modifier.height(8.dp))
        }

        // Giving strip
        item {
            Row(
                Modifier
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable(onClick = onGive)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (give.mealsFunded == 0) "Fund your first meal" else "${give.mealsFunded} meal${if (give.mealsFunded == 1) "" else "s"} funded  ·  ${give.level.title}",
                        style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        if (give.streakDays > 1) "${give.streakDays}-day streak. Keep it going." else "Watch a short ad or round up a grocery run.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Text("${give.embers} ✦", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }
        }

        if (favourites.isNotEmpty()) {
            item { RowHeader("Favourites", onLibrary) }
            item { RecipeRow(favourites, onOpen) }
        }
        if (recent.isNotEmpty()) {
            item { RowHeader("Recently added", onLibrary) }
            item { RecipeRow(recent, onOpen) }
        }
        categories.forEach { (cat, list) ->
            item { RowHeader(cat, onLibrary) }
            item { RecipeRow(list.take(10), onOpen) }
        }
        if (recipes.isNotEmpty()) {
            item {
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onLibrary, modifier = Modifier.padding(horizontal = 12.dp)) { Text("See all ${recipes.size} recipes") }
            }
        }
    }
}

@Composable
private fun QuickAction(icon: ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun RowHeader(title: String, onMore: () -> Unit) {
    Row(Modifier.padding(start = 20.dp, end = 8.dp, top = 18.dp, bottom = 6.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        TextButton(onClick = onMore) { Text("All") }
    }
}

@Composable
private fun RecipeRow(list: List<Recipe>, onOpen: (Recipe) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(horizontal = 20.dp)) {
        items(list, key = { it.id }) { r ->
            Column(Modifier.width(150.dp).clickable { onOpen(r) }) {
                Box(Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(16.dp))) {
                    RecipeImage(r.imageUrl, Modifier.fillMaxSize())
                }
                Spacer(Modifier.height(6.dp))
                Text(r.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold)
                if (r.time.isNotBlank()) Text(r.time, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
