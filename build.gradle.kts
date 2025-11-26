// Top-Level build file where you can add configuration options common to all sub-projects
plugins {
    id("com.android.application") version "8.5.0" apply false
    id("org.jetbrains.kotlin.android") version "2.2.21" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.21" apply false
    id("com.google.devtools.ksp") version "2.2.21-2.0.4" apply false
}

//💡 KSP versiyonunu burada tanımla
buildscript {
    dependencies {
        classpath("com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin:2.2.21-2.0.4")
    }
}

// ✅ 16 KB Alignment için ek konfigürasyon
allprojects {
    configurations.all {
        resolutionStrategy {
            // Native kütüphane çakışmalarını önle
            force(
                "androidx.core:core-ktx:1.13.1",
                "androidx.appcompat:appcompat:1.6.1",
                "com.google.android.material:material:1.11.0"
            )
        }
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}