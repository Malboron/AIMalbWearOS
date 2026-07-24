package com.malbandco.phonecompanionmodule.data

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Header

interface CompanionGroqApi {
    @GET("openai/v1/models")
    suspend fun verifyKey(
        @Header("Authorization") authHeader: String
    ): ResponseBody
}
