package com.malbandco.phonecompanionmodule.presentation

import android.app.Application
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.malbandco.phonecompanionmodule.data.CompanionGroqApi
import com.malbandco.phonecompanionmodule.data.local.CompanionPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

sealed class SyncStatus {
    object Idle : SyncStatus()
    object Verifying : SyncStatus()
    object Syncing : SyncStatus()
    object Success : SyncStatus()
    data class Error(val message: String) : SyncStatus()
}

/**
 * ViewModel компаньона.
 * v1.2.6: Удалена логика прокси и DNS, поддержка полного промпта и локализации.
 */
class CompanionViewModel(application: Application) : AndroidViewModel(application) {

    private val _syncStatus = mutableStateOf<SyncStatus>(SyncStatus.Idle)
    val syncStatus: State<SyncStatus> = _syncStatus

    private val prefs = CompanionPrefs(application)

    private val api: CompanionGroqApi by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl("https://api.groq.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(CompanionGroqApi::class.java)
    }

    fun getSystemPrompt() = prefs.systemPrompt
    fun setSystemPrompt(v: String) { prefs.systemPrompt = v }
    fun resetSystemPrompt() { prefs.resetPrompt() }

    fun getAppLanguage() = prefs.appLanguage
    fun setAppLanguage(v: String) { prefs.appLanguage = v }

    fun verifyKey(key: String) {
        if (key.isBlank()) {
            _syncStatus.value = SyncStatus.Error("Key empty")
            return
        }

        _syncStatus.value = SyncStatus.Verifying
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val authHeader = if (key.startsWith("Bearer ")) key else "Bearer $key"
                api.verifyKey(authHeader)
                _syncStatus.value = SyncStatus.Success
            } catch (e: Exception) {
                Log.e("CompanionVM", "Verification failed", e)
                _syncStatus.value = SyncStatus.Error("Fail: ${e.message}")
            }
        }
    }

    /**
     * Синхронизация всех данных (Ключ + Полный Промпт) с часами.
     */
    fun syncToWatch(key: String) {
        if (key.isBlank()) {
            _syncStatus.value = SyncStatus.Error("Key empty")
            return
        }
        
        _syncStatus.value = SyncStatus.Syncing
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dataClient = Wearable.getDataClient(getApplication<Application>())
                val messageClient = Wearable.getMessageClient(getApplication<Application>())
                val nodeClient = Wearable.getNodeClient(getApplication<Application>())
                val currentPrompt = prefs.systemPrompt

                // 1. Data Layer Sync
                val putDataReq = PutDataMapRequest.create("/sync_data").apply {
                    dataMap.putString("key", key)
                    dataMap.putString("prompt", currentPrompt)
                    dataMap.putLong("timestamp", System.currentTimeMillis())
                }.asPutDataRequest()
                putDataReq.setUrgent()
                dataClient.putDataItem(putDataReq).await()

                // 2. Message API Sync
                val nodes = nodeClient.connectedNodes.await()
                nodes.forEach { node ->
                    messageClient.sendMessage(node.id, "/sync_key", key.toByteArray()).await()
                    messageClient.sendMessage(node.id, "/sync_prompt", currentPrompt.toByteArray()).await()
                }
                
                _syncStatus.value = SyncStatus.Success
            } catch (e: Exception) {
                Log.e("CompanionVM", "Sync failed", e)
                _syncStatus.value = SyncStatus.Error("Sync failed: ${e.message}")
            }
        }
    }

    fun resetStatus() {
        _syncStatus.value = SyncStatus.Idle
    }
}
