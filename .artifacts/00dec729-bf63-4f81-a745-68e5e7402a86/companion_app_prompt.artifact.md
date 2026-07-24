# Companion App Development Prompt

Copy and paste the text below into a new AI chat to create the phone companion app for AIMalb.

---

**Role**: Senior Android Developer.
**Project Goal**: Create a simple "Companion App" for a Wear OS AI Assistant named "AIMalb".

**Core Requirements**:
1. **Tech Stack**: Kotlin, Jetpack Compose, Material 3, Android Data Layer API.
2. **UI**:
   - A single clean screen with a "Neon Cyan" theme.
   - An input field for "Groq API Key".
   - A "Verify Key" button that checks the key against `https://api.groq.com/openai/v1/models` (Auth: Bearer).
   - A "Sync to Watch" button.
3. **Connectivity**:
   - Use `Wearable.getDataClient(this)` to communicate with the watch.
   - When the user taps "Sync to Watch", send a `PutDataRequest` to the path `/groq_key`.
   - The DataMap must contain a string value with the key `"key"`.
4. **Permissions**: Include necessary internet and wearable permissions in the manifest.
5. **Branding**: Use a dark theme with neon blue accents to match the watch app's aesthetic.

**Watch App Info (for context)**:
The watch app is already listening for data changes on the `/groq_key` path and expects a string named `"key"`. It will automatically encrypt and store the received key.

**Task**: Generate the complete project structure, including the `MainActivity.kt`, `AndroidManifest.xml`, and necessary Gradle dependencies for the Data Layer API.
