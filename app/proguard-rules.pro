# Keep jsoup (used for HTML parsing)
-keep class org.jsoup.** { *; }
-dontwarn org.jsoup.**

# Keep kotlinx.serialization
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
}
-keep class kotlinx.serialization.json.** { *; }

# Keep QuickJS JNI bridge
-keep class com.quickjs.** { *; }

# Keep update model classes (parsed from GitHub API JSON)
-keepclassmembers class com.kuroanime.data.update.** { *; }

# Keep Coil internals
-keep class coil.** { *; }
-dontwarn coil.**

# Keep Compose runtime
-keepattributes *Annotation*, EnclosingMethod, Signature
