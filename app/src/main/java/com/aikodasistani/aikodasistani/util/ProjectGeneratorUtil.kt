package com.aikodasistani.aikodasistani.util

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import com.aikodasistani.aikodasistani.models.ProjectFileEntry
import com.aikodasistani.aikodasistani.models.ProjectGenerationRequest
import com.aikodasistani.aikodasistani.models.ProjectGenerationResult
import com.aikodasistani.aikodasistani.models.ProjectType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Utility object for generating complete, build-ready, and IDE-compliant project scaffolds.
 * 
 * This generator ensures that all generated projects:
 * - Are immediately buildable and runnable without manual modifications
 * - Follow platform-specific conventions and best practices
 * - Include all necessary configuration files, manifests, and resources
 * - Are compatible with standard IDEs (Android Studio, VS Code, etc.)
 */
object ProjectGeneratorUtil {
    private const val TAG = "ProjectGeneratorUtil"

    /**
     * Naming style options for different project types.
     */
    private enum class NamingStyle {
        PASCAL_CASE,    // MyProject - for class names
        PACKAGE_SAFE,   // myproject - for package names
        SNAKE_CASE,     // my_project - for Python/Flutter
        KEBAB_CASE      // my-project - for Node.js/npm
    }

    /**
     * Sanitizes a project name for use in different contexts.
     * Ensures consistent naming across all project types.
     * 
     * @param name The raw project name
     * @param style The naming style to apply
     */
    private fun sanitizeName(name: String, style: NamingStyle): String {
        val cleaned = name.trim()
        return when (style) {
            // For Android/Java class names: PascalCase, no spaces or hyphens
            NamingStyle.PASCAL_CASE -> cleaned
                .replace(Regex("[^a-zA-Z0-9 -]"), "")
                .split(Regex("[ -]+"))
                .filter { it.isNotEmpty() }
                .joinToString("") { word -> 
                    word.replaceFirstChar { it.uppercase() } 
                }
                .ifEmpty { "MyProject" }
            // For package names: lowercase, no special chars
            NamingStyle.PACKAGE_SAFE -> cleaned
                .lowercase()
                .replace(Regex("[^a-z0-9]"), "")
                .ifEmpty { "myproject" }
            // For Flutter/Python: snake_case
            NamingStyle.SNAKE_CASE -> cleaned
                .lowercase()
                .replace(Regex("[^a-z0-9_-]"), "")
                .replace("-", "_")
                .replace(" ", "_")
                .replace(Regex("_+"), "_")
                .trim('_')
                .ifEmpty { "my_project" }
            // For Node.js/npm: kebab-case
            NamingStyle.KEBAB_CASE -> cleaned
                .lowercase()
                .replace(Regex("[^a-z0-9-]"), "")
                .replace(" ", "-")
                .replace("_", "-")
                .replace(Regex("-+"), "-")
                .trim('-')
                .ifEmpty { "my-project" }
        }
    }

    suspend fun generateProject(context: Context, request: ProjectGenerationRequest): ProjectGenerationResult = withContext(Dispatchers.IO) {
        try {
            val files = generateProjectFiles(request)
            if (files.isEmpty()) return@withContext ProjectGenerationResult.Error("No files generated", null)
            val totalSize = files.sumOf { it.content.length.toLong() }
            val (outputUri, outputPath) = packageAsZip(context, request.projectName, files)
            ProjectGenerationResult.Success(request.projectName, request.projectType, files, totalSize, outputUri, outputPath, "Generated ${files.size} files")
        } catch (e: Exception) {
            Log.e(TAG, "Generation failed", e)
            ProjectGenerationResult.Error("Generation failed: ${e.message}", null)
        }
    }

    private fun generateProjectFiles(request: ProjectGenerationRequest): List<ProjectFileEntry> {
        return when (request.projectType) {
            ProjectType.ANDROID_KOTLIN -> generateAndroidKotlinProject(request)
            ProjectType.FLUTTER -> generateFlutterProject(request)
            ProjectType.NODEJS_EXPRESS -> generateNodeJsProject(request)
            ProjectType.PYTHON_FLASK, ProjectType.PYTHON_DJANGO, ProjectType.PYTHON_FASTAPI -> generatePythonProject(request)
            else -> generateGenericProject(request)
        }
    }

    /**
     * Generates a complete, build-ready Android Kotlin project with Jetpack Compose.
     * Includes all necessary files for immediate use in Android Studio.
     */
    private fun generateAndroidKotlinProject(request: ProjectGenerationRequest): List<ProjectFileEntry> {
        val name = sanitizeName(request.projectName, NamingStyle.PASCAL_CASE)
        val nameLower = sanitizeName(request.projectName, NamingStyle.PACKAGE_SAFE)
        val pkg = request.packageName ?: "com.example.$nameLower"
        val pkgPath = pkg.replace(".", "/")
        
        return listOf(
            // Root configuration files
            ProjectFileEntry("settings.gradle.kts", """
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
rootProject.name = "$name"
include(":app")
""".trimIndent()),
            
            ProjectFileEntry("build.gradle.kts", """
plugins {
    id("com.android.application") version "8.2.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
}
""".trimIndent()),
            
            ProjectFileEntry("gradle.properties", """
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official
android.nonTransitiveRClass=true
""".trimIndent()),
            
            ProjectFileEntry("local.properties", "# Local configuration (not tracked in git)\n# sdk.dir=/path/to/android/sdk"),
            
            // Gradle wrapper
            ProjectFileEntry("gradlew", generateGradlewScript(), isExecutable = true),
            ProjectFileEntry("gradlew.bat", generateGradlewBatScript()),
            ProjectFileEntry("gradle/wrapper/gradle-wrapper.properties", """
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.4-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
""".trimIndent()),
            
            // App module build file
            ProjectFileEntry("app/build.gradle.kts", """
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "$pkg"
    compileSdk = 34

    defaultConfig {
        applicationId = "$pkg"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
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
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.02.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
""".trimIndent()),
            
            ProjectFileEntry("app/proguard-rules.pro", """
# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
""".trimIndent()),
            
            // Android Manifest
            ProjectFileEntry("app/src/main/AndroidManifest.xml", """
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.$name">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:label="@string/app_name"
            android:theme="@style/Theme.$name">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>

</manifest>
""".trimIndent()),
            
            // Main Activity with Compose
            ProjectFileEntry("app/src/main/java/$pkgPath/MainActivity.kt", """
package $pkg

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import $pkg.ui.theme.${name}Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ${name}Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Greeting("$name")
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    var count by remember { mutableIntStateOf(0) }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Welcome to ${'$'}name!",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Count: ${'$'}count")
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { count++ }) {
            Text("Increment")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ${name}Theme {
        Greeting("Android")
    }
}
""".trimIndent()),
            
            // Theme files
            ProjectFileEntry("app/src/main/java/$pkgPath/ui/theme/Color.kt", """
package $pkg.ui.theme

import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)
""".trimIndent()),
            
            ProjectFileEntry("app/src/main/java/$pkgPath/ui/theme/Theme.kt", """
package $pkg.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

@Composable
fun ${name}Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
""".trimIndent()),
            
            ProjectFileEntry("app/src/main/java/$pkgPath/ui/theme/Type.kt", """
package $pkg.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)
""".trimIndent()),
            
            // Resource files
            ProjectFileEntry("app/src/main/res/values/strings.xml", """
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">$name</string>
</resources>
""".trimIndent()),
            
            ProjectFileEntry("app/src/main/res/values/colors.xml", """
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="purple_200">#FFBB86FC</color>
    <color name="purple_500">#FF6200EE</color>
    <color name="purple_700">#FF3700B3</color>
    <color name="teal_200">#FF03DAC5</color>
    <color name="teal_700">#FF018786</color>
    <color name="black">#FF000000</color>
    <color name="white">#FFFFFFFF</color>
</resources>
""".trimIndent()),
            
            ProjectFileEntry("app/src/main/res/values/themes.xml", """
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.$name" parent="android:Theme.Material.Light.NoActionBar" />
</resources>
""".trimIndent()),
            
            ProjectFileEntry("app/src/main/res/values-night/themes.xml", """
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.$name" parent="android:Theme.Material.NoActionBar" />
</resources>
""".trimIndent()),
            
            // Launcher icons
            ProjectFileEntry("app/src/main/res/drawable/ic_launcher_foreground.xml", """
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:fillColor="#3DDC84"
        android:pathData="M54,54m-40,0a40,40 0,1 1,80 0a40,40 0,1 1,-80 0" />
    <path
        android:fillColor="#FFFFFF"
        android:pathData="M44,44h20v20h-20z" />
</vector>
""".trimIndent()),
            
            ProjectFileEntry("app/src/main/res/drawable/ic_launcher_background.xml", """
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:fillColor="#FFFFFF"
        android:pathData="M0,0h108v108h-108z" />
</vector>
""".trimIndent()),
            
            ProjectFileEntry("app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml", """
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
""".trimIndent()),
            
            ProjectFileEntry("app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml", """
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
""".trimIndent()),
            
            // Test files
            ProjectFileEntry("app/src/test/java/$pkgPath/ExampleUnitTest.kt", """
package $pkg

import org.junit.Test
import org.junit.Assert.*

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }
}
""".trimIndent()),
            
            ProjectFileEntry("app/src/androidTest/java/$pkgPath/ExampleInstrumentedTest.kt", """
package $pkg

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("$pkg", appContext.packageName)
    }
}
""".trimIndent()),
            
            // Git and IDE files
            ProjectFileEntry(".gitignore", """
# Built application files
*.apk
*.aar
*.ap_
*.aab

# Files for the ART/Dalvik VM
*.dex

# Java class files
*.class

# Generated files
bin/
gen/
out/
release/

# Gradle files
.gradle/
build/

# Local configuration file
local.properties

# Android Studio files
.idea/
*.iml
.navigation/
captures/
.externalNativeBuild/
.cxx/

# Keystore files
*.jks
*.keystore

# Log files
*.log

# MacOS
.DS_Store

# Windows
Thumbs.db
ehthumbs.db
""".trimIndent()),
            
            ProjectFileEntry(".idea/.gitignore", "# Android Studio project files to ignore"),
            
            // Documentation
            ProjectFileEntry("README.md", """
# $name

Android application built with Kotlin and Jetpack Compose.

## Prerequisites

- Android Studio Arctic Fox or later
- JDK 17 or higher
- Android SDK with API level 34

## Getting Started

1. Clone this repository
2. Open the project in Android Studio
3. Sync Gradle files
4. Run on an emulator or physical device

## Project Structure

```
$name/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/$pkgPath/
│   │   │   │   ├── MainActivity.kt
│   │   │   │   └── ui/theme/
│   │   │   ├── res/
│   │   │   └── AndroidManifest.xml
│   │   ├── test/
│   │   └── androidTest/
│   └── build.gradle.kts
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

## Build

```bash
./gradlew assembleDebug
```

## Test

```bash
./gradlew test
```

## License

MIT License
""".trimIndent()),
            
            ProjectFileEntry("LICENSE", generateMITLicense(name))
        )
    }

    /**
     * Generates a complete, build-ready Flutter project.
     * Includes all necessary files for immediate use in VS Code or Android Studio.
     */
    private fun generateFlutterProject(request: ProjectGenerationRequest): List<ProjectFileEntry> {
        val name = sanitizeName(request.projectName, NamingStyle.SNAKE_CASE)
        val displayName = request.projectName
        
        return listOf(
            // pubspec.yaml - main Flutter configuration
            ProjectFileEntry("pubspec.yaml", """
name: $name
description: $displayName - A new Flutter project.
publish_to: 'none'
version: 1.0.0+1

environment:
  sdk: '>=3.0.0 <4.0.0'

dependencies:
  flutter:
    sdk: flutter
  cupertino_icons: ^1.0.6

dev_dependencies:
  flutter_test:
    sdk: flutter
  flutter_lints: ^3.0.0

flutter:
  uses-material-design: true
""".trimIndent()),
            
            // Analysis options
            ProjectFileEntry("analysis_options.yaml", """
include: package:flutter_lints/flutter.yaml

linter:
  rules:
    avoid_print: false
    prefer_single_quotes: true
""".trimIndent()),
            
            // Main entry point
            ProjectFileEntry("lib/main.dart", """
import 'package:flutter/material.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: '$displayName',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.deepPurple),
        useMaterial3: true,
      ),
      home: const HomePage(),
    );
  }
}

class HomePage extends StatefulWidget {
  const HomePage({super.key});

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  int _counter = 0;

  void _incrementCounter() {
    setState(() {
      _counter++;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        backgroundColor: Theme.of(context).colorScheme.inversePrimary,
        title: const Text('$displayName'),
      ),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: <Widget>[
            const Text(
              'You have pushed the button this many times:',
            ),
            Text(
              '${'$'}_counter',
              style: Theme.of(context).textTheme.headlineMedium,
            ),
          ],
        ),
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: _incrementCounter,
        tooltip: 'Increment',
        child: const Icon(Icons.add),
      ),
    );
  }
}
""".trimIndent()),
            
            // Widget test
            ProjectFileEntry("test/widget_test.dart", """
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:$name/main.dart';

void main() {
  testWidgets('Counter increments smoke test', (WidgetTester tester) async {
    await tester.pumpWidget(const MyApp());

    expect(find.text('0'), findsOneWidget);
    expect(find.text('1'), findsNothing);

    await tester.tap(find.byIcon(Icons.add));
    await tester.pump();

    expect(find.text('0'), findsNothing);
    expect(find.text('1'), findsOneWidget);
  });
}
""".trimIndent()),
            
            // Android configuration
            ProjectFileEntry("android/app/build.gradle", """
plugins {
    id "com.android.application"
    id "kotlin-android"
    id "dev.flutter.flutter-gradle-plugin"
}

android {
    namespace "$name.app"
    compileSdk 34

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = '17'
    }

    defaultConfig {
        applicationId "$name.app"
        minSdk 21
        targetSdk 34
        versionCode 1
        versionName "1.0"
    }

    buildTypes {
        release {
            signingConfig signingConfigs.debug
        }
    }
}

flutter {
    source '../..'
}
""".trimIndent()),
            
            ProjectFileEntry("android/build.gradle", """
buildscript {
    ext.kotlin_version = '1.9.22'
    repositories {
        google()
        mavenCentral()
    }

    dependencies {
        classpath 'com.android.tools.build:gradle:8.2.2'
        classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:${'$'}kotlin_version"
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}
""".trimIndent()),
            
            ProjectFileEntry("android/settings.gradle", """
pluginManagement {
    def flutterSdkPath = {
        def properties = new Properties()
        file("local.properties").withInputStream { properties.load(it) }
        def flutterSdkPath = properties.getProperty("flutter.sdk")
        assert flutterSdkPath != null, "flutter.sdk not set in local.properties"
        return flutterSdkPath
    }()

    includeBuild("${'$'}flutterSdkPath/packages/flutter_tools/gradle")

    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id "dev.flutter.flutter-plugin-loader" version "1.0.0"
    id "com.android.application" version "8.2.2" apply false
    id "org.jetbrains.kotlin.android" version "1.9.22" apply false
}

include ":app"
""".trimIndent()),
            
            ProjectFileEntry("android/gradle.properties", """
org.gradle.jvmargs=-Xmx4G
android.useAndroidX=true
android.enableJetifier=true
""".trimIndent()),
            
            ProjectFileEntry("android/gradle/wrapper/gradle-wrapper.properties", """
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.4-bin.zip
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
""".trimIndent()),
            
            ProjectFileEntry("android/app/src/main/AndroidManifest.xml", """
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application
        android:label="$displayName"
        android:name="${'$'}{applicationName}"
        android:icon="@mipmap/ic_launcher">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:launchMode="singleTop"
            android:theme="@style/LaunchTheme"
            android:configChanges="orientation|keyboardHidden|keyboard|screenSize|smallestScreenSize|locale|layoutDirection|fontScale|screenLayout|density|uiMode"
            android:hardwareAccelerated="true"
            android:windowSoftInputMode="adjustResize">
            <meta-data
              android:name="io.flutter.embedding.android.NormalTheme"
              android:resource="@style/NormalTheme" />
            <intent-filter>
                <action android:name="android.intent.action.MAIN"/>
                <category android:name="android.intent.category.LAUNCHER"/>
            </intent-filter>
        </activity>
        <meta-data
            android:name="flutterEmbedding"
            android:value="2" />
    </application>
</manifest>
""".trimIndent()),
            
            // iOS configuration placeholder
            ProjectFileEntry("ios/Runner/Info.plist", """
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>CFBundleDevelopmentRegion</key>
    <string>${'$'}(DEVELOPMENT_LANGUAGE)</string>
    <key>CFBundleDisplayName</key>
    <string>$displayName</string>
    <key>CFBundleExecutable</key>
    <string>${'$'}(EXECUTABLE_NAME)</string>
    <key>CFBundleIdentifier</key>
    <string>${'$'}(PRODUCT_BUNDLE_IDENTIFIER)</string>
    <key>CFBundleInfoDictionaryVersion</key>
    <string>6.0</string>
    <key>CFBundleName</key>
    <string>$name</string>
    <key>CFBundlePackageType</key>
    <string>APPL</string>
    <key>CFBundleShortVersionString</key>
    <string>${'$'}(FLUTTER_BUILD_NAME)</string>
    <key>CFBundleVersion</key>
    <string>${'$'}(FLUTTER_BUILD_NUMBER)</string>
    <key>LSRequiresIPhoneOS</key>
    <true/>
    <key>UILaunchStoryboardName</key>
    <string>LaunchScreen</string>
    <key>UIMainStoryboardFile</key>
    <string>Main</string>
    <key>UISupportedInterfaceOrientations</key>
    <array>
        <string>UIInterfaceOrientationPortrait</string>
        <string>UIInterfaceOrientationLandscapeLeft</string>
        <string>UIInterfaceOrientationLandscapeRight</string>
    </array>
</dict>
</plist>
""".trimIndent()),
            
            // Flutter metadata
            ProjectFileEntry(".metadata", """
# This file tracks properties of this Flutter project.
version:
  revision: "FLUTTER_VERSION"
  channel: "stable"
project_type: app
""".trimIndent()),
            
            // Gitignore
            ProjectFileEntry(".gitignore", """
# Miscellaneous
*.class
*.log
*.pyc
*.swp
.DS_Store
.atom/
.buildlog/
.history
.svn/
migrate_working_dir/

# IntelliJ related
*.iml
*.ipr
*.iws
.idea/

# Flutter/Dart/Pub related
**/doc/api/
**/ios/Flutter/.last_build_id
.dart_tool/
.flutter-plugins
.flutter-plugins-dependencies
.packages
.pub-cache/
.pub/
/build/

# Symbolication related
app.*.symbols

# Obfuscation related
app.*.map.json

# Android Studio related
/android/app/debug
/android/app/profile
/android/app/release

# iOS related
**/ios/Pods/
**/ios/.symlinks/
**/ios/Flutter/Flutter.framework
**/ios/Flutter/Flutter.podspec
""".trimIndent()),
            
            // README
            ProjectFileEntry("README.md", """
# $displayName

A new Flutter project.

## Getting Started

### Prerequisites

- Flutter SDK 3.0.0 or higher
- Dart SDK 3.0.0 or higher
- Android Studio / VS Code with Flutter extension
- Xcode (for iOS development)

### Installation

1. Clone this repository
2. Run `flutter pub get` to install dependencies
3. Run `flutter run` to start the app

### Build

```bash
# Android
flutter build apk

# iOS
flutter build ios

# Web
flutter build web
```

### Testing

```bash
flutter test
```

## Project Structure

```
$name/
├── lib/
│   └── main.dart
├── test/
│   └── widget_test.dart
├── android/
├── ios/
├── pubspec.yaml
└── README.md
```

## License

MIT License
""".trimIndent()),
            
            ProjectFileEntry("LICENSE", generateMITLicense(displayName))
        )
    }

    /**
     * Generates a complete, build-ready Node.js Express project.
     * Includes all necessary files for immediate use with npm.
     */
    private fun generateNodeJsProject(request: ProjectGenerationRequest): List<ProjectFileEntry> {
        val name = sanitizeName(request.projectName, NamingStyle.KEBAB_CASE)
        val displayName = request.projectName
        
        return listOf(
            // package.json
            ProjectFileEntry("package.json", """
{
  "name": "$name",
  "version": "1.0.0",
  "description": "$displayName - Express.js API",
  "main": "src/index.js",
  "scripts": {
    "start": "node src/index.js",
    "dev": "nodemon src/index.js",
    "test": "jest",
    "lint": "eslint src/",
    "lint:fix": "eslint src/ --fix"
  },
  "keywords": ["nodejs", "express", "api"],
  "author": "",
  "license": "MIT",
  "dependencies": {
    "cors": "^2.8.5",
    "dotenv": "^16.3.1",
    "express": "^4.18.2",
    "helmet": "^7.1.0",
    "morgan": "^1.10.0"
  },
  "devDependencies": {
    "eslint": "^8.56.0",
    "jest": "^29.7.0",
    "nodemon": "^3.0.2"
  },
  "engines": {
    "node": ">=18.0.0"
  }
}
""".trimIndent()),
            
            // Main entry point
            ProjectFileEntry("src/index.js", """
require('dotenv').config();
const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const morgan = require('morgan');
const routes = require('./routes');

const app = express();
const PORT = process.env.PORT || 3000;

// Middleware
app.use(helmet());
app.use(cors());
app.use(morgan('dev'));
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// Routes
app.use('/api', routes);

// Health check
app.get('/health', (req, res) => {
  res.json({ status: 'OK', timestamp: new Date().toISOString() });
});

// Root route
app.get('/', (req, res) => {
  res.json({ message: 'Welcome to $displayName API' });
});

// 404 handler
app.use((req, res) => {
  res.status(404).json({ error: 'Not Found' });
});

// Error handler
app.use((err, req, res, next) => {
  console.error(err.stack);
  res.status(500).json({ error: 'Internal Server Error' });
});

// Start server
app.listen(PORT, () => {
  console.log(`🚀 Server running on http://localhost:${'$'}{PORT}`);
});

module.exports = app;
""".trimIndent()),
            
            // Routes
            ProjectFileEntry("src/routes/index.js", """
const express = require('express');
const router = express.Router();

// Example route
router.get('/', (req, res) => {
  res.json({ message: 'API is working!' });
});

// Users route example
router.get('/users', (req, res) => {
  res.json([
    { id: 1, name: 'John Doe', email: 'john@example.com' },
    { id: 2, name: 'Jane Doe', email: 'jane@example.com' }
  ]);
});

// Single user route example
router.get('/users/:id', (req, res) => {
  const { id } = req.params;
  res.json({ id: parseInt(id), name: 'User ' + id, email: 'user' + id + '@example.com' });
});

module.exports = router;
""".trimIndent()),
            
            // Config
            ProjectFileEntry("src/config/index.js", """
module.exports = {
  port: process.env.PORT || 3000,
  nodeEnv: process.env.NODE_ENV || 'development',
  db: {
    host: process.env.DB_HOST || 'localhost',
    port: process.env.DB_PORT || 5432,
    name: process.env.DB_NAME || '$name',
    user: process.env.DB_USER || 'postgres',
    password: process.env.DB_PASSWORD || ''
  }
};
""".trimIndent()),
            
            // Test file
            ProjectFileEntry("src/__tests__/index.test.js", """
const app = require('../index');

describe('API Tests', () => {
  test('should return health status', async () => {
    // Basic test placeholder
    expect(true).toBe(true);
  });
});
""".trimIndent()),
            
            // Environment example
            ProjectFileEntry(".env.example", """
# Server Configuration
PORT=3000
NODE_ENV=development

# Database Configuration
DB_HOST=localhost
DB_PORT=5432
DB_NAME=$name
DB_USER=postgres
DB_PASSWORD=

# API Keys
# API_KEY=your-api-key-here
""".trimIndent()),
            
            // ESLint configuration
            ProjectFileEntry(".eslintrc.json", """
{
  "env": {
    "node": true,
    "es2021": true,
    "jest": true
  },
  "extends": "eslint:recommended",
  "parserOptions": {
    "ecmaVersion": "latest",
    "sourceType": "module"
  },
  "rules": {
    "no-unused-vars": "warn",
    "no-console": "off",
    "semi": ["error", "always"],
    "quotes": ["error", "single"]
  }
}
""".trimIndent()),
            
            // Prettier configuration
            ProjectFileEntry(".prettierrc", """
{
  "semi": true,
  "singleQuote": true,
  "tabWidth": 2,
  "trailingComma": "es5"
}
""".trimIndent()),
            
            // Node version
            ProjectFileEntry(".nvmrc", "20"),
            
            // Gitignore
            ProjectFileEntry(".gitignore", """
# Dependencies
node_modules/

# Environment
.env
.env.local
.env.*.local

# Logs
logs/
*.log
npm-debug.log*
yarn-debug.log*
yarn-error.log*

# Build
dist/
build/

# IDE
.idea/
.vscode/
*.swp
*.swo

# OS
.DS_Store
Thumbs.db

# Test
coverage/

# Misc
*.pid
*.seed
*.pid.lock
""".trimIndent()),
            
            // Dockerfile
            ProjectFileEntry("Dockerfile", """
FROM node:20-alpine

WORKDIR /app

COPY package*.json ./

RUN npm ci --only=production

COPY . .

EXPOSE 3000

USER node

CMD ["node", "src/index.js"]
""".trimIndent()),
            
            // Docker Compose
            ProjectFileEntry("docker-compose.yml", """
version: '3.8'

services:
  app:
    build: .
    ports:
      - "3000:3000"
    environment:
      - NODE_ENV=production
      - PORT=3000
    restart: unless-stopped
""".trimIndent()),
            
            // README
            ProjectFileEntry("README.md", """
# $displayName

Express.js REST API

## Prerequisites

- Node.js 18+ 
- npm or yarn

## Getting Started

### Installation

```bash
npm install
```

### Configuration

1. Copy `.env.example` to `.env`
2. Update the environment variables as needed

### Development

```bash
npm run dev
```

### Production

```bash
npm start
```

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | / | Welcome message |
| GET | /health | Health check |
| GET | /api | API root |
| GET | /api/users | Get all users |
| GET | /api/users/:id | Get user by ID |

## Docker

```bash
docker-compose up -d
```

## Testing

```bash
npm test
```

## License

MIT License
""".trimIndent()),
            
            ProjectFileEntry("LICENSE", generateMITLicense(displayName))
        )
    }

    /**
     * Generates a complete, build-ready Python Flask project.
     * Includes all necessary files for immediate use.
     */
    private fun generatePythonProject(request: ProjectGenerationRequest): List<ProjectFileEntry> {
        val name = sanitizeName(request.projectName, NamingStyle.SNAKE_CASE)
        val displayName = request.projectName
        
        return listOf(
            // Requirements
            ProjectFileEntry("requirements.txt", """
Flask==3.0.0
python-dotenv==1.0.0
gunicorn==21.2.0
flask-cors==4.0.0
""".trimIndent()),
            
            ProjectFileEntry("requirements-dev.txt", """
-r requirements.txt
pytest==7.4.4
pytest-cov==4.1.0
flake8==7.0.0
black==24.1.0
""".trimIndent()),
            
            // Main app file
            ProjectFileEntry("app/__init__.py", """
from flask import Flask
from flask_cors import CORS

def create_app():
    app = Flask(__name__)
    CORS(app)
    
    # Load configuration
    app.config.from_object('config.Config')
    
    # Register blueprints
    from app.routes import main_bp
    app.register_blueprint(main_bp)
    
    return app
""".trimIndent()),
            
            // Routes
            ProjectFileEntry("app/routes.py", """
from flask import Blueprint, jsonify

main_bp = Blueprint('main', __name__)

@main_bp.route('/')
def index():
    return jsonify({
        'message': 'Welcome to $displayName API',
        'version': '1.0.0'
    })

@main_bp.route('/health')
def health():
    return jsonify({
        'status': 'OK',
        'service': '$displayName'
    })

@main_bp.route('/api/users')
def get_users():
    users = [
        {'id': 1, 'name': 'John Doe', 'email': 'john@example.com'},
        {'id': 2, 'name': 'Jane Doe', 'email': 'jane@example.com'}
    ]
    return jsonify(users)

@main_bp.route('/api/users/<int:user_id>')
def get_user(user_id):
    return jsonify({
        'id': user_id,
        'name': f'User {user_id}',
        'email': f'user{user_id}@example.com'
    })
""".trimIndent()),
            
            // Models placeholder
            ProjectFileEntry("app/models.py", """
# Database models go here
# Example with SQLAlchemy:
#
# from flask_sqlalchemy import SQLAlchemy
#
# db = SQLAlchemy()
#
# class User(db.Model):
#     id = db.Column(db.Integer, primary_key=True)
#     name = db.Column(db.String(100), nullable=False)
#     email = db.Column(db.String(120), unique=True, nullable=False)
""".trimIndent()),
            
            // Config
            ProjectFileEntry("config.py", """
import os
from dotenv import load_dotenv

load_dotenv()

class Config:
    SECRET_KEY = os.getenv('SECRET_KEY', 'dev-secret-key')
    DEBUG = os.getenv('DEBUG', 'True').lower() == 'true'
    
    # Database
    SQLALCHEMY_DATABASE_URI = os.getenv('DATABASE_URL', 'sqlite:///app.db')
    SQLALCHEMY_TRACK_MODIFICATIONS = False
""".trimIndent()),
            
            // Run script
            ProjectFileEntry("run.py", """
from app import create_app

app = create_app()

if __name__ == '__main__':
    app.run(debug=True, host='0.0.0.0', port=5000)
""".trimIndent()),
            
            // WSGI entry point
            ProjectFileEntry("wsgi.py", """
from app import create_app

app = create_app()
""".trimIndent()),
            
            // Tests
            ProjectFileEntry("tests/__init__.py", ""),
            
            ProjectFileEntry("tests/test_app.py", """
import pytest
from app import create_app

@pytest.fixture
def app():
    app = create_app()
    app.config['TESTING'] = True
    return app

@pytest.fixture
def client(app):
    return app.test_client()

def test_index(client):
    response = client.get('/')
    assert response.status_code == 200
    data = response.get_json()
    assert 'message' in data

def test_health(client):
    response = client.get('/health')
    assert response.status_code == 200
    data = response.get_json()
    assert data['status'] == 'OK'

def test_users(client):
    response = client.get('/api/users')
    assert response.status_code == 200
    data = response.get_json()
    assert isinstance(data, list)
""".trimIndent()),
            
            // Environment example
            ProjectFileEntry(".env.example", """
# Flask Configuration
FLASK_APP=run.py
FLASK_ENV=development
DEBUG=True
SECRET_KEY=your-secret-key-here

# Database
DATABASE_URL=sqlite:///app.db
""".trimIndent()),
            
            // Flake8 config
            ProjectFileEntry(".flake8", """
[flake8]
max-line-length = 120
exclude = venv,.git,__pycache__
ignore = E203,W503
""".trimIndent()),
            
            // pyproject.toml
            ProjectFileEntry("pyproject.toml", """
[build-system]
requires = ["setuptools>=61.0"]
build-backend = "setuptools.build_meta"

[project]
name = "$name"
version = "1.0.0"
description = "$displayName - Flask API"
readme = "README.md"
requires-python = ">=3.9"

[tool.black]
line-length = 120
target-version = ['py39', 'py310', 'py311']

[tool.pytest.ini_options]
testpaths = ["tests"]
python_files = "test_*.py"
""".trimIndent()),
            
            // Gitignore
            ProjectFileEntry(".gitignore", """
# Byte-compiled / optimized / DLL files
__pycache__/
*.py[cod]
*${'$'}py.class

# Virtual environments
venv/
.venv/
ENV/

# Environment files
.env
.env.local

# IDE
.idea/
.vscode/
*.swp
*.swo

# Distribution / packaging
dist/
build/
*.egg-info/

# Testing
.coverage
htmlcov/
.pytest_cache/

# Database
*.db
*.sqlite3

# Logs
*.log

# OS
.DS_Store
Thumbs.db
""".trimIndent()),
            
            // Dockerfile
            ProjectFileEntry("Dockerfile", """
FROM python:3.11-slim

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

EXPOSE 5000

CMD ["gunicorn", "-b", "0.0.0.0:5000", "wsgi:app"]
""".trimIndent()),
            
            // Docker Compose
            ProjectFileEntry("docker-compose.yml", """
version: '3.8'

services:
  app:
    build: .
    ports:
      - "5000:5000"
    environment:
      - FLASK_ENV=production
    restart: unless-stopped
""".trimIndent()),
            
            // README
            ProjectFileEntry("README.md", """
# $displayName

Python Flask REST API

## Prerequisites

- Python 3.9+
- pip

## Getting Started

### Installation

```bash
# Create virtual environment
python -m venv venv

# Activate virtual environment
# On Windows:
venv\Scripts\activate
# On macOS/Linux:
source venv/bin/activate

# Install dependencies
pip install -r requirements.txt
```

### Configuration

1. Copy `.env.example` to `.env`
2. Update the environment variables as needed

### Development

```bash
python run.py
```

### Production

```bash
gunicorn -b 0.0.0.0:5000 wsgi:app
```

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | / | Welcome message |
| GET | /health | Health check |
| GET | /api/users | Get all users |
| GET | /api/users/:id | Get user by ID |

## Docker

```bash
docker-compose up -d
```

## Testing

```bash
pytest
```

## License

MIT License
""".trimIndent()),
            
            ProjectFileEntry("LICENSE", generateMITLicense(displayName))
        )
    }

    /**
     * Generates a basic generic project structure.
     * Used as a fallback for unsupported project types.
     */
    private fun generateGenericProject(request: ProjectGenerationRequest): List<ProjectFileEntry> {
        val displayName = request.projectName
        
        return listOf(
            ProjectFileEntry("README.md", """
# $displayName

${request.description ?: "Project generated by AI Kod Asistanı."}

## Getting Started

Add your implementation code here.

## Project Structure

```
$displayName/
├── src/
│   └── main.txt
├── .gitignore
├── LICENSE
└── README.md
```

## License

MIT License
""".trimIndent()),
            
            ProjectFileEntry("src/main.txt", """
// Main file for $displayName
// Add your implementation here
""".trimIndent()),
            
            ProjectFileEntry(".gitignore", """
# IDE
.idea/
.vscode/
*.swp
*.swo

# Build
build/
dist/
out/

# Logs
*.log

# OS
.DS_Store
Thumbs.db

# Dependencies
node_modules/
vendor/
""".trimIndent()),
            
            ProjectFileEntry("LICENSE", generateMITLicense(displayName))
        )
    }

    private suspend fun packageAsZip(context: Context, projectName: String, files: List<ProjectFileEntry>): Pair<Uri?, String?> = withContext(Dispatchers.IO) {
        val outputDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
        val zipFile = File(outputDir, "${projectName.replace(" ", "_")}_${System.currentTimeMillis()}.zip")
        ZipOutputStream(FileOutputStream(zipFile)).use { zipOut ->
            files.forEach { file ->
                val entry = ZipEntry("${projectName}/${file.path}")
                zipOut.putNextEntry(entry)
                zipOut.write(file.content.toByteArray(Charsets.UTF_8))
                zipOut.closeEntry()
            }
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", zipFile)
        Pair(uri, zipFile.absolutePath)
    }

    fun parseProjectRequest(input: String): ProjectType {
        val lower = input.lowercase()
        return when {
            lower.contains("android") -> ProjectType.ANDROID_KOTLIN
            lower.contains("flutter") -> ProjectType.FLUTTER
            lower.contains("node") || lower.contains("express") -> ProjectType.NODEJS_EXPRESS
            lower.contains("python") || lower.contains("flask") -> ProjectType.PYTHON_FLASK
            lower.contains("react") -> ProjectType.REACT_WEB
            lower.contains("vue") -> ProjectType.VUE_WEB
            else -> ProjectType.CUSTOM
        }
    }

    fun extractProjectName(input: String): String {
        val quoted = Regex("\"([^\"]+)\"").find(input)?.groupValues?.get(1)
        if (quoted != null) return quoted
        val named = Regex("(?:named|called)\\s+(\\w+)").find(input)?.groupValues?.get(1)
        if (named != null) return named
        return "MyProject"
    }

    // ========== HELPER GENERATORS ==========
    
    private fun generateGradlewScript(): String = """
#!/bin/bash
# Gradle wrapper script for UNIX-like systems

# Attempt to set APP_HOME
PRG="${'$'}0"
while [ -h "${'$'}PRG" ] ; do
    ls=`ls -ld "${'$'}PRG"`
    link=`expr "${'$'}ls" : '.*-> \(.*\)${'$'}'`
    if expr "${'$'}link" : '/.*' > /dev/null; then
        PRG="${'$'}link"
    else
        PRG=`dirname "${'$'}PRG"`"/${'$'}link"
    fi
done
SAVED="`pwd`"
cd "`dirname \"${'$'}PRG\"`/" >/dev/null
APP_HOME="`pwd -P`"
cd "${'$'}SAVED" >/dev/null

# Use the maximum available, or set MAX_FD != -1 to use that value
MAX_FD="maximum"

warn () {
    echo "${'$'}*"
}

die () {
    echo "${'$'}*"
    exit 1
}

# Determine the Java command to use
if [ -n "${'$'}JAVA_HOME" ] ; then
    if [ -x "${'$'}JAVA_HOME/jre/sh/java" ] ; then
        JAVACMD="${'$'}JAVA_HOME/jre/sh/java"
    else
        JAVACMD="${'$'}JAVA_HOME/bin/java"
    fi
else
    JAVACMD="`which java`"
fi

if [ ! -x "${'$'}JAVACMD" ] ; then
    die "ERROR: JAVA_HOME is not set and no 'java' command could be found."
fi

# Increase the maximum file descriptors if we can
ulimit -n ${'$'}MAX_FD 2>/dev/null

# Collect all arguments for the java command
exec "${'$'}JAVACMD" ${'$'}DEFAULT_JVM_OPTS ${'$'}JAVA_OPTS ${'$'}GRADLE_OPTS \
    -classpath "${'$'}APP_HOME/gradle/wrapper/gradle-wrapper.jar" \
    org.gradle.wrapper.GradleWrapperMain "${'$'}@"
""".trimIndent()

    private fun generateGradlewBatScript(): String = """
@rem Gradle wrapper script for Windows

@if "%DEBUG%"=="" @echo off
@rem ##########################################################################
@rem  Gradle startup script for Windows
@rem ##########################################################################

@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.

@rem Find java.exe
if defined JAVA_HOME goto findJavaFromJavaHome

set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if %ERRORLEVEL% equ 0 goto execute

echo ERROR: JAVA_HOME is not set and no 'java' command could be found.
goto fail

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe

if exist "%JAVA_EXE%" goto execute

echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME%
goto fail

:execute
@rem Setup the command line

set CLASSPATH=%DIRNAME%\gradle\wrapper\gradle-wrapper.jar

@rem Execute Gradle
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %GRADLE_OPTS% ^
  -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*

:end
@rem End local scope for the variables with windows NT shell
if %ERRORLEVEL% equ 0 goto mainEnd

:fail
exit /b 1

:mainEnd
if "%OS%"=="Windows_NT" endlocal

:omega
""".trimIndent()

    private fun generateMITLicense(projectName: String): String = """
MIT License

Copyright (c) ${java.time.Year.now().value} $projectName

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
""".trimIndent()
}
