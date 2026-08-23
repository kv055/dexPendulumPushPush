-keepattributes *Annotation*, InnerClasses

-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class de.dexarb.pushpushrelay.**$$serializer { *; }
-keepclassmembers class de.dexarb.pushpushrelay.** {
    *** Companion;
}
-keepclasseswithmembers class de.dexarb.pushpushrelay.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
