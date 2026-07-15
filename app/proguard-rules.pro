# ============================================
# Aura Music — ProGuard / R8 Rules (Release)
# ============================================

# ---- Compose (R8 handles most Compose optimizations automatically) ----
-dontwarn androidx.compose.**
-keep class androidx.compose.** { *; }

# ---- Hilt / Dagger ----
-dontwarn dagger.hilt.**
-keep class dagger.hilt.** { *; }
-keep class dagger.hilt.internal.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$ActivityContextWrapper { *; }
-keepclasseswithmembers class * {
    @dagger.hilt.android.EarlyEntryPoint <methods>;
}
-keepclassmembers class * {
    @dagger.hilt.android.internal.managers.HiltWrapper_xxx <fields>;
}

# ---- Media3 (ExoPlayer) ----
-dontwarn androidx.media3.**
-keep class androidx.media3.** { *; }
-keepclassmembers class androidx.media3.** { *; }

# ---- Room ----
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @interface androidx.room.*
-keep class **.*_Impl { *; }
-keep class **.*Dao { *; }
-keep class **.*Dao_Impl { *; }
-keep class **.*Database { *; }
-keep class **.*Database_Impl { *; }
-keepclassmembers class * {
    @androidx.room.Entity <fields>;
    @androidx.room.ColumnInfo <fields>;
    @androidx.room.PrimaryKey <fields>;
    @androidx.room.ForeignKey <fields>;
    @androidx.room.Index <fields>;
    @androidx.room.Ignore <fields>;
    @androidx.room.Embedded <fields>;
    @androidx.room.Relation <fields>;
}

# ---- Room entities (model classes) ----
-keep class com.auramusic.data.local.entity.** { *; }
-keepclassmembers class com.auramusic.data.local.entity.** { *; }

# ---- DataStore ----
-dontwarn androidx.datastore.**
-keep class androidx.datastore.** { *; }

# ---- Kotlin Coroutines ----
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ---- Kotlin Metadata ----
-keep class kotlin.Metadata { *; }
-keepattributes *Annotation*, Signature, EnclosingMethod, InnerClasses, RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations, RuntimeInvisibleAnnotations

# ---- Kotlin Serialization ----
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class kotlinx.serialization.** { *; }
-keepclasseswithmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keepclasseswithmembers class * {
    @kotlinx.serialization.SerialName <methods>;
    @kotlinx.serialization.SerialName <fields>;
}
-keep class * {
    @kotlinx.serialization.Serializable *;
}
-keepclassmembers class * {
    @kotlinx.serialization.Serializer <methods>;
}

# ---- Coil ----
-dontwarn coil.**
-keep class coil.** { *; }
-keepclassmembers class coil.** { *; }

# ---- Firebase Crashlytics ----
-dontwarn com.google.firebase.crashlytics.**
-keep class com.google.firebase.crashlytics.** { *; }
-keepattributes SourceFile, LineNumberTable
-keep class com.auramusic.BuildConfig { *; }

# ---- Firebase Analytics ----
-dontwarn com.google.firebase.analytics.**
-keep class com.google.firebase.analytics.** { *; }

# ---- WorkManager ----
-dontwarn androidx.work.**
-keep class androidx.work.** { *; }
-keep class * extends androidx.work.Worker { *; }

# ---- Navigation Compose ----
-dontwarn androidx.navigation.**
-keep class androidx.navigation.** { *; }

# ---- Lifecycle ----
-dontwarn androidx.lifecycle.**
-keep class androidx.lifecycle.** { *; }

# ---- Hilt ViewModel ----
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keepclassmembers class * {
    @dagger.hilt.android.lifecycle.HiltViewModelMap <fields>;
}

# ---- Keep Application class ----
-keep class com.auramusic.AuraMusicApplication { *; }

# ---- Keep custom exceptions ----
-keep class com.auramusic.** { *; }

# ---- Keep Parcelable / Serializable ----
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}
-keep class * implements java.io.Serializable { *; }
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ---- Keep R8 optimizations for Compose (already enabled by default) ----
-assumevalues class java.lang.Boolean {
    boolean booleanValue() return false;
}
