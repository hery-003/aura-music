# Compose (R8 handles most Compose optimizations automatically)
-dontwarn androidx.compose.**

# Hilt
-dontwarn dagger.hilt.**
-keep class dagger.hilt.** { *; }

# Media3
-dontwarn androidx.media3.**

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @interface androidx.room.*
-keep class **.*_Impl { *; }

# DataStore
-dontwarn androidx.datastore.**

# Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Kotlin metadata
-keep class kotlin.Metadata { *; }

# Serialization
-keepattributes *Annotation*, Signature, EnclosingMethod
