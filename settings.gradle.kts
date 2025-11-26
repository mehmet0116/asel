pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        // Additional repositories for plugins (helpful for KSP and Kotlin plugin artifacts)
        maven("https://maven.pkg.jetbrains.space/public/p/kotlinx/maven")
        maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev")
        maven("https://plugins.gradle.org/m2/")
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://repo1.maven.org/maven2") }
        maven { url = uri("https://storage.googleapis.com/generative-ai-android/maven/") }
    }
}

rootProject.name = "AIKodAsistani"
include(":app")