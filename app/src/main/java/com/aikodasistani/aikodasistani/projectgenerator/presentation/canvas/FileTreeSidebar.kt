package com.aikodasistani.aikodasistani.projectgenerator.presentation.canvas

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Tree indentation per depth level
private const val TREE_INDENT_DP = 16

/**
 * File tree sidebar component for project structure visualization.
 * Shows hierarchical structure with clickable files and status indicators.
 */
@Composable
fun FileTreeSidebar(
    tree: FileTreeNode,
    selectedFileIndex: Int,
    onFileClick: (Int) -> Unit,
    onDirectoryToggle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            // Header
            Text(
                text = "📁 ${tree.name}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(8.dp)
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            
            // Tree content
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(flattenTree(tree.children, mutableMapOf())) { (node, expandedDirs) ->
                    FileTreeItem(
                        node = node,
                        isSelected = node.fileIndex == selectedFileIndex,
                        isExpanded = expandedDirs[node.path] ?: node.isExpanded,
                        onItemClick = {
                            if (node.isDirectory) {
                                onDirectoryToggle(node.path)
                            } else {
                                onFileClick(node.fileIndex)
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * Flattens the tree structure for LazyColumn display.
 */
private fun flattenTree(
    nodes: List<FileTreeNode>,
    expandedDirs: MutableMap<String, Boolean>
): List<Pair<FileTreeNode, Map<String, Boolean>>> {
    val result = mutableListOf<Pair<FileTreeNode, Map<String, Boolean>>>()
    
    fun traverse(nodeList: List<FileTreeNode>) {
        for (node in nodeList) {
            result.add(node to expandedDirs.toMap())
            if (node.isDirectory && (expandedDirs[node.path] ?: node.isExpanded)) {
                traverse(node.children)
            }
        }
    }
    
    traverse(nodes)
    return result
}

/**
 * Individual file tree item with icon, name, and status indicator.
 */
@Composable
private fun FileTreeItem(
    node: FileTreeNode,
    isSelected: Boolean,
    isExpanded: Boolean,
    onItemClick: () -> Unit
) {
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 90f else 0f,
        label = "expand_rotation"
    )
    
    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        else -> Color.Transparent
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (node.depth * TREE_INDENT_DP).dp)
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .clickable(onClick = onItemClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Expand/Collapse indicator for directories
        if (node.isDirectory) {
            Text(
                text = "▶",
                fontSize = 10.sp,
                modifier = Modifier.rotate(rotationAngle)
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        
        // File/Folder icon
        Text(
            text = getFileIcon(node),
            fontSize = 14.sp
        )
        
        Spacer(modifier = Modifier.width(6.dp))
        
        // File name
        Text(
            text = node.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (node.isDirectory) FontWeight.Medium else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        
        // Status badge for files
        if (!node.isDirectory) {
            StatusBadge(status = node.status)
        }
    }
}

/**
 * Status badge showing file generation status.
 */
@Composable
private fun StatusBadge(status: FileStatus) {
    val (icon, color) = when (status) {
        FileStatus.PENDING -> "⏳" to MaterialTheme.colorScheme.outline
        FileStatus.GENERATING -> "⚡" to MaterialTheme.colorScheme.primary
        FileStatus.COMPLETED -> "✓" to Color(0xFF4CAF50)
        FileStatus.ERROR -> "✗" to MaterialTheme.colorScheme.error
        FileStatus.SKIPPED -> "⊘" to MaterialTheme.colorScheme.outline
    }
    
    Text(
        text = icon,
        fontSize = 12.sp,
        color = color
    )
}

/**
 * Gets the appropriate icon for a file or directory based on type.
 */
private fun getFileIcon(node: FileTreeNode): String {
    if (node.isDirectory) {
        return "📁"
    }
    
    val extension = node.name.substringAfterLast('.', "").lowercase()
    return when (extension) {
        "kt", "kts" -> "📜"  // Kotlin
        "java" -> "☕"       // Java
        "xml" -> "📋"        // XML
        "json" -> "📊"       // JSON
        "gradle" -> "🔧"     // Gradle
        "py" -> "🐍"         // Python
        "js", "jsx" -> "📒"  // JavaScript
        "ts", "tsx" -> "📘"  // TypeScript
        "swift" -> "🔶"      // Swift
        "dart" -> "🎯"       // Dart
        "go" -> "🔵"         // Go
        "rs" -> "🦀"         // Rust
        "html" -> "🌐"       // HTML
        "css", "scss" -> "🎨" // CSS
        "md" -> "📝"         // Markdown
        "yaml", "yml" -> "⚙️" // YAML
        "sh", "bash" -> "💻" // Shell
        "sql" -> "🗄️"        // SQL
        "txt" -> "📄"        // Text
        "png", "jpg", "gif", "svg" -> "🖼️" // Images
        else -> "📄"
    }
}

/**
 * Expandable file tree section header.
 */
@Composable
fun FileTreeSectionHeader(
    title: String,
    count: Int,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 90f else 0f,
        label = "section_rotation"
    )
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "▶",
            fontSize = 12.sp,
            modifier = Modifier.rotate(rotationAngle)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

/**
 * Summary footer showing file counts and stats.
 */
@Composable
fun FileTreeSummary(
    totalFiles: Int,
    completedFiles: Int,
    pendingFiles: Int,
    errorFiles: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                icon = "📄",
                value = totalFiles.toString(),
                label = "Total"
            )
            StatItem(
                icon = "✓",
                value = completedFiles.toString(),
                label = "Done",
                valueColor = Color(0xFF4CAF50)
            )
            if (pendingFiles > 0) {
                StatItem(
                    icon = "⏳",
                    value = pendingFiles.toString(),
                    label = "Pending"
                )
            }
            if (errorFiles > 0) {
                StatItem(
                    icon = "✗",
                    value = errorFiles.toString(),
                    label = "Errors",
                    valueColor = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    icon: String,
    value: String,
    label: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = icon, fontSize = 16.sp)
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
