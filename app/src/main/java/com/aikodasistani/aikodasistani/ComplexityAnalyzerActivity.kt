package com.aikodasistani.aikodasistani

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

/**
 * Complexity Analyzer Activity
 * Big O karmaşıklık analizi aracı
 */
class ComplexityAnalyzerActivity : AppCompatActivity() {

    private lateinit var codeInput: EditText
    private lateinit var analyzeButton: Button
    private lateinit var resultContainer: LinearLayout
    private lateinit var timeComplexityText: TextView
    private lateinit var spaceComplexityText: TextView
    private lateinit var explanationText: TextView
    private lateinit var suggestionsText: TextView
    private lateinit var languageChipGroup: ChipGroup
    private lateinit var examplesContainer: LinearLayout

    private var selectedLanguage = "kotlin"

    // Big O Complexity patterns
    private val complexityPatterns = listOf(
        ComplexityPattern(
            name = "O(1) - Sabit Zaman",
            patterns = listOf("return", "if (", "arr[", "map.get", "set.contains"),
            description = "İşlem sayısı girdiden bağımsız, her zaman sabit",
            color = "#4CAF50"
        ),
        ComplexityPattern(
            name = "O(log n) - Logaritmik",
            patterns = listOf("binary", "/ 2", ">> 1", "binarySearch", "bisect"),
            description = "Her adımda problem yarıya indirilir (binary search gibi)",
            color = "#8BC34A"
        ),
        ComplexityPattern(
            name = "O(n) - Doğrusal",
            patterns = listOf("for (", "while (", "forEach", ".map(", ".filter(", "for i in"),
            description = "Her eleman bir kez ziyaret edilir",
            color = "#FFEB3B"
        ),
        ComplexityPattern(
            name = "O(n log n) - Doğrusal Logaritmik",
            patterns = listOf("sort(", ".sorted(", "mergeSort", "quickSort", "heapSort"),
            description = "Verimli sıralama algoritmaları",
            color = "#FFC107"
        ),
        ComplexityPattern(
            name = "O(n²) - Karesel",
            patterns = listOf("for.*for", "nested loop", "bubble", "selection", "insertion"),
            description = "İç içe iki döngü (her eleman için tüm elemanlar)",
            color = "#FF9800"
        ),
        ComplexityPattern(
            name = "O(n³) - Kübik",
            patterns = listOf("for.*for.*for", "triple nested"),
            description = "İç içe üç döngü",
            color = "#FF5722"
        ),
        ComplexityPattern(
            name = "O(2ⁿ) - Üstel",
            patterns = listOf("fibonacci", "subset", "powerset", "recursion.*2"),
            description = "Her adımda iki alt problem oluşur (örn: naive Fibonacci)",
            color = "#F44336"
        ),
        ComplexityPattern(
            name = "O(n!) - Faktöriyel",
            patterns = listOf("permutation", "factorial", "travelling salesman"),
            description = "Tüm permütasyonlar denenir",
            color = "#9C27B0"
        )
    )

    // Example algorithms
    private val exampleAlgorithms = listOf(
        AlgorithmExample(
            name = "Linear Search",
            timeComplexity = "O(n)",
            spaceComplexity = "O(1)",
            code = """fun linearSearch(arr: IntArray, target: Int): Int {
    for (i in arr.indices) {
        if (arr[i] == target) return i
    }
    return -1
}"""
        ),
        AlgorithmExample(
            name = "Binary Search",
            timeComplexity = "O(log n)",
            spaceComplexity = "O(1)",
            code = """fun binarySearch(arr: IntArray, target: Int): Int {
    var left = 0
    var right = arr.size - 1
    while (left <= right) {
        val mid = left + (right - left) / 2
        when {
            arr[mid] == target -> return mid
            arr[mid] < target -> left = mid + 1
            else -> right = mid - 1
        }
    }
    return -1
}"""
        ),
        AlgorithmExample(
            name = "Bubble Sort",
            timeComplexity = "O(n²)",
            spaceComplexity = "O(1)",
            code = """fun bubbleSort(arr: IntArray) {
    for (i in arr.indices) {
        for (j in 0 until arr.size - i - 1) {
            if (arr[j] > arr[j + 1]) {
                val temp = arr[j]
                arr[j] = arr[j + 1]
                arr[j + 1] = temp
            }
        }
    }
}"""
        ),
        AlgorithmExample(
            name = "Merge Sort",
            timeComplexity = "O(n log n)",
            spaceComplexity = "O(n)",
            code = """fun mergeSort(arr: IntArray): IntArray {
    if (arr.size <= 1) return arr
    val mid = arr.size / 2
    val left = mergeSort(arr.sliceArray(0 until mid))
    val right = mergeSort(arr.sliceArray(mid until arr.size))
    return merge(left, right)
}"""
        ),
        AlgorithmExample(
            name = "Quick Sort",
            timeComplexity = "O(n log n)",
            spaceComplexity = "O(log n)",
            code = """fun quickSort(arr: IntArray, low: Int, high: Int) {
    if (low < high) {
        val pi = partition(arr, low, high)
        quickSort(arr, low, pi - 1)
        quickSort(arr, pi + 1, high)
    }
}"""
        ),
        AlgorithmExample(
            name = "Fibonacci (Recursive)",
            timeComplexity = "O(2ⁿ)",
            spaceComplexity = "O(n)",
            code = """fun fibonacci(n: Int): Int {
    if (n <= 1) return n
    return fibonacci(n - 1) + fibonacci(n - 2)
}"""
        ),
        AlgorithmExample(
            name = "Fibonacci (DP)",
            timeComplexity = "O(n)",
            spaceComplexity = "O(n)",
            code = """fun fibonacciDP(n: Int): Int {
    val dp = IntArray(n + 1)
    dp[0] = 0
    dp[1] = 1
    for (i in 2..n) {
        dp[i] = dp[i-1] + dp[i-2]
    }
    return dp[n]
}"""
        ),
        AlgorithmExample(
            name = "Two Sum",
            timeComplexity = "O(n)",
            spaceComplexity = "O(n)",
            code = """fun twoSum(nums: IntArray, target: Int): IntArray {
    val map = mutableMapOf<Int, Int>()
    for ((i, num) in nums.withIndex()) {
        val complement = target - num
        if (map.containsKey(complement)) {
            return intArrayOf(map[complement]!!, i)
        }
        map[num] = i
    }
    return intArrayOf()
}"""
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_complexity_analyzer)

        setupViews()
        setupLanguageChips()
        setupExamples()
    }

    private fun setupViews() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.complexity_analyzer_title)

        codeInput = findViewById(R.id.codeInput)
        analyzeButton = findViewById(R.id.analyzeButton)
        resultContainer = findViewById(R.id.resultContainer)
        timeComplexityText = findViewById(R.id.timeComplexityText)
        spaceComplexityText = findViewById(R.id.spaceComplexityText)
        explanationText = findViewById(R.id.explanationText)
        suggestionsText = findViewById(R.id.suggestionsText)
        languageChipGroup = findViewById(R.id.languageChipGroup)
        examplesContainer = findViewById(R.id.examplesContainer)

        analyzeButton.setOnClickListener {
            analyzeCode()
        }

        codeInput.addTextChangedListener {
            resultContainer.visibility = android.view.View.GONE
        }
    }

    private fun setupLanguageChips() {
        val languages = listOf("Kotlin", "Java", "Python", "JavaScript", "C++", "Go")
        
        languages.forEach { lang ->
            val chip = Chip(this).apply {
                text = lang
                isCheckable = true
                isChecked = lang.lowercase() == selectedLanguage
                setOnClickListener {
                    selectedLanguage = lang.lowercase()
                }
            }
            languageChipGroup.addView(chip)
        }
    }

    private fun setupExamples() {
        examplesContainer.removeAllViews()
        
        exampleAlgorithms.forEach { example ->
            val exampleView = layoutInflater.inflate(R.layout.item_algorithm_example, examplesContainer, false)
            
            exampleView.findViewById<TextView>(R.id.algorithmName).text = example.name
            exampleView.findViewById<TextView>(R.id.timeComplexity).text = "Zaman: ${example.timeComplexity}"
            exampleView.findViewById<TextView>(R.id.spaceComplexity).text = "Alan: ${example.spaceComplexity}"
            
            exampleView.setOnClickListener {
                codeInput.setText(example.code)
            }
            
            examplesContainer.addView(exampleView)
        }
    }

    private fun analyzeCode() {
        val code = codeInput.text.toString()
        
        if (code.isBlank()) {
            Toast.makeText(this, getString(R.string.enter_code), Toast.LENGTH_SHORT).show()
            return
        }

        // Analyze time complexity
        val timeResult = analyzeTimeComplexity(code)
        val spaceResult = analyzeSpaceComplexity(code)

        // Show results
        resultContainer.visibility = android.view.View.VISIBLE
        
        timeComplexityText.text = "⏱️ Zaman: ${timeResult.complexity}"
        spaceComplexityText.text = "💾 Alan: ${spaceResult.complexity}"
        explanationText.text = "📝 ${timeResult.explanation}"
        
        val suggestions = buildSuggestions(timeResult, spaceResult)
        suggestionsText.text = suggestions
    }

    private fun analyzeTimeComplexity(code: String): ComplexityResult {
        val lowercaseCode = code.lowercase()
        
        // Count nested loops
        val loopPatterns = listOf("for ", "for(", "while ", "while(", "forEach", ".map(", ".filter(")
        var loopCount = 0
        var currentIndex = 0
        
        for (pattern in loopPatterns) {
            var index = lowercaseCode.indexOf(pattern, currentIndex)
            while (index != -1) {
                loopCount++
                index = lowercaseCode.indexOf(pattern, index + 1)
            }
        }

        // Check for recursive patterns
        val hasRecursion = code.contains("return ") && 
            (code.contains("(n - 1)") || code.contains("(n-1)") || 
             code.contains("(n + 1)") || code.contains("(n+1)"))

        // Check for binary search pattern
        val hasBinaryPattern = lowercaseCode.contains("/ 2") || 
            lowercaseCode.contains("/2") || 
            lowercaseCode.contains(">> 1") ||
            lowercaseCode.contains("binarysearch")

        // Check for sorting
        val hasSorting = lowercaseCode.contains("sort(") || 
            lowercaseCode.contains(".sorted(") ||
            lowercaseCode.contains("mergesort") ||
            lowercaseCode.contains("quicksort")

        // Determine complexity based on patterns
        return when {
            lowercaseCode.contains("permutation") || lowercaseCode.contains("factorial") -> {
                ComplexityResult("O(n!)", "Faktöriyel karmaşıklık tespit edildi (tüm permütasyonlar)")
            }
            hasRecursion && code.contains("+ fibonacci") -> {
                ComplexityResult("O(2ⁿ)", "Üstel karmaşıklık (naive recursive pattern)")
            }
            loopCount >= 3 -> {
                ComplexityResult("O(n³)", "Üç iç içe döngü tespit edildi")
            }
            loopCount >= 2 -> {
                ComplexityResult("O(n²)", "İki iç içe döngü tespit edildi")
            }
            hasSorting -> {
                ComplexityResult("O(n log n)", "Sıralama algoritması tespit edildi")
            }
            hasBinaryPattern && loopCount >= 1 -> {
                ComplexityResult("O(log n)", "Binary search pattern tespit edildi")
            }
            loopCount >= 1 -> {
                ComplexityResult("O(n)", "Tek döngü tespit edildi (doğrusal)")
            }
            else -> {
                ComplexityResult("O(1)", "Sabit zaman karmaşıklığı (döngü/recursion yok)")
            }
        }
    }

    private fun analyzeSpaceComplexity(code: String): ComplexityResult {
        val lowercaseCode = code.lowercase()
        
        // Check for array/list creation
        val hasNewArray = lowercaseCode.contains("intarray(") ||
            lowercaseCode.contains("arrayof") ||
            lowercaseCode.contains("mutablelistof") ||
            lowercaseCode.contains("new int[") ||
            lowercaseCode.contains("new array") ||
            lowercaseCode.contains("[]") ||
            lowercaseCode.contains("dp =")

        // Check for HashMap/Set creation
        val hasHashStructure = lowercaseCode.contains("hashmap") ||
            lowercaseCode.contains("hashset") ||
            lowercaseCode.contains("mutablemapof") ||
            lowercaseCode.contains("mutablesetof")

        // Check for recursion (stack space)
        val hasRecursion = code.contains("return ") && 
            (code.contains("(n - 1)") || code.contains("(n-1)"))

        return when {
            hasNewArray && hasHashStructure -> {
                ComplexityResult("O(n)", "Dinamik veri yapıları kullanılıyor (array + hash)")
            }
            hasNewArray -> {
                ComplexityResult("O(n)", "n boyutunda array/list oluşturuluyor")
            }
            hasHashStructure -> {
                ComplexityResult("O(n)", "Hash tablosu kullanılıyor")
            }
            hasRecursion -> {
                ComplexityResult("O(n)", "Recursion için call stack kullanılıyor")
            }
            else -> {
                ComplexityResult("O(1)", "Sabit alan kullanımı (ek veri yapısı yok)")
            }
        }
    }

    private fun buildSuggestions(time: ComplexityResult, space: ComplexityResult): String {
        val suggestions = mutableListOf<String>()
        
        if (time.complexity == "O(n²)") {
            suggestions.add("💡 İç içe döngüleri azaltmak için HashMap kullanmayı düşünün")
            suggestions.add("💡 Sorting + Two Pointers tekniği O(n log n) verebilir")
        }
        
        if (time.complexity == "O(2ⁿ)") {
            suggestions.add("💡 Dynamic Programming ile O(n)'e düşürülebilir")
            suggestions.add("💡 Memoization ekleyerek tekrar hesaplamaları önleyin")
        }
        
        if (space.complexity == "O(n)" && time.complexity == "O(n²)") {
            suggestions.add("💡 Space-Time tradeoff: Ekstra alan kullanarak zaman kazanabilirsiniz")
        }
        
        if (suggestions.isEmpty()) {
            suggestions.add("✅ Algoritma optimize görünüyor")
        }
        
        return suggestions.joinToString("\n")
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    data class ComplexityPattern(
        val name: String,
        val patterns: List<String>,
        val description: String,
        val color: String
    )

    data class ComplexityResult(
        val complexity: String,
        val explanation: String
    )

    data class AlgorithmExample(
        val name: String,
        val timeComplexity: String,
        val spaceComplexity: String,
        val code: String
    )
}
