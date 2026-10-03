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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.simmer.data.Recipe
import app.simmer.importer.RecipeImporter
import app.simmer.importer.WebResult
import app.simmer.importer.WebSearch
import kotlinx.coroutines.launch

private val quickChips = listOf("Chicken", "Pasta", "Vegetarian", "Dessert", "Seafood", "Beef", "Breakfast", "Soup", "Budget", "30-minute")
private val mealDbCategories = setOf("Chicken", "Pasta", "Vegetarian", "Dessert", "Seafood", "Beef", "Breakfast")

/** Search the web for a recipe and save it without leaving the app. */
@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun DiscoverScreen(
    pantrySuggestion: String,
    onSave: (Recipe) -> Unit,
    onToast: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var query by rememberSaveable { mutableStateOf("") }
    var results by remember { mutableStateOf<List<WebResult>>(emptyList()) }
    var inspiration by remember { mutableStateOf<List<WebResult>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var heading by remember { mutableStateOf("") }
    var preview by remember { mutableStateOf<Recipe?>(null) }
    var previewing by remember { mutableStateOf<String?>(null) }
    var previewError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (inspiration.isEmpty()) inspiration = runCatching { WebSearch.inspiration() }.getOrDefault(emptyList())
    }

    fun run(q: String) {
        val text = q.trim()
        if (text.isEmpty()) return
        query = text
        loading = true
        scope.launch {
            results = if (text in mealDbCategories) {
                WebSearch.byCategory(text) + WebSearch.search(text).filter { it.site != "themealdb.com" }
            } else WebSearch.search(text)
            heading = if (results.isEmpty()) "Nothing found for “$text”. Try different words." else "Results for “$text”"
            loading = false
        }
    }

    fun open(r: WebResult) {
        previewError = null
        if (r.ready != null) { preview = r.ready; return }
        previewing = r.url
        scope.launch {
            val recipe = if (r.site == "themealdb.com") WebSearch.lookupMeal(r.url) else runCatching { RecipeImporter.fromUrl(r.url) }.getOrNull()
            previewing = null
            if (recipe == null) previewError = "Couldn't read a recipe from ${r.site}. Try another result."
            else preview = recipe.copy(imageUrl = recipe.imageUrl.ifBlank { r.imageUrl })
        }
    }

    val p = preview
    if (p != null) {
        val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = { preview = null }, sheetState = sheet) {
            LazyColumn(Modifier.fillMaxWidth(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 40.dp)) {
                item {
                    Box(Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(18.dp))) { RecipeImage(p.imageUrl, Modifier.fillMaxSize()) }
                    Spacer(Modifier.height(12.dp))
                    Text(p.title, style = MaterialTheme.typography.headlineSmall)
                    val meta = listOf(p.category, p.time, p.servings.takeIf { it.isNotBlank() }?.let { "Serves $it" }.orEmpty()).filter { it.isNotBlank() }
                    if (meta.isNotEmpty()) Text(meta.joinToString("  ·  "), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { onSave(p); preview = null }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                        Text("Save to my recipes")
                    }
                    SectionHeading("Ingredients (${p.ingredients.size})")
                }
                items(p.ingredients) { Text("•  $it", Modifier.padding(vertical = 3.dp)) }
                item { SectionHeading("Method (${p.steps.size} steps)") }
                items(p.steps.take(3)) { Text(it, Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis) }
                if (p.steps.size > 3) item { Text("…and ${p.steps.size - 3} more once saved.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Text("Discover", style = MaterialTheme.typography.displaySmall)
                Text("Search the whole web, save with one tap.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Try “lemon chicken” or “vegan chili”") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { run(query) }),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                    ),
                )
            }
            Spacer(Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 20.dp)) {
                items(quickChips) { c ->
                    FilterChip(
                        selected = query.equals(c, true),
                        onClick = { run(if (c == "Budget") "cheap budget dinner" else if (c == "30-minute") "30 minute dinner" else c) },
                        label = { Text(c) },
                        shape = RoundedCornerShape(999.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                        border = null,
                    )
                }
            }
            if (pantrySuggestion.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = { run(pantrySuggestion) }, modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                    Text("Find recipes with what's in my pantry")
                }
            }
            val err = previewError
            if (err != null) {
                Text(err, Modifier.padding(horizontal = 20.dp, vertical = 8.dp), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
        }

        if (loading) {
            item { Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
        } else if (results.isNotEmpty() || heading.isNotBlank()) {
            item { Text(heading, Modifier.padding(horizontal = 20.dp, vertical = 14.dp), style = MaterialTheme.typography.titleMedium) }
            items(results, key = { it.url }) { r -> ResultRow(r, busy = previewing == r.url, onClick = { open(r) }) }
        } else {
            item {
                Text("Tonight's inspiration", Modifier.padding(horizontal = 20.dp, vertical = 14.dp), style = MaterialTheme.typography.titleMedium)
            }
            if (inspiration.isEmpty()) {
                item { Box(Modifier.fillMaxWidth().padding(30.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
            }
            items(inspiration, key = { it.url }) { r -> ResultRow(r, busy = false, onClick = { open(r) }) }
        }
    }
}

@Composable
private fun ResultRow(r: WebResult, busy: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .padding(horizontal = 20.dp)
            .padding(bottom = 10.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(72.dp).clip(RoundedCornerShape(12.dp))) {
            if (r.imageUrl.isNotBlank()) RecipeImage(r.imageUrl, Modifier.fillMaxSize())
            else Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                Text(r.site.take(1).uppercase(), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(r.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(r.site, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            if (r.snippet.isNotBlank()) Text(r.snippet, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        if (busy) { Spacer(Modifier.width(8.dp)); CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) }
    }
}
