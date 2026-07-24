# Wear OS AI Assistant - Speed Optimization & Phone Sync (v43)

This plan focuses on extreme performance tuning, removing hardcoded secrets, and implementing a dual-input system (Watch + Phone) for the API key with instant verification.

## Proposed Changes

### 1. Speed Optimization (Extreme Response)

#### [MODIFY] [AiRepository.kt](file:///C:/Users/kazakov_ai/AndroidStudioProjects/AIMalb/app/src/main/java/com/malbandco/aimalb/data/AiRepository.kt)
- **Remove Delays**: Delete all artificial `delay()` calls after search or status updates.
- **Verification API**: Add `verifyApiKey(key: String): Result<Boolean>` to check if the key is valid by fetching the models list from Groq.

#### [MODIFY] [TtsManager.kt](file:///C:/Users/kazakov_ai/AndroidStudioProjects/AIMalb/app/src/main/java/com/malbandco/aimalb/presentation/TtsManager.kt)
- **Controlled Pacing**: Rewrite `enqueueAll` to use a step-by-step approach.
- **200ms Gap**: Instead of enqueuing all at once, wait for `onDone`, then use a `Handler` or `Coroutine` to wait exactly **200ms** before speaking the next phrase.
- **Latency**: Reduce initial safety buffer to **100ms**.

### 2. API Key Infrastructure (Watch & Phone)

#### [MODIFY] [PreferencesManager.kt](file:///C:/Users/kazakov_ai/AndroidStudioProjects/AIMalb/app/src/main/java/com/malbandco/aimalb/data/local/PreferencesManager.kt)
- **Zero Defaults**: Remove hardcoded API key. Default to empty.

#### [MODIFY] [MainViewModel.kt](file:///C:/Users/kazakov_ai/AndroidStudioProjects/AIMalb/app/src/main/java/com/malbandco/aimalb/presentation/MainViewModel.kt)
- **Phone Data Sync**: Implement `DataClient.OnDataChangedListener`. When the phone sends a `GROQ_KEY` through the Android Data Layer, automatically update the local settings.
- **Verification State**: Add `keyVerificationStatus` flow to show "Validating...", "Success", or "Error" on the UI.

### 3. UI Refinement (Dual Input)

#### [MODIFY] [MainActivity.kt](file:///C:/Users/kazakov_ai/AndroidStudioProjects/AIMalb/app/src/main/java/com/malbandco/aimalb/presentation/MainActivity.kt)
- **Settings Screen**:
    - Keep the **Manual Input** field for the API Key.
    - Add a **"Verify Key"** button below the input.
    - Display status: 🟢 (Working) or 🔴 (Invalid/Expired).

### 4. Companion App Technical Specification
- I will provide a prompt for a separate project to create a phone app that:
    1. Connects to the watch via `Wearable.getDataClient`.
    2. Has a field to enter and verify the Groq Key.
    3. Syncs the key to the watch automatically.

## Verification Plan

### Manual Verification
1.  **Response Speed**: Verify AI starts speaking almost instantly after processing.
2.  **Speech Flow**: Listen to the 200ms pauses between phrases—it should sound fast and rhythmic.
3.  **Manual Check**: Enter a key on the watch and tap "Verify".
4.  **Sync Check**: (Post-companion app) Verify entering key on phone updates the watch UI instantly.
