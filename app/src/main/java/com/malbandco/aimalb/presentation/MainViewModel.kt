package com.malbandco.aimalb.presentation

import android.content.Context
import android.util.Log
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

/**
 * Состояния основного экрана приложения
 */
enum class AppState {
    IDLE,       
    LOADING,    
    RESPONDING  
}

/**
 * Состояния воспроизведения голоса
 */
enum class PlaybackState {
    PLAYING, PAUSED, FINISHED
}

sealed class VerificationStatus {
    object Idle : VerificationStatus()
    object Verifying : VerificationStatus()
    object Success : VerificationStatus()
    data class Error(val message: String) : VerificationStatus()
}

/**
 * Главная ViewModel: управляет логикой, состояниями и связью между UI и данными.
 * v1.2.1: Монолитная озвучка и синхронизация по символам.
 */
class MainViewModel : ViewModel() {

    private val _appState = mutableStateOf(AppState.IDLE)
    val appState: State<AppState> = _appState

    private val _playbackState = mutableStateOf(PlaybackState.FINISHED)
    val playbackState: State<PlaybackState> = _playbackState

    private val _responseText = mutableStateOf("")
    val responseText: State<String> = _responseText

    private val _statusText = mutableStateOf("")
    val statusText: State<String> = _statusText

    private val _visiblePhrases = mutableStateOf<List<String>>(emptyList())
    val visiblePhrases: State<List<String>> = _visiblePhrases

    private val _currentIndex = mutableIntStateOf(-1)
    val currentIndex: State<Int> = _currentIndex

    private val _isScreenLockActive = mutableStateOf(false)
    val isScreenLockActive: State<Boolean> = _isScreenLockActive

    private val _verificationStatus = mutableStateOf<VerificationStatus>(VerificationStatus.Idle)
    val verificationStatus: State<VerificationStatus> = _verificationStatus

    private val _shouldTriggerVoice = mutableStateOf(false)
    val shouldTriggerVoice: State<Boolean> = _shouldTriggerVoice

    private var allPhrases: List<String> = emptyList()
    private var phraseOffsets: List<Int> = emptyList() // v1.2.1: Начальные индексы символов для сегментов
    
    private var ttsManager: TtsManager? = null
    private var screenStayAwakeJob: Job? = null
    
    private var _repository: AiRepository? = null
    private val repository: AiRepository
        get() = _repository ?: throw IllegalStateException("Repository not initialized.")

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
                onCharacterReached = { charIndex ->
                    viewModelScope.launch(Dispatchers.Main) {
                        // Находим индекс сегмента, которому принадлежит текущий символ
                        val segmentIndex = phraseOffsets.indexOfLast { it <= charIndex }
                        if (segmentIndex >= 0 && segmentIndex != _currentIndex.intValue) {
                            _currentIndex.intValue = segmentIndex
                            _playbackState.value = PlaybackState.PLAYING
                        }
                    }
                },
                onAllCompleted = {
                    viewModelScope.launch(Dispatchers.Main) {
                        _playbackState.value = PlaybackState.FINISHED
                        startScreenTimeoutCountdown(10000)
                    }
                }
            )
        }

        if (_repository == null) {
            val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .addInterceptor { chain ->
                    val original = chain.request()
                    val requestBuilder = original.newBuilder()
                    requestBuilder.header("User-Agent", "AIMalb/1.0 WearOS")
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

    fun triggerVoiceManually() { _shouldTriggerVoice.value = true }
    
    /**
     * v1.2.3: Мгновенный запуск микрофона с экрана ответа.
     */
    fun triggerVoiceDirectly() {
        ttsManager?.stop()
        _currentIndex.intValue = -1
        _playbackState.value = PlaybackState.FINISHED
        _shouldTriggerVoice.value = true
    }

    fun onVoiceTriggerConsumed() { _shouldTriggerVoice.value = false }

    fun verifyKey() {
        val key = getApiKey()
        if (key.isBlank()) {
            _verificationStatus.value = VerificationStatus.Error("Ключ пуст")
            return
        }
        _verificationStatus.value = VerificationStatus.Verifying
        viewModelScope.launch {
            repository.verifyApiKey(key).fold(
                onSuccess = { _verificationStatus.value = VerificationStatus.Success },
                onFailure = { _verificationStatus.value = VerificationStatus.Error("Сбой") }
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
        _playbackState.value = PlaybackState.FINISHED
        
        viewModelScope.launch {
            try {
                val aiResponse = repository.getChatCompletion(
                    userText = text,
                    apiKey = getApiKey(),
                    model = getModel(),
                    systemPromptTemplate = getSystemPrompt()
                ) { status -> _statusText.value = status }
                startResponding(aiResponse)
            } catch (e: Exception) {
                _responseText.value = "Ошибка: ${e.message}"
                _appState.value = AppState.IDLE
                startScreenTimeoutCountdown(5000)
            }
        }
    }

    /**
     * v1.2.1: Мгновенное переключение экрана и умная сегментация.
     */
    private fun startResponding(text: String) {
        stopScreenTimeoutCountdown()
        _isScreenLockActive.value = true
        
        val sanitizedText = text.replace("\\n", "\n").replace("/n", "\n")
        _responseText.value = sanitizedText
        
        // Умная сегментация (12-40 символов)
        val result = smartSplit(sanitizedText)
        allPhrases = result.first
        phraseOffsets = result.second
        
        _visiblePhrases.value = allPhrases
        _currentIndex.intValue = 0
        
        // Мгновенно переходим к ответу
        _appState.value = AppState.RESPONDING
        
        // Запуск монолитного голоса
        ttsManager?.speak(sanitizedText)
    }

    /**
     * Алгоритм разбиения текста на сегменты.
     * v1.2.2: Приоритет - одно предложение на строку (лимит 15-45 символов).
     */
    private fun smartSplit(text: String): Pair<List<String>, List<Int>> {
        val segments = mutableListOf<String>()
        val offsets = mutableListOf<Int>()
        
        // Разделяем по знакам препинания (предложениям) и переносам строк
        val rawSentences = text.split(Regex("(?<=[.!?])\\s+|\\n")).filter { it.isNotBlank() }
        var lastSearchIndex = 0

        for (sentence in rawSentences) {
            val sTrim = sentence.trim()
            val startIdx = text.indexOf(sTrim, lastSearchIndex)
            if (startIdx == -1) continue
            lastSearchIndex = startIdx + sTrim.length

            if (sTrim.length <= 45) {
                // Одно предложение на строку - это приоритет (даже если оно короче 15)
                segments.add(sTrim)
                offsets.add(startIdx)
            } else {
                // Если предложение слишком длинное (> 45), делим его по словам
                val words = sTrim.split(" ")
                var currentChunk = StringBuilder()
                var currentChunkOffset = startIdx

                for (word in words) {
                    val wordToAppend = if (currentChunk.isEmpty()) word else " $word"
                    
                    if (currentChunk.length + wordToAppend.length > 45 && currentChunk.isNotEmpty()) {
                        segments.add(currentChunk.toString())
                        offsets.add(currentChunkOffset)
                        
                        // Смещение для следующего куска внутри того же предложения
                        val nextWordIdx = text.indexOf(word, currentChunkOffset + 1)
                        currentChunk = StringBuilder(word)
                        currentChunkOffset = if (nextWordIdx != -1) nextWordIdx else currentChunkOffset
                    } else {
                        currentChunk.append(wordToAppend)
                    }
                }
                if (currentChunk.isNotEmpty()) {
                    segments.add(currentChunk.toString())
                    offsets.add(currentChunkOffset)
                }
            }
        }
        return Pair(segments, offsets)
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
        when (_playbackState.value) {
            PlaybackState.PLAYING -> {
                ttsManager?.pause()
                _playbackState.value = PlaybackState.PAUSED
                startScreenTimeoutCountdown(15000)
            }
            PlaybackState.PAUSED -> {
                stopScreenTimeoutCountdown()
                _isScreenLockActive.value = true
                ttsManager?.resume()
                _playbackState.value = PlaybackState.PLAYING
            }
            PlaybackState.FINISHED -> {
                stopScreenTimeoutCountdown()
                _isScreenLockActive.value = true
                ttsManager?.speak(_responseText.value)
            }
        }
    }

    fun reset() {
        stopScreenTimeoutCountdown()
        ttsManager?.stop()
        _appState.value = AppState.IDLE
        _responseText.value = ""
        _statusText.value = ""
        _visiblePhrases.value = emptyList()
        _currentIndex.intValue = -1
        _playbackState.value = PlaybackState.FINISHED
        startScreenTimeoutCountdown(5000)
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager?.release()
    }
}
