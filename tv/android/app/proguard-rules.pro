# Topster TV ProGuard Rules
# Following SmartTube's approach: minimal obfuscation for better debugging

# Don't obfuscate - keep class and method names readable (following SmartTube)
-dontobfuscate

# Keep source file and line numbers for stack traces
-keepattributes SourceFile,LineNumberTable

# Keep all Topster classes
-keep class com.topster.tv.** { *; }

# Keep Kotlin metadata
-keep class kotlin.Metadata { *; }
-keep class kotlin.** { *; }
-keep class kotlinx.** { *; }

# Keep coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# ExoPlayer rules
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# OkHttp rules
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Gson rules
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Keep data classes (network models)
-keep class com.topster.tv.network.** { *; }
-keep class com.topster.tv.player.** { *; }
-keep class com.topster.tv.api.models.** { *; }

# Room database
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Coil image loading
-keep class coil.** { *; }
-keep interface coil.** { *; }

# Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep Parcelable classes
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep enum classes
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Debugging attributes
-keepattributes *Annotation*
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable

# Remove logging in release builds (optional, uncomment if desired)
# -assumenosideeffects class android.util.Log {
#     public static *** d(...);
#     public static *** v(...);
# }
