package com.plugin.aiassist

import android.content.Context

/**
 * Holds all user-configurable AI provider settings.
 * Persisted in SharedPreferences so settings survive plugin reload.
 */
data class AIConfig(
    val baseUrl: String = "https://api.openai.com/v1",
    val apiKey:  String = "",
    val modelId: String = "gpt-4o",
    val systemPrompt: String = DEFAULT_SYSTEM_PROMPT,
    val maxTokens: Int = 2048,
    val temperature: Float = 0.7f
) {
    companion object {

        const val DEFAULT_SYSTEM_PROMPT =
            "You are an expert software engineer assistant embedded inside a mobile IDE. " +
            "Provide concise, accurate, and helpful responses about code, debugging, and software concepts."

        private const val PREF_BASE_URL      = "ai_base_url"
        private const val PREF_API_KEY       = "ai_api_key"
        private const val PREF_MODEL_ID      = "ai_model_id"
        private const val PREF_SYSTEM_PROMPT = "ai_system_prompt"
        private const val PREF_MAX_TOKENS    = "ai_max_tokens"
        private const val PREF_TEMPERATURE   = "ai_temperature"

        fun load(context: Context, pluginId: String): AIConfig {
            val prefs = context.getSharedPreferences("${pluginId}_prefs", Context.MODE_PRIVATE)
            return AIConfig(
                baseUrl      = prefs.getString(PREF_BASE_URL, "https://api.openai.com/v1") ?: "https://api.openai.com/v1",
                apiKey       = prefs.getString(PREF_API_KEY, "") ?: "",
                modelId      = prefs.getString(PREF_MODEL_ID, "gpt-4o") ?: "gpt-4o",
                systemPrompt = prefs.getString(PREF_SYSTEM_PROMPT, DEFAULT_SYSTEM_PROMPT) ?: DEFAULT_SYSTEM_PROMPT,
                maxTokens    = prefs.getInt(PREF_MAX_TOKENS, 2048),
                temperature  = prefs.getFloat(PREF_TEMPERATURE, 0.7f)
            )
        }

        fun save(context: Context, pluginId: String, config: AIConfig) {
            context.getSharedPreferences("${pluginId}_prefs", Context.MODE_PRIVATE)
                .edit()
                .putString(PREF_BASE_URL,      config.baseUrl)
                .putString(PREF_API_KEY,       config.apiKey)
                .putString(PREF_MODEL_ID,      config.modelId)
                .putString(PREF_SYSTEM_PROMPT, config.systemPrompt)
                .putInt(PREF_MAX_TOKENS,       config.maxTokens)
                .putFloat(PREF_TEMPERATURE,    config.temperature)
                .apply()
        }
    }
}