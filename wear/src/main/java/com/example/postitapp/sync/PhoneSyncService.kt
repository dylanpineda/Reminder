package com.example.postitapp.sync

import android.util.Log
import com.example.postitapp.data.TodoItem
import com.example.postitapp.data.TodoRepository
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.serialization.json.Json

class PhoneSyncService : WearableListenerService() {
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        Log.d("PhoneSyncService", "onDataChanged triggered on watch")
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
                            Log.d("PhoneSyncService", "Successfully synced items from Phone: $list")
                        }
                    } catch (e: Exception) {
                        Log.e("PhoneSyncService", "Error processing data change from Phone", e)
                    }
                }
            }
        }
    }
}
