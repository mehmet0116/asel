package com.aikodasistani.aikodasistani

import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText

class CodeDiffActivity : AppCompatActivity() {

    private lateinit var code1Input: TextInputEditText
    private lateinit var code2Input: TextInputEditText
    private lateinit var compareButton: MaterialButton
    private lateinit var diffOutput: TextView
    private lateinit var statsText: TextView
    private lateinit var viewModeChipGroup: ChipGroup
    private lateinit var sideBySideContainer: LinearLayout
    private lateinit var leftCode: TextView
    private lateinit var rightCode: TextView
    private lateinit var unifiedContainer: ScrollView

    private var currentViewMode = "unified"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_code_diff)

        setupViews()
        setupListeners()
    }

    private fun setupViews() {
        code1Input = findViewById(R.id.code1Input)
        code2Input = findViewById(R.id.code2Input)
        compareButton = findViewById(R.id.compareButton)
        diffOutput = findViewById(R.id.diffOutput)
        statsText = findViewById(R.id.statsText)
        viewModeChipGroup = findViewById(R.id.viewModeChipGroup)
        sideBySideContainer = findViewById(R.id.sideBySideContainer)
        leftCode = findViewById(R.id.leftCode)
        rightCode = findViewById(R.id.rightCode)
        unifiedContainer = findViewById(R.id.unifiedContainer)

        // Toolbar setup
        findViewById<ImageButton>(R.id.backButton).setOnClickListener { finish() }

        // Set example code
        code1Input.setText("""fun greet(name: String) {
    println("Hello, ${'$'}name!")
}

fun main() {
    greet("World")
}""")

        code2Input.setText("""fun greet(name: String, greeting: String = "Hello") {
    println("${'$'}greeting, ${'$'}name!")
}

fun farewell(name: String) {
    println("Goodbye, ${'$'}name!")
}

fun main() {
    greet("World")
    farewell("World")
}""")
    }

    private fun setupListeners() {
        compareButton.setOnClickListener {
            compareCodes()
        }

        viewModeChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val chip = findViewById<Chip>(checkedIds[0])
                currentViewMode = when (chip.id) {
                    R.id.chipUnified -> "unified"
                    R.id.chipSideBySide -> "side_by_side"
                    else -> "unified"
                }
                compareCodes()
            }
        }

        // Swap button
        findViewById<ImageButton>(R.id.swapButton).setOnClickListener {
            val temp = code1Input.text.toString()
            code1Input.setText(code2Input.text.toString())
            code2Input.setText(temp)
            compareCodes()
        }

        // Clear buttons
        findViewById<ImageButton>(R.id.clearCode1).setOnClickListener {
            code1Input.setText("")
        }
        findViewById<ImageButton>(R.id.clearCode2).setOnClickListener {
            code2Input.setText("")
        }
    }

    private fun compareCodes() {
        val code1 = code1Input.text?.toString() ?: ""
        val code2 = code2Input.text?.toString() ?: ""

        if (code1.isEmpty() && code2.isEmpty()) {
            Toast.makeText(this, getString(R.string.diff_codes_required), Toast.LENGTH_SHORT).show()
            return
        }

        val lines1 = code1.lines()
        val lines2 = code2.lines()

        val diff = computeDiff(lines1, lines2)

        // Calculate stats
        val added = diff.count { it.type == DiffType.ADDED }
        val removed = diff.count { it.type == DiffType.REMOVED }
        val unchanged = diff.count { it.type == DiffType.UNCHANGED }
        
        statsText.text = getString(R.string.diff_stats, added, removed, unchanged)

        when (currentViewMode) {
            "unified" -> showUnifiedDiff(diff)
            "side_by_side" -> showSideBySideDiff(lines1, lines2, diff)
        }
    }

    private fun showUnifiedDiff(diff: List<DiffLine>) {
        unifiedContainer.visibility = View.VISIBLE
        sideBySideContainer.visibility = View.GONE

        val builder = SpannableStringBuilder()

        diff.forEachIndexed { index, line ->
            val prefix = when (line.type) {
                DiffType.ADDED -> "+ "
                DiffType.REMOVED -> "- "
                DiffType.UNCHANGED -> "  "
            }

            val lineText = "$prefix${line.content}\n"
            val start = builder.length
            builder.append(lineText)

            val bgColor = when (line.type) {
                DiffType.ADDED -> Color.parseColor("#1A4CAF50")
                DiffType.REMOVED -> Color.parseColor("#1AF44336")
                DiffType.UNCHANGED -> Color.TRANSPARENT
            }

            val fgColor = when (line.type) {
                DiffType.ADDED -> Color.parseColor("#4CAF50")
                DiffType.REMOVED -> Color.parseColor("#F44336")
                DiffType.UNCHANGED -> Color.parseColor("#CCCCCC")
            }

            if (bgColor != Color.TRANSPARENT) {
                builder.setSpan(
                    BackgroundColorSpan(bgColor),
                    start,
                    builder.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            builder.setSpan(
                ForegroundColorSpan(fgColor),
                start,
                builder.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        diffOutput.text = builder
    }

    private fun showSideBySideDiff(lines1: List<String>, lines2: List<String>, diff: List<DiffLine>) {
        unifiedContainer.visibility = View.GONE
        sideBySideContainer.visibility = View.VISIBLE

        val leftBuilder = SpannableStringBuilder()
        val rightBuilder = SpannableStringBuilder()

        var leftIndex = 0
        var rightIndex = 0

        diff.forEach { line ->
            when (line.type) {
                DiffType.REMOVED -> {
                    val leftLine = if (leftIndex < lines1.size) lines1[leftIndex++] else ""
                    appendColoredLine(leftBuilder, leftLine, Color.parseColor("#1AF44336"), Color.parseColor("#F44336"))
                    appendColoredLine(rightBuilder, "", Color.TRANSPARENT, Color.parseColor("#666666"))
                }
                DiffType.ADDED -> {
                    appendColoredLine(leftBuilder, "", Color.TRANSPARENT, Color.parseColor("#666666"))
                    val rightLine = if (rightIndex < lines2.size) lines2[rightIndex++] else ""
                    appendColoredLine(rightBuilder, rightLine, Color.parseColor("#1A4CAF50"), Color.parseColor("#4CAF50"))
                }
                DiffType.UNCHANGED -> {
                    val leftLine = if (leftIndex < lines1.size) lines1[leftIndex++] else ""
                    val rightLine = if (rightIndex < lines2.size) lines2[rightIndex++] else ""
                    appendColoredLine(leftBuilder, leftLine, Color.TRANSPARENT, Color.parseColor("#CCCCCC"))
                    appendColoredLine(rightBuilder, rightLine, Color.TRANSPARENT, Color.parseColor("#CCCCCC"))
                }
            }
        }

        leftCode.text = leftBuilder
        rightCode.text = rightBuilder
    }

    private fun appendColoredLine(builder: SpannableStringBuilder, text: String, bgColor: Int, fgColor: Int) {
        val start = builder.length
        builder.append("$text\n")
        
        if (bgColor != Color.TRANSPARENT) {
            builder.setSpan(
                BackgroundColorSpan(bgColor),
                start,
                builder.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        builder.setSpan(
            ForegroundColorSpan(fgColor),
            start,
            builder.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    private fun computeDiff(lines1: List<String>, lines2: List<String>): List<DiffLine> {
        val result = mutableListOf<DiffLine>()
        
        // Simple LCS-based diff algorithm
        val lcs = computeLCS(lines1, lines2)
        
        var i = 0
        var j = 0
        var k = 0
        
        while (i < lines1.size || j < lines2.size) {
            if (k < lcs.size && i < lines1.size && lines1[i] == lcs[k]) {
                if (j < lines2.size && lines2[j] == lcs[k]) {
                    result.add(DiffLine(DiffType.UNCHANGED, lcs[k]))
                    i++
                    j++
                    k++
                } else if (j < lines2.size) {
                    result.add(DiffLine(DiffType.ADDED, lines2[j]))
                    j++
                }
            } else if (i < lines1.size && (k >= lcs.size || lines1[i] != lcs[k])) {
                result.add(DiffLine(DiffType.REMOVED, lines1[i]))
                i++
            } else if (j < lines2.size && (k >= lcs.size || lines2[j] != lcs[k])) {
                result.add(DiffLine(DiffType.ADDED, lines2[j]))
                j++
            } else {
                break
            }
        }
        
        return result
    }

    private fun computeLCS(lines1: List<String>, lines2: List<String>): List<String> {
        val m = lines1.size
        val n = lines2.size
        val dp = Array(m + 1) { IntArray(n + 1) }
        
        for (i in 1..m) {
            for (j in 1..n) {
                if (lines1[i - 1] == lines2[j - 1]) {
                    dp[i][j] = dp[i - 1][j - 1] + 1
                } else {
                    dp[i][j] = maxOf(dp[i - 1][j], dp[i][j - 1])
                }
            }
        }
        
        // Backtrack to find LCS
        val lcs = mutableListOf<String>()
        var i = m
        var j = n
        while (i > 0 && j > 0) {
            when {
                lines1[i - 1] == lines2[j - 1] -> {
                    lcs.add(0, lines1[i - 1])
                    i--
                    j--
                }
                dp[i - 1][j] > dp[i][j - 1] -> i--
                else -> j--
            }
        }
        
        return lcs
    }

    enum class DiffType {
        ADDED, REMOVED, UNCHANGED
    }

    data class DiffLine(val type: DiffType, val content: String)
}
