# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep line numbers and source file attributes for Play Console deobfuscation
-keepattributes SourceFile,LineNumberTable,*Annotation*,Signature,EnclosingMethod,InnerClasses
-renamesourcefileattribute SourceFile

# Keep all app code in com.watchable package completely
-keep class com.watchable.** { *; }
-keepclassmembers class com.watchable.** { *; }

# Keep all Serializable and Parcelable implementations
-keepnames class * implements java.io.Serializable
-keepclassmembers class * implements java.io.Serializable {
    *;
}
-keep class * implements java.io.Serializable {
    *;
}
-keep class * implements android.os.Parcelable {
    *;
}

# Keep Fragment and Activity classes
-keep public class * extends androidx.fragment.app.Fragment {
    public <init>();
    *;
}
-keep public class * extends androidx.appcompat.app.AppCompatActivity {
    public <init>();
    *;
}
-keep public class * extends androidx.recyclerview.widget.RecyclerView$Adapter {
    *;
}
-keep public class * extends androidx.recyclerview.widget.RecyclerView$ViewHolder {
    *;
}

# Gson rules
-keepattributes *Annotation*
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.google.gson.** { *; }

# Retrofit / OkHttp / Moshi
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule {
    <init>(...);
}
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
    **[] $VALUES;
    public *;
}

# Google Play Ads / Services
-keep public class com.google.android.gms.ads.** {
   public *;
}
-keep public class com.google.ads.** {
   public *;
}

# Google Play In-App Updates
-keep class com.google.android.play.core.** { *; }

# WebView JavaScript interface
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

