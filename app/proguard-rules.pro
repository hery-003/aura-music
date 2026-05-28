# Keep Compose
-dontwarn androidx.compose.**
-keep class androidx.compose.** { *; }

# Keep Coil
-dontwarn coil.**

# Keep Hilt
-dontwarn dagger.hilt.**
-keep class dagger.hilt.** { *; }
-keep class dagger.hilt.android.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Keep Media3
-dontwarn androidx.media3.**
-keep class androidx.media3.** { *; }

# Keep Room
-dontwarn androidx.room.**
-keep class * extends androidx.room.RoomDatabase
-keep @interface androidx.room.*
-keep class * extends androidx.room.* { *; }
-keep class **.*_Impl { *; }

# Keep Kotlin Coroutines
-dontwarn kotlinx.coroutines.**
-keep class kotlinx.coroutines.** { *; }
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Keep DataStore
-dontwarn androidx.datastore.**
-keep class androidx.datastore.** { *; }

# Keep Kotlin metadata (required for reflection in Hilt/Room)
-keep class kotlin.Metadata { *; }
-keep class kotlin.reflect.** { *; }

# Keep Gson/Serialization if used
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
