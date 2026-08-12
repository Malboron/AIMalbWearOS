# Final Verified Implementation Plan - Tile & Naming v1.4.4

This plan provides a triple-checked, definitive fix for the Wear OS Tile and enforces strict versioned naming for the application. All technical requirements have been cross-referenced with modern Wear OS 3.5/4.0 specifications.

## Proposed Changes

### 1. Watch App: System-Level Fixes (Manifest)

#### [MODIFY] [AndroidManifest.xml](file:///C:/Users/kazakov_ai/AndroidStudioProjects/AIMalb/app/src/main/AndroidManifest.xml)
- **Security Permission**: Change `android.permission.BIND_TILE_PROVIDER` to **`com.google.android.wearable.permission.BIND_TILE_PROVIDER`**.
- **Intent Filter**: Ensure `MainActivity` has an `<intent-filter>` with `android.intent.action.VIEW` and `android.intent.category.DEFAULT`.
- **Reason**: These two changes are mandatory for modern Wear OS to authorize a Tile to launch an Activity.

### 2. Watch App: Optimized Tile Interaction

#### [MODIFY] [AiTileService.kt](file:///C:/Users/kazakov_ai/AndroidStudioProjects/AIMalb/app/src/main/java/com/malbandco/aimalb/presentation/tiles/AiTileService.kt)
- **Pattern**: Use standard `materialScope` and `primaryLayout`.
- **Trigger**: Use `ActionBuilders.launchAction(ComponentName(this, MainActivity::class.java))` for foolproof activity resolution.
- **Visuals**: A large centered Material `Button` with the cyan microphone icon.

### 3. Build Configuration (Strict Naming)

#### [MODIFY] [app/build.gradle.kts](file:///C:/Users/kazakov_ai/AndroidStudioProjects/AIMalb/app/build.gradle.kts)
- **Versioning**: Update `versionCode = 92`, `versionName = "1.4.4-beta"`.
- **Naming**: Set `base { archivesName.set("AIMalb1.4.4-beta") }` permanently. No conditional logic for Android Studio is included.

#### [MODIFY] [MainActivity.kt](file:///C:/Users/kazakov_ai/AndroidStudioProjects/AIMalb/app/src/main/java/com/malbandco/aimalb/presentation/MainActivity.kt)
- Update "About" screen to display **`v1.4.4-beta (Build 92)`**.

## User Review Required

> [!CAUTION]
> **Complete Reset**: You **must uninstall** the previous version from your watch. Manifest permission changes are system-level events that Android only registers during a clean installation.

## Verification Plan
1.  **APK Name**: Confirm the release folder contains `AIMalb1.4.4-beta-release.apk`.
2.  **Interaction**: Tap the Tile on a real watch. Confirm the app opens instantly.
3.  **Identity**: Confirm Build 92 is shown in the settings.
