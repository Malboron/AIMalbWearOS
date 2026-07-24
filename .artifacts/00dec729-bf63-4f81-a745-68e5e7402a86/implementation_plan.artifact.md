# Wear OS AI Assistant - Extreme Speed Optimization (v46)

This plan focuses on eliminating all remaining UI and audio latencies to make the application feel "instant" on real hardware.

## Proposed Changes

### 1. Voice Engine (Minimal Latency)

#### [MODIFY] [TtsManager.kt](file:///C:/Users/kazakov_ai/AndroidStudioProjects/AIMalb/app/src/main/java/com/malbandco/aimalb/presentation/TtsManager.kt)
- **Zero Start Delay**: Remove the 100ms safety delay in `speak()`. The engine should start immediately.
- **Micro-Pause Between Lines**: Reduce the inter-phrase delay from 200ms to **50ms**. This maintains the distinction between sentences but feels much faster.
- **Pre-loading**: Ensure the next phrase is prepared as soon as the current one starts.

### 2. UI Transitions (High Response)

#### [MODIFY] [MainActivity.kt](file:///C:/Users/kazakov_ai/AndroidStudioProjects/AIMalb/app/src/main/java/com/malbandco/aimalb/presentation/MainActivity.kt)
- **Fast Animations**: Reduce the `animateFloatAsState` (alpha transition) duration from 300ms to **100ms**.
- **Snappy Scrolling**: Optimize the `animateScrollToItem` call. We'll use a faster animation spec to ensure the text "snaps" into the center position instantly as the voice moves.
- **Hysteresis Refinement**: Reduce the centering threshold to make the highlight switch more responsive to small movements.

### 3. Networking & Logic

#### [MODIFY] [AiRepository.kt](file:///C:/Users/kazakov_ai/AndroidStudioProjects/AIMalb/app/src/main/java/com/malbandco/aimalb/data/AiRepository.kt)
- **Streamlining**: Remove any hidden logging or non-essential processing during the main query flow.

## Verification Plan

### Manual Verification
1.  **Response Start**: Ask a short question. Confirm the voice starts almost immediately after the loading screen appears.
2.  **Rhythm Check**: Listen to a long paragraph. The 50ms pause should feel continuous yet punctuated.
3.  **UI Flick**: Manually scroll the list. Confirm the highlight shifts to the center item with zero perceived lag (100ms animation).
4.  **Auto-Scroll**: Confirm the list "flies" to the next spoken item without sluggishness.
