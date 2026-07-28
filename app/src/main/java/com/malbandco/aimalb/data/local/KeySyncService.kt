package com.malbandco.aimalb.data.local

import android.util.Log
import androidx.annotation.Keep
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

/**
 * Фоновая служба для синхронизации данных (API-ключ, Промпт) между телефоном и часами.
 * v1.2.3: Добавлена поддержка синхронизации системного промпта.
 */
@Keep
class KeySyncService : WearableListenerService() {
    
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        val prefs = PreferencesManager(this)
        Log.d("KeySyncService", "Data Changed Event Received")
        
        dataEvents.forEach { event ->
            val path = event.dataItem.uri.path
            Log.d("KeySyncService", "Path: $path")
            
            if (event.type == DataEvent.TYPE_CHANGED) {
                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                
                // Обработка нового комплексного пути синхронизации
                if (path?.endsWith("/sync_data") == true) {
                    val key = dataMap.getString("key", "")
                    val prompt = dataMap.getString("prompt", "")
                    
                    if (key.isNotEmpty()) {
                        prefs.apiKey = key
                        Log.d("KeySyncService", "Groq key updated via Data Layer ✅")
                    }
                    if (prompt.isNotEmpty()) {
                        prefs.systemPrompt = prompt
                        Log.d("KeySyncService", "System prompt updated via Data Layer ✅")
                    }
                }
                
                // Совместимость со старым путем (только ключ)
                if (path?.endsWith("/groq_key") == true) {
                    val key = dataMap.getString("key", "")
                    if (key.isNotEmpty()) {
                        prefs.apiKey = key
                        Log.d("KeySyncService", "Groq key updated via Legacy Data Layer ✅")
                    }
                }
            }
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        val prefs = PreferencesManager(this)
        Log.d("KeySyncService", "Message Received: ${messageEvent.path}")
        
        when (messageEvent.path) {
            "/sync_key" -> {
                val key = String(messageEvent.data)
                if (key.isNotEmpty()) {
                    prefs.apiKey = key
                    Log.d("KeySyncService", "Groq key updated via Message API ✅")
                }
            }
            "/sync_prompt" -> {
                val prompt = String(messageEvent.data)
                if (prompt.isNotEmpty()) {
                    prefs.systemPrompt = prompt
                    Log.d("KeySyncService", "System prompt updated via Message API ✅")
                }
            }
        }
    }
}
