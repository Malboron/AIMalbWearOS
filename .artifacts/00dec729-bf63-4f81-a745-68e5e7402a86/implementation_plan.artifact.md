# Implementation Plan - Reliable Data Sync & Resilient Connectivity (v45)

This plan fixes the synchronization issue between the phone and watch by unifying identities, splits the companion UI into separate Verify/Sync actions, and implements a resilient DNS strategy using providers currently functional in restricted regions.

## Proposed Changes

### 1. Visual Identity & Connectivity (Phone)

#### [MODIFY] [phonecompanionmodule/build.gradle.kts](file:///C:/Users/kazakov_ai/AndroidStudioProjects/AIMalb/phonecompanionmodule/build.gradle.kts)
- **Mandatory ID Sync**: Change `applicationId` to **`com.malbandco.aimalb`**. This is required for the Data Layer API to link the phone and watch.

#### [MODIFY] [CompanionViewModel.kt](file:///C:/Users/kazakov_ai/AndroidStudioProjects/AIMalb/phonecompanionmodule/src/main/java/com/malbandco/phonecompanionmodule/presentation/CompanionViewModel.kt)
- **Resilient DoH Strategy**: Replace Cloudflare (1.1.1.1) with providers confirmed working in 2026:
    - Primary: **Quad9** (`dns.quad9.net`)
    - Secondary: **Comss.one** (`dns.comss.one`)
- **DoH Chain**: Implement a multi-stage DNS resolver that tries Quad9, then Comss.one, and finally falls back to the system DNS.

### 2. Reliable Background Sync (Watch)

#### [NEW] [KeySyncService.kt](file:///C:/Users/kazakov_ai/AndroidStudioProjects/AIMalb/app/src/main/java/com/malbandco/aimalb/data/local/KeySyncService.kt)
- Create a `WearableListenerService` on the watch.
- This service listens for the `/groq_key` data event and saves it to `PreferencesManager` even if the main AI app is closed.

#### [MODIFY] [app/src/main/AndroidManifest.xml](file:///C:/Users/kazakov_ai/AndroidStudioProjects/AIMalb/app/src/main/AndroidManifest.xml)
- Register the `KeySyncService` with the `com.google.android.gms.wearable.BIND_LISTENER` intent filter.

### 3. Companion UI Refinement

#### [MODIFY] [MainActivity.kt](file:///C:/Users/kazakov_ai/AndroidStudioProjects/AIMalb/phonecompanionmodule/src/main/java/com/malbandco/phonecompanionmodule/MainActivity.kt)
- **Split Actions**:
    - **Verify Key**: Uses the new resilient DNS to check the key. Shows status 🟢/🔴.
    - **Sync to Watch**: Manually pushes the key to the watch regardless of verification success.
- **Improved DoH Toggle**: Allow users to choose between Quad9 and Comss.one or disable it.

## Verification Plan

### Manual Verification
1.  **Identity Check**: Confirm both apps now use `com.malbandco.aimalb`.
2.  **DNS Resilience**: Enter key on phone, tap "Verify". Ensure host resolution succeeds using Quad9/Comss.one.
3.  **Background Sync**: Close AI app on watch. Tap "Sync" on phone. Open AI app on watch and confirm the key is updated.
4.  **Independent Buttons**: Confirm you can tap "Sync" even if "Verify" returned an error.
