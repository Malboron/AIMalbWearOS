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
    private var phraseOffsets: List<Int> = emptyList()
    
    private var ttsManager: TtsManager? = null
    private var cloudTtsManager: CloudTtsManager? = null
    private var screenStayAwakeJob: Job? = null
    private var estimatedSyncJob: Job? = null
    
    private var _repository: AiRepository? = null
    private val repository: AiRepository
        get() = _repository ?: throw IllegalStateException("Repository not initialized.")

    private var preferencesManager: PreferencesManager? = null
    private var hasAutoListened = false

    private val _availableModels = mutableStateOf<List<String>>(listOf(
        "openai/gpt-oss-120b",
        "llama-3.3-70b-versatile",
        "llama-3.1-8b-instant",
        "mixtral-8x22b-instruct"
    ))
    val availableModels: State<List<String>> = _availableModels

    // v1.6.0: Список качественных мультиязычных голосов
    val edgeVoices = listOf(
        "en-US-AvaMultilingualNeural",
        "en-US-AndrewMultilingualNeural",
        "en-US-EmmaMultilingualNeural",
        "en-US-BrianMultilingualNeural",
        "de-DE-SeraphinaMultilingualNeural",
        "fr-FR-RemyMultilingualNeural"
    )

    fun init(context: Context) {
        if (preferencesManager == null) {
            preferencesManager = PreferencesManager(context)
        }
        
        if (ttsManager == null) {
            ttsManager = TtsManager(
                context,
                onCharacterReached = { charIndex ->
                    syncTextToCharacter(charIndex)
                },
                onAllCompleted = {
                    viewModelScope.launch(Dispatchers.Main) {
                        if (getTtsProvider() == "system") {
                            _playbackState.value = PlaybackState.FINISHED
                            startScreenTimeoutCountdown(10000)
                        }
                    }
                }
            )
        }

        if (cloudTtsManager == null) {
            cloudTtsManager = CloudTtsManager(
                context,
                onStart = {
                    viewModelScope.launch(Dispatchers.Main) {
                        ttsManager?.stop()
                        _playbackState.value = PlaybackState.PLAYING
                        if (_appState.value == AppState.LOADING) {
                            _appState.value = AppState.RESPONDING
                            _currentIndex.intValue = 0
                            _visiblePhrases.value = allPhrases
                        }
                        // v1.6.3: Запуск расчетной синхронизации при старте облачного голоса
                        startEstimatedSync()
                    }
                },
                onCompleted = {
                    viewModelScope.launch(Dispatchers.Main) {
                        _playbackState.value = PlaybackState.FINISHED
                        startScreenTimeoutCountdown(10000)
                        estimatedSyncJob?.cancel()
                    }
                },
                onError = { error ->
                    viewModelScope.launch(Dispatchers.Main) {
                        Log.e("MainViewModel", "Cloud TTS Error: $error")
                        if (_appState.value == AppState.LOADING) {
                            _statusText.value = ""
                            _appState.value = AppState.RESPONDING
                            _currentIndex.intValue = 0
                            _visiblePhrases.value = allPhrases
                            _playbackState.value = PlaybackState.FINISHED
                        }
                        estimatedSyncJob?.cancel()
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
            
            refreshModels()
        }
    }

    /**
     * v1.6.8: Оптимизированная расчетная синхронизация.
     * Базовая скорость увеличена до 17.5 для исключения отставания.
     */
    private fun startEstimatedSync() {
        estimatedSyncJob?.cancel()
        estimatedSyncJob = viewModelScope.launch(Dispatchers.Main) {
            // v1.6.8: Калибровка под реальную скорость Ava (17.5 симв/сек)
            val baseCharsPerSecond = 17.5f
            val startTime = System.currentTimeMillis()
            var accumulatedTargetMs = 0L
            
            for (i in allPhrases.indices) {
                _currentIndex.intValue = i
                
                val speed = getTtsSpeed()
                val adjustedCharsPerMs = (baseCharsPerSecond * speed) / 1000.0f
                
                val phraseLength = allPhrases[i].length
                val phraseDurationMs = (phraseLength / adjustedCharsPerMs).toLong()
                
                accumulatedTargetMs += phraseDurationMs
                
                val currentTime = System.currentTimeMillis()
                val delayTime = (startTime + accumulatedTargetMs) - currentTime
                
                if (delayTime > 0) {
                    delay(delayTime)
                }
                
                if (_playbackState.value != PlaybackState.PLAYING) break
            }
        }
    }

    private fun syncTextToCharacter(charIndex: Int) {
        viewModelScope.launch(Dispatchers.Main) {
            if (phraseOffsets.isEmpty()) return@launch
            val segmentIndex = phraseOffsets.indexOfLast { it <= charIndex }
            if (segmentIndex >= 0 && segmentIndex != _currentIndex.intValue) {
                _currentIndex.intValue = segmentIndex
                if (_appState.value == AppState.LOADING) {
                    _appState.value = AppState.RESPONDING
                    _visiblePhrases.value = allPhrases
                }
            }
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
    fun getAppLanguage() = preferencesManager?.appLanguage ?: "system"
    fun setAppLanguage(value: String) { preferencesManager?.appLanguage = value }

    fun getTtsProvider() = preferencesManager?.ttsProvider ?: PreferencesManager.DEFAULT_TTS_PROVIDER
    fun setTtsProvider(value: String) { preferencesManager?.ttsProvider = value }
    
    fun getTtsSpeed() = preferencesManager?.ttsSpeed ?: PreferencesManager.DEFAULT_TTS_SPEED
    fun setTtsSpeed(value: Float) { preferencesManager?.ttsSpeed = value }
    
    fun getEdgeVoice() = preferencesManager?.edgeVoice ?: PreferencesManager.DEFAULT_EDGE_VOICE
    fun setEdgeVoice(value: String) { preferencesManager?.edgeVoice = value }

    fun refreshModels() {
        val key = getApiKey()
        if (key.isBlank()) return
        
        viewModelScope.launch {
            repository.getAvailableModels(key).onSuccess { models ->
                if (models.isNotEmpty()) {
                    _availableModels.value = models
                    val current = getModel()
                    if (current !in models) {
                        setModel(models.first())
                    }
                }
            }
        }
    }

    fun triggerAutoListenIfNeeded() {
        if (!hasAutoListened && getAutoListen()) {
            hasAutoListened = true
            _shouldTriggerVoice.value = true
        }
    }

    fun triggerVoiceManually() { _shouldTriggerVoice.value = true }
    
    fun triggerVoiceDirectly() {
        reset()
        _shouldTriggerVoice.value = true
    }

    fun onVoiceTriggerConsumed() { _shouldTriggerVoice.value = false }

    fun verifyKey() {
        val key = getApiKey()
        if (key.isBlank()) {
            _verificationStatus.value = VerificationStatus.Error("Key empty")
            return
        }
        _verificationStatus.value = VerificationStatus.Verifying
        viewModelScope.launch {
            repository.verifyApiKey(key).fold(
                onSuccess = { 
                    _verificationStatus.value = VerificationStatus.Success
                    refreshModels()
                },
                onFailure = { _verificationStatus.value = VerificationStatus.Error("Fail") }
            )
        }
    }

    fun onVoiceInputReceived(text: String) {
        if (text.isBlank()) return
        stopScreenTimeoutCountdown()
        _isScreenLockActive.value = true
        _appState.value = AppState.LOADING
        _responseText.value = ""
        _statusText.value = "start_status"
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
                ) { status -> 
                    _statusText.value = status
                }
                startResponding(aiResponse)
            } catch (e: Exception) {
                _responseText.value = e.message ?: "Error"
                _appState.value = AppState.IDLE
                startScreenTimeoutCountdown(5000)
            }
        }
    }

    private fun startResponding(text: String) {
        stopScreenTimeoutCountdown()
        _isScreenLockActive.value = true
        
        val sanitizedText = text.replace("\\n", "\n").replace("/n", "\n")
        _responseText.value = sanitizedText
        val result = smartSplit(sanitizedText)
        allPhrases = result.first
        phraseOffsets = result.second
        _visiblePhrases.value = allPhrases
        _currentIndex.intValue = 0

        val provider = getTtsProvider()
        val speed = getTtsSpeed()
        
        _playbackState.value = PlaybackState.PLAYING
        
        when (provider) {
            "edge" -> {
                _statusText.value = "preparing_voice"
                ttsManager?.stop()
                cloudTtsManager?.speakEdge(sanitizedText, getEdgeVoice(), speed)
            }
            else -> {
                cloudTtsManager?.stop()
                ttsManager?.speak(sanitizedText, speed)
            }
        }
    }

    /**
     * v1.6.8: Улучшенная разбивка текста с гарантированным захватом "хвоста".
     */
    private fun smartSplit(text: String): Pair<List<String>, List<Int>> {
        val segments = mutableListOf<String>()
        val offsets = mutableListOf<Int>()
        
        // v1.6.8: Используем split с сохранением разделителей для точного маппинга
        val rawSentences = text.split(Regex("(?<=[.!?])\\s+|\\n")).filter { it.isNotBlank() }
        
        var lastSearchIndex = 0
        for (sentence in rawSentences) {
            val sTrim = sentence.trim()
            if (sTrim.isEmpty()) continue
            
            val startIdx = text.indexOf(sTrim, lastSearchIndex)
            if (startIdx == -1) continue
            lastSearchIndex = startIdx + sTrim.length
            
            if (sTrim.length <= 45) {
                segments.add(sTrim)
                offsets.add(startIdx)
            } else {
                val words = sTrim.split(" ")
                var currentChunk = StringBuilder()
                var currentChunkOffset = startIdx
                for (word in words) {
                    val wordToAppend = if (currentChunk.isEmpty()) word else " $word"
                    if (currentChunk.length + wordToAppend.length > 45 && currentChunk.isNotEmpty()) {
                        segments.add(currentChunk.toString())
                        offsets.add(currentChunkOffset)
                        
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
        
        // Гарантированная проверка на "хвост", если он был пропущен
        if (segments.isEmpty() && text.isNotBlank()) {
            segments.add(text.trim())
            offsets.add(0)
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
        val provider = getTtsProvider()
        val speed = getTtsSpeed()
        when (_playbackState.value) {
            PlaybackState.PLAYING -> {
                estimatedSyncJob?.cancel()
                if (provider == "system") ttsManager?.pause() 
                else cloudTtsManager?.stop() 
                
                _playbackState.value = PlaybackState.PAUSED
                startScreenTimeoutCountdown(15000)
            }
            PlaybackState.PAUSED -> {
                stopScreenTimeoutCountdown()
                _isScreenLockActive.value = true
                
                if (provider == "system") ttsManager?.resume() 
                else {
                    // При возобновлении облака пересоздаем поток
                    startResponding(_responseText.value)
                }
                
                _playbackState.value = PlaybackState.PLAYING
            }
            PlaybackState.FINISHED -> {
                // v1.6.8: Оптимизированный повтор БЕЗ скачивания
                stopScreenTimeoutCountdown()
                _isScreenLockActive.value = true
                _playbackState.value = PlaybackState.PLAYING
                _currentIndex.intValue = 0
                
                if (provider == "edge") {
                    cloudTtsManager?.restart()
                } else {
                    ttsManager?.restart()
                }
            }
        }
    }

    fun reset() {
        stopScreenTimeoutCountdown()
        estimatedSyncJob?.cancel()
        ttsManager?.stop()
        cloudTtsManager?.stop()
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
        estimatedSyncJob?.cancel()
        ttsManager?.release()
        cloudTtsManager?.release()
    }
}
