package app.simmer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.simmer.data.Recipe
import app.simmer.util.Kitchen

/** "What can I make tonight?" Type what's in the fridge; recipes you can cook float to the top. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PantryScreen(
    pantry: Set<String>,
    recipes: List<Recipe>,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
    onOpen: (Recipe) -> Unit,
    onAddMissing: (Recipe, List<String>) -> Unit,
    onToast: (String) -> Unit,
) {
    var input by remember { mutableStateOf("") }

    val ranked = remember(recipes, pantry) {
        if (pantry.isEmpty()) emptyList()
        else recipes.map { it to Kitchen.match(it.ingredients, pantry) }
            .filter { it.second.needed > 0 }
            .sortedWith(compareBy<Pair<Recipe, Kitchen.Match>> { it.second.missing.size }.thenByDescending { it.second.have })
    }
    val canMake = ranked.filter { it.second.canMake }
    val almost = ranked.filter { !it.second.canMake && it.second.missing.size <= 3 }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp)) {
        item {
            Text("What can I make?", style = MaterialTheme.typography.displaySmall)
            Text("Tell Simmer what's in the kitchen. Salt, pepper, oil, flour, sugar and butter are assumed.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("chicken, rice, broccoli…") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                    ),
                )
                FilledIconButton(onClick = { onAdd(input); input = "" }, enabled = input.isNotBlank()) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                }
            }
            Spacer(Modifier.height(10.dp))
            if (pantry.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    pantry.forEach { item ->
                        InputChip(
                            selected = false,
                            onClick = { onRemove(item) },
                            label = { Text(item) },
                            trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Remove", Modifier.size(16.dp)) },
                        )
                    }
                }
            }
        }

        if (pantry.isEmpty()) {
            item { EmptyState("Your kitchen is a mystery", "Add a few things you have on hand and Simmer will tell you what you can cook from your saved recipes.") }
        } else if (ranked.isEmpty()) {
            item { EmptyState("No matches yet", "Save a few more recipes, or add more ingredients.") }
        } else {
            if (canMake.isNotEmpty()) {
                item { SectionHeading("You can make now") }
                items(canMake, key = { "c" + it.first.id }) { (r, m) -> MatchRow(r, m, onOpen = { onOpen(r) }, onAddMissing = null) }
            }
            if (almost.isNotEmpty()) {
                item { SectionHeading("Almost there") }
                items(almost, key = { "a" + it.first.id }) { (r, m) ->
                    MatchRow(r, m, onOpen = { onOpen(r) }, onAddMissing = { onAddMissing(r, m.missing); onToast("Added ${m.missing.size} to groceries") })
                }
            }
        }
    }
}

@Composable
private fun MatchRow(r: Recipe, m: Kitchen.Match, onOpen: () -> Unit, onAddMissing: (() -> Unit)?) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onOpen)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(64.dp).clip(RoundedCornerShape(12.dp))) { RecipeImage(r.imageUrl, Modifier.fillMaxSize()) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(r.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (m.canMake) {
                Text("All ${m.needed} ingredients on hand", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            } else {
                Text("Missing: ${m.missing.joinToString(", ")}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (onAddMissing != null) {
                    TextButton(onClick = onAddMissing, contentPadding = PaddingValues(0.dp)) { Text("Add missing to groceries") }
                }
            }
        }
    }
}
