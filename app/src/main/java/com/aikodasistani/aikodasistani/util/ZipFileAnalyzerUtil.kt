package com.aikodasistani.aikodasistani.util

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * ZIP dosyası analiz aracı
 * ZIP dosyalarını açıp içeriklerini okur ve uygulama yapısını çıkarır
 */
object ZipFileAnalyzerUtil {

    private const val TAG = "ZipFileAnalyzer"

    // Maksimum dosya boyutu sınırları
    private const val MAX_FILE_SIZE = 5 * 1024 * 1024 // 5MB per file
    private const val MAX_TOTAL_SIZE = 50 * 1024 * 1024 // 50MB total
    private const val MAX_FILES = 500 // Maksimum dosya sayısı
    private const val MAX_CHARS_PER_FILE = 100000 // Maksimum karakter per dosya
    private const val MAX_LANGUAGES_TO_SHOW = 10 // Gösterilecek maksimum dil sayısı
    private const val MAX_CONTENT_DISPLAY_LENGTH = 3000 // İçerik gösterim limiti

    // Desteklenen kod dosya uzantıları
    private val CODE_EXTENSIONS = setOf(
        // Kotlin/Java
        ".kt", ".java", ".kts",
        // Web
        ".js", ".ts", ".jsx", ".tsx", ".html", ".css", ".scss", ".vue",
        // Python
        ".py",
        // C/C++
        ".c", ".cpp", ".h", ".hpp",
        // C#
        ".cs",
        // Ruby
        ".rb",
        // Go
        ".go",
        // Rust
        ".rs",
        // Swift
        ".swift",
        // PHP
        ".php",
        // Shell
        ".sh", ".bash",
        // Yapılandırma ve manifest dosyaları
        ".xml", ".json", ".yaml", ".yml", ".toml", ".properties", ".gradle", ".gradle.kts",
        // Markdown/Docs
        ".md", ".txt", ".readme"
    )

    // Göz ardı edilecek klasörler
    private val IGNORED_DIRECTORIES = setOf(
        "node_modules",
        ".git",
        ".idea",
        ".gradle",
        "build",
        "bin",
        "obj",
        "__pycache__",
        ".venv",
        "venv",
        "target",
        ".svn",
        ".hg"
    )

    /**
     * ZIP dosyasını analiz eder ve içerik özeti döner
     */
    suspend fun analyzeZipFile(
        contentResolver: ContentResolver,
        uri: Uri
    ): ZipAnalysisResult = withContext(Dispatchers.IO) {
        val fileEntries = mutableListOf<ZipFileEntry>()
        val directoryStructure = mutableSetOf<String>()
        var totalSize = 0L
        var fileCount = 0
        var errorMessage: String? = null

        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zipInputStream ->
                    var entry: ZipEntry? = zipInputStream.nextEntry

                    while (entry != null && fileCount < MAX_FILES && totalSize < MAX_TOTAL_SIZE) {
                        yield() // Coroutine iptal kontrolü

                        val entryName = entry.name

                        // Göz ardı edilecek klasörleri kontrol et
                        if (shouldIgnoreEntry(entryName)) {
                            entry = try {
                                zipInputStream.nextEntry
                            } catch (e: Exception) {
                                null
                            }
                            continue
                        }

                        if (entry.isDirectory) {
                            directoryStructure.add(entryName.trimEnd('/'))
                        } else {
                            // Dosya uzantısını kontrol et
                            val extension = getExtension(entryName)
                            val isCodeFile = CODE_EXTENSIONS.contains(extension.lowercase())

                            // Klasör yolunu ekle
                            val parentPath = entryName.substringBeforeLast('/', "")
                            if (parentPath.isNotEmpty()) {
                                directoryStructure.add(parentPath)
                            }

                            // Kod dosyası ise içeriği oku
                            val content = if (isCodeFile && entry.size < MAX_FILE_SIZE) {
                                try {
                                    readZipEntryContent(zipInputStream, entry)
                                } catch (e: Exception) {
                                    Log.e(TAG, "Dosya okunamadı: $entryName", e)
                                    null
                                }
                            } else {
                                null
                            }

                            fileEntries.add(
                                ZipFileEntry(
                                    name = entryName,
                                    path = entryName,
                                    size = entry.size,
                                    extension = extension,
                                    isCodeFile = isCodeFile,
                                    content = content,
                                    language = detectLanguage(extension)
                                )
                            )

                            totalSize += entry.size
                            fileCount++
                        }

                        entry = try {
                            zipInputStream.nextEntry
                        } catch (e: Exception) {
                            Log.e(TAG, "Sonraki entry alınamadı", e)
                            null
                        }
                    }
                }
            } ?: run {
                errorMessage = "ZIP dosyası açılamadı"
            }
        } catch (e: Exception) {
            Log.e(TAG, "ZIP analiz hatası", e)
            errorMessage = "ZIP analiz hatası: ${e.message}"
        }

        ZipAnalysisResult(
            success = errorMessage == null,
            errorMessage = errorMessage,
            totalFiles = fileCount,
            totalSize = totalSize,
            files = fileEntries,
            directoryStructure = directoryStructure.toList().sorted(),
            projectType = detectProjectType(fileEntries, directoryStructure)
        )
    }

    /**
     * ZIP entry içeriğini okur
     */
    private fun readZipEntryContent(
        zipInputStream: ZipInputStream,
        entry: ZipEntry
    ): String {
        val stringBuilder = StringBuilder()

        BufferedReader(InputStreamReader(zipInputStream, Charsets.UTF_8)).use { reader ->
            var line: String?
            var charCount = 0

            while (reader.readLine().also { line = it } != null && charCount < MAX_CHARS_PER_FILE) {
                stringBuilder.append(line).append('\n')
                charCount += line!!.length + 1
            }

            if (charCount >= MAX_CHARS_PER_FILE) {
                stringBuilder.append("\n[...dosya devamı kesildi - çok büyük...]")
            }
        }

        return stringBuilder.toString()
    }

    /**
     * Entry'nin göz ardı edilip edilmeyeceğini kontrol eder
     */
    private fun shouldIgnoreEntry(entryName: String): Boolean {
        val parts = entryName.split("/")
        return parts.any { part -> IGNORED_DIRECTORIES.contains(part) }
    }

    /**
     * Dosya uzantısını alır
     */
    private fun getExtension(fileName: String): String {
        val lastDotIndex = fileName.lastIndexOf('.')
        return if (lastDotIndex >= 0) {
            fileName.substring(lastDotIndex)
        } else {
            ""
        }
    }

    /**
     * Uzantıya göre programlama dilini tespit eder
     */
    private fun detectLanguage(extension: String): String? {
        return when (extension.lowercase()) {
            ".kt", ".kts" -> "Kotlin"
            ".java" -> "Java"
            ".py" -> "Python"
            ".js" -> "JavaScript"
            ".ts" -> "TypeScript"
            ".jsx", ".tsx" -> "React"
            ".html", ".htm" -> "HTML"
            ".css", ".scss" -> "CSS"
            ".vue" -> "Vue"
            ".c", ".h" -> "C"
            ".cpp", ".hpp" -> "C++"
            ".cs" -> "C#"
            ".rb" -> "Ruby"
            ".go" -> "Go"
            ".rs" -> "Rust"
            ".swift" -> "Swift"
            ".php" -> "PHP"
            ".sh", ".bash" -> "Shell"
            ".xml" -> "XML"
            ".json" -> "JSON"
            ".yaml", ".yml" -> "YAML"
            ".toml" -> "TOML"
            ".gradle" -> "Gradle"
            ".gradle.kts" -> "Gradle Kotlin DSL"
            ".md" -> "Markdown"
            else -> null
        }
    }

    /**
     * Proje tipini tespit eder
     */
    private fun detectProjectType(
        files: List<ZipFileEntry>,
        directories: Set<String>
    ): ProjectType {
        val fileNames = files.map { it.name.substringAfterLast('/') }.toSet()
        val extensions = files.map { it.extension.lowercase() }.toSet()

        return when {
            // Android projesi
            fileNames.contains("AndroidManifest.xml") ||
            (fileNames.any { it.endsWith(".gradle") || it.endsWith(".gradle.kts") } &&
            directories.any { it.contains("app/src/main") }) -> ProjectType.ANDROID

            // iOS projesi
            fileNames.any { it.endsWith(".xcodeproj") || it.endsWith(".xcworkspace") } ||
            fileNames.contains("Podfile") -> ProjectType.IOS

            // React/React Native projesi
            fileNames.contains("package.json") &&
            (extensions.contains(".jsx") || extensions.contains(".tsx") ||
             files.any { it.content?.contains("react", ignoreCase = true) == true }) -> ProjectType.REACT

            // Node.js projesi
            fileNames.contains("package.json") -> ProjectType.NODEJS

            // Python projesi
            fileNames.contains("requirements.txt") ||
            fileNames.contains("setup.py") ||
            fileNames.contains("pyproject.toml") -> ProjectType.PYTHON

            // Java projesi
            fileNames.contains("pom.xml") -> ProjectType.JAVA_MAVEN

            // Gradle projesi
            fileNames.any { it.endsWith(".gradle") || it.endsWith(".gradle.kts") } -> ProjectType.GRADLE

            // .NET projesi
            fileNames.any { it.endsWith(".csproj") || it.endsWith(".sln") } -> ProjectType.DOTNET

            // Flutter projesi
            fileNames.contains("pubspec.yaml") -> ProjectType.FLUTTER

            // Go projesi
            fileNames.contains("go.mod") -> ProjectType.GO

            // Rust projesi
            fileNames.contains("Cargo.toml") -> ProjectType.RUST

            // Web projesi
            extensions.contains(".html") && extensions.contains(".css") -> ProjectType.WEB

            else -> ProjectType.UNKNOWN
        }
    }

    /**
     * Analiz sonucunu formatlar ve metin olarak döner
     */
    fun formatAnalysisResult(result: ZipAnalysisResult): String {
        if (!result.success) {
            return "❌ ZIP Analiz Hatası: ${result.errorMessage}"
        }

        val sb = StringBuilder()

        // Başlık
        sb.appendLine("📦 ZIP DOSYASI ANALİZİ")
        sb.appendLine("═".repeat(50))
        sb.appendLine()

        // Genel bilgiler
        sb.appendLine("📊 GENEL BİLGİLER:")
        sb.appendLine("• Proje Tipi: ${getProjectTypeDescription(result.projectType)}")
        sb.appendLine("• Toplam Dosya: ${result.totalFiles}")
        sb.appendLine("• Toplam Boyut: ${formatFileSize(result.totalSize)}")
        sb.appendLine()

        // Klasör yapısı
        sb.appendLine("📁 KLASÖR YAPISI:")
        result.directoryStructure.take(30).forEach { dir ->
            val depth = dir.count { it == '/' }
            val indent = "  ".repeat(depth)
            val folderName = dir.substringAfterLast('/')
            sb.appendLine("$indent📂 $folderName")
        }
        if (result.directoryStructure.size > 30) {
            sb.appendLine("  ... ve ${result.directoryStructure.size - 30} klasör daha")
        }
        sb.appendLine()

        // Dosya türü dağılımı
        val languageDistribution = result.files
            .filter { it.language != null }
            .groupBy { it.language!! }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }

        sb.appendLine("💻 PROGRAMLAMA DİLLERİ:")
        languageDistribution.take(MAX_LANGUAGES_TO_SHOW).forEach { (lang, count) ->
            val percentage = (count * 100.0 / result.files.size).toInt()
            sb.appendLine("• $lang: $count dosya ($percentage%)")
        }
        sb.appendLine()

        // Önemli dosyalar
        val importantFiles = result.files.filter { file ->
            file.name.contains("MainActivity", ignoreCase = true) ||
            file.name.contains("Application", ignoreCase = true) ||
            file.name.contains("build.gradle", ignoreCase = true) ||
            file.name.contains("package.json", ignoreCase = true) ||
            file.name.contains("AndroidManifest", ignoreCase = true) ||
            file.name.contains("index.", ignoreCase = true) ||
            file.name.contains("app.", ignoreCase = true) ||
            file.name.contains("main.", ignoreCase = true) ||
            file.name.contains("config", ignoreCase = true)
        }

        if (importantFiles.isNotEmpty()) {
            sb.appendLine("⭐ ÖNEMLİ DOSYALAR:")
            importantFiles.take(15).forEach { file ->
                sb.appendLine("• ${file.path} (${formatFileSize(file.size)})")
            }
            sb.appendLine()
        }

        // Kod dosyalarının içerikleri
        val codeFiles = result.files.filter { it.isCodeFile && it.content != null }
        if (codeFiles.isNotEmpty()) {
            sb.appendLine("═".repeat(50))
            sb.appendLine("📝 KOD DOSYALARI İÇERİĞİ:")
            sb.appendLine("═".repeat(50))

            codeFiles.take(20).forEach { file ->
                sb.appendLine()
                sb.appendLine("┌─────────────────────────────────────────────────")
                sb.appendLine("│ 📄 ${file.path}")
                sb.appendLine("│ Dil: ${file.language ?: "Bilinmiyor"} | Boyut: ${formatFileSize(file.size)}")
                sb.appendLine("└─────────────────────────────────────────────────")

                // İçeriği ekle (çok uzunsa kısalt)
                val content = file.content!!
                if (content.length > MAX_CONTENT_DISPLAY_LENGTH) {
                    sb.appendLine(content.take(MAX_CONTENT_DISPLAY_LENGTH))
                    sb.appendLine("\n[...${content.length - MAX_CONTENT_DISPLAY_LENGTH} karakter daha...]")
                } else {
                    sb.appendLine(content)
                }
                sb.appendLine()
            }

            if (codeFiles.size > 20) {
                sb.appendLine("... ve ${codeFiles.size - 20} kod dosyası daha")
            }
        }

        // Sonuç
        sb.appendLine()
        sb.appendLine("═".repeat(50))
        sb.appendLine("✅ ANALİZ TAMAMLANDI")

        return sb.toString()
    }

    /**
     * Proje tipi açıklamasını döner
     */
    private fun getProjectTypeDescription(type: ProjectType): String {
        return when (type) {
            ProjectType.ANDROID -> "📱 Android (Kotlin/Java)"
            ProjectType.IOS -> "🍎 iOS (Swift/Objective-C)"
            ProjectType.REACT -> "⚛️ React / React Native"
            ProjectType.NODEJS -> "🟢 Node.js"
            ProjectType.PYTHON -> "🐍 Python"
            ProjectType.JAVA_MAVEN -> "☕ Java (Maven)"
            ProjectType.GRADLE -> "🐘 Gradle Projesi"
            ProjectType.DOTNET -> "💜 .NET"
            ProjectType.FLUTTER -> "🦋 Flutter (Dart)"
            ProjectType.GO -> "🔵 Go"
            ProjectType.RUST -> "🦀 Rust"
            ProjectType.WEB -> "🌐 Web (HTML/CSS/JS)"
            ProjectType.UNKNOWN -> "❓ Bilinmeyen Proje Tipi"
        }
    }

    /**
     * Dosya boyutunu okunabilir formata çevirir
     */
    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> "${bytes / (1024 * 1024)} MB"
        }
    }

    /**
     * ZIP analiz sonucu
     */
    data class ZipAnalysisResult(
        val success: Boolean,
        val errorMessage: String?,
        val totalFiles: Int,
        val totalSize: Long,
        val files: List<ZipFileEntry>,
        val directoryStructure: List<String>,
        val projectType: ProjectType
    )

    /**
     * ZIP dosya girişi
     */
    data class ZipFileEntry(
        val name: String,
        val path: String,
        val size: Long,
        val extension: String,
        val isCodeFile: Boolean,
        val content: String?,
        val language: String?
    )

    /**
     * Proje tipi
     */
    enum class ProjectType {
        ANDROID,
        IOS,
        REACT,
        NODEJS,
        PYTHON,
        JAVA_MAVEN,
        GRADLE,
        DOTNET,
        FLUTTER,
        GO,
        RUST,
        WEB,
        UNKNOWN
    }
}
