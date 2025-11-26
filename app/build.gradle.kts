import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
}

// FIX for "Duplicate Class" errors by excluding the problematic module globally
configurations.all {
    exclude(group = "org.jetbrains", module = "annotations-java5")
}

val props = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) load(f.inputStream())
}
val openai: String = props.getProperty("OPENAI_API_KEY", "")
val deepseek: String = props.getProperty("DEEPSEEK_API_KEY", "")
val gemini: String = props.getProperty("GEMINI_API_KEY", "")
val dashscope: String = props.getProperty("DASHSCOPE_API_KEY", "")

android {
    namespace = "com.aikodasistani.aikodasistani"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.aikodasistani.aikodasistani"
        minSdk = 26
        targetSdk = 34
        versionCode = 3
        versionName = "1.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "OPENAI_API_KEY", "\"$openai\"")
        buildConfigField("String", "DEEPSEEK_API_KEY", "\"$deepseek\"")
        buildConfigField("String", "GEMINI_API_KEY", "\"$gemini\"")
        buildConfigField("String", "DASHSCOPE_API_KEY", "\"$dashscope\"")

        // ✅ 16 KB Alignment için NDK ayarları
        ndk {
            abiFilters.addAll(setOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64"))
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }

    // ✅ 16 KB Alignment FIX - Kritik packaging options
    packagingOptions {
        jniLibs {
            useLegacyPackaging = false
            pickFirsts += setOf("**/libc++_shared.so", "**/libopenh264.so")
            // ✅ Yeni paketleme sistemi
        }
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/*.version"
            excludes += "**/libimage_processing_util_jni.so" // ✅ Sorunlu kütüphaneyi exclude et
        }
        // ✅ Native library alignment için
    }

    // ✅ Native kütüphane optimizasyonları
    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            isUniversalApk = true
        }
    }
}

dependencies {
    // AndroidX Core
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.drawerlayout:drawerlayout:1.2.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

    // CameraX Dependencies
    implementation("androidx.camera:camera-core:1.3.2")
    implementation("androidx.camera:camera-camera2:1.3.2")
    implementation("androidx.camera:camera-lifecycle:1.3.2")
    implementation("androidx.camera:camera-view:1.3.2")
    implementation("androidx.camera:camera-video:1.3.2")

    // Networking & Serialization
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:okhttp-sse:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // Room Database - Updated to 2.7.0-alpha11 for Kotlin 2.2.21 compatibility
    implementation("androidx.room:room-runtime:2.7.0-alpha11")
    implementation("androidx.room:room-ktx:2.7.0-alpha11")
    ksp("androidx.room:room-compiler:2.7.0-alpha11")

    // Google Generative AI (for Gemini) - GÜNCELLENDİ
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0") // ✅ 2.5 flash desteği

    // Markdown Rendering
    implementation("io.noties.markwon:core:4.6.2")
    implementation("io.noties.markwon:ext-strikethrough:4.6.2")
    implementation("io.noties.markwon:ext-tables:4.6.2")
    implementation("io.noties.markwon:linkify:4.6.2")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // PDF okuma için iText - GÜNCELLENDİ
    implementation("com.itextpdf:itext7-core:8.0.2") // ✅ Yeni sürüm

    // Office dosyaları için - GÜNCELLENDİ
    implementation("org.apache.poi:poi:5.2.5") // ✅ Yeni sürüm
    implementation("org.apache.poi:poi-ooxml:5.2.5") // ✅ Yeni sürüm
    implementation("org.apache.xmlbeans:xmlbeans:5.2.0") // ✅ Yeni sürüm
    implementation("commons-io:commons-io:2.15.1") // ✅ Yeni sürüm

    // CSV okuma için
    implementation("com.opencsv:opencsv:5.9") // ✅ Yeni sürüm

    // HTML parsing için
    implementation("org.jsoup:jsoup:1.17.2") // ✅ Yeni sürüm

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    // ✅ 16 KB Alignment için ek optimizasyonlar
    implementation("androidx.tracing:tracing-ktx:1.2.0")
}