# Implementation Plan - Scrolling Baseline & Replay Optimization (v1.6.8)

This plan fixes the text-audio lag by increasing the character baseline, ensures the last line is never missed by refining the splitter, and optimizes the "Replay" feature to work locally.

## Proposed Changes

### 1. Watch App: Precision Scrolling & Splitting

#### [MODIFY] [MainViewModel.kt](file:///C:/Users/kazakov_ai/AndroidStudioProjects/AIMalb/app/src/main/java/com/malbandco/aimalb/presentation/MainViewModel.kt)
- **Tempo Calibration**: Increase `baseCharsPerSecond` from 14.5f to **17.5f**.
    - **Reason**: 14.5 was proven too slow for modern neural voices, causing the text to lag behind the audio. 17.5 matches the actual "speech-to-character" density of the Ava voice.
- **Splitter Tail Fix**: Refactor `smartSplit()` to ensure any remaining text after the last delimiter is explicitly added as the final segment. This fixes the "spoken but not shown" bug.
- **Speed Multiplier**: Re-verify that `getTtsSpeed()` is strictly factored into the absolute time calculation.

### 2. Watch App: Instant Local Replay

#### [MODIFY] [MainViewModel.kt](file:///C:/Users/kazakov_ai/AndroidStudioProjects/AIMalb/app/src/main/java/com/malbandco/aimalb/presentation/MainViewModel.kt)
- **Toggle Optimization**: In `togglePauseResume()`, when state is `FINISHED`:
    - Do NOT call `startResponding()` (which re-downloads the file).
    - Call `cloudTtsManager?.restart()` or `ttsManager?.restart()`.
    - Manually trigger `startEstimatedSync()` to restore scrolling for the cached audio.

### 3. Build & Versioning (Build 180)
- Update app version to **`v1.6.8-beta`** (Build **`180`**).
- Maintain strict naming: **`AIMalb1.6.8-beta-release.apk`**.

## User Review Required

> [!TIP]
> **Performance**: By using the local cache for replays, we save approx. 2-3 seconds of loading time and reduce API costs/network usage. The "Last line" fix ensures the text list is a 100% faithful representation of the audio content.

## Verification Plan
1.  **Last Line Test**: Ask for a short sentence without a dot (e.g., "Tell me one word"). Verify it appears in the list.
2.  **Sync Test (1.15x)**: Verify the text no longer drifts and stays aligned with the voice.
3.  **Replay Test**: Click Replay. Verify the audio starts **instantly** without the "Preparing voice..." loading state.
