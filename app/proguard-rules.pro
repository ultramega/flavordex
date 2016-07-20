# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\android-sdk/tools/proguard/proguard-android.txt
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

-keep public class android.support.v7.widget.ShareActionProvider {
    <init>(...);
}

# Needed to keep generic types and @Key annotations accessed via reflection
-keepattributes Signature,RuntimeVisibleAnnotations,AnnotationDefault
-keepclassmembers class * {
    @com.google.api.client.util.Key <fields>;
}

# com.google.client.util.IOUtils references java.nio.file.Files when on Java 7+
-dontnote java.nio.file.Files, java.nio.file.Path

# Suppress warnings on sun.misc.Unsafe
-dontnote sun.misc.Unsafe
-dontwarn sun.misc.Unsafe

# Gson
-keepattributes Signature
-keep class com.ultramegasoft.flavordex2.backend.model.** { *; }
