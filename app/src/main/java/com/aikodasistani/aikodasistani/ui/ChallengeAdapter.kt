package com.aikodasistani.aikodasistani.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aikodasistani.aikodasistani.R
import com.aikodasistani.aikodasistani.data.CodingChallenge
import com.google.android.material.chip.Chip

/**
 * RecyclerView Adapter for displaying coding challenges
 */
class ChallengeAdapter(
    private val onSolveClick: (CodingChallenge) -> Unit
) : ListAdapter<CodingChallenge, ChallengeAdapter.ChallengeViewHolder>(ChallengeDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChallengeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_challenge, parent, false)
        return ChallengeViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChallengeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ChallengeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivCompleted: ImageView = itemView.findViewById(R.id.ivCompleted)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        private val tvDifficulty: TextView = itemView.findViewById(R.id.tvDifficulty)
        private val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        private val chipCategory: Chip = itemView.findViewById(R.id.chipCategory)
        private val chipLanguage: Chip = itemView.findViewById(R.id.chipLanguage)
        private val btnSolve: Button = itemView.findViewById(R.id.btnSolve)

        fun bind(challenge: CodingChallenge) {
            val context = itemView.context

            // Title
            tvTitle.text = challenge.title

            // Completed icon
            ivCompleted.isVisible = challenge.isCompleted

            // Description
            tvDescription.text = challenge.description

            // Difficulty badge
            val (difficultyText, difficultyColor) = when (challenge.difficulty.lowercase()) {
                "easy" -> Pair(context.getString(R.string.difficulty_easy), R.color.success)
                "medium" -> Pair(context.getString(R.string.difficulty_medium), R.color.warning)
                "hard" -> Pair(context.getString(R.string.difficulty_hard), R.color.error)
                else -> Pair(challenge.difficulty, R.color.gray)
            }
            tvDifficulty.text = difficultyText
            tvDifficulty.setBackgroundColor(ContextCompat.getColor(context, difficultyColor))

            // Category chip
            val categoryEmoji = when (challenge.category.lowercase()) {
                "array" -> "📊"
                "string" -> "📝"
                "algorithm" -> "⚙️"
                "math" -> "🔢"
                "data-structure" -> "🏗️"
                else -> "📚"
            }
            chipCategory.text = "$categoryEmoji ${challenge.category.replaceFirstChar { it.uppercase() }}"

            // Language chip
            chipLanguage.text = challenge.language.replaceFirstChar { it.uppercase() }

            // Solve button
            btnSolve.text = if (challenge.isCompleted) {
                context.getString(R.string.view_solution)
            } else {
                context.getString(R.string.solve_challenge)
            }

            btnSolve.setOnClickListener {
                onSolveClick(challenge)
            }

            itemView.setOnClickListener {
                onSolveClick(challenge)
            }
        }
    }

    class ChallengeDiffCallback : DiffUtil.ItemCallback<CodingChallenge>() {
        override fun areItemsTheSame(oldItem: CodingChallenge, newItem: CodingChallenge): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CodingChallenge, newItem: CodingChallenge): Boolean {
            return oldItem == newItem
        }
    }
}
