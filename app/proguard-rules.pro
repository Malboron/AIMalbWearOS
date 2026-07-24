# Tink / Crypto dependencies
-dontwarn com.google.errorprone.annotations.**
-dontwarn com.google.api.client.http.**
-dontwarn org.joda.time.Instant
-keep class com.google.crypto.tink.** { *; }

# General Compose / Wear rules
-keep class androidx.wear.compose.** { *; }
-keep class com.malbandco.aimalb.** { *; }
