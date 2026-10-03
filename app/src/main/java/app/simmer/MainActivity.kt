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
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Kitchen
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.TravelExplore
import androidx.compose.material3.ExperimentalMaterial3Api
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
import app.simmer.ui.CustomizeScreen
import app.simmer.ui.DiscoverScreen
import app.simmer.ui.GiveScreen
import app.simmer.ui.GroceryScreen
import app.simmer.ui.HomeScreen
import app.simmer.ui.PantryScreen
import app.simmer.ui.RecipeDetailScreen
import app.simmer.ui.RecipeListScreen
import app.simmer.ui.SimmerTheme

sealed class Screen {
    data object Home : Screen()
    data object Library : Screen()
    data class Detail(val id: Long) : Screen()
    data class Cook(val id: Long, val ingredients: List<String>) : Screen()
    data class Add(val editId: Long? = null, val sharedUrl: String = "") : Screen()
    data object Discover : Screen()
    data object Pantry : Screen()
    data object Grocery : Screen()
    data object Give : Screen()
    data object Customize : Screen()
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
            val give by vm.give.collectAsStateWithLifecycle()
            SimmerTheme(themeKey = give.theme, sansFont = give.sansFont) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimmerApp(vm: AppViewModel, sharedUrl: String?, onSharedUrlConsumed: () -> Unit) {
    val context = LocalContext.current
    val recipes by vm.recipes.collectAsStateWithLifecycle()
    val grocery by vm.grocery.collectAsStateWithLifecycle()
    val give by vm.give.collectAsStateWithLifecycle()
    val pantry by vm.pantry.collectAsStateWithLifecycle()
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }
    var afterCook by remember { mutableStateOf(false) }

    fun toast(msg: String) = Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    fun embers(n: Int, what: String) { if (n > 0) toast("$what  +$n ✦") }

    fun watchAd() {
        val activity = context as? Activity ?: return
        if (give.adsLeftToday <= 0) { toast("You've hit today's limit. Come back tomorrow!"); return }
        val shown = AdsManager.show(activity, onRewarded = { val e = vm.recordAdWatched(); toast("Meal funded. Thank you!  +$e ✦") })
        if (!shown) toast("Loading an ad, try again in a few seconds")
    }

    fun tip(cents: Int) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Config.DONATE_URL))) }
        embers(vm.recordTip(cents), "Thank you")
    }

    fun saveNew(r: Recipe, onDone: (Long) -> Unit) {
        vm.saveRecipe(r) { id ->
            val bonus = vm.claimWelcomeBonus()
            val e = vm.recordSaved()
            toast(if (bonus) "Saved. First recipe: a meal's on us! 🍲  +5 ✦" else if (e > 0) "Saved  +$e ✦" else "Saved")
            onDone(id)
        }
    }

    // A shared link jumps straight to the import screen.
    LaunchedEffect(sharedUrl) {
        if (sharedUrl != null) {
            screen = Screen.Add(sharedUrl = sharedUrl)
            onSharedUrlConsumed()
        }
    }

    val current = screen
    BackHandler(enabled = current !is Screen.Home) {
        screen = when (current) {
            is Screen.Add -> current.editId?.let { Screen.Detail(it) } ?: Screen.Home
            is Screen.Cook -> Screen.Detail(current.id)
            is Screen.Detail, Screen.Library -> Screen.Home
            Screen.Customize -> Screen.Give
            else -> Screen.Home
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
                onFinished = { val e = vm.recordCooked(); screen = Screen.Detail(r.id); afterCook = true; embers(e, "Cooked") },
                confetti = give.confetti && give.has(app.simmer.data.Unlock.CONFETTI),
            )
        }
        return
    }

    val detailRecipe = (current as? Screen.Detail)?.let { d -> recipes.firstOrNull { it.id == d.id } }
    val isDetail = current is Screen.Detail
    val pantrySuggestion = pantry.take(4).joinToString(" ")

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                val colors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                )
                val onHome = current is Screen.Home || current is Screen.Detail || current is Screen.Add || current is Screen.Library
                val onDiscover = current is Screen.Discover
                val onPantry = current is Screen.Pantry
                val onGrocery = current is Screen.Grocery
                val onGive = current is Screen.Give || current is Screen.Customize
                NavigationBarItem(selected = onHome, onClick = { screen = Screen.Home }, colors = colors,
                    icon = { Icon(if (onHome) Icons.Filled.Home else Icons.Outlined.Home, contentDescription = null) }, label = { Text("Home") })
                NavigationBarItem(selected = onDiscover, onClick = { screen = Screen.Discover }, colors = colors,
                    icon = { Icon(if (onDiscover) Icons.Filled.TravelExplore else Icons.Outlined.TravelExplore, contentDescription = null) }, label = { Text("Discover") })
                NavigationBarItem(selected = onPantry, onClick = { screen = Screen.Pantry }, colors = colors,
                    icon = { Icon(if (onPantry) Icons.Filled.Kitchen else Icons.Outlined.Kitchen, contentDescription = null) }, label = { Text("Pantry") })
                NavigationBarItem(selected = onGrocery, onClick = { screen = Screen.Grocery }, colors = colors,
                    icon = { Icon(if (onGrocery) Icons.Filled.ShoppingCart else Icons.Outlined.ShoppingCart, contentDescription = null) }, label = { Text("Grocery") })
                NavigationBarItem(selected = onGive, onClick = { screen = Screen.Give }, colors = colors,
                    icon = { Icon(if (onGive) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, contentDescription = null) }, label = { Text("Give") })
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        // The recipe page draws its photo under the status bar; other screens keep clear of it.
        contentWindowInsets = if (isDetail) WindowInsets(0, 0, 0, 0) else ScaffoldDefaults.contentWindowInsets,
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (current) {
                Screen.Home -> Box(Modifier.padding(top = 12.dp)) {
                    HomeScreen(
                        recipes = recipes, give = give, groceryLeft = grocery.count { !it.done },
                        onOpen = { screen = Screen.Detail(it.id) },
                        onLibrary = { screen = Screen.Library },
                        onAdd = { screen = Screen.Add() },
                        onDiscover = { screen = Screen.Discover },
                        onPantry = { screen = Screen.Pantry },
                        onGrocery = { screen = Screen.Grocery },
                        onGive = { screen = Screen.Give },
                    )
                }
                Screen.Library -> Box(Modifier.padding(top = 12.dp)) {
                    RecipeListScreen(
                        recipes = recipes,
                        onOpen = { screen = Screen.Detail(it.id) },
                        onToggleFavorite = { vm.toggleFavorite(it) },
                        onAdd = { screen = Screen.Add() },
                        compact = give.compactGrid && give.has(app.simmer.data.Unlock.COMPACT_GRID),
                        onBack = { screen = Screen.Home },
                    )
                }
                is Screen.Detail -> {
                    if (detailRecipe != null) {
                        RecipeDetailScreen(
                            recipe = detailRecipe,
                            onBack = { screen = Screen.Home },
                            onEdit = { screen = Screen.Add(editId = detailRecipe.id) },
                            onAddToGrocery = { lines -> vm.addRecipeToGrocery(detailRecipe, lines); toast("Added to grocery list") },
                            onToggleFavorite = { vm.toggleFavorite(detailRecipe) },
                            onDelete = { vm.deleteRecipe(detailRecipe.id); screen = Screen.Home },
                            onCook = { lines -> screen = Screen.Cook(detailRecipe.id, lines) },
                            chefSignature = if (give.chefTitleOnShares && give.has(app.simmer.data.Unlock.CHEF_TITLE)) "Shared by a Simmer ${give.level.title}" else "",
                            onShared = { embers(vm.recordSharedRecipe(), "Shared") },
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
                                if (editing == null) saveNew(r) { id -> screen = Screen.Detail(id) }
                                else vm.saveRecipe(r) { id -> toast("Saved"); screen = Screen.Detail(id) }
                            },
                            onCancel = { screen = current.editId?.let { Screen.Detail(it) } ?: Screen.Home },
                        )
                    }
                }
                Screen.Discover -> Box(Modifier.padding(top = 12.dp)) {
                    DiscoverScreen(
                        pantrySuggestion = pantrySuggestion,
                        onSave = { r -> saveNew(r) { id -> screen = Screen.Detail(id) } },
                        onToast = ::toast,
                    )
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
                        onPlan = { ids -> vm.addRecipesToGrocery(ids); toast("Added ${ids.size} recipe${if (ids.size == 1) "" else "s"}") },
                        onRoundUp = { tip(100) },
                    )
                }
                Screen.Give -> Box(Modifier.padding(top = 12.dp)) {
                    GiveScreen(
                        state = give,
                        onAdWatched = { vm.recordAdWatched() },
                        onTip = { vm.recordTip(it) },
                        onToast = ::toast,
                        onCustomize = { screen = Screen.Customize },
                        onSharedApp = { embers(vm.recordSharedApp(), "Thanks for spreading the word") },
                    )
                }
                Screen.Customize -> Box(Modifier.padding(top = 12.dp)) {
                    CustomizeScreen(
                        state = give,
                        onBack = { screen = Screen.Give },
                        onTheme = { vm.setTheme(it) },
                        onIcon = { vm.setIcon(it); toast("Icon changed. It may take a moment to show on your home screen.") },
                        onToggle = { k, on -> vm.setToggle(k, on) },
                    )
                }
                is Screen.Cook -> Unit
            }
        }
    }
}
