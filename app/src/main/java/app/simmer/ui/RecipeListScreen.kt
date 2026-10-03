package app.simmer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.simmer.data.Recipe

enum class SortMode(val label: String) { NEWEST("Newest first"), OLDEST("Oldest first"), AZ("A to Z"), CATEGORY("By category") }

@Composable
fun RecipeListScreen(
    recipes: List<Recipe>,
    onOpen: (Recipe) -> Unit,
    onToggleFavorite: (Recipe) -> Unit,
    onAdd: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var sort by rememberSaveable { mutableStateOf(SortMode.NEWEST) }
    var category by rememberSaveable { mutableStateOf("") } // "" = all, "★" = favourites
    var sortMenu by remember { mutableStateOf(false) }

    val categories = remember(recipes) { recipes.map { it.category }.filter { it.isNotBlank() }.distinct().sorted() }

    val shown = remember(recipes, query, sort, category) {
        val q = query.trim().lowercase()
        recipes.asSequence()
            .filter {
                when (category) {
                    "" -> true
                    "★" -> it.favorite
                    else -> it.category == category
                }
            }
            .filter { r ->
                q.isEmpty() || listOf(r.title, r.category, r.notes, r.ingredients.joinToString(" "))
                    .joinToString(" ").lowercase().contains(q)
            }
            .sortedWith(
                when (sort) {
                    SortMode.NEWEST -> compareByDescending<Recipe> { it.createdAt }
                    SortMode.OLDEST -> compareBy<Recipe> { it.createdAt }
                    SortMode.AZ -> compareBy<Recipe, String>(String.CASE_INSENSITIVE_ORDER) { it.title }
                    SortMode.CATEGORY -> compareBy<Recipe, String>(String.CASE_INSENSITIVE_ORDER) { it.category.ifBlank { "zzz" } }
                        .thenBy<Recipe, String>(String.CASE_INSENSITIVE_ORDER) { it.title }
                }
            )
            .toList()
    }

    Column(Modifier.fillMaxSize()) {
        // Header
        Column(Modifier.padding(horizontal = 20.dp)) {
            Text("Simmer", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onBackground)
            Text(
                when {
                    recipes.isEmpty() -> "Your recipes, in your pocket."
                    recipes.size == 1 -> "1 recipe saved"
                    else -> "${recipes.size} recipes saved"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Search recipes or ingredients") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                    ),
                )
                Box {
                    IconButton(onClick = { sortMenu = true }) {
                        Icon(Icons.Default.Sort, contentDescription = "Sort")
                    }
                    DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                        SortMode.entries.forEach { mode ->
                            DropdownMenuItem(
                                text = { Text(if (mode == sort) "✓  ${mode.label}" else "    ${mode.label}") },
                                onClick = { sort = mode; sortMenu = false },
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 2.dp),
        ) {
            item { CategoryChip("All", category == "") { category = "" } }
            item { CategoryChip("Favourites", category == "★") { category = "★" } }
            items(categories) { c -> CategoryChip(c, category == c) { category = c } }
        }
        Spacer(Modifier.height(6.dp))

        when {
            recipes.isEmpty() -> EmptyState(
                title = "No recipes yet",
                body = "Paste a link from any recipe site and Simmer saves the recipe as text and a photo, so it's yours even if the site goes away.",
                action = "Add your first recipe",
                onAction = onAdd,
            )
            shown.isEmpty() -> EmptyState(title = "Nothing matches", body = "Try a different search or category.")
            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(shown, key = { it.id }) { r ->
                    RecipeTile(r, onClick = { onOpen(r) }, onToggleFavorite = { onToggleFavorite(r) })
                }
            }
        }
    }
}

@Composable
private fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        shape = RoundedCornerShape(999.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        border = null,
    )
}

@Composable
private fun RecipeTile(r: Recipe, onClick: () -> Unit, onToggleFavorite: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(0.82f)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
    ) {
        RecipeImage(r.imageUrl, Modifier.fillMaxSize())
        PhotoScrim(Modifier.fillMaxSize())

        if (r.category.isNotBlank()) {
            Pill(
                r.category,
                container = Color.White.copy(alpha = 0.9f),
                content = Ink,
                modifier = Modifier.align(Alignment.TopStart).padding(10.dp),
            )
        }
        IconButton(
            onClick = onToggleFavorite,
            modifier = Modifier.align(Alignment.TopEnd).padding(2.dp),
        ) {
            Icon(
                if (r.favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = "Favourite",
                tint = if (r.favorite) MaterialTheme.colorScheme.secondary else Color.White,
            )
        }
        Column(Modifier.align(Alignment.BottomStart).padding(12.dp)) {
            Text(
                r.title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val meta = buildList {
                if (r.time.isNotBlank()) add(r.time)
                if (r.servings.isNotBlank()) add("Serves ${r.servings}")
            }
            if (meta.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text(meta.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.85f))
            }
        }
    }
}
