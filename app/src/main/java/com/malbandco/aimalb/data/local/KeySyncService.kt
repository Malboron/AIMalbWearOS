package com.malbandco.aimalb.data.local

import android.util.Log
import androidx.annotation.Keep
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

@Keep
class KeySyncService : WearableListenerService() {
    
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        val prefs = PreferencesManager(this)
        Log.d("KeySyncService", "Data Changed Event Received")
        
        dataEvents.forEach { event ->
            Log.d("KeySyncService", "Path: ${event.dataItem.uri.path}")
            if (event.type == DataEvent.TYPE_CHANGED && event.dataItem.uri.path?.endsWith("/groq_key") == true) {
                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                val key = dataMap.getString("key", "")
                if (!key.isNullOrEmpty()) {
                    prefs.apiKey = key
                    Log.d("KeySyncService", "Groq key updated via Data Layer ✅")
                }
            }
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        Log.d("KeySyncService", "Message Received: ${messageEvent.path}")
        if (messageEvent.path == "/sync_key") {
            val key = String(messageEvent.data)
            if (key.isNotEmpty()) {
                PreferencesManager(this).apiKey = key
                Log.d("KeySyncService", "Groq key updated via Message API ✅")
            }
        }
    }
}
