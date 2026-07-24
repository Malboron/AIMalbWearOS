package com.malbandco.aimalb.presentation

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class TtsManager(
    context: Context,
    private val onPhraseCompleted: (Int) -> Unit,
    private val onAllCompleted: () -> Unit
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech = TextToSpeech(context, this)
    private var isInitialized = false
    private var phrases: List<String> = emptyList()
    private var isPaused = false
    private var currentPlayingIndex = -1
    private val handler = Handler(Looper.getMainLooper())

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.getDefault()
            isInitialized = true
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    utteranceId?.toIntOrNull()?.let { index ->
                        currentPlayingIndex = index
                        onPhraseCompleted(index)
                    }
                }
                
                override fun onDone(utteranceId: String?) {
                    utteranceId?.toIntOrNull()?.let { index ->
                        if (index == phrases.size - 1) {
                            onAllCompleted()
                        } else if (!isPaused) {
                            // Reduced to 50ms for extreme speed as requested
                            handler.postDelayed({
                                speakNext(index + 1)
                            }, 50)
                        }
                    }
                }

                override fun onError(utteranceId: String?) {}
            })
        }
    }

    fun speak(text: String) {
        phrases = text.split(Regex("(?<=[.!?])\\s+|\\n")).filter { it.isNotBlank() }
        currentPlayingIndex = -1
        isPaused = false
        handler.removeCallbacksAndMessages(null)
        tts.stop()
        
        // Zero delay start as requested in v46
        speakNext(0)
    }

    private fun speakNext(index: Int) {
        if (!isInitialized || isPaused || index >= phrases.size) return
        tts.speak(phrases[index], TextToSpeech.QUEUE_FLUSH, null, index.toString())
    }

    fun pause() {
        isPaused = true
        handler.removeCallbacksAndMessages(null)
        tts.stop()
    }

    fun resume() {
        if (isPaused) {
            isPaused = false
            val resumeIndex = if (currentPlayingIndex >= 0) currentPlayingIndex else 0
            speakNext(resumeIndex)
        }
    }

    fun stop() {
        isPaused = false
        currentPlayingIndex = -1
        phrases = emptyList()
        handler.removeCallbacksAndMessages(null)
        tts.stop()
    }

    fun release() {
        handler.removeCallbacksAndMessages(null)
        tts.stop()
        tts.shutdown()
    }
}
