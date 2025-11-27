package com.aikodasistani.aikodasistani

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
import com.aikodasistani.aikodasistani.data.Lesson
import com.aikodasistani.aikodasistani.ui.LessonAdapter
import com.google.android.material.chip.Chip
import io.noties.markwon.Markwon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Activity for displaying programming lessons and tutorials
 */
class LearningHubActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var tvCompletedCount: TextView
    private lateinit var tvTotalMinutes: TextView
    private lateinit var tvTotalCount: TextView
    private lateinit var btnLoadLessons: Button
    private lateinit var lessonAdapter: LessonAdapter
    private lateinit var markwon: Markwon

    private lateinit var chipAll: Chip
    private lateinit var chipKotlin: Chip
    private lateinit var chipAlgorithms: Chip

    private var currentFilter: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_learning_hub)

        db = AppDatabase.getDatabase(this)
        markwon = Markwon.create(this)

        setupToolbar()
        initializeViews()
        setupRecyclerView()
        setupFilterChips()
        setupLoadButton()

        loadLessons()
        updateStats()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun initializeViews() {
        recyclerView = findViewById(R.id.recyclerViewLessons)
        emptyStateLayout = findViewById(R.id.emptyStateLayout)
        tvCompletedCount = findViewById(R.id.tvCompletedCount)
        tvTotalMinutes = findViewById(R.id.tvTotalMinutes)
        tvTotalCount = findViewById(R.id.tvTotalCount)
        btnLoadLessons = findViewById(R.id.btnLoadLessons)

        chipAll = findViewById(R.id.chipAll)
        chipKotlin = findViewById(R.id.chipKotlin)
        chipAlgorithms = findViewById(R.id.chipAlgorithms)
    }

    private fun setupRecyclerView() {
        lessonAdapter = LessonAdapter { lesson ->
            openLesson(lesson)
        }

        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@LearningHubActivity)
            adapter = lessonAdapter
        }
    }

    private fun setupFilterChips() {
        chipAll.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                currentFilter = null
                loadLessons()
            }
        }

        chipKotlin.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                currentFilter = "kotlin"
                loadLessons()
            }
        }

        chipAlgorithms.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                currentFilter = "algorithms"
                loadLessons()
            }
        }
    }

    private fun setupLoadButton() {
        btnLoadLessons.setOnClickListener {
            loadDefaultLessons()
        }
    }

    private fun loadLessons() {
        lifecycleScope.launch {
            val lessonsFlow = if (currentFilter != null) {
                db.lessonDao().getLessonsByCategory(currentFilter!!)
            } else {
                db.lessonDao().getAllLessons()
            }

            lessonsFlow.collectLatest { lessons ->
                lessonAdapter.submitList(lessons)
                updateEmptyState(lessons.isEmpty())
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
                db.lessonDao().getLessonCount()
            }
            val completed = withContext(Dispatchers.IO) {
                db.lessonDao().getCompletedCount()
            }
            val minutes = withContext(Dispatchers.IO) {
                db.lessonDao().getTotalLearningMinutes() ?: 0
            }

            tvTotalCount.text = total.toString()
            tvCompletedCount.text = completed.toString()
            tvTotalMinutes.text = minutes.toString()
        }
    }

    private fun loadDefaultLessons() {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val existingCount = db.lessonDao().getLessonCount()
                if (existingCount == 0) {
                    val defaultLessons = Lesson.getDefaultLessons()
                    db.lessonDao().insertAllLessons(defaultLessons)
                }
            }
            Toast.makeText(this@LearningHubActivity, "Dersler yüklendi!", Toast.LENGTH_SHORT).show()
            updateStats()
        }
    }

    private fun openLesson(lesson: Lesson) {
        // Show lesson content in a dialog
        val dialogView = layoutInflater.inflate(R.layout.dialog_lesson_content, null)
        val tvLessonTitle = dialogView.findViewById<TextView>(R.id.tvLessonTitle)
        val tvLessonContent = dialogView.findViewById<TextView>(R.id.tvLessonContent)
        val btnMarkComplete = dialogView.findViewById<Button>(R.id.btnMarkComplete)

        tvLessonTitle.text = lesson.title
        markwon.setMarkdown(tvLessonContent, lesson.content)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        btnMarkComplete.text = if (lesson.isCompleted) {
            getString(R.string.lesson_completed)
        } else {
            getString(R.string.submit_solution)
        }

        btnMarkComplete.setOnClickListener {
            if (!lesson.isCompleted) {
                markLessonComplete(lesson)
            }
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun markLessonComplete(lesson: Lesson) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                db.lessonDao().markAsCompleted(lesson.id)
            }
            Toast.makeText(this@LearningHubActivity, R.string.lesson_completed, Toast.LENGTH_SHORT).show()
            updateStats()
        }
    }

    override fun onResume() {
        super.onResume()
        updateStats()
    }
}
