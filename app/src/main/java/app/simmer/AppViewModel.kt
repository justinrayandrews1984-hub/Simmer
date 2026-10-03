package app.simmer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.simmer.data.AppDatabase
import app.simmer.data.GroceryItem
import app.simmer.data.Recipe
import app.simmer.importer.RecipeImporter
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val db = AppDatabase.get(app)
    private val recipeDao = db.recipeDao()
    private val groceryDao = db.groceryDao()

    val recipes: StateFlow<List<Recipe>> =
        recipeDao.all().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val grocery: StateFlow<List<GroceryItem>> =
        groceryDao.all().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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

    fun addRecipeToGrocery(recipe: Recipe) = viewModelScope.launch {
        val existing = groceryDao.forRecipe(recipe.id).map { it.text }.toSet()
        val fresh = recipe.ingredients.filter { it !in existing }
            .map { GroceryItem(text = it, recipeId = recipe.id, recipeTitle = recipe.title) }
        if (fresh.isNotEmpty()) groceryDao.insertAll(fresh)
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
}
