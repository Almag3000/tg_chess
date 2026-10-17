# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.almag.audioshelf.**$$serializer { *; }
-keepclassmembers class com.almag.audioshelf.** { *** Companion; }
-keepclasseswithmembers class com.almag.audioshelf.** { kotlinx.serialization.KSerializer serializer(...); }
