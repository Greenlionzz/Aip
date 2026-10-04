package com.plugin.aiassist

import android.content.Context
import com.itsaky.androidide.plugins.api.IPlugin
import com.itsaky.androidide.plugins.api.PluginContext
import com.itsaky.androidide.plugins.api.UIExtension
import com.itsaky.androidide.plugins.api.ui.MenuItem
import com.itsaky.androidide.plugins.api.ui.NavigationItem
import com.itsaky.androidide.plugins.api.services.IdeEditorService
import com.itsaky.androidide.plugins.api.services.IdeUIService

/**
 * AIAssistPlugin — Main entry point.
 *
 * Implements:
 *   • IPlugin        — lifecycle (initialize / activate / deactivate / dispose)
 *   • UIExtension    — sidebar nav item + main-menu entry
 */
class AIAssistPlugin : IPlugin, UIExtension {

    private lateinit var pluginContext: PluginContext

    // ── Shared configuration held in memory after first load ──────────────────
    internal lateinit var config: AIConfig

    // ─────────────────────────────────────────────────────────────────────────
    // IPlugin lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    override fun initialize(context: PluginContext): Boolean {
        pluginContext = context
        context.logger.info("AIAssistPlugin: Initializing …")

        // Load persisted config from SharedPreferences
        config = AIConfig.load(context.androidContext, context.pluginId)

        context.logger.info("AIAssistPlugin: Initialized. Base URL = ${config.baseUrl}")
        return true
    }

    override fun activate(): Boolean {
        pluginContext.logger.info("AIAssistPlugin: Activating …")
        return true
    }

    override fun deactivate(): Boolean {
        pluginContext.logger.info("AIAssistPlugin: Deactivating …")
        return true
    }

    override fun dispose() {
        pluginContext.logger.info("AIAssistPlugin: Disposed.")
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UIExtension — sidebar
    // ─────────────────────────────────────────────────────────────────────────

    override fun getSideMenuItems(): List<NavigationItem> {
        return listOf(
            NavigationItem(
                id    = "ai_assist_panel",
                title = "AI Assistant",
                icon  = android.R.drawable.ic_menu_help,
                group = "tools",
                order = 50,
                action = { openAssistantPanel() }
            )
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UIExtension — main toolbar menu (three-dot menu)
    // ─────────────────────────────────────────────────────────────────────────

    override fun getMainMenuItems(): List<MenuItem> {
        return listOf(
            MenuItem(
                id       = "ai_assist_open",
                title    = "AI Assistant",
                isEnabled = true,
                action   = { openAssistantPanel() }
            ),
            MenuItem(
                id       = "ai_assist_settings",
                title    = "AI Assistant Settings",
                isEnabled = true,
                action   = { openSettingsDialog() }
            ),
            MenuItem(
                id       = "ai_assist_explain",
                title    = "Explain Selected Code",
                isEnabled = true,
                action   = { explainSelectedCode() }
            )
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Context-menu items (long-press in editor)
    // ─────────────────────────────────────────────────────────────────────────

    override fun getContextMenuItems(
        context: com.itsaky.androidide.plugins.api.ui.ContextMenuContext
    ): List<MenuItem> {
        if (context.selectedText.isNullOrBlank()) return emptyList()

        return listOf(
            MenuItem(
                id    = "ai_explain_selection",
                title = "✦ Explain with AI",
                action = { explainText(context.selectedText!!) }
            ),
            MenuItem(
                id    = "ai_improve_selection",
                title = "✦ Improve with AI",
                action = { improveText(context.selectedText!!) }
            ),
            MenuItem(
                id    = "ai_fix_selection",
                title = "✦ Fix with AI",
                action = { fixText(context.selectedText!!) }
            )
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internal helpers
    // ─────────────────────────────────────────────────────────────────────────

    private fun openAssistantPanel() {
        val uiService = pluginContext.services.get(IdeUIService::class.java) ?: return
        if (!uiService.isUIAvailable()) return
        val activity = uiService.getCurrentActivity() ?: return

        // Show the main AI chat fragment in a bottom sheet dialog
        val sheet = AIAssistBottomSheet.newInstance(this)
        sheet.show(activity.supportFragmentManager, "ai_assist")
    }

    private fun openSettingsDialog() {
        val uiService = pluginContext.services.get(IdeUIService::class.java) ?: return
        if (!uiService.isUIAvailable()) return
        val activity = uiService.getCurrentActivity() ?: return

        AISettingsDialog.show(activity, this)
    }

    private fun explainSelectedCode() {
        val editorService = pluginContext.services.get(IdeEditorService::class.java)
        val selected = editorService?.getCurrentSelection()
        if (selected.isNullOrBlank()) {
            showToast("No text selected in the editor.")
            return
        }
        explainText(selected)
    }

    private fun explainText(text: String) {
        promptAndShow("Explain the following code clearly and concisely:\n\n```\n$text\n```")
    }

    private fun improveText(text: String) {
        promptAndShow("Improve the following code for clarity, performance, and best practices. Return only the improved code:\n\n```\n$text\n```")
    }

    private fun fixText(text: String) {
        promptAndShow("Identify and fix any bugs in the following code. Return only the corrected code:\n\n```\n$text\n```")
    }

    internal fun promptAndShow(prompt: String) {
        val uiService = pluginContext.services.get(IdeUIService::class.java) ?: return
        if (!uiService.isUIAvailable()) return
        val activity = uiService.getCurrentActivity() ?: return

        val sheet = AIAssistBottomSheet.newInstance(this, initialPrompt = prompt)
        sheet.show(activity.supportFragmentManager, "ai_assist_prompt")
    }

    private fun showToast(msg: String) {
        val uiService = pluginContext.services.get(IdeUIService::class.java) ?: return
        val activity  = uiService.getCurrentActivity() ?: return
        activity.runOnUiThread {
            android.widget.Toast.makeText(activity, msg, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    /** Expose logger for use inside Fragments */
    internal fun logInfo(msg: String)  = pluginContext.logger.info(msg)
    internal fun logError(msg: String, t: Throwable? = null) {
        if (t != null) pluginContext.logger.error(msg, t)
        else pluginContext.logger.error(msg)
    }

    /** Save updated config to SharedPreferences */
    internal fun saveConfig(newConfig: AIConfig) {
        config = newConfig
        AIConfig.save(pluginContext.androidContext, pluginContext.pluginId, newConfig)
    }
}