package com.malbandco.aimalb.data

import android.content.Context
import android.util.Log
import com.malbandco.aimalb.data.remote.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.regex.Pattern

/**
 * Репозиторий данных: объединяет логику запросов к ИИ и внешним источникам данных.
 */
class AiRepository(
    private val context: Context,
    private val groqApi: GroqApi,
    private val searxngApi: SearxngApi
) {
    private val ddgLiteUrl = "https://lite.duckduckgo.com/lite/"
    private val cbrUrl = "https://www.cbr-xml-daily.ru/daily_json.js"

    /**
     * Получить полный ответ от чата. 
     * Включает в себя автоматический поиск курсов валют и информации в сети.
     */
    suspend fun getChatCompletion(
        userText: String,
        apiKey: String,
        model: String,
        systemPromptTemplate: String,
        onStatusUpdate: suspend (String) -> Unit
    ): String = withContext(Dispatchers.IO) {

        val lowerText = userText.lowercase()
        var searchContext = ""

        // 1. Проверка необходимости запроса курса валют ЦБ РФ
        if (lowerText.contains("курс") || lowerText.contains("доллар") || lowerText.contains("евро") || lowerText.contains("руб")) {
            onStatusUpdate("cbr_status")
            val currencyData = fetchCurrencyData()
            if (currencyData != null) {
                searchContext += "\n\nКУРСЫ ВАЛЮТ ЦБ РФ:\n$currencyData"
                onStatusUpdate("cbr_done")
            }
        }

        // 2. Если контекст еще пуст, выполняем поиск в DuckDuckGo
        if (searchContext.isEmpty()) {
            onStatusUpdate("search_status")
            val searchData = performDDGSearch(userText)
            if (!searchData.isNullOrBlank()) {
                searchContext += "\n\nДАННЫЕ ИЗ СЕТИ:\n$searchData"
                onStatusUpdate("search_done")
            } else {
                onStatusUpdate("no_results")
            }
        }

        // 3. Формирование системного промпта с текущей датой
        val now = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy, HH:mm", Locale("ru"))
        val dateString = now.format(formatter)

        val formattedSystemPrompt = try {
            String.format(systemPromptTemplate, dateString)
        } catch (e: Exception) {
            systemPromptTemplate.replace("%s", dateString)
        }

        val messages = listOf(
            Message(role = "system", content = formattedSystemPrompt),
            Message(role = "user", content = userText + searchContext)
        )

        onStatusUpdate("ai_thinking")

        val authHeader = if (apiKey.startsWith("Bearer ")) apiKey else "Bearer $apiKey"

        // 4. Финальный запрос к LLM через Groq API
        try {
            val response = groqApi.getChatCompletion(
                authHeader,
                GroqRequest(
                    model = model,
                    messages = messages,
                    temperature = 0.1f
                )
            )
            response.choices.firstOrNull()?.message?.content ?: "Empty response"
        } catch (e: Exception) {
            Log.e("AiRepository", "Groq error", e)
            "Groq Error: ${e.message}"
        }
    }

    suspend fun verifyApiKey(apiKey: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val authHeader = if (apiKey.startsWith("Bearer ")) apiKey else "Bearer $apiKey"
        try {
            groqApi.getModels(authHeader)
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Запрос списка доступных моделей с фильтрацией.
     */
    suspend fun getAvailableModels(apiKey: String): Result<List<String>> = withContext(Dispatchers.IO) {
        val authHeader = if (apiKey.startsWith("Bearer ")) apiKey else "Bearer $apiKey"
        try {
            val response = groqApi.getModels(authHeader)
            // Логический фильтр: исключаем vision, whisper, embed, guard, llava
            val models = response.data
                .filter { it.active }
                .map { it.id }
                .filter { id ->
                    val lowerId = id.lowercase()
                    !lowerId.contains("vision") &&
                    !lowerId.contains("whisper") &&
                    !lowerId.contains("embed") &&
                    !lowerId.contains("guard") &&
                    !lowerId.contains("llava")
                }
                .sorted()
            Result.success(models)
        } catch (e: Exception) {
            Log.e("AiRepository", "Failed to fetch models", e)
            Result.failure(e)
        }
    }

    /**
     * Получение курсов валют в формате JSON и парсинг регулярными выражениями.
     */
    private suspend fun fetchCurrencyData(): String? {
        return try {
            val response = searxngApi.getRawJson(cbrUrl)
            val json = response.string()
            val usd = extractCurrencyAndFormat(json, "USD")
            val eur = extractCurrencyAndFormat(json, "EUR")
            "Курс доллара: $usd\nКурс евро: $eur"
        } catch (e: Exception) {
            Log.e("AiRepository", "Currency fetch failed", e)
            null
        }
    }

    private fun extractCurrencyAndFormat(json: String, code: String): String {
        val pattern = Pattern.compile("\"$code\".*?\"Value\":\\s*(\\d+\\.?(\\d*))", Pattern.DOTALL)
        val matcher = pattern.matcher(json)
        return if (matcher.find()) {
            val fullValue = matcher.group(1) ?: return "No data"
            val parts = fullValue.split(".")
            val whole = parts[0]
            val fractional = if (parts.size > 1) parts[1].take(2).padEnd(2, '0') else "00"
            
            "$whole рублей $fractional копеек"
        } else "Нет данных"
    }

    /**
     * Выполнение поиска через HTML-версию DuckDuckGo Lite (быстрее и легче для парсинга).
     */
    private suspend fun performDDGSearch(query: String): String? {
        return try {
            val responseBody = searxngApi.searchHtml(url = ddgLiteUrl, query = query)
            val html = responseBody.string()
            extractDetailedTextFromDDG(html)
        } catch (e: Exception) {
            Log.e("AiRepository", "DDG Search failed", e)
            null
        }
    }

    /**
     * Извлечение заголовков и сниппетов из HTML разметки поисковой выдачи.
     */
    private fun extractDetailedTextFromDDG(html: String): String? {
        val results = mutableListOf<String>()
        val rowPattern = Pattern.compile("<tr[^>]*>(.*?)</tr>", Pattern.DOTALL)
        val matcher = rowPattern.matcher(html)
        
        var currentTitle = ""
        var rowCount = 0
        
        while (matcher.find() && rowCount < 20) {
            val row = matcher.group(1) ?: ""
            val cleanRow = row.replace(Regex("<[^>]*>"), " ").replace("&nbsp;", " ").trim()
            
            if (row.contains("result-link")) {
                currentTitle = cleanRow
            } else if (row.contains("result-snippet") || (currentTitle.isNotEmpty() && cleanRow.length > 40)) {
                if (cleanRow.isNotEmpty()) {
                    results.add("Источник: $currentTitle\nИнформация: $cleanRow")
                    currentTitle = ""
                }
            }
            rowCount++
        }

        return if (results.isNotEmpty()) results.take(5).joinToString("\n---\n") else null
    }
}
