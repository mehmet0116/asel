package com.aikodasistani.aikodasistani

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.progressindicator.LinearProgressIndicator

/**
 * Code Quiz Activity
 * İnteraktif programlama quizleri
 */
class CodeQuizActivity : AppCompatActivity() {

    private lateinit var statsContainer: LinearLayout
    private lateinit var correctText: TextView
    private lateinit var totalText: TextView
    private lateinit var streakText: TextView
    private lateinit var categoryChipGroup: ChipGroup
    private lateinit var quizRecyclerView: RecyclerView
    private lateinit var progressIndicator: LinearProgressIndicator

    private var correctAnswers = 0
    private var totalAnswered = 0
    private var currentStreak = 0
    private var selectedCategory = "all"

    private val quizQuestions = listOf(
        // Kotlin Basics
        QuizQuestion(
            id = 1,
            category = "kotlin",
            difficulty = "easy",
            question = "Kotlin'de değişmez (immutable) değişken nasıl tanımlanır?",
            codeSnippet = null,
            options = listOf("var", "val", "const", "let"),
            correctAnswer = 1,
            explanation = "Kotlin'de 'val' keyword'ü değişmez (immutable) değişkenler için kullanılır. 'var' ise değiştirilebilir değişkenler içindir."
        ),
        QuizQuestion(
            id = 2,
            category = "kotlin",
            difficulty = "easy",
            question = "Bu kod ne çıktı verir?",
            codeSnippet = """val list = listOf(1, 2, 3, 4, 5)
println(list.filter { it > 2 }.size)""",
            options = listOf("2", "3", "4", "5"),
            correctAnswer = 1,
            explanation = "filter { it > 2 } → [3, 4, 5] döner, bu listenin boyutu 3'tür."
        ),
        QuizQuestion(
            id = 3,
            category = "kotlin",
            difficulty = "medium",
            question = "Null safety için hangi operatör kullanılır?",
            codeSnippet = "val name: String? = null\nval length = name___ .length",
            options = listOf("!", "?.", "!!", "??"),
            correctAnswer = 1,
            explanation = "'?.' safe call operatörü, null ise null döner, exception fırlatmaz."
        ),
        QuizQuestion(
            id = 4,
            category = "kotlin",
            difficulty = "medium",
            question = "Bu kodun çıktısı nedir?",
            codeSnippet = """val result = (1..5).fold(0) { acc, i -> acc + i }
println(result)""",
            options = listOf("5", "10", "15", "20"),
            correctAnswer = 2,
            explanation = "fold(0) başlangıç değeri 0 ile: 0+1+2+3+4+5 = 15"
        ),
        QuizQuestion(
            id = 5,
            category = "kotlin",
            difficulty = "hard",
            question = "Hangi scope function 'this' ile receiver olarak çalışır ve aynı nesneyi döner?",
            codeSnippet = null,
            options = listOf("let", "run", "apply", "also"),
            correctAnswer = 2,
            explanation = "'apply' this kullanır ve aynı nesneyi döner. 'also' it kullanır ve nesneyi döner."
        ),

        // Algorithms
        QuizQuestion(
            id = 6,
            category = "algorithms",
            difficulty = "easy",
            question = "Binary Search'ün zaman karmaşıklığı nedir?",
            codeSnippet = null,
            options = listOf("O(n)", "O(log n)", "O(n²)", "O(1)"),
            correctAnswer = 1,
            explanation = "Binary search her adımda arama alanını yarıya indirir → O(log n)"
        ),
        QuizQuestion(
            id = 7,
            category = "algorithms",
            difficulty = "medium",
            question = "Bu fonksiyonun çıktısı nedir?",
            codeSnippet = """fun mystery(n: Int): Int {
    if (n <= 1) return n
    return mystery(n-1) + mystery(n-2)
}
println(mystery(6))""",
            options = listOf("6", "8", "10", "13"),
            correctAnswer = 1,
            explanation = "Bu Fibonacci fonksiyonu. F(6) = F(5) + F(4) = 5 + 3 = 8"
        ),
        QuizQuestion(
            id = 8,
            category = "algorithms",
            difficulty = "medium",
            question = "Quick Sort'un ortalama zaman karmaşıklığı nedir?",
            codeSnippet = null,
            options = listOf("O(n)", "O(n log n)", "O(n²)", "O(log n)"),
            correctAnswer = 1,
            explanation = "Quick Sort ortalamada O(n log n), en kötü durumda O(n²)"
        ),
        QuizQuestion(
            id = 9,
            category = "algorithms",
            difficulty = "hard",
            question = "Bu algoritma hangi problemi çözer?",
            codeSnippet = """fun solve(nums: IntArray, target: Int): IntArray {
    val map = mutableMapOf<Int, Int>()
    for ((i, num) in nums.withIndex()) {
        val complement = target - num
        if (map.containsKey(complement)) {
            return intArrayOf(map[complement]!!, i)
        }
        map[num] = i
    }
    return intArrayOf()
}""",
            options = listOf("Maximum Subarray", "Two Sum", "Three Sum", "Binary Search"),
            correctAnswer = 1,
            explanation = "Bu Two Sum problemi çözümü - HashMap ile O(n) karmaşıklık"
        ),

        // Data Structures
        QuizQuestion(
            id = 10,
            category = "data_structures",
            difficulty = "easy",
            question = "Stack veri yapısı hangi prensiple çalışır?",
            codeSnippet = null,
            options = listOf("FIFO", "LIFO", "Random", "Priority"),
            correctAnswer = 1,
            explanation = "Stack LIFO (Last In First Out) prensibiyle çalışır - son giren ilk çıkar"
        ),
        QuizQuestion(
            id = 11,
            category = "data_structures",
            difficulty = "easy",
            question = "HashMap'te arama işleminin ortalama zaman karmaşıklığı nedir?",
            codeSnippet = null,
            options = listOf("O(n)", "O(log n)", "O(1)", "O(n²)"),
            correctAnswer = 2,
            explanation = "HashMap hash fonksiyonu sayesinde ortalama O(1) erişim sağlar"
        ),
        QuizQuestion(
            id = 12,
            category = "data_structures",
            difficulty = "medium",
            question = "Binary Tree'nin maksimum derinliği nasıl hesaplanır?",
            codeSnippet = """fun maxDepth(root: TreeNode?): Int {
    if (root == null) return 0
    return ???
}""",
            options = listOf(
                "maxDepth(root.left)", 
                "1 + maxDepth(root.left) + maxDepth(root.right)",
                "1 + maxOf(maxDepth(root.left), maxDepth(root.right))",
                "maxOf(maxDepth(root.left), maxDepth(root.right))"
            ),
            correctAnswer = 2,
            explanation = "Derinlik = 1 (current node) + max(sol alt ağaç, sağ alt ağaç derinlikleri)"
        ),
        QuizQuestion(
            id = 13,
            category = "data_structures",
            difficulty = "hard",
            question = "LRU Cache için en uygun veri yapısı kombinasyonu hangisidir?",
            codeSnippet = null,
            options = listOf(
                "Array + Stack",
                "HashMap + Doubly Linked List",
                "Binary Tree + Queue",
                "HashSet + Array"
            ),
            correctAnswer = 1,
            explanation = "LRU Cache için HashMap (O(1) erişim) + Doubly Linked List (O(1) insert/delete) kullanılır"
        ),

        // OOP
        QuizQuestion(
            id = 14,
            category = "oop",
            difficulty = "easy",
            question = "Kotlin'de hangi keyword sınıftan miras almayı sağlar?",
            codeSnippet = null,
            options = listOf("extends", "implements", ":", "inherits"),
            correctAnswer = 2,
            explanation = "Kotlin'de ':' operatörü hem miras hem de interface implement için kullanılır"
        ),
        QuizQuestion(
            id = 15,
            category = "oop",
            difficulty = "medium",
            question = "Bu kodda hangi OOP prensibi uygulanmaktadır?",
            codeSnippet = """interface Shape {
    fun area(): Double
}
class Circle(val radius: Double) : Shape {
    override fun area() = 3.14 * radius * radius
}
class Rectangle(val width: Double, val height: Double) : Shape {
    override fun area() = width * height
}""",
            options = listOf("Encapsulation", "Inheritance", "Polymorphism", "Abstraction"),
            correctAnswer = 2,
            explanation = "Aynı interface'i farklı şekillerde implement etmek Polymorphism (çok biçimlilik)"
        ),

        // Android
        QuizQuestion(
            id = 16,
            category = "android",
            difficulty = "easy",
            question = "Android'de Activity lifecycle'ın ilk çağrılan metodu hangisidir?",
            codeSnippet = null,
            options = listOf("onStart()", "onResume()", "onCreate()", "onRestart()"),
            correctAnswer = 2,
            explanation = "onCreate() Activity oluşturulduğunda ilk çağrılan lifecycle metodudur"
        ),
        QuizQuestion(
            id = 17,
            category = "android",
            difficulty = "medium",
            question = "RecyclerView'da görünür öğe sayısını optimize etmek için hangi pattern kullanılır?",
            codeSnippet = null,
            options = listOf("Singleton", "ViewHolder", "Observer", "Factory"),
            correctAnswer = 1,
            explanation = "ViewHolder pattern view'ları yeniden kullanarak performansı artırır"
        ),
        QuizQuestion(
            id = 18,
            category = "android",
            difficulty = "hard",
            question = "Coroutine'de hangi dispatcher UI thread'de çalışır?",
            codeSnippet = null,
            options = listOf("Dispatchers.IO", "Dispatchers.Default", "Dispatchers.Main", "Dispatchers.Unconfined"),
            correctAnswer = 2,
            explanation = "Dispatchers.Main Android'de main/UI thread üzerinde çalışır"
        ),

        // SQL
        QuizQuestion(
            id = 19,
            category = "sql",
            difficulty = "easy",
            question = "Tüm kayıtları seçmek için hangi SQL komutu kullanılır?",
            codeSnippet = null,
            options = listOf("GET * FROM table", "SELECT * FROM table", "FETCH * FROM table", "READ * FROM table"),
            correctAnswer = 1,
            explanation = "SELECT * FROM table_name tüm sütunları ve satırları seçer"
        ),
        QuizQuestion(
            id = 20,
            category = "sql",
            difficulty = "medium",
            question = "Bu SQL sorgusunun sonucu nedir?",
            codeSnippet = """SELECT COUNT(*) FROM users WHERE age > 18 AND status = 'active'""",
            options = listOf("Tüm kullanıcı sayısı", "18 yaş üstü kullanıcı sayısı", "Aktif ve 18 yaş üstü kullanıcı sayısı", "Aktif kullanıcı sayısı"),
            correctAnswer = 2,
            explanation = "WHERE koşulu hem yaş > 18 hem de status = 'active' olan kayıtları sayar"
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_code_quiz)

        setupViews()
        setupCategoryChips()
        loadQuestions()
    }

    private fun setupViews() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.code_quiz_title)

        statsContainer = findViewById(R.id.statsContainer)
        correctText = findViewById(R.id.correctText)
        totalText = findViewById(R.id.totalText)
        streakText = findViewById(R.id.streakText)
        categoryChipGroup = findViewById(R.id.categoryChipGroup)
        quizRecyclerView = findViewById(R.id.quizRecyclerView)
        progressIndicator = findViewById(R.id.progressIndicator)

        quizRecyclerView.layoutManager = LinearLayoutManager(this)
        updateStats()
    }

    private fun setupCategoryChips() {
        val categories = listOf(
            "all" to "Tümü",
            "kotlin" to "Kotlin",
            "algorithms" to "Algoritmalar",
            "data_structures" to "Veri Yapıları",
            "oop" to "OOP",
            "android" to "Android",
            "sql" to "SQL"
        )

        categories.forEach { (id, name) ->
            val chip = Chip(this).apply {
                text = name
                isCheckable = true
                isChecked = id == selectedCategory
                setOnClickListener {
                    selectedCategory = id
                    loadQuestions()
                }
            }
            categoryChipGroup.addView(chip)
        }
    }

    private fun loadQuestions() {
        val filteredQuestions = if (selectedCategory == "all") {
            quizQuestions
        } else {
            quizQuestions.filter { it.category == selectedCategory }
        }

        quizRecyclerView.adapter = QuizAdapter(filteredQuestions) { question, selectedIndex ->
            checkAnswer(question, selectedIndex)
        }

        progressIndicator.max = filteredQuestions.size
        progressIndicator.progress = 0
    }

    private fun checkAnswer(question: QuizQuestion, selectedIndex: Int) {
        totalAnswered++
        
        val isCorrect = selectedIndex == question.correctAnswer
        
        if (isCorrect) {
            correctAnswers++
            currentStreak++
        } else {
            currentStreak = 0
        }

        updateStats()
        progressIndicator.progress = totalAnswered

        // Show explanation dialog
        showExplanationDialog(question, selectedIndex, isCorrect)
    }

    private fun showExplanationDialog(question: QuizQuestion, selectedIndex: Int, isCorrect: Boolean) {
        val title = if (isCorrect) "✅ Doğru!" else "❌ Yanlış"
        val selectedOption = question.options[selectedIndex]
        val correctOption = question.options[question.correctAnswer]

        val message = buildString {
            if (!isCorrect) {
                append("Senin cevabın: $selectedOption\n")
                append("Doğru cevap: $correctOption\n\n")
            }
            append("📝 Açıklama:\n")
            append(question.explanation)
        }

        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Tamam", null)
            .show()
    }

    private fun updateStats() {
        correctText.text = correctAnswers.toString()
        totalText.text = totalAnswered.toString()
        streakText.text = currentStreak.toString()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    data class QuizQuestion(
        val id: Int,
        val category: String,
        val difficulty: String,
        val question: String,
        val codeSnippet: String?,
        val options: List<String>,
        val correctAnswer: Int,
        val explanation: String
    )

    inner class QuizAdapter(
        private val questions: List<QuizQuestion>,
        private val onAnswerSelected: (QuizQuestion, Int) -> Unit
    ) : RecyclerView.Adapter<QuizAdapter.QuizViewHolder>() {

        private val answeredQuestions = mutableSetOf<Int>()

        inner class QuizViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val questionNumber: TextView = view.findViewById(R.id.questionNumber)
            val difficultyChip: Chip = view.findViewById(R.id.difficultyChip)
            val questionText: TextView = view.findViewById(R.id.questionText)
            val codeSnippet: TextView = view.findViewById(R.id.codeSnippet)
            val optionsContainer: RadioGroup = view.findViewById(R.id.optionsContainer)
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): QuizViewHolder {
            val view = layoutInflater.inflate(R.layout.item_quiz_question, parent, false)
            return QuizViewHolder(view)
        }

        override fun onBindViewHolder(holder: QuizViewHolder, position: Int) {
            val question = questions[position]
            
            holder.questionNumber.text = "Soru ${position + 1}"
            holder.questionText.text = question.question

            // Set difficulty chip
            when (question.difficulty) {
                "easy" -> {
                    holder.difficultyChip.text = "Kolay"
                    holder.difficultyChip.setChipBackgroundColorResource(android.R.color.holo_green_light)
                }
                "medium" -> {
                    holder.difficultyChip.text = "Orta"
                    holder.difficultyChip.setChipBackgroundColorResource(android.R.color.holo_orange_light)
                }
                "hard" -> {
                    holder.difficultyChip.text = "Zor"
                    holder.difficultyChip.setChipBackgroundColorResource(android.R.color.holo_red_light)
                }
            }

            // Show code snippet if exists
            if (question.codeSnippet != null) {
                holder.codeSnippet.visibility = View.VISIBLE
                holder.codeSnippet.text = question.codeSnippet
            } else {
                holder.codeSnippet.visibility = View.GONE
            }

            // Clear and add options
            holder.optionsContainer.removeAllViews()
            question.options.forEachIndexed { index, option ->
                val radioButton = RadioButton(this@CodeQuizActivity).apply {
                    text = option
                    id = index
                    isEnabled = question.id !in answeredQuestions
                }
                holder.optionsContainer.addView(radioButton)
            }

            holder.optionsContainer.setOnCheckedChangeListener { _, checkedId ->
                if (checkedId != -1 && question.id !in answeredQuestions) {
                    answeredQuestions.add(question.id)
                    onAnswerSelected(question, checkedId)
                    
                    // Disable all radio buttons after answering
                    for (i in 0 until holder.optionsContainer.childCount) {
                        holder.optionsContainer.getChildAt(i).isEnabled = false
                    }
                }
            }
        }

        override fun getItemCount() = questions.size
    }
}
