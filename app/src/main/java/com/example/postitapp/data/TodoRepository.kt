package com.example.postitapp.data

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class TodoRepository private constructor(private val context: Context) {

    private val sharedPrefs = context.getSharedPreferences("todo_prefs", Context.MODE_PRIVATE)
    private val _todoItems = MutableStateFlow<List<TodoItem>>(emptyList())
    val todoItems: StateFlow<List<TodoItem>> = _todoItems.asStateFlow()
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _selectedPalette = MutableStateFlow(
        ColorPalette("Negro", Color(0xFF000000), Color(0xFF2C2C2C), Color(0xFFFFFFFF))
    )
    val selectedPalette: StateFlow<ColorPalette> = _selectedPalette.asStateFlow()

    private val _customPalette = MutableStateFlow(
        ColorPalette("Personalizado", Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFFFFFFFF))
    )
    val customPalette: StateFlow<ColorPalette> = _customPalette.asStateFlow()

    init {
        loadItems()
        loadTheme()
    }

    private fun loadItems() {
        val json = sharedPrefs.getString("items_json", null)
        if (json != null) {
            try {
                _todoItems.value = Json.decodeFromString(json)
            } catch (e: Exception) {
                Log.e("TodoRepository", "Error loading items", e)
                _todoItems.value = emptyList()
            }
        } else {
            _todoItems.value = listOf(
                TodoItem("1", "Comprar leche", false),
                TodoItem("2", "Ir al gimnasio", true),
                TodoItem("3", "Aprender Kotlin", false)
            )
            saveItemsToSharedPrefs(_todoItems.value)
        }
    }

    private fun loadTheme() {
        val selectedName = sharedPrefs.getString("selected_palette_name", "Negro")
        val selectedBg = sharedPrefs.getInt("selected_palette_bg", 0xFF000000.toInt())
        val selectedChip = sharedPrefs.getInt("selected_palette_chip", 0xFF2C2C2C.toInt())
        val selectedText = sharedPrefs.getInt("selected_palette_text", 0xFFFFFFFF.toInt())
        _selectedPalette.value = ColorPalette(selectedName ?: "Negro", Color(selectedBg), Color(selectedChip), Color(selectedText))

        val customBg = sharedPrefs.getInt("custom_palette_bg", 0xFF0F172A.toInt())
        val customChip = sharedPrefs.getInt("custom_palette_chip", 0xFF1E293B.toInt())
        val customText = sharedPrefs.getInt("custom_palette_text", 0xFFFFFFFF.toInt())
        _customPalette.value = ColorPalette("Personalizado", Color(customBg), Color(customChip), Color(customText))
    }

    fun updatePalette(palette: ColorPalette) {
        _selectedPalette.value = palette
        sharedPrefs.edit().apply {
            putString("selected_palette_name", palette.name)
            putInt("selected_palette_bg", palette.background.toArgb())
            putInt("selected_palette_chip", palette.chip.toArgb())
            putInt("selected_palette_text", palette.textColor.toArgb())
        }.apply()
        syncThemeToWear(palette, _customPalette.value)
    }

    fun updateCustomPalette(palette: ColorPalette) {
        _customPalette.value = palette
        _selectedPalette.value = palette
        sharedPrefs.edit().apply {
            putString("selected_palette_name", palette.name)
            putInt("selected_palette_bg", palette.background.toArgb())
            putInt("selected_palette_chip", palette.chip.toArgb())
            putInt("selected_palette_text", palette.textColor.toArgb())
            putInt("custom_palette_bg", palette.background.toArgb())
            putInt("custom_palette_chip", palette.chip.toArgb())
            putInt("custom_palette_text", palette.textColor.toArgb())
        }.apply()
        syncThemeToWear(palette, palette)
    }

    fun updateThemeFromSync(selected: ColorPalette, custom: ColorPalette) {
        _selectedPalette.value = selected
        _customPalette.value = custom
        sharedPrefs.edit().apply {
            putString("selected_palette_name", selected.name)
            putInt("selected_palette_bg", selected.background.toArgb())
            putInt("selected_palette_chip", selected.chip.toArgb())
            putInt("selected_palette_text", selected.textColor.toArgb())
            putInt("custom_palette_bg", custom.background.toArgb())
            putInt("custom_palette_chip", custom.chip.toArgb())
            putInt("custom_palette_text", custom.textColor.toArgb())
        }.apply()
    }


    fun addItem(text: String) {
        val newItem = TodoItem(
            id = java.util.UUID.randomUUID().toString(),
            text = text,
            isChecked = false
        )
        val updated = _todoItems.value + newItem
        updateList(updated)
    }

    fun toggleItem(id: String) {
        val updated = _todoItems.value.map {
            if (it.id == id) it.copy(isChecked = !it.isChecked) else it
        }
        updateList(updated)
    }

    fun deleteItem(id: String) {
        val updated = _todoItems.value.filter { it.id != id }
        updateList(updated)
    }

    fun editItem(id: String, newText: String) {
        val updated = _todoItems.value.map {
            if (it.id == id) it.copy(text = newText) else it
        }
        updateList(updated)
    }

    fun updateFromSync(newList: List<TodoItem>) {
        saveItemsToSharedPrefs(newList)
        _todoItems.value = newList
        notifyWidgetChanged()
    }

    private fun updateList(newList: List<TodoItem>) {
        saveItemsToSharedPrefs(newList)
        _todoItems.value = newList
        notifyWidgetChanged()
        syncToWear(newList)
    }

    private fun saveItemsToSharedPrefs(list: List<TodoItem>) {
        val json = Json.encodeToString(list)
        sharedPrefs.edit().putString("items_json", json).apply()
    }

    private fun syncToWear(list: List<TodoItem>) {
        scope.launch {
            try {
                val json = Json.encodeToString(list)
                val putDataMapReq = PutDataMapRequest.create("/todos").apply {
                    dataMap.putString("items_json", json)
                    dataMap.putLong("timestamp", System.currentTimeMillis())
                }
                val putDataReq = putDataMapReq.asPutDataRequest().setUrgent()
                Wearable.getDataClient(context).putDataItem(putDataReq)
                Log.d("TodoRepository", "Successfully synced items to Wear OS")
            } catch (e: Exception) {
                Log.e("TodoRepository", "Error syncing items to Wear OS", e)
            }
        }
    }

    private fun syncThemeToWear(selected: ColorPalette, custom: ColorPalette) {
        scope.launch {
            try {
                val putDataMapReq = PutDataMapRequest.create("/theme").apply {
                    dataMap.putString("selected_name", selected.name)
                    dataMap.putInt("selected_bg", selected.background.toArgb())
                    dataMap.putInt("selected_chip", selected.chip.toArgb())
                    dataMap.putInt("selected_text", selected.textColor.toArgb())
                    dataMap.putInt("custom_bg", custom.background.toArgb())
                    dataMap.putInt("custom_chip", custom.chip.toArgb())
                    dataMap.putInt("custom_text", custom.textColor.toArgb())
                    dataMap.putLong("timestamp", System.currentTimeMillis())
                }
                val putDataReq = putDataMapReq.asPutDataRequest().setUrgent()
                Wearable.getDataClient(context).putDataItem(putDataReq)
                Log.d("TodoRepository", "Successfully synced theme to Wear OS")
            } catch (e: Exception) {
                Log.e("TodoRepository", "Error syncing theme to Wear OS", e)
            }
        }
    }


    fun notifyWidgetChanged() {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        try {
            val providerClass = Class.forName("com.example.postitapp.widget.ListWidgetProvider")
            val componentName = ComponentName(context, providerClass)
            val ids = appWidgetManager.getAppWidgetIds(componentName)
            
            val listResId = context.resources.getIdentifier("widget_list", "id", context.packageName)
            if (listResId != 0 && ids.isNotEmpty()) {
                appWidgetManager.notifyAppWidgetViewDataChanged(ids, listResId)
            }

            val updateIntent = Intent(context, providerClass).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(updateIntent)
        } catch (e: Exception) {
            Log.e("TodoRepository", "Error updating widget", e)
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: TodoRepository? = null

        fun getInstance(context: Context): TodoRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = TodoRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
