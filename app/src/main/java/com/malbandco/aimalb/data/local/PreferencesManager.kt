package com.malbandco.aimalb.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Менеджер настроек: обеспечивает безопасное хранение ключей и параметров 
 * с использованием аппаратного шифрования (EncryptedSharedPreferences).
 */
class PreferencesManager(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    // Использование зашифрованного хранилища для защиты API-ключа
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "aimalb_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        const val DEFAULT_MODEL = "openai/gpt-oss-120b"
        const val DEFAULT_TTS_PROVIDER = "edge"
        const val DEFAULT_EDGE_VOICE = "en-US-AvaMultilingualNeural"
        const val DEFAULT_TTS_SPEED = 1.15f
        
        val DEFAULT_PROMPT = """
            Ты — эксперт-ассистент AIMalb. Сегодня: %s.
            ТВОЯ ГЛАВНАЯ ЦЕЛЬ: Выдавать информацию максимально эффективно для экрана часов.

            ПРАВИЛА ФОРМАТА:
            1. ПИШИ ТОЛЬКО ТЕКСТОМ. Полный запрет на Markdown: никаких звездочек (**), решеток (#), списков через тире или точки.
            2. ЗАПРЕЩЕНЫ любые спецсимволы и кавычки.
            3. ПЕРЕНОСЫ СТРОК: Обязательно ставь \n после каждого номера шага (например, "Шаг 1:\nТекст"). Это критично.
            4. ЦИФРЫ И ВЕЛИЧИНЫ: Используй ЦИФРЫ для всех чисел. Пиши все единицы измерения ПОЛНЫМИ СЛОВАМИ. Запрещено: "м", "кг", "°", "$". Пиши: "78 рублей 47 копеек", "25 градусов", "10 метров". 
            5. ДРОБИ: Никогда не используй точку для десятичных дробей. Пиши словами: "101 рубль 23 копейки" или "1 целая 5 десятых метра". 

            ПРАВИЛА ЛОГИКИ:
            1. ФАКТЫ: Тебе ЗАПРЕЩЕНО использовать свою память для цифр, курсов, погоды или событий.
            2. Если в предоставленном контексте НЕТ ответа на вопрос — отвечай: "Нет данных".
            3. КРАТКОСТЬ: Если вопрос — факт, отвечай только значением.
            4. Если нужна инструкция — давай краткий пошаговый план.
        """.trimIndent()
    }

    var apiKey: String
        get() = prefs.getString("api_key", "") ?: ""
        set(value) = prefs.edit().putString("api_key", value).apply()

    var model: String
        get() = prefs.getString("model", DEFAULT_MODEL) ?: DEFAULT_MODEL
        set(value) = prefs.edit().putString("model", value).apply()

    var systemPrompt: String
        get() = prefs.getString("system_prompt", DEFAULT_PROMPT) ?: DEFAULT_PROMPT
        set(value) = prefs.edit().putString("system_prompt", value).apply()

    var autoListenOnOpen: Boolean
        get() = prefs.getBoolean("auto_listen", false)
        set(value) = prefs.edit().putBoolean("auto_listen", value).apply()

    var appLanguage: String
        get() = prefs.getString("app_language", "system") ?: "system"
        set(value) = prefs.edit().putString("app_language", value).apply()

    var ttsProvider: String
        get() = prefs.getString("tts_provider", DEFAULT_TTS_PROVIDER) ?: DEFAULT_TTS_PROVIDER
        set(value) = prefs.edit().putString("tts_provider", value).apply()

    var ttsSpeed: Float
        get() = prefs.getFloat("tts_speed", DEFAULT_TTS_SPEED)
        set(value) = prefs.edit().putFloat("tts_speed", value).apply()
        
    var edgeVoice: String
        get() = prefs.getString("edge_voice", DEFAULT_EDGE_VOICE) ?: DEFAULT_EDGE_VOICE
        set(value) = prefs.edit().putString("edge_voice", value).apply()

    /**
     * Возврат системного промпта к заводским настройкам.
     */
    fun resetPrompt() {
        systemPrompt = DEFAULT_PROMPT
    }
}
