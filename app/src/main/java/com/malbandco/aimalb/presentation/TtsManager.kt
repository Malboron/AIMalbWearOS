package com.malbandco.aimalb.presentation

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/**
 * Менеджер управления синтезом речи (TTS). 
 * v1.2.1: Монолитная озвучка (весь текст за один вызов) для исключения пауз на реальных часах.
 */
class TtsManager(
    context: Context,
    private val onCharacterReached: (Int) -> Unit, // v1.2.1: Передаем индекс текущего символа
    private val onAllCompleted: () -> Unit
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech = TextToSpeech(context, this)
    private var isInitialized = false
    private var currentText: String = ""
    private var isPaused = false

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.getDefault()
            isInitialized = true
            
            // v1.2.4: Разогрев движка пустой строкой
            tts.speak(" ", TextToSpeech.QUEUE_FLUSH, null, "priming")
            
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    // Сигнализируем о начале воспроизведения всего монолита
                    onCharacterReached(0)
                }
                
                override fun onDone(utteranceId: String?) {
                    onAllCompleted()
                }

                override fun onError(utteranceId: String?) {}

                /**
                 * v1.2.1: Критически важный метод для синхронизации текста с монолитным звуком.
                 * Вызывается системой при начале произношения определенного диапазона символов.
                 */
                override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                    onCharacterReached(start)
                }
            })
        }
    }

    /**
     * Зачитать весь текст одним потоком.
     */
    fun speak(text: String) {
        if (!isInitialized) return
        currentText = text
        isPaused = false
        // QUEUE_FLUSH обрывает старое и мгновенно начинает новое
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "monolithic_utterance")
    }

    fun pause() {
        isPaused = true
        tts.stop()
    }

    fun resume() {
        if (isPaused && currentText.isNotEmpty()) {
            isPaused = false
            // К сожалению, системный TTS не умеет возобновлять с середины фразы.
            // При нажатии Плей мы перезапускаем монолит.
            speak(currentText)
        }
    }

    fun stop() {
        isPaused = false
        currentText = ""
        tts.stop()
    }

    fun release() {
        tts.stop()
        tts.shutdown()
    }
}
