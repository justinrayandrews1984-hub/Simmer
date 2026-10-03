package app.simmer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.simmer.data.AppDatabase
import app.simmer.data.GiveState
import app.simmer.data.GiveStore
import app.simmer.data.GroceryItem
import app.simmer.data.Recipe
import app.simmer.importer.RecipeImporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val db = AppDatabase.get(app)
    private val recipeDao = db.recipeDao()
    private val groceryDao = db.groceryDao()
    private val giveStore = GiveStore(app)
    private val prefs = app.getSharedPreferences("simmer.pantry", android.content.Context.MODE_PRIVATE)

    val recipes: StateFlow<List<Recipe>> =
        recipeDao.all().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val grocery: StateFlow<List<GroceryItem>> =
        groceryDao.all().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val give: StateFlow<GiveState> = giveStore.state

    private val _pantry = MutableStateFlow(prefs.getStringSet("items", emptySet())?.toSortedSet()?.toSet() ?: emptySet())
    val pantry: StateFlow<Set<String>> = _pantry

    // ---------- recipes ----------

    fun saveRecipe(recipe: Recipe, onSaved: (Long) -> Unit) = viewModelScope.launch {
        val id = recipeDao.upsert(recipe)
        onSaved(if (recipe.id == 0L) id else recipe.id)
    }

    fun toggleFavorite(recipe: Recipe) = viewModelScope.launch {
        recipeDao.upsert(recipe.copy(favorite = !recipe.favorite))
    }

    fun deleteRecipe(id: Long) = viewModelScope.launch { recipeDao.delete(id) }

    /** Fetches and parses a recipe page. Calls back on the main thread. */
    fun importFromUrl(url: String, onResult: (Result<Recipe>) -> Unit) = viewModelScope.launch {
        val result = try {
            Result.success(RecipeImporter.fromUrl(url))
        } catch (e: Exception) {
            Result.failure(e)
        }
        onResult(result)
    }

    // ---------- grocery ----------

    fun addRecipeToGrocery(recipe: Recipe, ingredients: List<String> = recipe.ingredients) = viewModelScope.launch {
        val existing = groceryDao.forRecipe(recipe.id).map { it.text }.toSet()
        val fresh = ingredients.filter { it !in existing }
            .map { GroceryItem(text = it, recipeId = recipe.id, recipeTitle = recipe.title) }
        if (fresh.isNotEmpty()) groceryDao.insertAll(fresh)
    }

    fun addMissingToGrocery(recipe: Recipe, missingKeywords: List<String>) = viewModelScope.launch {
        val lines = recipe.ingredients.filter { app.simmer.util.Kitchen.keyword(it) in missingKeywords }
        addRecipeToGrocery(recipe, lines)
    }

    fun addRecipesToGrocery(ids: Set<Long>) = viewModelScope.launch {
        for (id in ids) {
            val r = recipeDao.byId(id) ?: continue
            val existing = groceryDao.forRecipe(r.id).map { it.text }.toSet()
            val fresh = r.ingredients.filter { it !in existing }
                .map { GroceryItem(text = it, recipeId = r.id, recipeTitle = r.title) }
            if (fresh.isNotEmpty()) groceryDao.insertAll(fresh)
        }
    }

    fun addGroceryItem(text: String) = viewModelScope.launch {
        if (text.isNotBlank()) groceryDao.insertAll(listOf(GroceryItem(text = text.trim())))
    }

    fun toggleGrocery(item: GroceryItem) = viewModelScope.launch { groceryDao.update(item.copy(done = !item.done)) }
    fun removeGrocery(item: GroceryItem) = viewModelScope.launch { groceryDao.delete(item) }
    fun clearDoneGrocery() = viewModelScope.launch { groceryDao.clearDone() }
    fun clearAllGrocery() = viewModelScope.launch { groceryDao.clearAll() }

    // ---------- pantry ----------

    fun addPantry(text: String) {
        val items = text.split(',', '\n').map { it.trim().lowercase() }.filter { it.isNotBlank() }
        if (items.isEmpty()) return
        val next = (_pantry.value + items).toSortedSet().toSet()
        _pantry.value = next
        prefs.edit().putStringSet("items", next).apply()
    }

    fun removePantry(item: String) {
        val next = _pantry.value - item
        _pantry.value = next
        prefs.edit().putStringSet("items", next).apply()
    }

    // ---------- giving ----------

    fun recordAdWatched(): Int = giveStore.recordAdWatched()
    fun recordTip(cents: Int): Int = giveStore.recordTip(cents)
    fun recordCooked(): Int = giveStore.recordCooked()
    fun recordSaved(): Int = giveStore.recordSaved()
    fun recordSharedRecipe(): Int = giveStore.recordSharedRecipe()
    fun recordSharedApp(): Int = giveStore.recordSharedApp()
    fun claimWelcomeBonus(): Boolean = giveStore.claimWelcomeBonus()
    fun setTheme(key: String) = giveStore.setTheme(key)
    fun setToggle(key: String, on: Boolean) = giveStore.setToggle(key, on)

    /** Switches the launcher icon by enabling one activity-alias and disabling the others. */
    fun setIcon(key: String) {
        val app = getApplication<Application>()
        val pm = app.packageManager
        listOf("herb", "paprika", "midnight", "gold").forEach { k ->
            val cn = android.content.ComponentName(app, "app.simmer.Icon_$k")
            pm.setComponentEnabledSetting(
                cn,
                if (k == key) android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED else android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                android.content.pm.PackageManager.DONT_KILL_APP,
            )
        }
        giveStore.setIcon(key)
    }
}
