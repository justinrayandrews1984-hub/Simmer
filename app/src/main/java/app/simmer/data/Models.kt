package app.simmer.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import org.json.JSONArray

@Entity(tableName = "recipes")
data class Recipe(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "",
    val category: String = "",
    val servings: String = "",
    val time: String = "",
    val source: String = "",
    val ingredients: List<String> = emptyList(),
    val steps: List<String> = emptyList(),
    val notes: String = "",
    val favorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val imageUrl: String = "",
)

@Entity(tableName = "grocery")
data class GroceryItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val recipeId: Long? = null,
    val recipeTitle: String = "",
    val done: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

class Converters {
    @TypeConverter
    fun listToJson(list: List<String>): String = JSONArray(list).toString()

    @TypeConverter
    fun jsonToList(json: String): List<String> = try {
        val arr = JSONArray(json)
        (0 until arr.length()).map { arr.getString(it) }
    } catch (e: Exception) {
        emptyList()
    }
}
