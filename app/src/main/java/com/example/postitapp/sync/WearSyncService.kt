package com.example.postitapp.sync

import android.util.Log
import androidx.compose.ui.graphics.Color
import com.example.postitapp.data.ColorPalette
import com.example.postitapp.data.TodoItem
import com.example.postitapp.data.TodoRepository
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.serialization.json.Json

class WearSyncService : WearableListenerService() {
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        Log.d("WearSyncService", "onDataChanged triggered on mobile")
        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED) {
                val path = event.dataItem.uri.path
                if (path == "/todos") {
                    try {
                        val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                        val json = dataMap.getString("items_json")
                        if (json != null) {
                            val list = Json.decodeFromString<List<TodoItem>>(json)
                            TodoRepository.getInstance(this).updateFromSync(list)
                            Log.d("WearSyncService", "Successfully synced items from Wear OS: $list")
                        }
                    } catch (e: Exception) {
                        Log.e("WearSyncService", "Error processing data change from Wear OS", e)
                    }
                } else if (path == "/theme") {
                    try {
                        val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                        val selectedName = dataMap.getString("selected_name") ?: "Negro"
                        val selectedBg = dataMap.getInt("selected_bg")
                        val selectedChip = dataMap.getInt("selected_chip")
                        val selectedText = if (dataMap.containsKey("selected_text")) dataMap.getInt("selected_text") else 0xFFFFFFFF.toInt()
                        val customBg = dataMap.getInt("custom_bg")
                        val customChip = dataMap.getInt("custom_chip")
                        val customText = if (dataMap.containsKey("custom_text")) dataMap.getInt("custom_text") else 0xFFFFFFFF.toInt()
                        
                        val selected = ColorPalette(selectedName, Color(selectedBg), Color(selectedChip), Color(selectedText))
                        val custom = ColorPalette("Personalizado", Color(customBg), Color(customChip), Color(customText))
                        
                        TodoRepository.getInstance(this).updateThemeFromSync(selected, custom)
                        Log.d("WearSyncService", "Successfully synced theme from Wear OS: selected=$selectedName")
                    } catch (e: Exception) {
                        Log.e("WearSyncService", "Error processing theme change from Wear OS", e)
                    }
                }
            }
        }
    }
}

