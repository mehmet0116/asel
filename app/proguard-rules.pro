# Add project specific ProGuard rules here.

# Yeni eklenen kütüphaneler için ProGuard kuralları

# iText için
-keep public class com.itextpdf.** { public *; }
-dontwarn com.itextpdf.**

# PDFBox için
-keep class org.apache.pdfbox.** { *; }
-dontwarn org.apache.pdfbox.**

# POI için
-keep public class org.apache.poi.** { public *; }
-dontwarn org.apache.poi.**

# OpenCSV için
-keep public class com.opencsv.** { public *; }

# JSoup için
-keep public class org.jsoup.** { public *; }

# ✅ 16 KB ALIGNMENT EKLENTİLERİ - MEVCUT KURALLAR KORUNDU

# CameraX koruma
-keep public class androidx.camera.** { public *; }
-dontwarn androidx.camera.**

# Google AI Generative koruma
-keep class com.google.ai.** { *; }
-dontwarn com.google.ai.**

# Room Database koruma
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep public class androidx.room.** { public *; }

# OkHttp & Okio koruma
-keep public class okhttp3.** { public *; }
-keep class okio.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# Markwon markdown koruma
-keep class io.noties.markwon.** { *; }
-dontwarn io.noties.markwon.**

# Kotlin Coroutines koruma
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Kotlin Serialization koruma
-keep class kotlinx.serialization.** { *; }
-dontwarn kotlinx.serialization.**

# Native metod koruma (16 KB alignment için kritik)
-keepclasseswithmembernames class * {
    native <methods>;
}

# JNI sınıfları koruma
-keep class * implements org.apache.poi.** { *; }
-keep class * implements com.itextpdf.** { *; }

# Reflection kullanan sınıflar
-keep public class com.opencsv.** { public *; }
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

# AndroidX Lifecycle
-keep class androidx.lifecycle.** { *; }

# RecyclerView ve diğer AndroidX component'leri
-keep class androidx.recyclerview.** { *; }

# Material Design component'leri
-keep class com.google.android.material.** { *; }

# ✅ UNRESOLVED CLASS HATASI İÇİN EKLENTİLER

# Tüm Activity ve Fragment sınıflarını koru
-keep public class * extends android.app.Activity
-keep public class * extends androidx.fragment.app.Fragment
-keep public class * extends android.app.Application

# View ve View sınıflarını koru
-keep public class * extends android.view.View
-keep class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# Tüm custom view'ları koru
-keep class * extends android.view.View {
    *;
}

# Tüm layout ID'lerini koru
-keepclassmembers class * {
    public void onClick(android.view.View);
}

# Tüm inflate metodlarını koru
-keepclassmembers class * {
    public void set*(***);
    public *** get*();
}

# Serializable/Parcelable sınıfları koru
-keep class * implements java.io.Serializable
-keep class * implements android.os.Parcelable

# Serializable sınıflarının özel metodlarını koru
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Parcelable sınıflarının CREATOR alanını koru
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}

# Tüm enum sınıflarını koru
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Tüm annotation'ları koru
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Kotlin-specific korumalar
-keep class kotlin.Metadata { *; }
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

# Kotlin coroutines için
-keep class kotlinx.coroutines.android.** { *; }

# Data binding için
-keep class androidx.databinding.** { *; }
-dontwarn androidx.databinding.**

# View binding için
-keep class * extends androidx.viewbinding.ViewBinding

# ✅ 16 KB ALIGNMENT OPTİMİZASYONLARI
-optimizations !code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 3
-allowaccessmodification
-mergeinterfacesaggressively

# Gerekli olmayan uyarıları kapat (alignment için)
-dontwarn org.apache.**
-dontwarn com.google.**
-dontwarn androidx.**
-dontwarn kotlin.**
-dontwarn okio.**

# Native library hataları için
-dontwarn **.libimage_processing_util_jni
-dontwarn **.native**

# Resource dosyalarını koru
-keepclassmembers class **.R$* {
    public static <fields>;
}

# BuildConfig koruma
-keep class com.aikodasistani.aikodasistani.BuildConfig { *; }

# ✅ UYGULAMA SINIFINI VE PAKETİ KORU
-keep class com.aikodasistani.aikodasistani.** { *; }
-dontwarn com.aikodasistani.aikodasistani.**

# MainActivity ve diğer activity'leri koru
-keep class com.aikodasistani.aikodasistani.MainActivity { *; }
-keep class com.aikodasistani.aikodasistani.SessionsActivity { *; }
-keep class com.aikodasistani.aikodasistani.VideoCaptureActivity { *; }

# Util sınıflarını koru
-keep class com.aikodasistani.aikodasistani.util.** { *; }
-keep class com.aikodasistani.aikodasistani.data.** { *; }

# Video analiz sınıflarını koru
-keep class com.aikodasistani.aikodasistani.VideoAnalysisManager { *; }
-keep class com.aikodasistani.aikodasistani.VideoAnalysisResult { *; }
-keep class com.aikodasistani.aikodasistani.VideoInfo { *; }

# MessageAdapter ve diğer adapter'ları koru
-keep class com.aikodasistani.aikodasistani.MessageAdapter { *; }
-keep class com.aikodasistani.aikodasistani.SessionAdapter { *; }

# Tüm inner class'ları koru
-keepclassmembers class **$* {
    *;
}
