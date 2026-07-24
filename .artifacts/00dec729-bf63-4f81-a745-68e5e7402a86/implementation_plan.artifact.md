# Implementation Plan - Versioning, APK Naming & Sync Reliability (v50)

This plan synchronizes the version information across all modules and UI components, ensures correct APK naming, and improves the reliability of the API key synchronization by adding a timestamp to the data packets.

## Proposed Changes

### 1. Build Configuration (Versioning & Naming)

#### [MODIFY] [app/build.gradle.kts](file:///C:/Users/kazakov_ai/AndroidStudioProjects/AIMalb/app/build.gradle.kts)
- **Versioning**:
    - `versionCode = 50`
    - `versionName = "1.0.7-beta"`
- **APK Naming**: Update `archivesName.set("AIMalb1.0.7-beta")`.

#### [MODIFY] [phonecompanionmodule/build.gradle.kts](file:///C:/Users/kazakov_ai/AndroidStudioProjects/AIMalb/phonecompanionmodule/build.gradle.kts)
- **Versioning**:
    - `versionCode = 7`
    - `versionName = "1.0.6-beta"`
- **APK Naming**: Update `archivesName.set("AIMalbCompanion1.0.6-beta")`.

### 2. Synchronization Reliability (Force Update)

#### [MODIFY] [CompanionViewModel.kt](file:///C:/Users/kazakov_ai/AndroidStudioProjects/AIMalb/phonecompanionmodule/src/main/java/com/malbandco/phonecompanionmodule/presentation/CompanionViewModel.kt)
- **Logic**: Add `dataMap.putLong("timestamp", System.currentTimeMillis())` to the sync request.
- **Reason**: The Android Data Layer only triggers an update if the data object is different. Adding a timestamp ensures every "Sync" tap is treated as a new event by the watch.

#### [MODIFY] [KeySyncService.kt](file:///C:/Users/kazakov_ai/AndroidStudioProjects/AIMalb/app/src/main/java/com/malbandco/aimalb/data/local/KeySyncService.kt)
- **Robustness**: Ensure `onDataChanged` properly logs receipt of the key even if it matches the current one.

### 3. UI Updates (Version Display)

#### [MODIFY] [MainActivity.kt (Watch)](file:///C:/Users/kazakov_ai/AndroidStudioProjects/AIMalb/app/src/main/java/com/malbandco/aimalb/presentation/MainActivity.kt)
- Update `AboutScreen` to show `v1.0.7-beta (Build 50)`.

#### [MODIFY] [MainActivity.kt (Phone)](file:///C:/Users/kazakov_ai/AndroidStudioProjects/AIMalb/phonecompanionmodule/src/main/java/com/malbandco/phonecompanionmodule/MainActivity.kt)
- Update the footer to show `v1.0.6-beta (Build 7)`.

## User Review Required

> [!IMPORTANT]
> **Complete Re-installation Required**: Because the version numbers and metadata have changed significantly, you **must uninstall** all previous versions of the app from your watch and phone. This prevents "Package Inconsistent" and "Signature Mismatch" errors.

## Verification Plan

### Automated Tests
- Build using `./gradlew assembleRelease`.
- Verify file names: `AIMalb1.0.7-beta-release.apk` and `AIMalbCompanion1.0.6-beta-release.apk`.

### Manual Verification
1.  **Watch UI**: Confirm "About" screen shows `v1.0.7-beta (Build 50)`.
2.  **Phone UI**: Confirm footer shows `v1.0.6-beta (Build 7)`.
3.  **Sync**: Verify key transfer works instantly with the new timestamp logic.
