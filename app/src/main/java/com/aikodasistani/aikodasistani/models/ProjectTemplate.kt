package com.aikodasistani.aikodasistani.models

import android.net.Uri

/**
 * Represents different types of project structures that can be generated.
 */
enum class ProjectType(val displayName: String, val description: String) {
    ANDROID_KOTLIN("Android (Kotlin)", "Android Studio project with Kotlin, Gradle, Jetpack Compose"),
    ANDROID_JAVA("Android (Java)", "Android Studio project with Java and Gradle"),
    FLUTTER("Flutter", "Flutter cross-platform project with Dart"),
    REACT_NATIVE("React Native", "React Native mobile application"),
    NODEJS_EXPRESS("Node.js (Express)", "Node.js backend with Express.js API"),
    NODEJS_NEST("Node.js (NestJS)", "Node.js backend with NestJS framework"),
    PYTHON_FLASK("Python (Flask)", "Python web application with Flask"),
    PYTHON_DJANGO("Python (Django)", "Python web application with Django"),
    PYTHON_FASTAPI("Python (FastAPI)", "Python modern API with FastAPI"),
    VSCODE_FULLSTACK("VS Code Full Stack", "Full stack project with backend and frontend folders"),
    REACT_WEB("React Web", "React.js web application"),
    VUE_WEB("Vue.js Web", "Vue.js web application"),
    ANGULAR_WEB("Angular Web", "Angular web application"),
    SPRING_BOOT("Spring Boot", "Java Spring Boot microservice"),
    DOTNET_API(".NET Web API", ".NET Core Web API project"),
    GO_API("Go API", "Go backend API project"),
    RUST_CLI("Rust CLI", "Rust command-line application"),
    CUSTOM("Custom", "Custom project structure")
}

/**
 * Represents a single file to be generated in the project.
 */
data class ProjectFileEntry(
    val path: String,                // Relative path within the project
    val content: String,             // File content
    val isDirectory: Boolean = false,
    val isExecutable: Boolean = false
)

/**
 * Request for project generation parsed from AI response or user input.
 */
data class ProjectGenerationRequest(
    val projectType: ProjectType,
    val projectName: String,
    val packageName: String? = null,      // For Android/Java projects
    val description: String? = null,
    val features: List<String> = emptyList(),  // Additional features requested
    val outputFormat: OutputFormat = OutputFormat.ZIP
)

/**
 * Output format options for generated projects.
 */
enum class OutputFormat(val displayName: String, val extension: String) {
    ZIP("ZIP Archive", ".zip"),
    FOLDER("Folder Structure", ""),
    SINGLE_FILE("Single File", ".txt")
}

/**
 * Result of project generation.
 */
sealed class ProjectGenerationResult {
    data class Success(
        val projectName: String,
        val projectType: ProjectType,
        val files: List<ProjectFileEntry>,
        val totalSize: Long,
        val outputUri: Uri? = null,
        val outputPath: String? = null,
        val description: String? = null
    ) : ProjectGenerationResult()
    
    data class Error(
        val message: String,
        val details: String? = null
    ) : ProjectGenerationResult()
}

/**
 * Represents a template category for UI grouping.
 */
data class TemplateCategory(
    val name: String,
    val icon: String,
    val templates: List<ProjectType>
)

/**
 * Pre-defined template categories for the UI.
 */
object TemplateCategories {
    val categories = listOf(
        TemplateCategory(
            name = "Mobile Development",
            icon = "📱",
            templates = listOf(
                ProjectType.ANDROID_KOTLIN,
                ProjectType.ANDROID_JAVA,
                ProjectType.FLUTTER,
                ProjectType.REACT_NATIVE
            )
        ),
        TemplateCategory(
            name = "Backend Development",
            icon = "⚙️",
            templates = listOf(
                ProjectType.NODEJS_EXPRESS,
                ProjectType.NODEJS_NEST,
                ProjectType.PYTHON_FLASK,
                ProjectType.PYTHON_DJANGO,
                ProjectType.PYTHON_FASTAPI,
                ProjectType.SPRING_BOOT,
                ProjectType.DOTNET_API,
                ProjectType.GO_API
            )
        ),
        TemplateCategory(
            name = "Web Development",
            icon = "🌐",
            templates = listOf(
                ProjectType.REACT_WEB,
                ProjectType.VUE_WEB,
                ProjectType.ANGULAR_WEB,
                ProjectType.VSCODE_FULLSTACK
            )
        ),
        TemplateCategory(
            name = "Other",
            icon = "🛠️",
            templates = listOf(
                ProjectType.RUST_CLI,
                ProjectType.CUSTOM
            )
        )
    )
}
