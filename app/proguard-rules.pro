# ── kotlinx-serialization ─────────────────────────────────────────────
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep all DTO classes used by the API
-keepclassmembers @kotlinx.serialization.Serializable class ru.diamko.paleta.data.remote.dto.** {
    *;
}
-keep class ru.diamko.paleta.data.remote.dto.** { *; }

# ── Retrofit ─────────────────────────────────────────────────────────
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# Keep API interfaces
-keep interface ru.diamko.paleta.data.remote.api.** { *; }

# ── OkHttp ───────────────────────────────────────────────────────────
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**

# ── Room ─────────────────────────────────────────────────────────────
-keep class ru.diamko.paleta.data.local.entity.** { *; }
-keep class ru.diamko.paleta.data.local.dao.** { *; }
