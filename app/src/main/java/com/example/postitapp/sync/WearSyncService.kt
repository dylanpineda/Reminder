package com.example.postitapp.sync

import android.util.Log
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
                }
            }
        }
    }
}
