# MotoNav Proguard Rules
-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
-dontwarn com.graphhopper.**
-keep class com.graphhopper.** { *; }
-dontwarn org.maplibre.**
-keep class org.maplibre.** { *; }
-dontwarn com.spotify.**
-keep class com.spotify.** { *; }
