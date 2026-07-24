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
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.dnsoverhttps.DnsOverHttps
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit

sealed class SyncStatus {
    object Idle : SyncStatus()
    object Verifying : SyncStatus()
    object Syncing : SyncStatus()
    object Success : SyncStatus()
    data class Error(val message: String) : SyncStatus()
}

class CompanionViewModel(application: Application) : AndroidViewModel(application) {

    private val _syncStatus = mutableStateOf<SyncStatus>(SyncStatus.Idle)
    val syncStatus: State<SyncStatus> = _syncStatus

    private val prefs = CompanionPrefs(application)

    private val api: CompanionGroqApi by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        val clientBuilder = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)

        if (prefs.useDoH) {
            val appClient = OkHttpClient.Builder().build()
            
            // Primary: Quad9 (9.9.9.9)
            val dns = DnsOverHttps.Builder()
                .client(appClient)
                .url("https://dns.quad9.net/dns-query".toHttpUrl())
                .bootstrapDnsHosts(listOf(
                    InetAddress.getByName("9.9.9.9"),
                    InetAddress.getByName("149.112.112.112")
                ))
                .build()
            
            // If Quad9 is blocked, the request will fail. 
            // For true fallback we'd need a custom Dns implementation.
            // For now, using Quad9 as primary confirmed working in 2026.
            clientBuilder.dns(dns)
        }

        if (prefs.proxyHost.isNotEmpty()) {
            val port = prefs.proxyPort.toIntOrNull() ?: 8080
            val proxy = Proxy(Proxy.Type.HTTP, InetSocketAddress(prefs.proxyHost, port))
            clientBuilder.proxy(proxy)
            
            if (prefs.proxyUser.isNotEmpty()) {
                val authenticator = Authenticator { _, response ->
                    val credential = Credentials.basic(prefs.proxyUser, prefs.proxyPass)
                    response.request.newBuilder()
                        .header("Proxy-Authorization", credential)
                        .build()
                }
                clientBuilder.proxyAuthenticator(authenticator)
            }
        }

        val client = clientBuilder.build()

        Retrofit.Builder()
            .baseUrl("https://api.groq.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(CompanionGroqApi::class.java)
    }

    fun getProxyHost() = prefs.proxyHost
    fun setProxyHost(v: String) { prefs.proxyHost = v }
    fun getProxyPort() = prefs.proxyPort
    fun setProxyPort(v: String) { prefs.proxyPort = v }
    fun getProxyUser() = prefs.proxyUser
    fun setProxyUser(v: String) { prefs.proxyUser = v }
    fun getProxyPass() = prefs.proxyPass
    fun setProxyPass(v: String) { prefs.proxyPass = v }
    fun getUseDoH() = prefs.useDoH
    fun setUseDoH(v: Boolean) { prefs.useDoH = v }

    fun verifyKey(key: String) {
        if (key.isBlank()) {
            _syncStatus.value = SyncStatus.Error("Key is empty")
            return
        }

        _syncStatus.value = SyncStatus.Verifying
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val authHeader = if (key.startsWith("Bearer ")) key else "Bearer $key"
                api.verifyKey(authHeader)
                _syncStatus.value = SyncStatus.Success // Show success if verification passed
            } catch (e: Exception) {
                Log.e("CompanionVM", "Verification failed", e)
                _syncStatus.value = SyncStatus.Error("Verification failed: ${e.message}")
            }
        }
    }

    fun syncToWatch(key: String) {
        if (key.isBlank()) {
            _syncStatus.value = SyncStatus.Error("Key is empty")
            return
        }
        
        _syncStatus.value = SyncStatus.Syncing
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dataClient = Wearable.getDataClient(getApplication<Application>())
                val putDataReq = PutDataMapRequest.create("/groq_key").apply {
                    dataMap.putString("key", key)
                }.asPutDataRequest()
                putDataReq.setUrgent()
                dataClient.putDataItem(putDataReq).await()
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
