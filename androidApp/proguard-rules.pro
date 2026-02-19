# Ktor
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.stc.terminowo.**$$serializer { *; }
-keepclassmembers class com.stc.terminowo.** {
    *** Companion;
}
-keepclasseswithmembers class com.stc.terminowo.** {
    kotlinx.serialization.KSerializer serializer(...);
}
