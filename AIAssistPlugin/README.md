# AI Assist Plugin for CodeOnTheGo

An OpenAI-compatible AI assistant plugin for the [CodeOnTheGo](https://appdevforall.org/codeonthego) mobile IDE.

---

## Features

- 💬 **Chat UI** — Bottom sheet chat panel with message history
- 🔍 **Quick actions** — Chips for Explain / Fix / Improve / Document / Write Tests
- 🖱️ **Context menu** — Right-click selected code to send directly to AI
- 📌 **Sidebar entry** — One-tap access from the IDE sidebar
- ⚙️ **Settings** — Configure Base URL, API Key, Model ID, system prompt, temperature, max tokens
- 🌙 **Dark mode** — Full light/dark theme support
- 🔌 **Any OpenAI-compatible API** — Works with OpenAI, Ollama, LM Studio, Groq, Azure OpenAI, etc.

---

## Configuration

Open the plugin settings dialog (⚙️ button in the chat panel or *Main Menu → AI Settings*):

| Field | Description | Default |
|---|---|---|
| **Base URL** | API endpoint root | `https://api.openai.com/v1` |
| **API Key** | Your provider's API key | *(empty)* |
| **Model ID** | Model name/ID | `gpt-4o` |
| **System Prompt** | Persona/instructions | *coding assistant* |
| **Max Tokens** | Max response length | `2048` |
| **Temperature** | Creativity (0.0–2.0) | `0.7` |

### Example providers

```
# OpenAI
Base URL: https://api.openai.com/v1
API Key:  sk-...
Model:    gpt-4o

# Ollama (local)
Base URL: http://localhost:11434/v1
API Key:  ollama
Model:    llama3.2

# Groq
Base URL: https://api.groq.com/openai/v1
API Key:  gsk_...
Model:    llama-3.3-70b-versatile

# LM Studio (local)
Base URL: http://localhost:1234/v1
API Key:  lm-studio
Model:    (whatever model is loaded)
```

---

## Project Structure

```
AIAssistPlugin/
├── settings.gradle.kts
├── build.gradle.kts
├── gradle/
│   └── libs.versions.toml          ← dependency versions
└── app/
    ├── build.gradle.kts            ← plugin builder config
    └── src/main/
        ├── AndroidManifest.xml     ← plugin metadata & permissions
        ├── assets/
        │   ├── icon_day.png        ← replace with real 192×192 PNG
        │   └── icon_night.png
        ├── kotlin/com/plugin/aiassist/
        │   ├── AIAssistPlugin.kt   ← IPlugin entry point, menus, sidebar
        │   ├── AIConfig.kt         ← SharedPreferences config model
        │   ├── AIChatClient.kt     ← OkHttp3 OpenAI-compatible client
        │   ├── AIAssistBottomSheet.kt ← Chat BottomSheetDialogFragment
        │   ├── ChatAdapter.kt      ← RecyclerView adapter for messages
        │   └── AISettingsDialog.kt ← Settings AlertDialog (programmatic)
        └── res/
            ├── drawable/
            │   ├── bg_bubble_user.xml
            │   ├── bg_bubble_assistant.xml
            │   ├── bg_input_field.xml
            │   └── bg_send_button.xml
            ├── layout/
            │   ├── fragment_ai_assist.xml
            │   ├── item_chat_message_user.xml
            │   └── item_chat_message_assistant.xml
            ├── values/
            │   ├── strings.xml
            │   ├── colors.xml
            │   └── styles.xml
            └── values-night/
                └── colors.xml
```

---

## Build Instructions

### Prerequisites

- Android Studio (or IntelliJ IDEA)
- JDK 17+
- The CodeOnTheGo **Plugin API JAR** — get it from:
  `https://github.com/appdevforall/CodeOnTheGo/releases` → `plugin-api.jar`

### Steps

1. **Add the plugin API JAR**
   ```bash
   mkdir -p app/libs
   cp /path/to/plugin-api.jar app/libs/
   ```

2. **Replace icons** (required — placeholder text files won't work as PNG)
   ```bash
   # Option A: copy your own 192×192 PNGs
   cp my_icon_day.png   app/src/main/assets/icon_day.png
   cp my_icon_night.png app/src/main/assets/icon_night.png

   # Option B: quick generate with Python + Pillow
   python3 -c "
   from PIL import Image, ImageDraw
   for bg, fg, name in [((33,150,243),(255,255,255),'icon_day'),((30,30,30),(100,181,246),'icon_night')]:
       img=Image.new('RGB',(192,192),bg); ImageDraw.Draw(img).text((60,80),'AI',fill=fg)
       img.save(f'app/src/main/assets/{name}.png')
   "
   ```

3. **Build the plugin**
   ```bash
   ./gradlew assemblePlugin
   ```
   Output: `app/build/outputs/plugin/ai-assist-<version>.cgp`

4. **Install in CodeOnTheGo**
   - Copy the `.cgp` file to your device
   - Open CodeOnTheGo → Settings → Plugins → Install from file
   - Select the `.cgp` file

---

## Permissions Used

| Permission | Reason |
|---|---|
| `network.access` | Calls the AI API |
| `filesystem.read` | Reads selected/active editor code |
| `ide.settings` | Persists plugin configuration |

---

## Architecture Notes

- **`AIAssistPlugin`** — Main entry point. Registers sidebar nav item (`getSidebarNavItems`), main menu items, and context menu items. Opens `AIAssistBottomSheet` on activation.
- **`AIChatClient`** — Stateless HTTP client using OkHttp3. POSTs to `/chat/completions` with full message history. Returns sealed `Result<String>`.
- **`AIAssistBottomSheet`** — `BottomSheetDialogFragment`. Reads selected code from `IdeEditorService`, manages `ChatAdapter`, handles coroutine lifecycle.
- **`AIConfig`** — Pure data class stored in `SharedPreferences` scoped to `pluginId`.
- **`AISettingsDialog`** — Programmatic `AlertDialog` (no extra layout XML needed) with all 6 config fields.

---

## License

MIT — free to use, modify, and distribute.
