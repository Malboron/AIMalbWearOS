package com.malbandco.aimalb.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface GroqApi {
    @POST("openai/v1/chat/completions")
    suspend fun getChatCompletion(
        @Header("Authorization") apiKey: String,
        @Body request: GroqRequest
    ): GroqResponse

    @GET("openai/v1/models")
    suspend fun getModels(
        @Header("Authorization") apiKey: String
    ): okhttp3.ResponseBody
}

data class GroqRequest(
    val messages: List<Message>,
    val model: String = "openai/gpt-oss-120b",
    val temperature: Float = 0.2f
)

data class Message(
    val role: String,
    val content: String
)

data class GroqResponse(
    val choices: List<Choice>
)

data class Choice(
    val message: Message
)
