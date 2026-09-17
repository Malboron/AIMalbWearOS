[Русская версия](https://github.com/Malboron/AIMalbWearOS/blob/master/readme.ru.md)

# AIMalb - Voice AI for Wear OS

AIMalb is a voice AI for Wear OS smartwatches. It allows you to ask questions directly from your watch, get answers from modern AI models, and retrieve up-to-date information from the Internet.

## The app is designed for quick everyday requests

When taking out your phone is inconvenient or you simply don't want to: ask a question by voice, get an answer, and listen to it directly on your watch.

## 🚀 Main Features

* **AI models:** Support for `gpt-oss-120b`, `llama-3.3-70b`, and other models through the Groq API.
* **Internet access:** Automatic search through DuckDuckGo Lite for up-to-date exchange rates, weather, news, and other information.
* **Direct data:** Instant access to official Central Bank of Russia exchange rates without relying on AI-generated answers.
* **Focus UI:** The answer text scrolls and centers as it is being read. The currently spoken phrase is highlighted.
* **Companion App:** A smartphone companion app for quickly synchronizing the API key and settings with the watch.
* **Widget:** Launch the app directly from a widget in the side menu.
* **Voice:** Two text-to-speech options are available: Google's local TTS and Microsoft Edge TTS with more natural-sounding voices. Edge TTS also allows you to select a specific voice and adjust the speech rate.

---

## 🛠 Installation

### 1. Watch (Wear OS)

1. Download the latest build: `AIMalb1.6.8-beta-release.apk`
2. Install it using ADB or any APK installer for Wear OS.

### 2. Smartphone (Companion)

1. Download `AIMalbCompanion1.1.1-beta-release.apk`
2. Install it on your phone to conveniently configure the app.

---

## ⚙️ Setup and Usage

### First Launch

1. Get a free API key from [Groq](https://console.groq.com/keys).
2. Open AIMalb Companion on your phone.
3. Paste the key and tap **Sync to Watch**. The key will be transferred to your watch immediately.

### Watch Settings

* **AI Settings:** Select the model and edit the system prompt. The list of available models is loaded through the Groq API. Not all models are compatible with the app.
* **Listen on Launch:** When enabled, the microphone activates immediately after opening the app.
* **Widget:** Add the widget to the side menu for quick access. When **Listen on Launch** is enabled, you can start speaking your request immediately.

---

## 📷 Screenshots

### ⌚ Wear OS

### 📱 Companion App

---

## Technologies

* Kotlin
* Jetpack Compose
* Wear OS
* Android Data Layer API
* Retrofit
* Kotlin Coroutines
* Groq API

## Architecture

The project consists of two applications:

* **AIMalb** — the main app for the watch
* **AIMalbCompanion** — the companion app for the smartphone

Communication between the devices is handled through the Wear OS Data Layer.

## Project Status

**In development** — updated as time and resources allow.

## Roadmap

* Context retention within a single session.
* Chat history.
* Improved text and voice synchronization.
* Automatically start processing the request after voice input ends, without requiring separate **Finish** and **Send** buttons.
* Run a local AI model on the smartphone through the companion app and interact with it from the watch for fully autonomous operation.

## System Requirements and Testing

The app is designed for devices running **Wear OS 3.5 and later**.

Currently tested only on **Mobvoi TicWatch Pro 3 GPS**.

## Why I Made This

The idea for the app came from the fact that Google removed the ability to launch its built-in voice assistant on older devices. I simply wanted a way to ask a modern AI something directly from my watch.

## Known Issues

Text and speech synchronization is not perfect. The transition to the next line is currently based approximately on the length of the corresponding audio. This issue appeared after adding Microsoft Edge TTS, since it generates a complete audio file rather than streaming the speech in real time.

Memory and conversation context are not implemented yet, so the bot cannot handle follow-up questions based on previous messages.

On some devices, possibly slower ones, the watch may go to sleep while launching the app if the screen timeout is set too short.

## AI-Assisted Development

AI tools were used throughout the development of the project for coding, code analysis, and finding solutions.

The main AI tool used during development:

* **Google Gemini** — assistance with code generation and analysis, architectural decisions, and debugging.

AI was used extensively throughout the development process, while the final decisions, component integration, and code verification were handled as part of the development process.

## License

The project is distributed under the [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0) license.

## Support the Project

If you find the project useful, you can support its development:

💙 [Support via YooMoney](https://yoomoney.ru/)
