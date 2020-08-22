# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html
-printmapping out.map
-renamesourcefileattribute SourceFile 
-keepattributes SourceFile,LineNumberTable
# Exclude R from ProGuard to enable the libraries auto detection
-keep class .R
-keep class **.R$* {
    <fields>;
}
# okhttp3 warnings
-dontwarn org.conscrypt.**