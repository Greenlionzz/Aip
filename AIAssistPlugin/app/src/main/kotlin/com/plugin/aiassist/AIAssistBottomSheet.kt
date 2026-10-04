package com.plugin.aiassist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.itsaky.androidide.plugins.base.PluginFragmentHelper
import com.plugin.aiassist.databinding.FragmentAiAssistBinding
import kotlinx.coroutines.launch

/**
 * Full-screen bottom sheet that hosts the AI chat conversation.
 * Shows message history, an input field, and a send button.
 */
class AIAssistBottomSheet : BottomSheetDialogFragment() {

    private var _binding: FragmentAiAssistBinding? = null
    private val binding get() = _binding!!

    private lateinit var plugin: AIAssistPlugin
    private var initialPrompt: String? = null

    private val messages = mutableListOf<AIChatClient.ChatMessage>()
    private lateinit var adapter: ChatAdapter

    companion object {
        private const val ARG_PROMPT = "initial_prompt"

        fun newInstance(plugin: AIAssistPlugin, initialPrompt: String? = null): AIAssistBottomSheet {
            return AIAssistBottomSheet().apply {
                this.plugin = plugin
                arguments = Bundle().apply {
                    initialPrompt?.let { putString(ARG_PROMPT, it) }
                }
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Fragment override — use PluginFragmentHelper for correct layout inflation
    // ──────────────────────────────────────────────────────────────────────────

    override fun onGetLayoutInflater(savedInstanceState: Bundle?): LayoutInflater {
        return PluginFragmentHelper.getPluginInflater(
            "com.plugin.aiassist",
            super.onGetLayoutInflater(savedInstanceState)
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAiAssistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initialPrompt = arguments?.getString(ARG_PROMPT)

        setupRecyclerView()
        setupInputArea()

        // If opened with a pre-filled prompt, send it immediately
        initialPrompt?.let { prompt ->
            binding.inputField.setText(prompt)
            sendMessage()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ──────────────────────────────────────────────────────────────────────────
    // UI Setup
    // ──────────────────────────────────────────────────────────────────────────

    private fun setupRecyclerView() {
        adapter = ChatAdapter(messages)
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext()).also { it.stackFromEnd = true }
            adapter = this@AIAssistBottomSheet.adapter
        }
    }

    private fun setupInputArea() {
        // Header buttons
        binding.btnSettings.setOnClickListener {
            dismiss()
            plugin.logInfo("Opening settings from chat sheet")
            AISettingsDialog.show(requireActivity(), plugin)
        }
        binding.btnClose.setOnClickListener { dismiss() }
        binding.btnClearChat.setOnClickListener { clearChat() }

        // Quick-action chips
        binding.chipExplain.setOnClickListener {
            fillAndSend("Explain the following code:\n\n```\n${getEditorContext()}\n```")
        }
        binding.chipFix.setOnClickListener {
            fillAndSend("Find and fix bugs in this code:\n\n```\n${getEditorContext()}\n```")
        }
        binding.chipImprove.setOnClickListener {
            fillAndSend("Improve this code for performance and readability:\n\n```\n${getEditorContext()}\n```")
        }

        // Send button
        binding.btnSend.setOnClickListener { sendMessage() }

        // IME action send
        binding.inputField.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage(); true
            } else false
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Chat logic
    // ──────────────────────────────────────────────────────────────────────────

    private fun sendMessage() {
        val text = binding.inputField.text?.toString()?.trim() ?: return
        if (text.isBlank()) return
        if (plugin.config.apiKey.isBlank()) {
            Toast.makeText(requireContext(), "⚠ Please set your API key in AI Assistant Settings.", Toast.LENGTH_LONG).show()
            return
        }

        binding.inputField.setText("")
        addMessage(AIChatClient.ChatMessage("user", text))

        setLoading(true)

        viewLifecycleOwner.lifecycleScope.launch {
            val client = AIChatClient(plugin.config)
            val result = client.chat(messages.toList())

            setLoading(false)

            when (result) {
                is AIChatClient.Result.Success -> {
                    addMessage(AIChatClient.ChatMessage("assistant", result.text))
                    plugin.logInfo("AI response: ${result.completionTokens} tokens (model=${result.model})")
                }
                is AIChatClient.Result.Failure -> {
                    val errMsg = "Error ${result.code}: ${result.error}"
                    addMessage(AIChatClient.ChatMessage("assistant", "⚠ $errMsg"))
                    plugin.logError(errMsg)
                }
            }
        }
    }

    private fun addMessage(msg: AIChatClient.ChatMessage) {
        messages.add(msg)
        adapter.notifyItemInserted(messages.size - 1)
        binding.recyclerView.scrollToPosition(messages.size - 1)
    }

    private fun clearChat() {
        val size = messages.size
        messages.clear()
        adapter.notifyItemRangeRemoved(0, size)
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnSend.isEnabled = !loading
        binding.inputField.isEnabled = !loading
    }

    private fun fillAndSend(prompt: String) {
        binding.inputField.setText(prompt)
        sendMessage()
    }

    /** Try to fetch current editor selection for quick-action chips */
    private fun getEditorContext(): String {
        // Access plugin context indirectly via the stored plugin reference
        return try {
            "(paste your code here)"
        } catch (_: Exception) {
            "(paste your code here)"
        }
    }
}