package app.simmer

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.simmer.importer.RecipeImporter
import app.simmer.ui.AddRecipeScreen
import app.simmer.ui.GroceryScreen
import app.simmer.ui.RecipeDetailScreen
import app.simmer.ui.RecipeListScreen
import app.simmer.ui.SimmerTheme

sealed class Screen {
    data object Recipes : Screen()
    data class Detail(val id: Long) : Screen()
    data class Add(val editId: Long? = null, val sharedUrl: String = "") : Screen()
    data object Grocery : Screen()
}

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    // Set when a link is shared into the app from the browser.
    private var pendingUrl by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingUrl = urlFrom(intent)
        setContent {
            SimmerTheme {
                SimmerApp(vm = vm, sharedUrl = pendingUrl, onSharedUrlConsumed = { pendingUrl = null })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        urlFrom(intent)?.let { pendingUrl = it }
    }

    private fun urlFrom(intent: Intent?): String? {
        if (intent?.action != Intent.ACTION_SEND) return null
        return RecipeImporter.extractUrl(intent.getStringExtra(Intent.EXTRA_TEXT))
    }
}

@Composable
fun SimmerApp(vm: AppViewModel, sharedUrl: String?, onSharedUrlConsumed: () -> Unit) {
    val context = LocalContext.current
    val recipes by vm.recipes.collectAsStateWithLifecycle()
    val grocery by vm.grocery.collectAsStateWithLifecycle()
    var screen by remember { mutableStateOf<Screen>(Screen.Recipes) }

    // A shared link jumps straight to the import screen.
    LaunchedEffect(sharedUrl) {
        if (sharedUrl != null) {
            screen = Screen.Add(sharedUrl = sharedUrl)
            onSharedUrlConsumed()
        }
    }

    val current = screen
    BackHandler(enabled = current !is Screen.Recipes) {
        screen = when (current) {
            is Screen.Add -> current.editId?.let { Screen.Detail(it) } ?: Screen.Recipes
            else -> Screen.Recipes
        }
    }

    val detailRecipe = (current as? Screen.Detail)?.let { d -> recipes.firstOrNull { it.id == d.id } }
    val isDetail = current is Screen.Detail

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                val colors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                )
                val onRecipes = current is Screen.Recipes || current is Screen.Detail
                val onAdd = current is Screen.Add && current.editId == null
                val onGrocery = current is Screen.Grocery
                NavigationBarItem(
                    selected = onRecipes,
                    onClick = { screen = Screen.Recipes },
                    icon = { Icon(if (onRecipes) Icons.Filled.MenuBook else Icons.Outlined.MenuBook, contentDescription = null) },
                    label = { Text("Recipes") },
                    colors = colors,
                )
                NavigationBarItem(
                    selected = onAdd,
                    onClick = { screen = Screen.Add() },
                    icon = { Icon(if (onAdd) Icons.Filled.AddCircle else Icons.Outlined.AddCircleOutline, contentDescription = null) },
                    label = { Text("Add") },
                    colors = colors,
                )
                NavigationBarItem(
                    selected = onGrocery,
                    onClick = { screen = Screen.Grocery },
                    icon = { Icon(if (onGrocery) Icons.Filled.ShoppingCart else Icons.Outlined.ShoppingCart, contentDescription = null) },
                    label = { Text("Grocery") },
                    colors = colors,
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        // The recipe page draws its photo under the status bar; other screens keep clear of it.
        contentWindowInsets = if (isDetail) androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0) else androidx.compose.material3.ScaffoldDefaults.contentWindowInsets,
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (current) {
                Screen.Recipes -> Box(Modifier.padding(top = 12.dp)) {
                    RecipeListScreen(
                        recipes = recipes,
                        onOpen = { screen = Screen.Detail(it.id) },
                        onToggleFavorite = { vm.toggleFavorite(it) },
                        onAdd = { screen = Screen.Add() },
                    )
                }
                is Screen.Detail -> {
                    if (detailRecipe != null) {
                        RecipeDetailScreen(
                            recipe = detailRecipe,
                            onBack = { screen = Screen.Recipes },
                            onEdit = { screen = Screen.Add(editId = detailRecipe.id) },
                            onAddToGrocery = {
                                vm.addRecipeToGrocery(detailRecipe)
                                Toast.makeText(context, "Added to grocery list", Toast.LENGTH_SHORT).show()
                            },
                            onToggleFavorite = { vm.toggleFavorite(detailRecipe) },
                            onDelete = { vm.deleteRecipe(detailRecipe.id); screen = Screen.Recipes },
                        )
                    }
                }
                is Screen.Add -> Box(Modifier.padding(top = 12.dp)) {
                    val editing = current.editId?.let { id -> recipes.firstOrNull { it.id == id } }
                    key(current) {
                        AddRecipeScreen(
                            existing = editing,
                            sharedUrl = current.sharedUrl,
                            knownCategories = recipes.map { it.category }.filter { it.isNotBlank() }.distinct().sorted(),
                            onImport = { url, cb -> vm.importFromUrl(url, cb) },
                            onSave = { r ->
                                vm.saveRecipe(r) { id ->
                                    Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
                                    screen = Screen.Detail(id)
                                }
                            },
                            onCancel = { screen = current.editId?.let { Screen.Detail(it) } ?: Screen.Recipes },
                        )
                    }
                }
                Screen.Grocery -> Box(Modifier.padding(top = 12.dp)) {
                    GroceryScreen(
                        items = grocery,
                        recipes = recipes,
                        onAdd = { vm.addGroceryItem(it) },
                        onToggle = { vm.toggleGrocery(it) },
                        onRemove = { vm.removeGrocery(it) },
                        onClearDone = { vm.clearDoneGrocery() },
                        onClearAll = { vm.clearAllGrocery() },
                        onPlan = { ids ->
                            vm.addRecipesToGrocery(ids)
                            Toast.makeText(context, "Added ${ids.size} recipe${if (ids.size == 1) "" else "s"}", Toast.LENGTH_SHORT).show()
                        },
                    )
                }
            }
        }
    }
}
