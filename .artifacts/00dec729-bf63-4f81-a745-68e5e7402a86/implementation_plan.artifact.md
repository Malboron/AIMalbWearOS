# Implementation Plan - Dynamic AI Model List (v1.5.0)

This plan implements dynamic fetching of available AI models from the Groq API, replacing the hardcoded list in the application.

## User Review Required

> [!IMPORTANT]
> **API Key Requirement**: The list of models will only be fetched after a valid API key is provided. The app will attempt to refresh the list on initialization and when the API key is verified.

## Proposed Changes

### 1. Data Layer: API Definition

#### [MODIFY] [GroqApi.kt](file:///C:/Users/kazakov_ai/AndroidStudioProjects/AIMalb/app/src/main/java/com/malbandco/aimalb/data/remote/GroqApi.kt)
- Define `GroqModelsResponse` and `GroqModel` data classes.
- Update `getModels` to return `GroqModelsResponse` instead of `ResponseBody`.

#### [MODIFY] [AiRepository.kt](file:///C:/Users/kazakov_ai/AndroidStudioProjects/AIMalb/app/src/main/java/com/malbandco/aimalb/data/AiRepository.kt)
- Add `getAvailableModels(apiKey: String): Result<List<String>>` method to fetch and parse the model IDs.

### 2. Presentation Layer: ViewModel Integration

#### [MODIFY] [MainViewModel.kt](file:///C:/Users/kazakov_ai/AndroidStudioProjects/AIMalb/app/src/main/java/com/malbandco/aimalb/presentation/MainViewModel.kt)
- Change `availableModels` from a hardcoded `List<String>` to a `State<List<String>>`.
- Add `refreshModels()` method to call the repository.
- Call `refreshModels()` during `init()` and after successful API key verification.
- Ensure the current selected model is preserved if it's still available in the new list, otherwise fallback to a default.

### 3. Build & Versioning
- Increment version to **`v1.5.0-beta`** (Build **`95`**).
- Maintain release naming convention.

## Verification Plan

### Manual Verification
1.  **Initialization**: Open the app with an existing API key. Verify that the model list in settings is populated dynamically.
2.  **Key Verification**: Enter a new API key and tap "Verify." Confirm the model list updates upon success.
3.  **Model Selection**: Change the model from the dynamic list and verify it is saved and used for subsequent chat requests.
4.  **Error Handling**: Verify that if the API request fails (e.g., no internet), the app falls back to a safe default list or shows an informative message.
