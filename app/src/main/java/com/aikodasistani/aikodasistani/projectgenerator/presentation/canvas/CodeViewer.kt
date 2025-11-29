package com.aikodasistani.aikodasistani.projectgenerator.presentation.canvas

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// Animation constants
private const val STREAMING_DELAY_FAST_MS = 10L
private const val STREAMING_DELAY_NORMAL_MS = 50L
private const val SLIDE_ANIMATION_OFFSET_PX = -10

/**
 * Code viewer component with syntax highlighting and streaming animation.
 * Displays code line-by-line with real-time progress tracking.
 */
@Composable
fun CodeViewer(
    file: GeneratedFileState?,
    isStreaming: Boolean,
    playbackState: PlaybackState,
    onCopyCode: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (file == null) {
        EmptyCodeViewer(modifier)
        return
    }
    
    val listState = rememberLazyListState()
    val lines = remember(file.content) { file.content.lines() }
    var visibleLineCount by remember { mutableIntStateOf(if (isStreaming) 0 else lines.size) }
    
    // Streaming animation
    LaunchedEffect(isStreaming, playbackState, lines.size) {
        if (isStreaming && playbackState != PlaybackState.PAUSED) {
            visibleLineCount = 0
            val delayMs = when (playbackState) {
                PlaybackState.FAST_FORWARD -> STREAMING_DELAY_FAST_MS
                else -> STREAMING_DELAY_NORMAL_MS
            }
            
            for (i in lines.indices) {
                if (playbackState == PlaybackState.PAUSED) break
                visibleLineCount = i + 1
                if (i < lines.size - 1) {
                    listState.animateScrollToItem(minOf(i + 1, lines.size - 1))
                }
                delay(delayMs)
            }
        } else {
            visibleLineCount = lines.size
        }
    }
    
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header with file info
            CodeViewerHeader(
                file = file,
                visibleLines = visibleLineCount,
                totalLines = lines.size,
                onCopyCode = { onCopyCode(file.content) }
            )
            
            HorizontalDivider()
            
            // Code content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF1E1E1E))
            ) {
                SelectionContainer {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                    ) {
                        itemsIndexed(
                            items = lines.take(visibleLineCount),
                            key = { index, _ -> index }
                        ) { index, line ->
                            CodeLine(
                                lineNumber = index + 1,
                                content = line,
                                language = file.languageHint,
                                isNewLine = index == visibleLineCount - 1 && isStreaming
                            )
                        }
                    }
                }
                
                // Streaming indicator
                if (isStreaming && visibleLineCount < lines.size) {
                    StreamingIndicator(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                    )
                }
            }
        }
    }
}

/**
 * Header showing file name, status, and actions.
 */
@Composable
private fun CodeViewerHeader(
    file: GeneratedFileState,
    visibleLines: Int,
    totalLines: Int,
    onCopyCode: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // File icon and name
        Text(
            text = getFileIcon(file.extension),
            fontSize = 16.sp
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = file.path,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        
        // Status badge
        StatusChip(status = file.status)
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Line count
        Text(
            text = "$visibleLines / $totalLines lines",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Copy button
        IconButton(
            onClick = onCopyCode,
            modifier = Modifier.size(32.dp)
        ) {
            Text(text = "📋", fontSize = 14.sp)
        }
    }
}

/**
 * Individual code line with line number and syntax highlighting.
 */
@Composable
private fun CodeLine(
    lineNumber: Int,
    content: String,
    language: String,
    isNewLine: Boolean
) {
    val lineNumberColor = Color(0xFF858585)
    val codeFont = FontFamily.Monospace
    
    AnimatedVisibility(
        visible = true,
        enter = if (isNewLine) fadeIn() + slideInVertically { SLIDE_ANIMATION_OFFSET_PX } else fadeIn(snap())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Line number
            Text(
                text = lineNumber.toString().padStart(4, ' '),
                fontFamily = codeFont,
                fontSize = 12.sp,
                color = lineNumberColor,
                modifier = Modifier.width(40.dp)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Code content with syntax highlighting
            Text(
                text = applySyntaxHighlighting(content, language),
                fontFamily = codeFont,
                fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Applies basic syntax highlighting based on language.
 */
@Composable
private fun applySyntaxHighlighting(code: String, language: String): AnnotatedString {
    val keywords = getKeywords(language)
    val keywordColor = Color(0xFF569CD6)
    val stringColor = Color(0xFFCE9178)
    val commentColor = Color(0xFF6A9955)
    val numberColor = Color(0xFFB5CEA8)
    val annotationColor = Color(0xFFDCDCAA)
    val defaultColor = Color(0xFFD4D4D4)
    
    return buildAnnotatedString {
        var i = 0
        while (i < code.length) {
            when {
                // String literals (double quotes)
                code[i] == '"' -> {
                    val endIndex = findStringEnd(code, i, '"')
                    withStyle(SpanStyle(color = stringColor)) {
                        append(code.substring(i, endIndex))
                    }
                    i = endIndex
                }
                // String literals (single quotes)
                code[i] == '\'' -> {
                    val endIndex = findStringEnd(code, i, '\'')
                    withStyle(SpanStyle(color = stringColor)) {
                        append(code.substring(i, endIndex))
                    }
                    i = endIndex
                }
                // Line comments
                code.substring(i).startsWith("//") -> {
                    withStyle(SpanStyle(color = commentColor)) {
                        append(code.substring(i))
                    }
                    i = code.length
                }
                // Hash comments (Python, Shell)
                code[i] == '#' && (language == "python" || language == "shell" || language == "yaml") -> {
                    withStyle(SpanStyle(color = commentColor)) {
                        append(code.substring(i))
                    }
                    i = code.length
                }
                // Annotations
                code[i] == '@' && i + 1 < code.length && code[i + 1].isLetter() -> {
                    val endIndex = findWordEnd(code, i + 1)
                    withStyle(SpanStyle(color = annotationColor)) {
                        append(code.substring(i, endIndex))
                    }
                    i = endIndex
                }
                // Numbers
                code[i].isDigit() -> {
                    val endIndex = findNumberEnd(code, i)
                    withStyle(SpanStyle(color = numberColor)) {
                        append(code.substring(i, endIndex))
                    }
                    i = endIndex
                }
                // Keywords and identifiers
                code[i].isLetter() || code[i] == '_' -> {
                    val endIndex = findWordEnd(code, i)
                    val word = code.substring(i, endIndex)
                    val color = if (word in keywords) keywordColor else defaultColor
                    withStyle(SpanStyle(color = color)) {
                        append(word)
                    }
                    i = endIndex
                }
                else -> {
                    withStyle(SpanStyle(color = defaultColor)) {
                        append(code[i])
                    }
                    i++
                }
            }
        }
    }
}

private fun findStringEnd(code: String, start: Int, quote: Char): Int {
    var i = start + 1
    while (i < code.length) {
        if (code[i] == '\\' && i + 1 < code.length) {
            i += 2
        } else if (code[i] == quote) {
            return i + 1
        } else {
            i++
        }
    }
    return code.length
}

private fun findWordEnd(code: String, start: Int): Int {
    var i = start
    while (i < code.length && (code[i].isLetterOrDigit() || code[i] == '_')) {
        i++
    }
    return i
}

private fun findNumberEnd(code: String, start: Int): Int {
    var i = start
    while (i < code.length && (code[i].isDigit() || code[i] == '.' || code[i] == 'x' || 
           code[i] == 'f' || code[i] == 'L' || code[i] in 'a'..'f' || code[i] in 'A'..'F')) {
        i++
    }
    return i
}

private fun getKeywords(language: String): Set<String> = when (language) {
    "kotlin" -> setOf(
        "fun", "val", "var", "class", "object", "interface", "sealed", "data", "enum",
        "if", "else", "when", "for", "while", "do", "return", "break", "continue",
        "package", "import", "private", "public", "protected", "internal", "override",
        "abstract", "open", "final", "companion", "suspend", "inline", "noinline",
        "crossinline", "reified", "by", "lazy", "lateinit", "const", "true", "false",
        "null", "this", "super", "is", "as", "in", "out", "typealias", "constructor",
        "init", "get", "set", "try", "catch", "finally", "throw"
    )
    "java" -> setOf(
        "public", "private", "protected", "class", "interface", "extends", "implements",
        "static", "final", "abstract", "void", "int", "long", "double", "float", "boolean",
        "byte", "char", "short", "if", "else", "for", "while", "do", "switch", "case",
        "default", "break", "continue", "return", "new", "this", "super", "null", "true",
        "false", "import", "package", "throws", "throw", "try", "catch", "finally"
    )
    "javascript", "typescript" -> setOf(
        "function", "const", "let", "var", "if", "else", "for", "while", "do", "switch",
        "case", "default", "break", "continue", "return", "class", "extends", "new",
        "this", "super", "import", "export", "from", "async", "await", "try", "catch",
        "finally", "throw", "null", "undefined", "true", "false", "typeof", "instanceof",
        "interface", "type", "enum", "implements", "private", "public", "protected"
    )
    "python" -> setOf(
        "def", "class", "if", "elif", "else", "for", "while", "break", "continue",
        "return", "yield", "import", "from", "as", "try", "except", "finally", "raise",
        "with", "assert", "pass", "lambda", "and", "or", "not", "in", "is", "None",
        "True", "False", "global", "nonlocal", "async", "await"
    )
    "swift" -> setOf(
        "func", "var", "let", "class", "struct", "enum", "protocol", "extension",
        "if", "else", "switch", "case", "default", "for", "while", "repeat", "break",
        "continue", "return", "import", "public", "private", "internal", "open", "final",
        "override", "static", "self", "Self", "nil", "true", "false", "guard", "defer",
        "do", "try", "catch", "throw", "throws", "async", "await"
    )
    "dart" -> setOf(
        "void", "var", "final", "const", "class", "extends", "implements", "with",
        "if", "else", "for", "while", "do", "switch", "case", "default", "break",
        "continue", "return", "import", "export", "part", "library", "async", "await",
        "try", "catch", "finally", "throw", "new", "this", "super", "null", "true", "false",
        "abstract", "static", "late", "required", "factory", "get", "set"
    )
    else -> emptySet()
}

private fun getFileIcon(extension: String): String = when (extension.lowercase()) {
    "kt", "kts" -> "📜"
    "java" -> "☕"
    "xml" -> "📋"
    "json" -> "📊"
    "gradle" -> "🔧"
    "py" -> "🐍"
    "js", "jsx" -> "📒"
    "ts", "tsx" -> "📘"
    "swift" -> "🔶"
    "dart" -> "🎯"
    "go" -> "🔵"
    "rs" -> "🦀"
    "html" -> "🌐"
    "css", "scss" -> "🎨"
    "md" -> "📝"
    "yaml", "yml" -> "⚙️"
    "sh", "bash" -> "💻"
    else -> "📄"
}

/**
 * Status chip for file generation status.
 */
@Composable
private fun StatusChip(status: FileStatus) {
    val (text, color) = when (status) {
        FileStatus.PENDING -> "Pending" to MaterialTheme.colorScheme.outline
        FileStatus.GENERATING -> "Generating" to MaterialTheme.colorScheme.primary
        FileStatus.COMPLETED -> "Complete" to Color(0xFF4CAF50)
        FileStatus.ERROR -> "Error" to MaterialTheme.colorScheme.error
        FileStatus.SKIPPED -> "Skipped" to MaterialTheme.colorScheme.outline
    }
    
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.2f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

/**
 * Streaming indicator showing that code is being written.
 */
@Composable
private fun StreamingIndicator(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "stream_pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = alpha)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "⚡", fontSize = 12.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Writing...",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

/**
 * Empty state when no file is selected.
 */
@Composable
private fun EmptyCodeViewer(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "📄", fontSize = 48.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Select a file to view its content",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
