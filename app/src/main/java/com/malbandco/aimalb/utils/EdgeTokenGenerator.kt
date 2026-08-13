package com.malbandco.aimalb.utils

import java.security.MessageDigest
import java.security.SecureRandom

/**
 * v1.5.4: Генератор безопасности для Edge TTS (Протокол v143).
 * Реализует логику SHA-256 и генерацию MUID, подтвержденную тестами.
 */
object EdgeTokenGenerator {
    private const val TRUSTED_CLIENT_TOKEN = "6A5AA1D4EAFF4E9FB37E23D68491D6F4"
    private const val WINDOWS_EPOCH_OFFSET = 11644473600L

    /**
     * Генерирует Sec-MS-GEC токен по формуле из edge_tts/drm.py
     */
    fun generateToken(): String {
        val unixTime = System.currentTimeMillis() / 1000.0
        var ticks = unixTime + WINDOWS_EPOCH_OFFSET
        
        // Округление до 300 секунд (5 минут)
        ticks -= (ticks % 300)
        
        // Перевод в 100-наносекундные тики
        val ticksFinal = (ticks * 10_000_000).toLong()
        
        val strToHash = "${ticksFinal}${TRUSTED_CLIENT_TOKEN}"
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(strToHash.toByteArray(Charsets.US_ASCII))
        
        return hash.joinToString("") { "%02X".format(it) }
    }

    /**
     * Генерирует случайный 16-байтный MUID в формате HEX.
     */
    fun generateMuid(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02X".format(it) }
    }
}
