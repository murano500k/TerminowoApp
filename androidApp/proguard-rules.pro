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
-keep,includedescriptorclasses class com.docscanner.**$$serializer { *; }
-keepclassmembers class com.docscanner.** {
    *** Companion;
}
-keepclasseswithmembers class com.docscanner.** {
    kotlinx.serialization.KSerializer serializer(...);
}
