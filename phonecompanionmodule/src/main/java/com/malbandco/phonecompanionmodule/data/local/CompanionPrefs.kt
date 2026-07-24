package com.malbandco.phonecompanionmodule.data.local

import android.content.Context

class CompanionPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("companion_prefs", Context.MODE_PRIVATE)

    var proxyHost: String
        get() = prefs.getString("proxy_host", "") ?: ""
        set(value) = prefs.edit().putString("proxy_host", value).apply()

    var proxyPort: String
        get() = prefs.getString("proxy_port", "") ?: ""
        set(value) = prefs.edit().putString("proxy_port", value).apply()

    var proxyUser: String
        get() = prefs.getString("proxy_user", "") ?: ""
        set(value) = prefs.edit().putString("proxy_user", value).apply()

    var proxyPass: String
        get() = prefs.getString("proxy_pass", "") ?: ""
        set(value) = prefs.edit().putString("proxy_pass", value).apply()

    var useDoH: Boolean
        get() = prefs.getBoolean("use_doh", true)
        set(value) = prefs.edit().putBoolean("use_doh", value).apply()

    var systemPrompt: String
        get() = prefs.getString("system_prompt", DEFAULT_PROMPT) ?: DEFAULT_PROMPT
        set(value) = prefs.edit().putString("system_prompt", value).apply()

    fun resetPrompt() {
        systemPrompt = DEFAULT_PROMPT
    }

    companion object {
        const val DEFAULT_PROMPT = "Ты — эксперт-ассистент AIMalb. Сегодня: %s. ТВОЯ ГЛАВНАЯ ЦЕЛЬ: Выдавать информацию максимально эффективно для экрана часов."
    }
}
