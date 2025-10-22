

-keep class androidx.media3.** { *; }
-keep interface androidx.media3.** { *; }
-dontwarn androidx.media3.**

-keep class org.jellyfin.** { *; }
-keep interface org.jellyfin.** { *; }
-dontwarn org.jellyfin.**

-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.hritwik.avoid.**$$serializer { *; }
-keepclassmembers class com.hritwik.avoid.** {
    *** Companion;
}
-keepclasseswithmembers class com.hritwik.avoid.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-dontwarn dagger.hilt.android.internal.managers.**
-dontwarn dagger.hilt.android.internal.modules.**

-keep class coil.** { *; }
-keep interface coil.** { *; }

-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

-keep class androidx.media3.exoplayer.mediacodec.** { *; }
-keep class androidx.media3.decoder.** { *; }

-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider