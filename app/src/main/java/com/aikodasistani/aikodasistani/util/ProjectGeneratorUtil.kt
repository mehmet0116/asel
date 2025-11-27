package com.aikodasistani.aikodasistani.util

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import com.aikodasistani.aikodasistani.models.OutputFormat
import com.aikodasistani.aikodasistani.models.ProjectFileEntry
import com.aikodasistani.aikodasistani.models.ProjectGenerationRequest
import com.aikodasistani.aikodasistani.models.ProjectGenerationResult
import com.aikodasistani.aikodasistani.models.ProjectType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ProjectGeneratorUtil {
    private const val TAG = "ProjectGeneratorUtil"

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

    private fun generateAndroidKotlinProject(request: ProjectGenerationRequest): List<ProjectFileEntry> {
        val name = request.projectName.replace(" ", "")
        val pkg = request.packageName ?: "com.example.${name.lowercase()}"
        val pkgPath = pkg.replace(".", "/")
        return listOf(
            ProjectFileEntry("settings.gradle.kts", "rootProject.name = \"$name\"\ninclude(\":app\")"),
            ProjectFileEntry("build.gradle.kts", "plugins {\n    id(\"com.android.application\") version \"8.2.0\" apply false\n    id(\"org.jetbrains.kotlin.android\") version \"1.9.0\" apply false\n}"),
            ProjectFileEntry("gradle.properties", "org.gradle.jvmargs=-Xmx2048m\nandroid.useAndroidX=true"),
            ProjectFileEntry("app/build.gradle.kts", "plugins {\n    id(\"com.android.application\")\n    id(\"org.jetbrains.kotlin.android\")\n}\n\nandroid {\n    namespace = \"$pkg\"\n    compileSdk = 34\n    defaultConfig {\n        applicationId = \"$pkg\"\n        minSdk = 24\n        targetSdk = 34\n        versionCode = 1\n        versionName = \"1.0\"\n    }\n    compileOptions {\n        sourceCompatibility = JavaVersion.VERSION_17\n        targetCompatibility = JavaVersion.VERSION_17\n    }\n    kotlinOptions { jvmTarget = \"17\" }\n    buildFeatures { compose = true }\n    composeOptions { kotlinCompilerExtensionVersion = \"1.5.1\" }\n}\n\ndependencies {\n    implementation(\"androidx.core:core-ktx:1.12.0\")\n    implementation(\"androidx.activity:activity-compose:1.8.2\")\n    implementation(platform(\"androidx.compose:compose-bom:2024.02.00\"))\n    implementation(\"androidx.compose.ui:ui\")\n    implementation(\"androidx.compose.material3:material3\")\n}"),
            ProjectFileEntry("app/src/main/AndroidManifest.xml", "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\">\n    <application android:label=\"$name\" android:theme=\"@style/Theme.Material3.DayNight\">\n        <activity android:name=\".MainActivity\" android:exported=\"true\">\n            <intent-filter>\n                <action android:name=\"android.intent.action.MAIN\"/>\n                <category android:name=\"android.intent.category.LAUNCHER\"/>\n            </intent-filter>\n        </activity>\n    </application>\n</manifest>"),
            ProjectFileEntry("app/src/main/java/$pkgPath/MainActivity.kt", "package $pkg\n\nimport android.os.Bundle\nimport androidx.activity.ComponentActivity\nimport androidx.activity.compose.setContent\nimport androidx.compose.material3.*\nimport androidx.compose.runtime.*\nimport androidx.compose.foundation.layout.*\nimport androidx.compose.ui.Modifier\nimport androidx.compose.ui.unit.dp\nimport androidx.compose.ui.Alignment\n\nclass MainActivity : ComponentActivity() {\n    override fun onCreate(savedInstanceState: Bundle?) {\n        super.onCreate(savedInstanceState)\n        setContent {\n            MaterialTheme {\n                var count by remember { mutableIntStateOf(0) }\n                Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {\n                    Text(\"Welcome to $name!\", style = MaterialTheme.typography.headlineMedium)\n                    Spacer(Modifier.height(16.dp))\n                    Text(\"Count: \$count\")\n                    Button(onClick = { count++ }) { Text(\"Increment\") }\n                }\n            }\n        }\n    }\n}"),
            ProjectFileEntry("README.md", "# $name\n\nAndroid project with Kotlin and Jetpack Compose.\n\n## Run\n1. Open in Android Studio\n2. Sync Gradle\n3. Run on emulator/device")
        )
    }

    private fun generateFlutterProject(request: ProjectGenerationRequest): List<ProjectFileEntry> {
        val name = request.projectName.lowercase().replace(" ", "_")
        return listOf(
            ProjectFileEntry("pubspec.yaml", "name: $name\ndescription: Flutter project\nenvironment:\n  sdk: '>=3.0.0 <4.0.0'\ndependencies:\n  flutter:\n    sdk: flutter\nflutter:\n  uses-material-design: true"),
            ProjectFileEntry("lib/main.dart", "import 'package:flutter/material.dart';\n\nvoid main() => runApp(const MyApp());\n\nclass MyApp extends StatelessWidget {\n  const MyApp({super.key});\n  @override\n  Widget build(BuildContext context) => MaterialApp(\n    title: '${request.projectName}',\n    theme: ThemeData(colorScheme: ColorScheme.fromSeed(seedColor: Colors.blue)),\n    home: const HomePage(),\n  );\n}\n\nclass HomePage extends StatefulWidget {\n  const HomePage({super.key});\n  @override\n  State<HomePage> createState() => _HomePageState();\n}\n\nclass _HomePageState extends State<HomePage> {\n  int _count = 0;\n  @override\n  Widget build(BuildContext context) => Scaffold(\n    appBar: AppBar(title: const Text('${request.projectName}')),\n    body: Center(child: Text('Count: \$_count', style: Theme.of(context).textTheme.headlineMedium)),\n    floatingActionButton: FloatingActionButton(onPressed: () => setState(() => _count++), child: const Icon(Icons.add)),\n  );\n}"),
            ProjectFileEntry("README.md", "# ${request.projectName}\n\nFlutter project.\n\n## Run\n```\nflutter pub get\nflutter run\n```")
        )
    }

    private fun generateNodeJsProject(request: ProjectGenerationRequest): List<ProjectFileEntry> {
        val name = request.projectName.lowercase().replace(" ", "-")
        return listOf(
            ProjectFileEntry("package.json", "{\n  \"name\": \"$name\",\n  \"version\": \"1.0.0\",\n  \"main\": \"src/index.js\",\n  \"scripts\": { \"start\": \"node src/index.js\", \"dev\": \"nodemon src/index.js\" },\n  \"dependencies\": { \"express\": \"^4.18.2\", \"cors\": \"^2.8.5\" }\n}"),
            ProjectFileEntry("src/index.js", "const express = require('express');\nconst cors = require('cors');\nconst app = express();\napp.use(cors());\napp.use(express.json());\n\napp.get('/api', (req, res) => res.json({ message: 'Hello from ${request.projectName}!' }));\napp.get('/health', (req, res) => res.json({ status: 'OK' }));\n\nconst PORT = process.env.PORT || 3000;\napp.listen(PORT, () => console.log(`Server on port \${PORT}`));"),
            ProjectFileEntry(".gitignore", "node_modules/\n.env"),
            ProjectFileEntry("README.md", "# ${request.projectName}\n\nNode.js Express API.\n\n## Run\n```\nnpm install\nnpm start\n```")
        )
    }

    private fun generatePythonProject(request: ProjectGenerationRequest): List<ProjectFileEntry> {
        val name = request.projectName.lowercase().replace(" ", "_")
        return listOf(
            ProjectFileEntry("requirements.txt", "flask==3.0.0\npython-dotenv==1.0.0"),
            ProjectFileEntry("app.py", "from flask import Flask, jsonify\n\napp = Flask(__name__)\n\n@app.route('/')\ndef index():\n    return jsonify({'message': 'Hello from ${request.projectName}!'})\n\n@app.route('/health')\ndef health():\n    return jsonify({'status': 'OK'})\n\nif __name__ == '__main__':\n    app.run(debug=True, port=5000)"),
            ProjectFileEntry(".gitignore", "__pycache__/\nvenv/\n.env"),
            ProjectFileEntry("README.md", "# ${request.projectName}\n\nPython Flask API.\n\n## Run\n```\npip install -r requirements.txt\npython app.py\n```")
        )
    }

    private fun generateGenericProject(request: ProjectGenerationRequest): List<ProjectFileEntry> {
        return listOf(
            ProjectFileEntry("README.md", "# ${request.projectName}\n\n${request.description ?: "Project generated by AI."}\n\n## Getting Started\n\nAdd your code here."),
            ProjectFileEntry("src/main.txt", "// Main file for ${request.projectName}")
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
}
