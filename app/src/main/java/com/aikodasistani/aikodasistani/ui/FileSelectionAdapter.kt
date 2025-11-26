package com.aikodasistani.aikodasistani.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.aikodasistani.aikodasistani.R
import com.aikodasistani.aikodasistani.util.ZipFileAnalyzerUtil

/**
 * RecyclerView adapter for displaying and selecting files from ZIP archive
 */
class FileSelectionAdapter(
    private var files: List<ZipFileAnalyzerUtil.ZipFileEntry>,
    private val onSelectionChanged: (Int) -> Unit
) : RecyclerView.Adapter<FileSelectionAdapter.FileViewHolder>() {

    private val selectedFiles = mutableSetOf<String>()
    private var filteredFiles: List<ZipFileAnalyzerUtil.ZipFileEntry> = files

    inner class FileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardFile: CardView = itemView.findViewById(R.id.cardFile)
        val cbFileSelect: CheckBox = itemView.findViewById(R.id.cbFileSelect)
        val ivFileIcon: ImageView = itemView.findViewById(R.id.ivFileIcon)
        val tvFileName: TextView = itemView.findViewById(R.id.tvFileName)
        val tvFilePath: TextView = itemView.findViewById(R.id.tvFilePath)
        val tvFileLanguage: TextView = itemView.findViewById(R.id.tvFileLanguage)
        val tvFileSize: TextView = itemView.findViewById(R.id.tvFileSize)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_file_selection, parent, false)
        return FileViewHolder(view)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        val file = filteredFiles[position]
        val fileName = file.name.substringAfterLast('/')
        val filePath = file.path.substringBeforeLast('/', "")

        holder.tvFileName.text = fileName
        holder.tvFilePath.text = if (filePath.isEmpty()) "/" else filePath
        holder.tvFileSize.text = formatFileSize(file.size)

        // Language badge
        if (file.language != null) {
            holder.tvFileLanguage.text = file.language
            holder.tvFileLanguage.visibility = View.VISIBLE
        } else {
            holder.tvFileLanguage.visibility = View.GONE
        }

        // Icon based on file type
        if (file.isCodeFile) {
            holder.ivFileIcon.setImageResource(R.drawable.ic_file)
        } else {
            holder.ivFileIcon.setImageResource(R.drawable.ic_folder)
        }

        // Selection state
        val isSelected = selectedFiles.contains(file.path)
        holder.cbFileSelect.isChecked = isSelected
        
        // Visual feedback for selection
        if (isSelected) {
            holder.cardFile.setCardBackgroundColor(
                holder.itemView.context.getColor(R.color.light_blue)
            )
        } else {
            holder.cardFile.setCardBackgroundColor(
                holder.itemView.context.getColor(android.R.color.white)
            )
        }

        // Click handlers
        holder.cardFile.setOnClickListener {
            toggleSelection(file.path)
            notifyItemChanged(position)
        }

        holder.cbFileSelect.setOnClickListener {
            toggleSelection(file.path)
            notifyItemChanged(position)
        }
    }

    override fun getItemCount(): Int = filteredFiles.size

    private fun toggleSelection(filePath: String) {
        if (selectedFiles.contains(filePath)) {
            selectedFiles.remove(filePath)
        } else {
            selectedFiles.add(filePath)
        }
        onSelectionChanged(selectedFiles.size)
    }

    fun selectAll() {
        selectedFiles.clear()
        selectedFiles.addAll(filteredFiles.map { it.path })
        notifyDataSetChanged()
        onSelectionChanged(selectedFiles.size)
    }

    fun clearSelection() {
        selectedFiles.clear()
        notifyDataSetChanged()
        onSelectionChanged(0)
    }

    fun getSelectedFiles(): List<ZipFileAnalyzerUtil.ZipFileEntry> {
        return files.filter { selectedFiles.contains(it.path) }
    }

    fun filter(query: String, codeFilesOnly: Boolean) {
        filteredFiles = files.filter { file ->
            val matchesQuery = if (query.isEmpty()) {
                true
            } else {
                file.name.contains(query, ignoreCase = true) ||
                file.path.contains(query, ignoreCase = true)
            }
            
            val matchesType = if (codeFilesOnly) {
                file.isCodeFile
            } else {
                true
            }
            
            matchesQuery && matchesType
        }
        notifyDataSetChanged()
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> "${bytes / (1024 * 1024)} MB"
        }
    }
}
