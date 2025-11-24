pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
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