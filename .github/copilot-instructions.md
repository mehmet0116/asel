# Copilot Instructions for AIKodAsistani

This document provides instructions for GitHub Copilot when working with the AIKodAsistani Android application.

## Project Overview

AIKodAsistani is an Android AI Code Assistant application built with Kotlin. It integrates with multiple AI providers (OpenAI, DeepSeek, Gemini, Dashscope) and supports features like video analysis using CameraX.

## Tech Stack

- **Language**: Kotlin 1.9.23
- **Platform**: Android (SDK 26-34)
- **Build System**: Gradle 8.13 with Kotlin DSL
- **Architecture**: Activity-based architecture with ViewBinding and coroutines
- **Database**: Room 2.6.1
- **Networking**: OkHttp 4.12.0
- **AI Integration**: Google Generative AI SDK 0.9.0
- **Camera**: CameraX 1.3.2
- **Markdown Rendering**: Markwon 4.6.2

## Project Structure

```
app/
├── src/
│   ├── main/
│   │   ├── java/com/aikodasistani/aikodasistani/
│   │   │   ├── data/          # Data layer (Room entities, DAOs)
│   │   │   ├── util/          # Utility classes
│   │   │   └── *.kt           # Activities and components
│   │   ├── res/               # Android resources
│   │   └── AndroidManifest.xml
│   ├── androidTest/           # Instrumented tests
│   └── test/                  # Unit tests
├── build.gradle.kts
└── proguard-rules.pro
```

## Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Full build with tests
./gradlew build

# Run lint checks
./gradlew :app:lintDebug

# Clean build
./gradlew clean
```

## Code Conventions

- Use Kotlin idiomatic code patterns
- Follow Android naming conventions (camelCase for variables/functions, PascalCase for classes)
- Use ViewBinding for UI interactions, not findViewById
- Use coroutines for asynchronous operations with `lifecycleScope` or `viewModelScope`
- Comments may be written in Turkish or English (existing codebase uses both)
- Keep Activities focused; extract complex logic to utility classes or managers

## Security Guidelines

- Never commit API keys or secrets to the repository
- API keys are loaded from `local.properties` which is not tracked in git
- Use BuildConfig fields for accessing API keys at runtime
- Do not log sensitive data like API keys or user content

## Testing

- Unit tests are located in `app/src/test/`
- Instrumented tests are located in `app/src/androidTest/`
- Run unit tests: `./gradlew test`
- Run instrumented tests: `./gradlew connectedAndroidTest`

## Dependencies

When adding new dependencies:
- Check for Android compatibility
- Prefer AndroidX libraries over older support libraries
- Use KSP instead of kapt for annotation processors where supported
- Keep dependency versions up to date for security patches

## Important Notes

- The project uses 16 KB alignment for native libraries (Android 15+ compatibility)
- JVM target is set to Java 17
- The app supports multiple ABI architectures: armeabi-v7a, arm64-v8a, x86, x86_64
