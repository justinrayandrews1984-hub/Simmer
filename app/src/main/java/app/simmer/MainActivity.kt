package app.simmer

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Kitchen
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
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
import app.simmer.ads.AdsManager
import app.simmer.data.Recipe
import app.simmer.importer.RecipeImporter
import app.simmer.ui.AddRecipeScreen
import app.simmer.ui.AfterCookDialog
import app.simmer.ui.CookModeScreen
import app.simmer.ui.GiveScreen
import app.simmer.ui.GroceryScreen
import app.simmer.ui.PantryScreen
import app.simmer.ui.RecipeDetailScreen
import app.simmer.ui.RecipeListScreen
import app.simmer.ui.SimmerTheme

sealed class Screen {
    data object Recipes : Screen()
    data class Detail(val id: Long) : Screen()
    data class Cook(val id: Long, val ingredients: List<String>) : Screen()
    data class Add(val editId: Long? = null, val sharedUrl: String = "") : Screen()
    data object Pantry : Screen()
    data object Grocery : Screen()
    data object Give : Screen()
}

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    // Set when a link is shared into the app from the browser.
    private var pendingUrl by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AdsManager.init(this)
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
    val give by vm.give.collectAsStateWithLifecycle()
    val pantry by vm.pantry.collectAsStateWithLifecycle()
    var screen by remember { mutableStateOf<Screen>(Screen.Recipes) }
    var afterCook by remember { mutableStateOf(false) }

    fun toast(msg: String) = Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()

    fun watchAd() {
        val activity = context as? Activity ?: return
        if (give.adsLeftToday <= 0) { toast("You've hit today's limit. Come back tomorrow!"); return }
        val shown = AdsManager.show(activity, onRewarded = { vm.recordAdWatched(); toast("Meal funded. Thank you!") })
        if (!shown) toast("Loading an ad, try again in a few seconds")
    }

    fun tip(cents: Int) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Config.DONATE_URL))) }
        vm.recordTip(cents)
    }

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
            is Screen.Cook -> Screen.Detail(current.id)
            else -> Screen.Recipes
        }
    }

    if (afterCook) {
        AfterCookDialog(
            cookedCount = give.recipesCooked,
            onWatchAd = { afterCook = false; watchAd() },
            onTip = { afterCook = false; tip(200) },
            onDismiss = { afterCook = false },
        )
    }

    // Cook mode takes over the whole screen.
    if (current is Screen.Cook) {
        val r = recipes.firstOrNull { it.id == current.id }
        if (r != null) {
            CookModeScreen(
                recipe = r,
                scaledIngredients = current.ingredients,
                onExit = { screen = Screen.Detail(r.id) },
                onFinished = { vm.recordCooked(); screen = Screen.Detail(r.id); afterCook = true },
            )
        }
        return
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
                val onRecipes = current is Screen.Recipes || current is Screen.Detail || current is Screen.Add
                val onPantry = current is Screen.Pantry
                val onGrocery = current is Screen.Grocery
                val onGive = current is Screen.Give
                NavigationBarItem(
                    selected = onRecipes, onClick = { screen = Screen.Recipes }, colors = colors,
                    icon = { Icon(if (onRecipes) Icons.Filled.MenuBook else Icons.Outlined.MenuBook, contentDescription = null) },
                    label = { Text("Recipes") },
                )
                NavigationBarItem(
                    selected = onPantry, onClick = { screen = Screen.Pantry }, colors = colors,
                    icon = { Icon(if (onPantry) Icons.Filled.Kitchen else Icons.Outlined.Kitchen, contentDescription = null) },
                    label = { Text("Pantry") },
                )
                NavigationBarItem(
                    selected = onGrocery, onClick = { screen = Screen.Grocery }, colors = colors,
                    icon = { Icon(if (onGrocery) Icons.Filled.ShoppingCart else Icons.Outlined.ShoppingCart, contentDescription = null) },
                    label = { Text("Grocery") },
                )
                NavigationBarItem(
                    selected = onGive, onClick = { screen = Screen.Give }, colors = colors,
                    icon = { Icon(if (onGive) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, contentDescription = null) },
                    label = { Text("Give") },
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        // The recipe page draws its photo under the status bar; other screens keep clear of it.
        contentWindowInsets = if (isDetail) WindowInsets(0, 0, 0, 0) else ScaffoldDefaults.contentWindowInsets,
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
                            onAddToGrocery = { lines -> vm.addRecipeToGrocery(detailRecipe, lines); toast("Added to grocery list") },
                            onToggleFavorite = { vm.toggleFavorite(detailRecipe) },
                            onDelete = { vm.deleteRecipe(detailRecipe.id); screen = Screen.Recipes },
                            onCook = { lines -> screen = Screen.Cook(detailRecipe.id, lines) },
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
                            onSave = { r: Recipe ->
                                vm.saveRecipe(r) { id ->
                                    if (editing == null && vm.claimWelcomeBonus()) toast("Saved. First recipe: a meal's on us! 🍲")
                                    else toast("Saved")
                                    screen = Screen.Detail(id)
                                }
                            },
                            onCancel = { screen = current.editId?.let { Screen.Detail(it) } ?: Screen.Recipes },
                        )
                    }
                }
                Screen.Pantry -> Box(Modifier.padding(top = 12.dp)) {
                    PantryScreen(
                        pantry = pantry,
                        recipes = recipes,
                        onAdd = { vm.addPantry(it) },
                        onRemove = { vm.removePantry(it) },
                        onOpen = { screen = Screen.Detail(it.id) },
                        onAddMissing = { r, missing -> vm.addMissingToGrocery(r, missing) },
                        onToast = ::toast,
                    )
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
                            toast("Added ${ids.size} recipe${if (ids.size == 1) "" else "s"}")
                        },
                        onRoundUp = { tip(100) },
                    )
                }
                Screen.Give -> Box(Modifier.padding(top = 12.dp)) {
                    GiveScreen(state = give, onAdWatched = { vm.recordAdWatched() }, onTip = { vm.recordTip(it) }, onToast = ::toast)
                }
                is Screen.Cook -> Unit
            }
        }
    }
}
