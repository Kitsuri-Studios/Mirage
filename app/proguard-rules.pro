-keep class io.kitsuri.m1rage.patcher.Patcher { *; }
-keep class io.kitsuri.m1rage.utils.ManifestEditor { *; }
-keep class com.apk.axml.** { *; }
-keep class com.android.apksig.** { *; }
-dontwarn **
-renamesourcefileattribute null
-optimizationpasses 10
-allowaccessmodification
-repackageclasses ''
-flattenpackagehierarchy ''
-allowaccessmodification
-dontskipnonpubliclibraryclasses
-overloadaggressively
-useuniqueclassmembernames
-dontusemixedcaseclassnames
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
    }
