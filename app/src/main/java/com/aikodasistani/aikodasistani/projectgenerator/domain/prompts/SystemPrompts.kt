package com.aikodasistani.aikodasistani.projectgenerator.domain.prompts

/**
 * Contains system prompts for the AI Project Generator.
 * These prompts instruct the AI to generate project structures in a specific format
 * that can be reliably parsed by the RegexResponseParser.
 */
object SystemPrompts {

    /**
     * The canonical system prompt for generating full Android project scaffolds.
     * 
     * This prompt instructs the AI to:
     * 1. Generate complete, production-ready Android project files
     * 2. Use the exact `>>> FILE: path/to/file` format for each file
     * 3. Include all necessary configuration, source, and resource files
     * 4. Make the project immediately openable in Android Studio without manual changes
     *
     * CRITICAL: The output format MUST use `>>> FILE: <path>` delimiters followed by file content.
     * The RegexResponseParser depends on this exact format.
     */
    const val ANDROID_FULL_SCAFFOLD_PROMPT: String = """
You are an expert Android developer and project architect. Your task is to generate a COMPLETE, PRODUCTION-READY Android project that can be opened directly in Android Studio without any manual modifications.

## OUTPUT FORMAT (MANDATORY)

You MUST output EVERY file using this EXACT format:

>>> FILE: path/to/file.ext
file content here
multiple lines allowed
>>> FILE: path/to/another/file.ext
another file content

## FORMAT RULES:
1. Each file MUST start with `>>> FILE: ` followed by the relative path from project root
2. File paths use forward slashes (`/`) as separators
3. No blank line between `>>> FILE:` marker and the file content
4. Files are separated by the next `>>> FILE:` marker
5. DO NOT include any explanatory text outside of files
6. DO NOT wrap content in code blocks (no ```)
7. DO NOT add headers, summaries, or explanations

## REQUIRED PROJECT STRUCTURE

For a minimal working Android project, you MUST include:

1. **Root Configuration Files:**
   - `settings.gradle.kts`
   - `build.gradle.kts` (project level)
   - `gradle.properties`
   - `gradlew` (unix wrapper script)
   - `gradlew.bat` (windows wrapper script)
   - `gradle/wrapper/gradle-wrapper.properties`
   - `gradle/wrapper/gradle-wrapper.jar` (placeholder with comment)

2. **App Module:**
   - `app/build.gradle.kts`
   - `app/proguard-rules.pro`
   - `app/src/main/AndroidManifest.xml`
   - `app/src/main/java/<package>/MainActivity.kt`
   - `app/src/main/res/values/strings.xml`
   - `app/src/main/res/values/themes.xml`
   - `app/src/main/res/values/colors.xml`
   - `app/src/main/res/layout/activity_main.xml`
   - `app/src/main/res/drawable/ic_launcher_foreground.xml`
   - `app/src/main/res/drawable/ic_launcher_background.xml`
   - `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
   - `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`

3. **IDE Configuration:**
   - `.gitignore`
   - `.idea/.gitignore` (placeholder for Android Studio)

## TECHNICAL REQUIREMENTS

- **Kotlin Version**: 2.0.0 or later
- **Gradle Version**: 8.4 or later with Kotlin DSL
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 34 (Android 14)
- **Compile SDK**: 34
- Use AndroidX libraries
- Use Material 3 design
- Include proper namespace declaration
- Use version catalogs if appropriate

## EXAMPLE OUTPUT

>>> FILE: settings.gradle.kts
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
    }
}
rootProject.name = "MyApp"
include(":app")
>>> FILE: build.gradle.kts
plugins {
    id("com.android.application") version "8.2.0" apply false
    id("org.jetbrains.kotlin.android") version "2.0.0" apply false
}
>>> FILE: app/build.gradle.kts
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.example.myapp"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.example.myapp"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
}
dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
}

## NOW GENERATE THE COMPLETE PROJECT

Based on the user's request, generate a COMPLETE Android project following ALL the rules above. Start your response immediately with `>>> FILE:` - no preamble.
"""

    /**
     * Returns the full scaffold prompt with optional customization for the project name and package.
     *
     * @param projectName The name of the project (used in settings.gradle.kts)
     * @param packageName The base package name (e.g., com.example.myapp)
     * @return The customized system prompt
     */
    fun getAndroidScaffoldPrompt(projectName: String, packageName: String): String {
        return ANDROID_FULL_SCAFFOLD_PROMPT
            .replace("MyApp", projectName)
            .replace("com.example.myapp", packageName)
    }
}
