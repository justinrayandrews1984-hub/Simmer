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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import app.simmer.data.GroceryItem
import app.simmer.data.Recipe

@Composable
fun GroceryScreen(
    items: List<GroceryItem>,
    recipes: List<Recipe>,
    onAdd: (String) -> Unit,
    onToggle: (GroceryItem) -> Unit,
    onRemove: (GroceryItem) -> Unit,
    onClearDone: () -> Unit,
    onClearAll: () -> Unit,
    onPlan: (Set<Long>) -> Unit,
) {
    var newItem by remember { mutableStateOf("") }
    var planning by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }

    if (planning) {
        PlanDialog(recipes, onDismiss = { planning = false }, onConfirm = { onPlan(it); planning = false })
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear the whole list?") },
            confirmButton = { TextButton(onClick = { confirmClear = false; onClearAll() }) { Text("Clear it") } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Keep it") } },
        )
    }

    val groups = remember(items) {
        items.groupBy { it.recipeTitle.ifBlank { "Other items" } }
            .toSortedMap(compareBy<String> { if (it == "Other items") 0 else 1 }.thenBy<String, String>(String.CASE_INSENSITIVE_ORDER) { it })
    }
    val done = items.count { it.done }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp)) {
        item {
            Text("Grocery list", style = MaterialTheme.typography.displaySmall)
            Text(
                when {
                    items.isEmpty() -> "Nothing on the list yet"
                    done == items.size -> "All ${items.size} picked up"
                    else -> "$done of ${items.size} picked up"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (items.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { done.toFloat() / items.size },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(999.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = newItem,
                    onValueChange = { newItem = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Add an item (milk, eggs…)") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                    ),
                )
                FilledIconButton(onClick = { onAdd(newItem); newItem = "" }, enabled = newItem.isNotBlank()) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = { planning = true }, label = { Text("Plan from recipes") })
                AssistChip(onClick = onClearDone, enabled = done > 0, label = { Text("Clear checked") })
                AssistChip(onClick = { confirmClear = true }, enabled = items.isNotEmpty(), label = { Text("Clear all") })
            }
        }

        if (items.isEmpty()) {
            item {
                EmptyState(
                    title = "List is empty",
                    body = "Add items above, open a recipe and tap “Add ingredients”, or plan several recipes at once.",
                )
            }
        } else {
            groups.forEach { (group, groupItems) ->
                item(key = "h-$group") {
                    Row(
                        Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(group, style = MaterialTheme.typography.titleMedium)
                        Pill(
                            "${groupItems.count { !it.done }} left",
                            container = MaterialTheme.colorScheme.surfaceVariant,
                            content = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                items(groupItems, key = { it.id }) { item ->
                    val index = groupItems.indexOf(item)
                    val shape = when {
                        groupItems.size == 1 -> RoundedCornerShape(14.dp)
                        index == 0 -> RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)
                        index == groupItems.lastIndex -> RoundedCornerShape(bottomStart = 14.dp, bottomEnd = 14.dp)
                        else -> RoundedCornerShape(0.dp)
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(shape)
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable { onToggle(item) }
                            .padding(start = 6.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = item.done,
                            onCheckedChange = { onToggle(item) },
                            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
                        )
                        Text(
                            item.text,
                            modifier = Modifier.weight(1f),
                            textDecoration = if (item.done) TextDecoration.LineThrough else null,
                            color = if (item.done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        )
                        IconButton(onClick = { onRemove(item) }) {
                            Icon(Icons.Default.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanDialog(recipes: List<Recipe>, onDismiss: () -> Unit, onConfirm: (Set<Long>) -> Unit) {
    var picked by remember { mutableStateOf(setOf<Long>()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("What are you cooking?") },
        text = {
            if (recipes.isEmpty()) {
                Text("No recipes saved yet.")
            } else {
                LazyColumn {
                    items(recipes.sortedBy { it.title.lowercase() }, key = { it.id }) { r ->
                        val on = r.id in picked
                        Row(
                            Modifier.fillMaxWidth().clickable { picked = if (on) picked - r.id else picked + r.id },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(checked = on, onCheckedChange = { picked = if (on) picked - r.id else picked + r.id })
                            Box(Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))) {
                                RecipeImage(r.imageUrl, Modifier.fillMaxSize())
                            }
                            Spacer(Modifier.size(10.dp))
                            Column {
                                Text(r.title, style = MaterialTheme.typography.bodyLarge)
                                if (r.category.isNotBlank()) {
                                    Text(r.category, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(picked) }, enabled = picked.isNotEmpty()) { Text("Add to list") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
