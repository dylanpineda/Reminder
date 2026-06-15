package com.example.postitapp.data

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
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

    init {
        loadItems()
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
