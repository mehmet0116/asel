package com.aikodasistani.aikodasistani

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aikodasistani.aikodasistani.data.AppDatabase
import com.aikodasistani.aikodasistani.data.CodingChallenge
import com.aikodasistani.aikodasistani.ui.ChallengeAdapter
import com.google.android.material.chip.Chip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Activity for displaying daily coding challenges
 */
class DailyChallengeActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var tvCompletedCount: TextView
    private lateinit var tvTotalCount: TextView
    private lateinit var tvStreakCount: TextView
    private lateinit var btnLoadChallenges: Button
    private lateinit var challengeAdapter: ChallengeAdapter

    private lateinit var chipAll: Chip
    private lateinit var chipEasy: Chip
    private lateinit var chipMedium: Chip
    private lateinit var chipHard: Chip

    private var currentFilter: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_daily_challenge)

        db = AppDatabase.getDatabase(this)

        setupToolbar()
        initializeViews()
        setupRecyclerView()
        setupFilterChips()
        setupLoadButton()

        loadChallenges()
        updateStats()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun initializeViews() {
        recyclerView = findViewById(R.id.recyclerViewChallenges)
        emptyStateLayout = findViewById(R.id.emptyStateLayout)
        tvCompletedCount = findViewById(R.id.tvCompletedCount)
        tvTotalCount = findViewById(R.id.tvTotalCount)
        tvStreakCount = findViewById(R.id.tvStreakCount)
        btnLoadChallenges = findViewById(R.id.btnLoadChallenges)

        chipAll = findViewById(R.id.chipAll)
        chipEasy = findViewById(R.id.chipEasy)
        chipMedium = findViewById(R.id.chipMedium)
        chipHard = findViewById(R.id.chipHard)
    }

    private fun setupRecyclerView() {
        challengeAdapter = ChallengeAdapter { challenge ->
            openChallengeSolver(challenge)
        }

        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@DailyChallengeActivity)
            adapter = challengeAdapter
        }
    }

    private fun setupFilterChips() {
        chipAll.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                currentFilter = null
                loadChallenges()
            }
        }

        chipEasy.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                currentFilter = "easy"
                loadChallenges()
            }
        }

        chipMedium.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                currentFilter = "medium"
                loadChallenges()
            }
        }

        chipHard.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                currentFilter = "hard"
                loadChallenges()
            }
        }
    }

    private fun setupLoadButton() {
        btnLoadChallenges.setOnClickListener {
            loadDefaultChallenges()
        }
    }

    private fun loadChallenges() {
        lifecycleScope.launch {
            val challengesFlow = if (currentFilter != null) {
                db.codingChallengeDao().getChallengesByDifficulty(currentFilter!!)
            } else {
                db.codingChallengeDao().getAllChallenges()
            }

            challengesFlow.collectLatest { challenges ->
                challengeAdapter.submitList(challenges)
                updateEmptyState(challenges.isEmpty())
            }
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        emptyStateLayout.isVisible = isEmpty
        recyclerView.isVisible = !isEmpty
    }

    private fun updateStats() {
        lifecycleScope.launch {
            val total = withContext(Dispatchers.IO) {
                db.codingChallengeDao().getChallengeCount()
            }
            val completed = withContext(Dispatchers.IO) {
                db.codingChallengeDao().getCompletedCount()
            }

            tvTotalCount.text = total.toString()
            tvCompletedCount.text = completed.toString()
            tvStreakCount.text = "🔥 ${calculateStreak()}"
        }
    }

    private suspend fun calculateStreak(): Int {
        // Simple streak calculation based on completed challenges
        return withContext(Dispatchers.IO) {
            db.codingChallengeDao().getCompletedCount()
        }
    }

    private fun loadDefaultChallenges() {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val existingCount = db.codingChallengeDao().getChallengeCount()
                if (existingCount == 0) {
                    val defaultChallenges = CodingChallenge.getDefaultChallenges()
                    db.codingChallengeDao().insertAllChallenges(defaultChallenges)
                }
            }
            Toast.makeText(this@DailyChallengeActivity, "Sorular yüklendi!", Toast.LENGTH_SHORT).show()
            updateStats()
        }
    }

    private fun openChallengeSolver(challenge: CodingChallenge) {
        // Open Code Playground with the challenge
        val intent = Intent(this, CodePlaygroundActivity::class.java).apply {
            putExtra(CodePlaygroundActivity.EXTRA_CODE, challenge.starterCode)
            putExtra(CodePlaygroundActivity.EXTRA_LANGUAGE, challenge.language)
        }
        startActivity(intent)

        // Show challenge info dialog
        showChallengeDialog(challenge)
    }

    private fun showChallengeDialog(challenge: CodingChallenge) {
        val builder = AlertDialog.Builder(this)
            .setTitle(challenge.title)
            .setMessage(challenge.description)
            .setPositiveButton(R.string.solve_challenge) { _, _ ->
                // Already opened in playground
            }

        if (!challenge.hints.isNullOrBlank()) {
            builder.setNeutralButton(R.string.show_hint) { _, _ ->
                showHints(challenge)
            }
        }

        if (challenge.isCompleted && !challenge.solution.isNullOrBlank()) {
            builder.setNegativeButton(R.string.view_solution) { _, _ ->
                showSolution(challenge)
            }
        }

        builder.show()
    }

    private fun showHints(challenge: CodingChallenge) {
        val hints = challenge.hints?.let {
            try {
                // Parse JSON array of hints
                it.replace("[", "").replace("]", "").replace("\"", "")
                    .split(",").mapIndexed { index, hint ->
                        "${index + 1}. ${hint.trim()}"
                    }.joinToString("\n\n")
            } catch (e: Exception) {
                it
            }
        } ?: "İpucu bulunamadı"

        AlertDialog.Builder(this)
            .setTitle("💡 İpuçları")
            .setMessage(hints)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun showSolution(challenge: CodingChallenge) {
        AlertDialog.Builder(this)
            .setTitle("✅ Çözüm")
            .setMessage(challenge.solution)
            .setPositiveButton(android.R.string.ok, null)
            .setNeutralButton(R.string.copy_code) { _, _ ->
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("Solution", challenge.solution)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, R.string.snippet_copied, Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    override fun onResume() {
        super.onResume()
        updateStats()
    }
}
