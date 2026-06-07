package com.example.data.api

import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// --- Gemini REST API Contracts ---

data class GeminiRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null
)

data class Content(
    val parts: List<Part>
)

data class Part(
    val text: String
)

data class GenerationConfig(
    val responseMimeType: String? = null,
    val responseSchema: ResponseSchema? = null,
    val temperature: Float? = null
)

data class ResponseSchema(
    val type: String, // "OBJECT" or "ARRAY"
    val properties: Map<String, SchemaProperty>? = null,
    val required: List<String>? = null,
    val items: ResponseSchema? = null
)

data class SchemaProperty(
    val type: String,
    val description: String? = null
)

// --- Retrofit Service ---

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): ResponseBody
}

object GeminiClient {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
        retrofit.create(GeminiApiService::class.java)
    }

    // Parse Response helper
    fun parseGeminiText(responseBody: String): String {
        try {
            // Manual parse to ignore deep nested structures
            val mapAdapter = moshi.adapter(Map::class.java)
            val parsed = mapAdapter.fromJson(responseBody) ?: return "Error parsing response"
            val candidates = parsed["candidates"] as? List<*> ?: return "No candidates found"
            val firstCandidate = candidates.firstOrNull() as? Map<*, *> ?: return "First candidate empty"
            val content = firstCandidate["content"] as? Map<*, *> ?: return "Content empty"
            val parts = content["parts"] as? List<*> ?: return "Parts empty"
            val firstPart = parts.firstOrNull() as? Map<*, *> ?: return "First part empty"
            return firstPart["text"] as? String ?: "No text content"
        } catch (e: Exception) {
            e.printStackTrace()
            return "Exception parsing text: ${e.message}"
        }
    }
}
