# Wear OS AI Assistant - Release Fix, Branding & Versioning (v47)

This plan fixes the release installation issue, restores the phone app's icon, and updates all versioning, build numbers, and project links in both applications with specific build numbers as requested.

## Proposed Changes

### 1. Release Build Fix (Signing)

#### [MODIFY] [app/build.gradle.kts](file:///C:/Users/kazakov_ai/AndroidStudioProjects/AIMalb/app/build.gradle.kts)
#### [MODIFY] [phonecompanionmodule/build.gradle.kts](file:///C:/Users/kazakov_ai/AndroidStudioProjects/AIMalb/phonecompanionmodule/build.gradle.kts)
- Configure `signingConfigs` to use the **debug key for release builds**.
- Set `versionCode` for the watch to **47**.
- Set `versionCode` for the phone to **4**.

### 2. Companion App Icon Fix

#### [MODIFY] [phonecompanionmodule/src/main/res/mipmap-anydpi/ic_launcher.xml](file:///C:/Users/kazakov_ai/AndroidStudioProjects/AIMalb/phonecompanionmodule/src/main/res/mipmap-anydpi/ic_launcher.xml)
#### [MODIFY] [phonecompanionmodule/src/main/res/mipmap-anydpi/ic_launcher_round.xml](file:///C:/Users/kazakov_ai/AndroidStudioProjects/AIMalb/phonecompanionmodule/src/main/res/mipmap-anydpi/ic_launcher_round.xml)
- Update adaptive icons to use `@drawable/ic_aimalb_logo` as the foreground element.

### 3. Application Versioning & Links

#### [MODIFY] [app/src/main/java/com/malbandco/aimalb/presentation/MainActivity.kt](file:///C:/Users/kazakov_ai/AndroidStudioProjects/AIMalb/app/src/main/java/com/malbandco/aimalb/presentation/MainActivity.kt)
- Update `AboutScreen` to display:
    - Version: **`v1.0.4-beta`**
    - Build: **`47`**
    - GitHub: **`github.com/Malboron/AIMalbWearOS`**

#### [MODIFY] [phonecompanionmodule/src/main/java/com/malbandco/phonecompanionmodule/MainActivity.kt](file:///C:/Users/kazakov_ai/AndroidStudioProjects/AIMalb/phonecompanionmodule/src/main/java/com/malbandco/phonecompanionmodule/MainActivity.kt)
- Update the UI to display:
    - Version: **`v1.0.3-beta`**
    - Build: **`4`**
    - GitHub: **`github.com/Malboron/AIMalbWearOS`**

## Verification Plan

### Manual Verification
1.  **Installation**: Build and install Release APKs on both phone and watch.
2.  **Versioning**:
    - Watch: Confirm `v1.0.4-beta`, Build `47`.
    - Phone: Confirm `v1.0.3-beta`, Build `4`.
3.  **Link**: Verify the GitHub link is correct in both apps.
4.  **Icon**: Verify the phone app icon is restored.
