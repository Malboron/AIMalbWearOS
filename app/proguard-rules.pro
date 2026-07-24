# Tink / Crypto dependencies
-dontwarn com.google.errorprone.annotations.**
-dontwarn com.google.api.client.http.**
-dontwarn org.joda.time.Instant
-keep class com.google.crypto.tink.** { *; }

# Google Play Services Wearable (Keep for Data Layer/Message API)
-keep class com.google.android.gms.wearable.** { *; }
-keep interface com.google.android.gms.wearable.** { *; }

# Our local sync service
-keep class com.malbandco.aimalb.data.local.KeySyncService { *; }

# General Compose / Wear rules
-keep class androidx.wear.compose.** { *; }
-keep class com.malbandco.aimalb.** { *; }
