# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/cylee/Library/Android/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}
-dontwarn kotlin.**
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    static void checkParameterIsNotNull(java.lang.Object, java.lang.String);
}

-keep class com.babt.smarthome.entity.** {
   *;
}

-keep class com.babt.smarthome.model.** {
    *;
}
#PreferenceUtils相关类不混淆
-keep class * implements com.cylee.androidlib.util.PreferenceUtils$DefaultValueInterface { *;}