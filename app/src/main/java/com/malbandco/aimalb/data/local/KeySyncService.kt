package com.malbandco.aimalb.data.local

import android.util.Log
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService

class KeySyncService : WearableListenerService() {
    
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        val prefs = PreferencesManager(this)
        
        dataEvents.forEach { event ->
            if (event.type == DataEvent.TYPE_CHANGED && event.dataItem.uri.path == "/groq_key") {
                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                val key = dataMap.getString("key", "")
                if (!key.isNullOrEmpty()) {
                    prefs.apiKey = key
                    Log.d("KeySyncService", "Groq key updated from phone in background")
                }
            }
        }
    }
}
