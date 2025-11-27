package com.aikodasistani.aikodasistani.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aikodasistani.aikodasistani.R
import com.aikodasistani.aikodasistani.data.Lesson
import com.google.android.material.chip.Chip

/**
 * RecyclerView Adapter for displaying lessons
 */
class LessonAdapter(
    private val onLessonClick: (Lesson) -> Unit
) : ListAdapter<Lesson, LessonAdapter.LessonViewHolder>(LessonDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LessonViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_lesson, parent, false)
        return LessonViewHolder(view)
    }

    override fun onBindViewHolder(holder: LessonViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class LessonViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivCompleted: ImageView = itemView.findViewById(R.id.ivCompleted)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        private val tvDuration: TextView = itemView.findViewById(R.id.tvDuration)
        private val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        private val progressBar: ProgressBar = itemView.findViewById(R.id.progressBar)
        private val tvProgress: TextView = itemView.findViewById(R.id.tvProgress)
        private val chipCategory: Chip = itemView.findViewById(R.id.chipCategory)
        private val chipDifficulty: Chip = itemView.findViewById(R.id.chipDifficulty)
        private val btnStart: Button = itemView.findViewById(R.id.btnStart)

        fun bind(lesson: Lesson) {
            val context = itemView.context

            // Title
            tvTitle.text = lesson.title

            // Completed icon
            ivCompleted.isVisible = lesson.isCompleted

            // Duration
            tvDuration.text = "⏱️ ${lesson.duration} dk"

            // Description
            tvDescription.text = lesson.description

            // Progress
            progressBar.progress = lesson.progress
            tvProgress.text = "${lesson.progress}%"

            // Category chip
            val categoryEmoji = when (lesson.category.lowercase()) {
                "kotlin" -> "🟣"
                "python" -> "🐍"
                "javascript" -> "💛"
                "algorithms" -> "⚙️"
                "data-structures" -> "🏗️"
                else -> "📚"
            }
            chipCategory.text = "$categoryEmoji ${lesson.category.replaceFirstChar { 
                if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() 
            }}"

            // Difficulty chip
            val (difficultyText, difficultyColor) = when (lesson.difficulty.lowercase()) {
                "beginner" -> Pair(context.getString(R.string.beginner), R.color.success)
                "intermediate" -> Pair(context.getString(R.string.intermediate), R.color.warning)
                "advanced" -> Pair(context.getString(R.string.advanced), R.color.error)
                else -> Pair(lesson.difficulty, R.color.gray)
            }
            chipDifficulty.text = difficultyText
            chipDifficulty.chipBackgroundColor = android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(context, difficultyColor)
            )

            // Button text
            btnStart.text = when {
                lesson.isCompleted -> context.getString(R.string.review_lesson)
                lesson.progress > 0 -> context.getString(R.string.continue_lesson)
                else -> context.getString(R.string.start_lesson)
            }

            btnStart.setOnClickListener {
                onLessonClick(lesson)
            }

            itemView.setOnClickListener {
                onLessonClick(lesson)
            }
        }
    }

    class LessonDiffCallback : DiffUtil.ItemCallback<Lesson>() {
        override fun areItemsTheSame(oldItem: Lesson, newItem: Lesson): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Lesson, newItem: Lesson): Boolean {
            return oldItem == newItem
        }
    }
}
