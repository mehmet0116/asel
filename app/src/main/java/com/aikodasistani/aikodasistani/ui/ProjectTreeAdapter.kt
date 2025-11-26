package com.aikodasistani.aikodasistani.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aikodasistani.aikodasistani.R
import com.aikodasistani.aikodasistani.util.ZipFileAnalyzerUtil

/**
 * Adapter for displaying project directory tree structure in a RecyclerView
 */
class ProjectTreeAdapter(
    private val onItemClick: ((TreeNode) -> Unit)? = null
) : RecyclerView.Adapter<ProjectTreeAdapter.TreeViewHolder>() {

    private val items = mutableListOf<TreeNode>()
    private val expandedNodes = mutableSetOf<String>()

    /**
     * Tree node data class
     */
    data class TreeNode(
        val path: String,
        val name: String,
        val isDirectory: Boolean,
        val depth: Int,
        val size: Long = 0,
        val language: String? = null,
        val hasChildren: Boolean = false
    )

    fun setData(
        directories: List<String>,
        files: List<ZipFileAnalyzerUtil.ZipFileEntry>
    ) {
        items.clear()
        expandedNodes.clear()
        
        // Build tree from directories and files
        val treeNodes = buildTreeNodes(directories, files)
        items.addAll(treeNodes)
        
        notifyDataSetChanged()
    }

    private fun buildTreeNodes(
        directories: List<String>,
        files: List<ZipFileAnalyzerUtil.ZipFileEntry>
    ): List<TreeNode> {
        val result = mutableListOf<TreeNode>()
        val allPaths = mutableSetOf<String>()
        
        // Add all directories
        directories.forEach { dir ->
            allPaths.add(dir)
        }
        
        // Add parent directories of files
        files.forEach { file ->
            val parentPath = file.path.substringBeforeLast('/', "")
            if (parentPath.isNotEmpty()) {
                allPaths.add(parentPath)
            }
        }
        
        // Create directory nodes map
        val dirMap = mutableMapOf<String, MutableList<TreeNode>>()
        
        // Process directories
        allPaths.sorted().forEach { dirPath ->
            val parts = dirPath.split("/")
            val depth = parts.size - 1
            val name = parts.last()
            val hasChildren = allPaths.any { it.startsWith("$dirPath/") } || 
                              files.any { it.path.startsWith("$dirPath/") }
            
            val node = TreeNode(
                path = dirPath,
                name = name,
                isDirectory = true,
                depth = depth,
                hasChildren = hasChildren
            )
            
            val parentPath = dirPath.substringBeforeLast('/', "")
            dirMap.getOrPut(parentPath) { mutableListOf() }.add(node)
        }
        
        // Process files
        files.forEach { file ->
            val parentPath = file.path.substringBeforeLast('/', "")
            val parts = file.path.split("/")
            val depth = parts.size - 1
            
            val node = TreeNode(
                path = file.path,
                name = file.name.substringAfterLast('/'),
                isDirectory = false,
                depth = depth,
                size = file.size,
                language = file.language,
                hasChildren = false
            )
            
            dirMap.getOrPut(parentPath) { mutableListOf() }.add(node)
        }
        
        // Build flat list from tree (only root level items initially)
        fun addNodesRecursively(parentPath: String, targetDepth: Int) {
            val children = dirMap[parentPath]?.sortedWith(
                compareByDescending<TreeNode> { it.isDirectory }.thenBy { it.name }
            ) ?: return
            
            children.forEach { child ->
                if (child.depth == targetDepth) {
                    result.add(child)
                    // If directory is expanded, add its children
                    if (child.isDirectory && expandedNodes.contains(child.path)) {
                        addNodesRecursively(child.path, targetDepth + 1)
                    }
                }
            }
        }
        
        // Start with root level (depth 0)
        addNodesRecursively("", 0)
        
        // If no items at root, try to find the common prefix
        if (result.isEmpty()) {
            val allDirs = dirMap.keys.filter { it.isNotEmpty() }.sorted()
            val firstDir = allDirs.firstOrNull() ?: return result
            
            // Add all items sorted
            val sortedItems = dirMap.values.flatten().sortedWith(
                compareBy<TreeNode> { it.path }.thenByDescending { it.isDirectory }
            )
            
            // Take first N items
            result.addAll(sortedItems.take(50))
        }
        
        return result.take(50) // Limit to 50 items for performance
    }

    fun toggleNode(position: Int) {
        if (position < 0 || position >= items.size) return
        
        val node = items[position]
        if (!node.isDirectory || !node.hasChildren) return
        
        if (expandedNodes.contains(node.path)) {
            expandedNodes.remove(node.path)
        } else {
            expandedNodes.add(node.path)
        }
        
        // Rebuild the tree
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TreeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_project_tree, parent, false)
        return TreeViewHolder(view)
    }

    override fun onBindViewHolder(holder: TreeViewHolder, position: Int) {
        val node = items[position]
        holder.bind(node, expandedNodes.contains(node.path))
        
        holder.itemView.setOnClickListener {
            if (node.isDirectory && node.hasChildren) {
                toggleNode(holder.bindingAdapterPosition)
            }
            onItemClick?.invoke(node)
        }
    }

    override fun getItemCount(): Int = items.size

    class TreeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val viewIndent: View = itemView.findViewById(R.id.viewIndent)
        private val ivExpand: ImageView = itemView.findViewById(R.id.ivExpand)
        private val ivIcon: ImageView = itemView.findViewById(R.id.ivIcon)
        private val tvName: TextView = itemView.findViewById(R.id.tvName)
        private val tvSize: TextView = itemView.findViewById(R.id.tvSize)

        fun bind(node: TreeNode, isExpanded: Boolean) {
            // Set indentation
            val indentWidth = node.depth * 20 // 20dp per level
            val params = viewIndent.layoutParams
            params.width = (indentWidth * itemView.context.resources.displayMetrics.density).toInt()
            viewIndent.layoutParams = params

            // Set name
            tvName.text = node.name

            // Set icon
            if (node.isDirectory) {
                ivIcon.setImageResource(R.drawable.ic_folder)
                tvSize.visibility = View.GONE
                
                // Show expand/collapse icon for directories with children
                if (node.hasChildren) {
                    ivExpand.visibility = View.VISIBLE
                    ivExpand.setImageResource(
                        if (isExpanded) R.drawable.ic_arrow_down else R.drawable.ic_arrow_right
                    )
                } else {
                    ivExpand.visibility = View.INVISIBLE
                }
            } else {
                ivIcon.setImageResource(getFileIcon(node.language))
                ivExpand.visibility = View.GONE
                
                // Show file size
                if (node.size > 0) {
                    tvSize.visibility = View.VISIBLE
                    tvSize.text = formatFileSize(node.size)
                } else {
                    tvSize.visibility = View.GONE
                }
            }
        }

        private fun getFileIcon(language: String?): Int {
            return when (language) {
                "Kotlin", "Java" -> R.drawable.ic_file
                "Python" -> R.drawable.ic_file
                "JavaScript", "TypeScript", "React" -> R.drawable.ic_file
                "HTML", "CSS" -> R.drawable.ic_file
                else -> R.drawable.ic_file
            }
        }

        private fun formatFileSize(bytes: Long): String {
            return when {
                bytes < 1024 -> "$bytes B"
                bytes < 1024 * 1024 -> "${bytes / 1024} KB"
                else -> "${bytes / (1024 * 1024)} MB"
            }
        }
    }
}
