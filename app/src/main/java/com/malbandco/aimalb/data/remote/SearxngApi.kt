package com.malbandco.aimalb.data.remote

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

interface SearxngApi {
    @GET
    suspend fun search(
        @Url url: String,
        @Query("q") query: String,
        @Query("format") format: String = "json"
    ): SearxngResponse

    @GET
    suspend fun searchHtml(
        @Url url: String,
        @Query("q") query: String
    ): ResponseBody

    @GET
    suspend fun getRawJson(
        @Url url: String
    ): ResponseBody
}

data class SearxngResponse(
    val results: List<SearxngResult> = emptyList()
)

data class SearxngResult(
    val title: String,
    val url: String,
    val content: String? // Some instances use 'content', others 'snippet'
)
