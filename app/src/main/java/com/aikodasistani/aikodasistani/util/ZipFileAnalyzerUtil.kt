package com.aikodasistani.aikodasistani.util

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Progress callback tipi - Canlı analiz güncellemeleri için
 */
typealias ZipProgressCallback = (progress: Int, currentFile: String, status: String) -> Unit

/**
 * ZIP dosyası analiz aracı - Profesyonel Versiyon
 * ZIP dosyalarını açıp içeriklerini okur, analiz eder ve düzenlenmiş ZIP oluşturur
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
     * Progress callback ile canlı güncelleme sağlar
     */
    suspend fun analyzeZipFile(
        contentResolver: ContentResolver,
        uri: Uri,
        progressCallback: ZipProgressCallback? = null
    ): ZipAnalysisResult = withContext(Dispatchers.IO) {
        val fileEntries = mutableListOf<ZipFileEntry>()
        val directoryStructure = mutableSetOf<String>()
        var totalSize = 0L
        var fileCount = 0
        var errorMessage: String? = null

        try {
            progressCallback?.invoke(0, "", "📦 ZIP dosyası açılıyor...")
            
            contentResolver.openInputStream(uri)?.use { inputStream ->
                progressCallback?.invoke(5, "", "✅ ZIP dosyası başarıyla açıldı, içerik okunuyor...")
                
                ZipInputStream(inputStream).use { zipInputStream ->
                    progressCallback?.invoke(10, "", "🔍 ZIP arşivi taranıyor...")
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
                            // Progress güncelleme - klasör
                            progressCallback?.invoke(
                                calculateProgress(fileCount, MAX_FILES),
                                entryName,
                                "📂 Klasör: ${entryName.trimEnd('/')}"
                            )
                        } else {
                            // Dosya uzantısını kontrol et
                            val extension = getExtension(entryName)
                            val isCodeFile = CODE_EXTENSIONS.contains(extension.lowercase())

                            // Klasör yolunu ekle
                            val parentPath = entryName.substringBeforeLast('/', "")
                            if (parentPath.isNotEmpty()) {
                                directoryStructure.add(parentPath)
                            }

                            // Progress güncelleme - dosya keşfedildi
                            val fileName = entryName.substringAfterLast('/')
                            val statusIcon = if (isCodeFile) "📄" else "📁"
                            val fileType = if (isCodeFile) "Kod dosyası" else "Dosya"
                            progressCallback?.invoke(
                                calculateProgress(fileCount, MAX_FILES),
                                entryName,
                                "$statusIcon $fileType bulundu: $fileName (${formatFileSize(entry.size)})"
                            )

                            // Kod dosyası ise içeriği oku
                            val content = if (isCodeFile && entry.size < MAX_FILE_SIZE) {
                                try {
                                    // 1. Dosya açılıyor bildirimi
                                    progressCallback?.invoke(
                                        calculateProgress(fileCount, MAX_FILES),
                                        entryName,
                                        "📖 Dosya açılıyor: $fileName"
                                    )
                                    
                                    // 2. Dosya okunuyor bildirimi
                                    progressCallback?.invoke(
                                        calculateProgress(fileCount, MAX_FILES),
                                        entryName,
                                        "📥 İçerik okunuyor: $fileName (${formatFileSize(entry.size)})"
                                    )
                                    
                                    // 3. İçeriği oku
                                    val readContent = readZipEntryContent(zipInputStream, entry)
                                    
                                    // 4. Başarılı okuma bildirimi
                                    val charCount = readContent.length
                                    progressCallback?.invoke(
                                        calculateProgress(fileCount, MAX_FILES),
                                        entryName,
                                        "✅ Başarıyla okundu: $fileName ($charCount karakter, ${formatFileSize(entry.size)})"
                                    )
                                    
                                    readContent
                                } catch (e: Exception) {
                                    Log.e(TAG, "Dosya okunamadı: $entryName", e)
                                    // 5. Hata durumu bildirimi - kullanıcıya geri bildirim
                                    progressCallback?.invoke(
                                        calculateProgress(fileCount, MAX_FILES),
                                        entryName,
                                        "❌ Okuma hatası: $fileName - ${e.message}"
                                    )
                                    null
                                }
                            } else if (isCodeFile && entry.size >= MAX_FILE_SIZE) {
                                // Dosya çok büyük bildirimi
                                progressCallback?.invoke(
                                    calculateProgress(fileCount, MAX_FILES),
                                    entryName,
                                    "⚠️ Atlandı (çok büyük): $fileName (${formatFileSize(entry.size)})"
                                )
                                null
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
                progressCallback?.invoke(0, "", "❌ ZIP dosyası açılamadı")
            }
            
            // Detaylı tamamlanma mesajı
            if (errorMessage == null) {
                val codeFilesRead = fileEntries.count { it.isCodeFile && it.content != null }
                progressCallback?.invoke(
                    100, 
                    "", 
                    "✅ Analiz tamamlandı! $fileCount dosya tarandı, $codeFilesRead kod dosyası okundu (${formatFileSize(totalSize)})"
                )
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "ZIP analiz hatası", e)
            errorMessage = "ZIP analiz hatası: ${e.message}"
            progressCallback?.invoke(100, "", "❌ Hata: ${e.message}")
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
     * Progress hesapla
     */
    private fun calculateProgress(current: Int, max: Int): Int {
        return ((current.toDouble() / max.toDouble()) * 100).toInt().coerceIn(0, 100)
    }

    /**
     * ZIP entry içeriğini okur
     */
    private fun readZipEntryContent(
        zipInputStream: ZipInputStream,
        entry: ZipEntry
    ): String {
        val stringBuilder = StringBuilder()

        // Don't use .use {} to avoid closing the underlying ZipInputStream
        val reader = BufferedReader(InputStreamReader(zipInputStream, Charsets.UTF_8))
        var line: String?
        var charCount = 0

        while (reader.readLine().also { line = it } != null && charCount < MAX_CHARS_PER_FILE) {
            stringBuilder.append(line).append('\n')
            charCount += line!!.length + 1
        }

        if (charCount >= MAX_CHARS_PER_FILE) {
            stringBuilder.append("\n[...dosya devamı kesildi - çok büyük...]")
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

        // Klasör yapısı - Geliştirilmiş ağaç görünümü
        sb.appendLine("📁 PROJE İSKELET YAPISI (TREE VIEW):")
        sb.appendLine(formatDirectoryTree(result.directoryStructure, result.files))
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
     * Klasör yapısını ağaç formatında gösterir (Tree View)
     */
    private fun formatDirectoryTree(directories: List<String>, files: List<ZipFileEntry>): String {
        val sb = StringBuilder()
        
        // Tüm yolları (klasörler + dosyalar) birleştir ve sırala
        data class TreeNode(val path: String, val isFile: Boolean, val size: Long = 0)
        
        val allPaths = mutableListOf<TreeNode>()
        directories.forEach { allPaths.add(TreeNode(it, false)) }
        files.forEach { file -> 
            allPaths.add(TreeNode(file.path, true, file.size))
        }
        
        // Yolları sırala
        val sortedPaths = allPaths.sortedBy { it.path }
        
        // Her bir yol için ağaç çizgilerini oluştur
        val pathsShown = mutableSetOf<String>()
        var count = 0
        val maxPaths = 50 // Maksimum gösterilecek öğe sayısı
        
        for (node in sortedPaths) {
            if (count >= maxPaths) {
                sb.appendLine("... ve ${sortedPaths.size - count} öğe daha")
                break
            }
            
            val parts = node.path.split("/")
            val depth = parts.size - 1
            
            // Aynı yolu tekrar gösterme
            if (pathsShown.contains(node.path)) continue
            pathsShown.add(node.path)
            
            // Ağaç çizgisi oluştur
            val prefix = buildString {
                for (i in 0 until depth) {
                    append("│   ")
                }
                if (depth > 0) {
                    append("├── ")
                }
            }
            
            val name = parts.lastOrNull() ?: node.path
            val icon = if (node.isFile) "📄" else "📂"
            val sizeInfo = if (node.isFile && node.size > 0) " (${formatFileSize(node.size)})" else ""
            
            sb.appendLine("$prefix$icon $name$sizeInfo")
            count++
        }
        
        return sb.toString()
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
     * ZIP dosya girişi - mutable content ile düzenleme desteği
     */
    data class ZipFileEntry(
        val name: String,
        val path: String,
        val size: Long,
        val extension: String,
        val isCodeFile: Boolean,
        var content: String?,
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
    
    /**
     * Düzenlenmiş dosyaları ZIP olarak kaydet ve dosya yolunu döndür
     */
    suspend fun createModifiedZip(
        context: Context,
        originalResult: ZipAnalysisResult,
        modifiedFiles: Map<String, String>, // path -> new content
        outputFileName: String = "modified_project_${System.currentTimeMillis()}.zip"
    ): ZipSaveResult = withContext(Dispatchers.IO) {
        try {
            val outputDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: context.filesDir
            val outputFile = File(outputDir, outputFileName)
            
            ZipOutputStream(FileOutputStream(outputFile)).use { zipOut ->
                // Önce klasörleri ekle
                originalResult.directoryStructure.forEach { dir ->
                    val dirEntry = ZipEntry("$dir/")
                    zipOut.putNextEntry(dirEntry)
                    zipOut.closeEntry()
                }
                
                // Dosyaları ekle
                originalResult.files.forEach { file ->
                    val zipEntry = ZipEntry(file.path)
                    zipOut.putNextEntry(zipEntry)
                    
                    // Değiştirilmiş dosya mı kontrol et
                    val content = modifiedFiles[file.path] ?: file.content
                    
                    if (content != null) {
                        // OutputStreamWriter flush after write, don't close (it would close underlying zipOut)
                        val outputStreamWriter = OutputStreamWriter(zipOut, Charsets.UTF_8)
                        outputStreamWriter.write(content)
                        outputStreamWriter.flush()
                    }
                    
                    zipOut.closeEntry()
                }
            }
            
            ZipSaveResult(
                success = true,
                filePath = outputFile.absolutePath,
                fileName = outputFileName,
                errorMessage = null
            )
        } catch (e: Exception) {
            Log.e(TAG, "ZIP oluşturma hatası", e)
            ZipSaveResult(
                success = false,
                filePath = null,
                fileName = null,
                errorMessage = "ZIP oluşturulamadı: ${e.message}"
            )
        }
    }
    
    /**
     * Orijinal ZIP'i modifiye edilmiş içeriklerle yeniden oluştur
     */
    suspend fun recreateZipWithModifications(
        context: Context,
        contentResolver: ContentResolver,
        originalUri: Uri,
        modifiedFiles: Map<String, String>,
        outputFileName: String = "fixed_project_${System.currentTimeMillis()}.zip"
    ): ZipSaveResult = withContext(Dispatchers.IO) {
        try {
            val outputDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: context.filesDir
            val outputFile = File(outputDir, outputFileName)
            
            contentResolver.openInputStream(originalUri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zipIn ->
                    ZipOutputStream(FileOutputStream(outputFile)).use { zipOut ->
                        var entry: ZipEntry? = zipIn.nextEntry
                        
                        while (entry != null) {
                            val entryName = entry.name
                            
                            if (entry.isDirectory) {
                                // Klasörleri olduğu gibi aktar
                                val newEntry = ZipEntry(entryName)
                                zipOut.putNextEntry(newEntry)
                                zipOut.closeEntry()
                            } else {
                                val newEntry = ZipEntry(entryName)
                                zipOut.putNextEntry(newEntry)
                                
                                if (modifiedFiles.containsKey(entryName)) {
                                    // Değiştirilmiş içeriği yaz
                                    val modifiedContent = modifiedFiles[entryName]!!
                                    zipOut.write(modifiedContent.toByteArray(Charsets.UTF_8))
                                } else {
                                    // Orijinal içeriği kopyala
                                    val buffer = ByteArray(4096)
                                    var len: Int
                                    while (zipIn.read(buffer).also { len = it } > 0) {
                                        zipOut.write(buffer, 0, len)
                                    }
                                }
                                
                                zipOut.closeEntry()
                            }
                            
                            entry = try {
                                zipIn.nextEntry
                            } catch (e: Exception) {
                                null
                            }
                        }
                    }
                }
            } ?: throw Exception("ZIP dosyası açılamadı")
            
            ZipSaveResult(
                success = true,
                filePath = outputFile.absolutePath,
                fileName = outputFileName,
                errorMessage = null
            )
        } catch (e: Exception) {
            Log.e(TAG, "ZIP yeniden oluşturma hatası", e)
            ZipSaveResult(
                success = false,
                filePath = null,
                fileName = null,
                errorMessage = "ZIP oluşturulamadı: ${e.message}"
            )
        }
    }
    
    /**
     * Builds a raw code bundle without any analysis or commentary.
     * This is used for silent code reading - only contains file paths and raw content.
     * No human-style analysis, summaries, or suggestions are included.
     */
    fun buildRawCodeBundle(result: ZipAnalysisResult): String {
        val sb = StringBuilder()
        result.files
            .filter { it.isCodeFile && it.content != null }
            .forEach { file ->
                sb.appendLine("/// FILE: ${file.path}")
                sb.appendLine(file.content)
                sb.appendLine()
            }
        return sb.toString()
    }
    
    /**
     * Builds neutral technical info for UI display.
     * Only shows counts, sizes, and structure - no opinionated analysis.
     */
    fun buildNeutralSummary(result: ZipAnalysisResult): String {
        if (!result.success) {
            return "❌ Error: ${result.errorMessage}"
        }
        
        val sb = StringBuilder()
        sb.appendLine("📁 Files read: ${result.totalFiles}")
        sb.appendLine("📂 Folders: ${result.directoryStructure.size}")
        sb.appendLine("💾 Total size: ${formatFileSize(result.totalSize)}")
        
        val codeFilesCount = result.files.count { it.isCodeFile && it.content != null }
        sb.appendLine("📝 Code files loaded: $codeFilesCount")
        sb.appendLine()
        sb.appendLine("✅ Files have been read successfully.")
        sb.appendLine("You can now ask questions about this code.")
        
        return sb.toString()
    }
    
    /**
     * Hata analiz promptu oluştur
     */
    fun generateErrorFixPrompt(result: ZipAnalysisResult): String {
        val sb = StringBuilder()
        sb.appendLine("🔧 HATA ANALİZ VE DÜZELTME TALEBİ")
        sb.appendLine()
        sb.appendLine("Bu ${getProjectTypeDescription(result.projectType)} projesindeki hataları bul ve düzelt.")
        sb.appendLine()
        sb.appendLine("📋 TALİMATLAR:")
        sb.appendLine("1. Her dosyayı analiz et ve hataları tespit et")
        sb.appendLine("2. Syntax hataları, mantık hataları, güvenlik açıkları ara")
        sb.appendLine("3. Her hata için:")
        sb.appendLine("   - Dosya yolunu belirt")
        sb.appendLine("   - Hatanın ne olduğunu açıkla")
        sb.appendLine("   - DÜZELTİLMİŞ KODUN TAMAMINI ver (parça değil)")
        sb.appendLine("4. Best practices önerilerini ekle")
        sb.appendLine()
        sb.appendLine("⚠️ ÖNEMLİ: Her düzeltilmiş dosya için TAMAMEN çalışır kod ver!")
        sb.appendLine()
        
        return sb.toString() + formatAnalysisResult(result)
    }
    
    /**
     * Özellik ekleme promptu oluştur
     */
    fun generateAddFeaturePrompt(result: ZipAnalysisResult, featureRequest: String): String {
        val sb = StringBuilder()
        sb.appendLine("➕ YENİ ÖZELLİK EKLEME TALEBİ")
        sb.appendLine()
        sb.appendLine("Proje Tipi: ${getProjectTypeDescription(result.projectType)}")
        sb.appendLine()
        sb.appendLine("🎯 İSTENEN ÖZELLİK:")
        sb.appendLine(featureRequest)
        sb.appendLine()
        sb.appendLine("📋 TALİMATLAR:")
        sb.appendLine("1. Mevcut proje yapısını koru")
        sb.appendLine("2. Gerekli dosyaları belirle ve değişiklikleri yap")
        sb.appendLine("3. Her değişiklik için:")
        sb.appendLine("   - Dosya yolunu belirt")
        sb.appendLine("   - Eklenen/değiştirilen kodun TAMAMINI ver")
        sb.appendLine("4. Yeni dosya gerekiyorsa tam içeriği ile oluştur")
        sb.appendLine("5. Bağımlılıklar gerekiyorsa listele")
        sb.appendLine()
        sb.appendLine("⚠️ ÖNEMLİ: Tüm kod değişiklikleri TAMAMEN çalışır olmalı!")
        sb.appendLine()
        
        return sb.toString() + formatAnalysisResult(result)
    }
    
    /**
     * Seçili dosyaları analiz et ve formatla
     */
    fun formatSelectedFilesAnalysis(
        selectedFiles: List<ZipFileEntry>,
        projectType: ProjectType
    ): String {
        val sb = StringBuilder()
        
        sb.appendLine("📝 SEÇİLİ DOSYA ANALİZİ")
        sb.appendLine("═".repeat(50))
        sb.appendLine()
        sb.appendLine("🎯 Proje Tipi: ${getProjectTypeDescription(projectType)}")
        sb.appendLine("📁 Seçili Dosya Sayısı: ${selectedFiles.size}")
        sb.appendLine()
        sb.appendLine("═".repeat(50))
        sb.appendLine()
        
        selectedFiles.forEach { file ->
            sb.appendLine("┌─────────────────────────────────────────────────")
            sb.appendLine("│ 📄 Dosya: ${file.path}")
            sb.appendLine("│ 💾 Boyut: ${formatFileSize(file.size)}")
            sb.appendLine("│ 🔤 Dil: ${file.language ?: "Bilinmiyor"}")
            sb.appendLine("│ 📋 Uzantı: ${file.extension}")
            sb.appendLine("└─────────────────────────────────────────────────")
            sb.appendLine()
            
            if (!file.content.isNullOrEmpty()) {
                sb.appendLine("📝 İçerik:")
                sb.appendLine(file.content ?: "")
                sb.appendLine()
            } else {
                sb.appendLine("⚠️ Dosya içeriği okunamadı veya boş.")
                sb.appendLine()
            }
            
            sb.appendLine("─".repeat(50))
            sb.appendLine()
        }
        
        sb.appendLine("✅ Toplam ${selectedFiles.size} dosya analiz edildi.")
        sb.appendLine()
        sb.appendLine("💡 Bu dosyaları inceleyip, kod kalitesi, hatalar, iyileştirmeler ve best practices hakkında geri bildirim ver.")
        
        return sb.toString()
    }
    
    /**
     * ZIP kaydetme sonucu
     */
    data class ZipSaveResult(
        val success: Boolean,
        val filePath: String?,
        val fileName: String?,
        val errorMessage: String?
    )
}
