# Keep kotlinx.serialization generated serializers
-keepclassmembers class com.tokenmonitor.mobile.data.** {
    *** Companion;
}
-keepclasseswithmembers class com.tokenmonitor.mobile.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.tokenmonitor.mobile.data.**$$serializer { *; }
-keepclassmembers class com.tokenmonitor.mobile.data.** {
    *** Companion;
}
-keepclasseswithmembers class com.tokenmonitor.mobile.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}
