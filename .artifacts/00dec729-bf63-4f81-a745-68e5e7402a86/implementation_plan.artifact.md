# Wear OS AI Assistant - Critical Sync & APK Naming Fix (v1.2.6)

This plan focuses strictly on two goals: ensuring the API Key and System Prompt are correctly transferred to the watch, and applying the requested versioned naming to the APK files. All other UI changes (QR code, prompt editor window) are excluded.

## Proposed Changes

### 1. Build Configuration (Build 66 / Build 11)

#### [MODIFY] [app/build.gradle.kts](file:///C:/Users/kazakov_ai/AndroidStudioProjects/AIMalb/app/build.gradle.kts)
- Update to **`v1.2.6-beta`** (Build **`66`**).
- Implement IDE-safe versioned naming:
    - **In IDE**: Standard name `app` (fixing the "Run" button).
    - **Release**: Custom name **`AIMalb1.2.6-beta`**.

#### [MODIFY] [phonecompanionmodule/build.gradle.kts](file:///C:/Users/kazakov_ai/AndroidStudioProjects/AIMalb/phonecompanionmodule/build.gradle.kts)
- Update to **`v1.0.9-beta`** (Build **`11`**).
- Implement IDE-safe versioned naming:
    - **In IDE**: Standard name `phonecompanionmodule`.
    - **Release**: Custom name **`AIMalbCompanion1.0.9-beta`**.

### 2. Reliable Synchronization (Phone to Watch)

#### [MODIFY] [AndroidManifest.xml](file:///C:/Users/kazakov_ai/AndroidStudioProjects/AIMalb/app/src/main/AndroidManifest.xml)
- **Problem**: The watch's system settings (Manifest) currently block the new synchronization paths.
- **Fix**: Update the `KeySyncService` intent filter to allow receiving data and messages on the `/sync_data` and `/sync_key` paths.

#### [MODIFY] [CompanionPrefs.kt](file:///C:/Users/kazakov_ai/AndroidStudioProjects/AIMalb/phonecompanionmodule/src/main/java/com/malbandco/phonecompanionmodule/data/local/CompanionPrefs.kt)
- Update the default system prompt to match the **full 5-rule text** from the watch.

#### [MODIFY] [CompanionViewModel.kt](file:///C:/Users/kazakov_ai/AndroidStudioProjects/AIMalb/phonecompanionmodule/src/main/java/com/malbandco/phonecompanionmodule/presentation/CompanionViewModel.kt)
- Ensure the "Sync to Watch" action sends **both** the API Key and the Prompt text in a single, high-priority packet.

## User Review Required

> [!IMPORTANT]
> **No UI Changes**: This update contains **no changes** to your screen layouts, buttons, or QR code functionality. It is a technical fix for synchronization and naming only.

## Verification Plan

### Manual Verification
1.  **APK Name**: Verify the generated APKs are named `AIMalb1.2.6-beta-release.apk` and `AIMalbCompanion1.0.9-beta-release.apk`.
2.  **Sync Test**: Change the prompt and key on the phone. Tap Sync. Confirm the watch updates immediately.
3.  **AS Compatibility**: Verify the green "Run" arrow in Android Studio remains functional.
