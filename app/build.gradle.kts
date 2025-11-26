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
    compileSdk = 36

    defaultConfig {
        applicationId = "com.aikodasistani.aikodasistani"
        minSdk = 26
        targetSdk = 36
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

    buildFeatures {
        buildConfig = true
        viewBinding = true
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3"
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf("-opt-in=androidx.compose.material3.ExperimentalMaterial3Api")
    }

    packaging {
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
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.13.0")
    // Jetpack Compose
    implementation("androidx.activity:activity-compose:1.12.0")
    implementation("androidx.compose.ui:ui:1.9.5")
    implementation("androidx.compose.material3:material3:1.4.0")
    implementation("androidx.compose.ui:ui-tooling-preview:1.9.5")
    debugImplementation("androidx.compose.ui:ui-tooling:1.9.5")

    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("androidx.drawerlayout:drawerlayout:1.2.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")

    // CameraX Dependencies
    implementation("androidx.camera:camera-core:1.5.1")
    implementation("androidx.camera:camera-camera2:1.5.1")
    implementation("androidx.camera:camera-lifecycle:1.5.1")
    implementation("androidx.camera:camera-view:1.5.1")
    implementation("androidx.camera:camera-video:1.5.1")

    // Networking & Serialization
    implementation("com.squareup.okhttp3:okhttp:5.3.2")
    implementation("com.squareup.okhttp3:okhttp-sse:5.3.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")

    // Room Database
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    ksp("androidx.room:room-compiler:2.8.4")

    // Google Generative AI (for Gemini) - GÜNCELLENDİ
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0") // ✅ 2.5 flash desteği

    // Markdown Rendering
    implementation("io.noties.markwon:core:4.6.2")
    implementation("io.noties.markwon:ext-strikethrough:4.6.2")
    implementation("io.noties.markwon:ext-tables:4.6.2")
    implementation("io.noties.markwon:linkify:4.6.2")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

    // PDF okuma için iText - GÜNCELLENDİ
    implementation("com.itextpdf:itext7-core:9.4.0") // ✅ Yeni sürüm

    // Office dosyaları için - GÜNCELLENDİ
    implementation("org.apache.poi:poi:5.5.0") // ✅ Yeni sürüm
    implementation("org.apache.poi:poi-ooxml:5.5.0") // ✅ Yeni sürüm
    implementation("org.apache.xmlbeans:xmlbeans:5.3.0") // ✅ Yeni sürüm
    implementation("commons-io:commons-io:2.21.0") // ✅ Yeni sürüm

    // CSV okuma için
    implementation("com.opencsv:opencsv:5.12.0") // ✅ Yeni sürüm

    // HTML parsing için
    implementation("org.jsoup:jsoup:1.21.2") // ✅ Yeni sürüm

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")

    // ✅ 16 KB Alignment için ek optimizasyonlar
    implementation("androidx.tracing:tracing-ktx:1.3.0")
}
