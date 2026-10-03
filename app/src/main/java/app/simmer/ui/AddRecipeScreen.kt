package app.simmer.ui

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.simmer.data.Recipe

/**
 * Two stages: paste a link and import, then review the fields and save.
 * When [existing] is given, we skip straight to the form to edit it.
 */
@Composable
fun AddRecipeScreen(
    existing: Recipe?,
    sharedUrl: String,
    knownCategories: List<String>,
    onImport: (String, (Result<Recipe>) -> Unit) -> Unit,
    onSave: (Recipe) -> Unit,
    onCancel: () -> Unit,
) {
    var draft by remember { mutableStateOf(existing) }
    var url by rememberSaveable { mutableStateOf(sharedUrl) }
    var importing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf<String?>(null) }

    fun runImport() {
        if (url.isBlank() || importing) return
        importing = true; error = null; info = null
        onImport(url) { result ->
            importing = false
            result.onSuccess { r ->
                draft = r
                info = "Found ${r.ingredients.size} ingredients and ${r.steps.size} steps" +
                    (if (r.imageUrl.isNotBlank()) ", plus a photo." else ".") + " Check them over, then save."
            }.onFailure { e ->
                error = e.message ?: "Couldn't import that page."
            }
        }
    }

    // A link shared from the browser starts importing on its own.
    LaunchedEffect(sharedUrl) {
        if (sharedUrl.isNotBlank() && existing == null && draft == null) runImport()
    }

    val d = draft
    if (d == null) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("Add a recipe", style = MaterialTheme.typography.displaySmall)
            Text(
                "Paste a link to any recipe page. Simmer saves the recipe as text and keeps the photo, so it stays even if the site changes.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Recipe link") },
                    placeholder = { Text("https://…") },
                    leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                )
                Button(
                    onClick = { runImport() },
                    enabled = !importing && url.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    if (importing) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        Spacer(Modifier.width(10.dp))
                        Text("Fetching recipe…")
                    } else Text("Import recipe")
                }
                val err = error
                if (err != null) {
                    Text(
                        err,
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.errorContainer)
                            .padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            OutlinedButton(
                onClick = { draft = Recipe(source = url.trim()) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
            ) { Text("Or type one in by hand") }
            Text(
                "Tip: in Chrome, tap Share on a recipe page and pick Simmer. It imports on its own.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
        }
    } else {
        RecipeForm(
            initial = d,
            info = info,
            isEdit = existing != null,
            knownCategories = knownCategories,
            onSave = onSave,
            onCancel = { if (existing != null) onCancel() else { draft = null; info = null } },
        )
    }
}

@Composable
private fun RecipeForm(
    initial: Recipe,
    info: String?,
    isEdit: Boolean,
    knownCategories: List<String>,
    onSave: (Recipe) -> Unit,
    onCancel: () -> Unit,
) {
    var title by remember(initial) { mutableStateOf(initial.title) }
    var category by remember(initial) { mutableStateOf(initial.category) }
    var servings by remember(initial) { mutableStateOf(initial.servings) }
    var time by remember(initial) { mutableStateOf(initial.time) }
    var source by remember(initial) { mutableStateOf(initial.source) }
    var imageUrl by remember(initial) { mutableStateOf(initial.imageUrl) }
    var ingredients by remember(initial) { mutableStateOf(initial.ingredients.joinToString("\n")) }
    var steps by remember(initial) { mutableStateOf(initial.steps.joinToString("\n")) }
    var notes by remember(initial) { mutableStateOf(initial.notes) }

    val suggestions = remember(knownCategories) {
        (listOf("Dinner", "Lunch", "Breakfast", "Baking", "Soup", "Dessert", "Snacks", "Drinks") + knownCategories).distinct()
    }
    val fieldShape = RoundedCornerShape(14.dp)

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(if (isEdit) "Edit recipe" else "Check and save", style = MaterialTheme.typography.displaySmall)
        if (info != null) {
            Text(
                info,
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(12.dp),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Box(Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(20.dp))) {
            RecipeImage(imageUrl, Modifier.fillMaxSize())
        }
        OutlinedTextField(value = imageUrl, onValueChange = { imageUrl = it }, label = { Text("Photo link (optional)") }, singleLine = true, shape = fieldShape, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, singleLine = true, shape = fieldShape, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") }, singleLine = true, shape = fieldShape, modifier = Modifier.fillMaxWidth())
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(suggestions) { c ->
                SuggestionChip(onClick = { category = c }, label = { Text(c) }, shape = RoundedCornerShape(999.dp))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(value = servings, onValueChange = { servings = it }, label = { Text("Servings") }, singleLine = true, shape = fieldShape, modifier = Modifier.weight(1f))
            OutlinedTextField(value = time, onValueChange = { time = it }, label = { Text("Time") }, singleLine = true, shape = fieldShape, modifier = Modifier.weight(1f))
        }
        OutlinedTextField(value = source, onValueChange = { source = it }, label = { Text("Source link") }, singleLine = true, shape = fieldShape, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(
            value = ingredients, onValueChange = { ingredients = it },
            label = { Text("Ingredients (one per line)") }, minLines = 5, shape = fieldShape, modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = steps, onValueChange = { steps = it },
            label = { Text("Steps (one per line)") }, minLines = 6, shape = fieldShape, modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = notes, onValueChange = { notes = it },
            label = { Text("Notes") }, minLines = 2, shape = fieldShape, modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = {
                fun lines(s: String) = s.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
                onSave(
                    initial.copy(
                        title = title.trim().ifBlank { "Untitled recipe" },
                        category = category.trim(),
                        servings = servings.trim(),
                        time = time.trim(),
                        source = source.trim(),
                        imageUrl = imageUrl.trim(),
                        ingredients = lines(ingredients),
                        steps = lines(steps),
                        notes = notes.trim(),
                    )
                )
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
        ) { Text("Save recipe") }
        OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) { Text("Cancel") }
        Spacer(Modifier.height(24.dp))
    }
}
