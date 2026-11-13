# GitHub Vault — R8 rules (v1.2.0 CSS-only UI)

# Keep the WebView JS bridge surface: JS calls postMessage and Kotlin dispatches by string action.
-keepclassmembers class com.saged.githubvault.core.ui.WebVaultKt { *; }

# kotlinx.serialization: keep serializers for data classes sent over the bridge/DB payloads.
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class kotlinx.serialization.json.** { *; }
-dontnote kotlinx.serialization.**
-keep,includedescriptorclasses class com.saged.githubvault.**$$serializer { *; }
-keepclassmembers class com.saged.githubvault.** {
    *** Companion;
}
-keepclasseswithmembers class com.saged.githubvault.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Room entities and WorkManager workers are looked up reflectively.
-keep class com.saged.githubvault.core.database.** { *; }
-keep class com.saged.githubvault.feature.downloads.** { *; }
-keep class com.saged.githubvault.feature.github.PublicTransferWorker { *; }
-keepclassmembers class * extends androidx.work.CoroutineWorker { <init>(...); }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Supabase config fields read at runtime
-keepclassmembers class com.saged.githubvault.BuildConfig { *; }
