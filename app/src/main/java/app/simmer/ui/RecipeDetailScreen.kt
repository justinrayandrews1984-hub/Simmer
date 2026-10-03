package app.simmer.ui

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import app.simmer.Config
import app.simmer.data.Recipe
import app.simmer.util.Kitchen

@Composable
fun RecipeDetailScreen(
    recipe: Recipe,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onAddToGrocery: (List<String>) -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    onCook: (List<String>) -> Unit,
) {
    val context = LocalContext.current
    var checked by remember(recipe.id) { mutableStateOf(setOf<Int>()) }
    var confirmDelete by remember { mutableStateOf(false) }

    val baseServings = remember(recipe.servings) { Kitchen.servingsNumber(recipe.servings) }
    var servings by remember(recipe.id) { mutableIntStateOf(baseServings ?: 0) }
    val factor = if (baseServings != null && servings > 0) servings.toDouble() / baseServings else 1.0
    val shownIngredients = remember(recipe.ingredients, factor) { recipe.ingredients.map { Kitchen.scaleIngredient(it, factor) } }

    fun shareRecipe() {
        val text = buildString {
            appendLine(recipe.title)
            if (recipe.servings.isNotBlank()) appendLine("Serves ${recipe.servings}")
            appendLine()
            appendLine("INGREDIENTS")
            shownIngredients.forEach { appendLine("• $it") }
            appendLine()
            appendLine("METHOD")
            recipe.steps.forEachIndexed { i, st -> appendLine("${i + 1}. $st") }
            if (recipe.source.isNotBlank()) { appendLine(); appendLine("Source: ${recipe.source}") }
            appendLine()
            appendLine("Saved with Simmer, the free recipe app that feeds people: ${Config.APP_URL}")
        }
        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_SUBJECT, recipe.title).putExtra(Intent.EXTRA_TEXT, text)
        runCatching { context.startActivity(Intent.createChooser(send, "Share recipe")) }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete “${recipe.title}”?") },
            text = { Text("This can't be undone.") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; onDelete() }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Keep") } },
        )
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 40.dp)) {
        // Hero photo with title over it
        item {
            Box(Modifier.fillMaxWidth().height(340.dp)) {
                RecipeImage(recipe.imageUrl, Modifier.fillMaxSize())
                PhotoScrim(Modifier.fillMaxSize())
                Row(
                    Modifier.fillMaxWidth().statusBarsPadding().padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    GlassIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Back", onBack)
                    Row {
                        GlassIconButton(
                            if (recipe.favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            "Favourite",
                            onToggleFavorite,
                            tint = if (recipe.favorite) MaterialTheme.colorScheme.secondary else Color.White,
                        )
                        Spacer(Modifier.width(6.dp))
                        GlassIconButton(Icons.Default.Share, "Share", ::shareRecipe)
                        Spacer(Modifier.width(6.dp))
                        GlassIconButton(Icons.Default.Edit, "Edit", onEdit)
                    }
                }
                Column(Modifier.align(Alignment.BottomStart).padding(20.dp)) {
                    if (recipe.category.isNotBlank()) {
                        Pill(recipe.category, container = Color.White.copy(alpha = 0.9f), content = Ink)
                        Spacer(Modifier.height(10.dp))
                    }
                    Text(recipe.title, style = MaterialTheme.typography.headlineMedium, color = Color.White)
                }
            }
        }

        // Stats + actions
        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (recipe.time.isNotBlank()) StatTile("Time", recipe.time, Modifier.weight(1f))
                    if (baseServings != null) {
                        ServingsTile(servings, onChange = { servings = it.coerceIn(1, 99) }, modifier = Modifier.weight(1.4f))
                    } else if (recipe.servings.isNotBlank()) {
                        StatTile("Serves", recipe.servings, Modifier.weight(1f))
                    }
                    StatTile("Ingredients", "${recipe.ingredients.size}", Modifier.weight(1f))
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { onCook(shownIngredients) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    ) {
                        Icon(Icons.Default.LocalFireDepartment, contentDescription = null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Cook")
                    }
                    Button(onClick = { onAddToGrocery(shownIngredients) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) {
                        Icon(Icons.Default.AddShoppingCart, contentDescription = null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Groceries")
                    }
                }
                if (recipe.source.isNotBlank()) {
                    TextButton(
                        onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(recipe.source))) } },
                        contentPadding = PaddingValues(horizontal = 4.dp),
                    ) {
                        Icon(Icons.Outlined.OpenInNew, contentDescription = null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(Uri.parse(recipe.source).host?.removePrefix("www.") ?: "Original page")
                    }
                }
                SectionHeading("Ingredients")
            }
        }

        if (recipe.ingredients.isEmpty()) {
            item { Text("None listed", Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        itemsIndexed(shownIngredients) { i, ing ->
            val done = i in checked
            val shape = when {
                recipe.ingredients.size == 1 -> RoundedCornerShape(14.dp)
                i == 0 -> RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)
                i == recipe.ingredients.lastIndex -> RoundedCornerShape(bottomStart = 14.dp, bottomEnd = 14.dp)
                else -> RoundedCornerShape(0.dp)
            }
            Row(
                Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth()
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable { checked = if (done) checked - i else checked + i }
                    .padding(start = 6.dp, end = 14.dp, top = 2.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = done,
                    onCheckedChange = { checked = if (done) checked - i else checked + i },
                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
                )
                Text(
                    ing,
                    textDecoration = if (done) TextDecoration.LineThrough else null,
                    color = if (done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        item { SectionHeading("Method", Modifier.padding(horizontal = 20.dp)) }
        if (recipe.steps.isEmpty()) {
            item { Text("None listed", Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        itemsIndexed(recipe.steps) { i, step ->
            Row(Modifier.padding(horizontal = 20.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(
                    Modifier.size(30.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("${i + 1}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                }
                Text(step, modifier = Modifier.weight(1f).padding(top = 4.dp), style = MaterialTheme.typography.bodyLarge)
            }
        }

        if (recipe.notes.isNotBlank()) {
            item {
                Column(Modifier.padding(horizontal = 20.dp)) {
                    SectionHeading("Notes")
                    Text(
                        recipe.notes,
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .padding(14.dp),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
        }

        item {
            Spacer(Modifier.height(32.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                OutlinedButton(onClick = { confirmDelete = true }) {
                    Text("Delete recipe", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

/** Stat tile with +/- to scale the recipe. */
@Composable
private fun ServingsTile(value: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text("SERVES", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onChange(value - 1) }, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.Remove, contentDescription = "Fewer", Modifier.size(16.dp)) }
            Text("$value", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 6.dp))
            IconButton(onClick = { onChange(value + 1) }, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.Add, contentDescription = "More", Modifier.size(16.dp)) }
        }
    }
}

/** Round translucent button that sits on top of a photo. */
@Composable
private fun GlassIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color = Color.White,
) {
    IconButton(
        onClick = onClick,
        colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.35f)),
    ) {
        Icon(icon, contentDescription = label, tint = tint)
    }
}
