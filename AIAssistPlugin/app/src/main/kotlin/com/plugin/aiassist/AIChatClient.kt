package com.plugin.aiassist

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * OpenAI-compatible chat completion client.
 *
 * Supports any provider that implements the /chat/completions endpoint
 * (OpenAI, Azure OpenAI, Anthropic via compatible proxy, Ollama, LM Studio, etc.)
 */
class AIChatClient(private val config: AIConfig) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val JSON  = "application/json; charset=utf-8".toMediaType()

    data class ChatMessage(val role: String, val content: String)

    sealed class Result {
        data class Success(val text: String, val model: String, val promptTokens: Int, val completionTokens: Int) : Result()
        data class Failure(val error: String, val code: Int = -1) : Result()
    }

    /**
     * Send a list of messages to the AI API.
     * Runs on IO dispatcher; safe to call from a coroutine.
     */
    suspend fun chat(messages: List<ChatMessage>): Result = withContext(Dispatchers.IO) {
        try {
            val messagesArray = JsonArray()

            // Prepend system prompt if not already in list
            if (messages.none { it.role == "system" } && config.systemPrompt.isNotBlank()) {
                messagesArray.add(buildMessage("system", config.systemPrompt))
            }

            messages.forEach { msg ->
                messagesArray.add(buildMessage(msg.role, msg.content))
            }

            val payload = JsonObject().apply {
                addProperty("model",       config.modelId)
                add("messages",            messagesArray)
                addProperty("max_tokens",  config.maxTokens)
                addProperty("temperature", config.temperature)
            }

            val baseUrl = config.baseUrl.trimEnd('/')
            val url     = "$baseUrl/chat/completions"

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer ${config.apiKey}")
                .addHeader("Content-Type",  "application/json")
                .post(payload.toString().toRequestBody(JSON))
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    val errorMsg = extractErrorMessage(body) ?: "HTTP ${response.code}"
                    return@withContext Result.Failure(errorMsg, response.code)
                }

                val json = gson.fromJson(body, JsonObject::class.java)

                val content = json
                    .getAsJsonArray("choices")
                    ?.get(0)?.asJsonObject
                    ?.getAsJsonObject("message")
                    ?.get("content")?.asString
                    ?: return@withContext Result.Failure("Empty response from API")

                val usage       = json.getAsJsonObject("usage")
                val promptTok   = usage?.get("prompt_tokens")?.asInt ?: 0
                val completeTok = usage?.get("completion_tokens")?.asInt ?: 0
                val model       = json.get("model")?.asString ?: config.modelId

                Result.Success(
                    text             = content,
                    model            = model,
                    promptTokens     = promptTok,
                    completionTokens = completeTok
                )
            }
        } catch (e: Exception) {
            Result.Failure("Network error: ${e.message}")
        }
    }

    /**
     * Simple one-shot prompt (wraps [chat] with a single user message).
     */
    suspend fun prompt(userMessage: String): Result =
        chat(listOf(ChatMessage("user", userMessage)))

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private fun buildMessage(role: String, content: String) =
        JsonObject().apply {
            addProperty("role",    role)
            addProperty("content", content)
        }

    private fun extractErrorMessage(body: String): String? = try {
        val json = gson.fromJson(body, JsonObject::class.java)
        json.getAsJsonObject("error")?.get("message")?.asString
    } catch (_: Exception) { null }
}