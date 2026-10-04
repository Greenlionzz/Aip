package com.plugin.aiassist

import android.content.Context
import android.text.InputType
import android.view.LayoutInflater
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog

/**
 * Settings dialog for configuring the AI assistant.
 * Allows the user to set base URL, API key, model ID,
 * system prompt, max tokens, and temperature.
 */
object AISettingsDialog {

    fun show(context: Context, pluginId: String, onSaved: ((AIConfig) -> Unit)? = null) {
        val config = AIConfig.load(context, pluginId)

        // Build the form layout programmatically
        val scroll = ScrollView(context)
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val p = (16 * context.resources.displayMetrics.density).toInt()
            setPadding(p, p, p, p)
        }

        fun label(text: String) = TextView(context).apply {
            this.text = text
            textSize = 12f
            setTextColor(0xFF9E9E9E.toInt())
            val mb = (4 * context.resources.displayMetrics.density).toInt()
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.topMargin = mb * 3
            layoutParams = params
        }

        fun field(hint: String, value: String, inputType: Int = InputType.TYPE_CLASS_TEXT) =
            EditText(context).apply {
                this.hint = hint
                setText(value)
                this.inputType = inputType
                setBackgroundResource(android.R.drawable.edit_text)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

        // Base URL
        layout.addView(label("Base URL"))
        val etBaseUrl = field(
            "e.g. https://api.openai.com/v1",
            config.baseUrl,
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
        )
        layout.addView(etBaseUrl)

        // API Key
        layout.addView(label("API Key"))
        val etApiKey = field(
            "sk-...",
            config.apiKey,
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        )
        layout.addView(etApiKey)

        // Model ID
        layout.addView(label("Model ID"))
        val etModelId = field("e.g. gpt-4o", config.modelId)
        layout.addView(etModelId)

        // System Prompt
        layout.addView(label("System Prompt"))
        val etSystemPrompt = EditText(context).apply {
            hint = "You are a helpful coding assistant..."
            setText(config.systemPrompt)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            minLines = 3
            maxLines = 6
            setBackgroundResource(android.R.drawable.edit_text)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        layout.addView(etSystemPrompt)

        // Max Tokens
        layout.addView(label("Max Tokens"))
        val etMaxTokens = field(
            "2048",
            config.maxTokens.toString(),
            InputType.TYPE_CLASS_NUMBER
        )
        layout.addView(etMaxTokens)

        // Temperature
        layout.addView(label("Temperature (0.0 – 2.0)"))
        val etTemperature = field(
            "0.7",
            config.temperature.toString(),
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        )
        layout.addView(etTemperature)

        // Stream toggle
        val cbStream = CheckBox(context).apply {
            text = "Enable streaming responses"
            isChecked = config.stream
            val mt = (12 * context.resources.displayMetrics.density).toInt()
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.topMargin = mt
            layoutParams = params
        }
        layout.addView(cbStream)

        scroll.addView(layout)

        AlertDialog.Builder(context)
            .setTitle("AI Assistant Settings")
            .setView(scroll)
            .setPositiveButton("Save") { _, _ ->
                val newConfig = config.copy(
                    baseUrl = etBaseUrl.text.toString().trim().trimEnd('/'),
                    apiKey = etApiKey.text.toString().trim(),
                    modelId = etModelId.text.toString().trim(),
                    systemPrompt = etSystemPrompt.text.toString().trim(),
                    maxTokens = etMaxTokens.text.toString().toIntOrNull() ?: config.maxTokens,
                    temperature = etTemperature.text.toString().toDoubleOrNull() ?: config.temperature,
                    stream = cbStream.isChecked
                )
                AIConfig.save(context, pluginId, newConfig)
                onSaved?.invoke(newConfig)
            }
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Reset Defaults") { _, _ ->
                val defaults = AIConfig()
                AIConfig.save(context, pluginId, defaults)
                onSaved?.invoke(defaults)
            }
            .show()
    }
}
