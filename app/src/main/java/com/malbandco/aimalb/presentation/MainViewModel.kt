package com.malbandco.aimalb.presentation

import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.malbandco.aimalb.data.AiRepository
import com.malbandco.aimalb.data.local.PreferencesManager
import com.malbandco.aimalb.data.remote.GroqApi
import com.malbandco.aimalb.data.remote.SearxngApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

enum class AppState {
    IDLE, LOADING, RESPONDING
}

sealed class VerificationStatus {
    object Idle : VerificationStatus()
    object Verifying : VerificationStatus()
    object Success : VerificationStatus()
    data class Error(val message: String) : VerificationStatus()
}

class MainViewModel : ViewModel() {

    private val _appState = mutableStateOf(AppState.IDLE)
    val appState: State<AppState> = _appState

    private val _responseText = mutableStateOf("")
    val responseText: State<String> = _responseText

    private val _statusText = mutableStateOf("")
    val statusText: State<String> = _statusText

    private val _visiblePhrases = mutableStateOf<List<String>>(emptyList())
    val visiblePhrases: State<List<String>> = _visiblePhrases

    private val _currentIndex = mutableIntStateOf(-1)
    val currentIndex: State<Int> = _currentIndex

    private val _isPaused = mutableStateOf(false)
    val isPaused: State<Boolean> = _isPaused

    private val _isScreenLockActive = mutableStateOf(false)
    val isScreenLockActive: State<Boolean> = _isScreenLockActive

    private val _verificationStatus = mutableStateOf<VerificationStatus>(VerificationStatus.Idle)
    val verificationStatus: State<VerificationStatus> = _verificationStatus

    private val _shouldTriggerVoice = mutableStateOf(false)
    val shouldTriggerVoice: State<Boolean> = _shouldTriggerVoice

    private var allPhrases: List<String> = emptyList()
    private var ttsManager: TtsManager? = null
    private var screenStayAwakeJob: Job? = null
    
    private var _repository: AiRepository? = null
    private val repository: AiRepository
        get() = _repository ?: throw IllegalStateException("Repository not initialized. Call init(context) first.")

    private var preferencesManager: PreferencesManager? = null
    private var hasAutoListened = false

    val availableModels = listOf(
        "openai/gpt-oss-120b",
        "openai/gpt-oss-20b",
        "llama-3.3-70b-versatile",
        "llama-3.1-8b-instant",
        "mixtral-8x22b-instruct"
    )

    fun init(context: Context) {
        if (preferencesManager == null) {
            preferencesManager = PreferencesManager(context)
        }
        
        if (ttsManager == null) {
            ttsManager = TtsManager(
                context,
                onPhraseCompleted = { index ->
                    viewModelScope.launch(Dispatchers.Main) {
                        _visiblePhrases.value = allPhrases.take(index + 1)
                        _currentIndex.intValue = index
                    }
                },
                onAllCompleted = {
                    startScreenTimeoutCountdown(10000)
                }
            )
        }

        if (_repository == null) {
            _isScreenLockActive.value = true
            startScreenTimeoutCountdown(10000)

            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .addInterceptor { chain ->
                    val original = chain.request()
                    val requestBuilder = original.newBuilder()
                    
                    val urlString = original.url.toString()
                    if (urlString.contains("duckduckgo.com")) {
                        requestBuilder
                            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/115.0")
                            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
                            .header("Accept-Language", "en-US,en;q=0.5")
                            .header("Referer", "https://lite.duckduckgo.com/")
                            .header("Upgrade-Insecure-Requests", "1")
                            .header("Sec-Fetch-Dest", "document")
                            .header("Sec-Fetch-Mode", "navigate")
                            .header("Sec-Fetch-Site", "same-origin")
                    } else {
                        requestBuilder.header("User-Agent", "AIMalb/1.0 WearOS")
                    }
                    chain.proceed(requestBuilder.build())
                }
                .build()

            val groqRetrofit = Retrofit.Builder()
                .baseUrl("https://api.groq.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build()

            val searxngRetrofit = Retrofit.Builder()
                .baseUrl("https://searx.be/") 
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build()

            _repository = AiRepository(
                context = context,
                groqApi = groqRetrofit.create(GroqApi::class.java),
                searxngApi = searxngRetrofit.create(SearxngApi::class.java)
            )
        }
    }

    fun getApiKey() = preferencesManager?.apiKey ?: ""
    fun setApiKey(key: String) { preferencesManager?.apiKey = key }
    
    fun getModel() = preferencesManager?.model ?: PreferencesManager.DEFAULT_MODEL
    fun setModel(model: String) { preferencesManager?.model = model }
    
    fun getSystemPrompt() = preferencesManager?.systemPrompt ?: PreferencesManager.DEFAULT_PROMPT
    fun setSystemPrompt(prompt: String) { preferencesManager?.systemPrompt = prompt }
    fun resetSystemPrompt() { preferencesManager?.resetPrompt() }

    fun getAutoListen() = preferencesManager?.autoListenOnOpen ?: false
    fun setAutoListen(value: Boolean) { preferencesManager?.autoListenOnOpen = value }

    fun getLongPressEnabled() = preferencesManager?.longPressShortcutEnabled ?: false
    fun setLongPressEnabled(value: Boolean) { preferencesManager?.longPressShortcutEnabled = value }

    fun triggerAutoListenIfNeeded() {
        if (!hasAutoListened && getAutoListen()) {
            hasAutoListened = true
            _shouldTriggerVoice.value = true
        }
    }

    fun triggerVoiceManually() {
        _shouldTriggerVoice.value = true
    }

    fun onVoiceTriggerConsumed() {
        _shouldTriggerVoice.value = false
    }

    fun verifyKey() {
        val key = getApiKey()
        if (key.isBlank()) {
            _verificationStatus.value = VerificationStatus.Error("Key is empty")
            return
        }

        _verificationStatus.value = VerificationStatus.Verifying
        viewModelScope.launch {
            repository.verifyApiKey(key).fold(
                onSuccess = { _verificationStatus.value = VerificationStatus.Success },
                onFailure = { _verificationStatus.value = VerificationStatus.Error(it.message ?: "Failed") }
            )
        }
    }

    fun onVoiceInputReceived(text: String) {
        if (text.isBlank()) return
        
        stopScreenTimeoutCountdown()
        _isScreenLockActive.value = true
        
        _appState.value = AppState.LOADING
        _responseText.value = ""
        _statusText.value = "Старт..."
        _visiblePhrases.value = emptyList()
        _currentIndex.intValue = -1
        _isPaused.value = false
        
        viewModelScope.launch {
            try {
                val aiResponse = repository.getChatCompletion(
                    userText = text,
                    apiKey = getApiKey(),
                    model = getModel(),
                    systemPromptTemplate = getSystemPrompt()
                ) { status ->
                    _statusText.value = status
                }
                startResponding(aiResponse)
            } catch (e: Exception) {
                _responseText.value = "Error: ${e.message ?: "Unknown error"}"
                _appState.value = AppState.IDLE
                startScreenTimeoutCountdown(5000)
            }
        }
    }

    private fun startResponding(text: String) {
        stopScreenTimeoutCountdown()
        _isScreenLockActive.value = true
        _appState.value = AppState.RESPONDING
        
        val sanitizedText = text.replace("\\n", "\n").replace("/n", "\n")
        _responseText.value = sanitizedText
        
        _currentIndex.intValue = -1
        _visiblePhrases.value = emptyList()
        
        allPhrases = sanitizedText.split(Regex("(?<=[.!?])\\s+|\\n")).filter { it.isNotBlank() }
        ttsManager?.speak(sanitizedText)
    }

    private fun startScreenTimeoutCountdown(millis: Long) {
        stopScreenTimeoutCountdown()
        screenStayAwakeJob = viewModelScope.launch {
            delay(millis)
            _isScreenLockActive.value = false
        }
    }

    private fun stopScreenTimeoutCountdown() {
        screenStayAwakeJob?.cancel()
        screenStayAwakeJob = null
    }

    fun togglePauseResume() {
        if (_isPaused.value) {
            stopScreenTimeoutCountdown()
            _isScreenLockActive.value = true
            ttsManager?.resume()
            _isPaused.value = false
        } else {
            ttsManager?.pause()
            _isPaused.value = true
            startScreenTimeoutCountdown(15000)
        }
    }

    fun reset() {
        stopScreenTimeoutCountdown()
        ttsManager?.stop()
        _appState.value = AppState.IDLE
        _responseText.value = ""
        _visiblePhrases.value = emptyList()
        _currentIndex.intValue = -1
        _isPaused.value = false
        startScreenTimeoutCountdown(5000)
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager?.release()
    }
}
