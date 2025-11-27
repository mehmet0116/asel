package com.aikodasistani.aikodasistani

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class InterviewPrepActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var chipGroupCategories: ChipGroup
    private lateinit var chipGroupDifficulties: ChipGroup
    private lateinit var tvStats: TextView
    
    private val allQuestions = mutableListOf<InterviewQuestion>()
    private val filteredQuestions = mutableListOf<InterviewQuestion>()
    private lateinit var adapter: QuestionAdapter
    
    private var selectedCategory = "all"
    private var selectedDifficulty = "all"
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_interview_prep)
        
        supportActionBar?.apply {
            title = getString(R.string.interview_prep_title)
            setDisplayHomeAsUpEnabled(true)
        }
        
        initializeViews()
        loadQuestions()
        setupAdapter()
        setupCategoryChips()
        setupDifficultyChips()
        updateStats()
    }
    
    private fun initializeViews() {
        recyclerView = findViewById(R.id.recyclerViewQuestions)
        chipGroupCategories = findViewById(R.id.chipGroupCategories)
        chipGroupDifficulties = findViewById(R.id.chipGroupDifficulties)
        tvStats = findViewById(R.id.tvStats)
    }
    
    private fun loadQuestions() {
        allQuestions.clear()
        
        // Data Structures
        allQuestions.add(InterviewQuestion(
            "Array Reverse",
            "Bir diziyi ters çeviren fonksiyon yazın.",
            "data_structures",
            "easy",
            listOf("İki pointer kullan", "Baştan ve sondan gel", "Elemanları swap et"),
            "fun reverseArray(arr: IntArray): IntArray {\n    var left = 0\n    var right = arr.lastIndex\n    while (left < right) {\n        val temp = arr[left]\n        arr[left] = arr[right]\n        arr[right] = temp\n        left++\n        right--\n    }\n    return arr\n}",
            "O(n) zaman, O(1) alan"
        ))
        
        allQuestions.add(InterviewQuestion(
            "Two Sum",
            "Bir dizide toplamı hedef sayıya eşit olan iki elemanın indekslerini bulun.",
            "data_structures",
            "easy",
            listOf("HashMap kullan", "Her elemanı kontrol et", "Tamamlayıcıyı ara"),
            "fun twoSum(nums: IntArray, target: Int): IntArray {\n    val map = HashMap<Int, Int>()\n    nums.forEachIndexed { index, num ->\n        val complement = target - num\n        if (map.containsKey(complement)) {\n            return intArrayOf(map[complement]!!, index)\n        }\n        map[num] = index\n    }\n    return intArrayOf()\n}",
            "O(n) zaman, O(n) alan"
        ))
        
        allQuestions.add(InterviewQuestion(
            "Valid Parentheses",
            "Parantezlerin doğru eşleşip eşleşmediğini kontrol edin: (), [], {}",
            "data_structures",
            "easy",
            listOf("Stack kullan", "Açan parantezleri push et", "Kapayan parantezlerde pop et ve kontrol et"),
            "fun isValid(s: String): Boolean {\n    val stack = ArrayDeque<Char>()\n    val pairs = mapOf(')' to '(', ']' to '[', '}' to '{')\n    \n    for (c in s) {\n        when (c) {\n            '(', '[', '{' -> stack.push(c)\n            ')', ']', '}' -> {\n                if (stack.isEmpty() || stack.pop() != pairs[c]) {\n                    return false\n                }\n            }\n        }\n    }\n    return stack.isEmpty()\n}",
            "O(n) zaman, O(n) alan"
        ))
        
        allQuestions.add(InterviewQuestion(
            "Linked List Cycle",
            "Bağlı listede döngü olup olmadığını tespit edin.",
            "data_structures",
            "medium",
            listOf("Floyd's Cycle Detection", "Yavaş ve hızlı pointer kullan", "Eğer kesişirlerse döngü var"),
            "fun hasCycle(head: ListNode?): Boolean {\n    var slow = head\n    var fast = head\n    \n    while (fast?.next != null) {\n        slow = slow?.next\n        fast = fast.next?.next\n        \n        if (slow == fast) return true\n    }\n    return false\n}",
            "O(n) zaman, O(1) alan"
        ))
        
        allQuestions.add(InterviewQuestion(
            "LRU Cache",
            "Least Recently Used (LRU) cache implementasyonu yapın.",
            "data_structures",
            "hard",
            listOf("HashMap + Doubly Linked List", "Get ve Put O(1) olmalı", "En eski kullanılanı çıkar"),
            "class LRUCache(private val capacity: Int) {\n    private val cache = LinkedHashMap<Int, Int>(capacity, 0.75f, true)\n    \n    fun get(key: Int): Int {\n        return cache[key] ?: -1\n    }\n    \n    fun put(key: Int, value: Int) {\n        cache[key] = value\n        if (cache.size > capacity) {\n            cache.remove(cache.keys.first())\n        }\n    }\n}",
            "O(1) get/put"
        ))
        
        // Algorithms
        allQuestions.add(InterviewQuestion(
            "Binary Search",
            "Sıralı dizide bir elemanı binary search ile bulun.",
            "algorithms",
            "easy",
            listOf("Ortadan başla", "Hedef küçükse sola, büyükse sağa git", "Bulana kadar devam et"),
            "fun binarySearch(nums: IntArray, target: Int): Int {\n    var left = 0\n    var right = nums.lastIndex\n    \n    while (left <= right) {\n        val mid = left + (right - left) / 2\n        when {\n            nums[mid] == target -> return mid\n            nums[mid] < target -> left = mid + 1\n            else -> right = mid - 1\n        }\n    }\n    return -1\n}",
            "O(log n) zaman"
        ))
        
        allQuestions.add(InterviewQuestion(
            "Merge Sort",
            "Merge sort algoritmasını implement edin.",
            "algorithms",
            "medium",
            listOf("Böl ve fethet", "Diziyi ikiye böl", "Sıralı şekilde birleştir"),
            "fun mergeSort(arr: IntArray): IntArray {\n    if (arr.size <= 1) return arr\n    \n    val mid = arr.size / 2\n    val left = mergeSort(arr.sliceArray(0 until mid))\n    val right = mergeSort(arr.sliceArray(mid until arr.size))\n    \n    return merge(left, right)\n}\n\nfun merge(left: IntArray, right: IntArray): IntArray {\n    val result = mutableListOf<Int>()\n    var i = 0\n    var j = 0\n    \n    while (i < left.size && j < right.size) {\n        if (left[i] <= right[j]) {\n            result.add(left[i++])\n        } else {\n            result.add(right[j++])\n        }\n    }\n    \n    while (i < left.size) result.add(left[i++])\n    while (j < right.size) result.add(right[j++])\n    \n    return result.toIntArray()\n}",
            "O(n log n) zaman, O(n) alan"
        ))
        
        allQuestions.add(InterviewQuestion(
            "Fibonacci Dynamic Programming",
            "Fibonacci sayısını dynamic programming ile hesaplayın.",
            "algorithms",
            "easy",
            listOf("Memoization veya tabulation", "Önceki sonuçları sakla", "Her değeri bir kez hesapla"),
            "// Tabulation (Bottom-up)\nfun fibonacci(n: Int): Long {\n    if (n <= 1) return n.toLong()\n    \n    var prev2 = 0L\n    var prev1 = 1L\n    var current = 0L\n    \n    for (i in 2..n) {\n        current = prev1 + prev2\n        prev2 = prev1\n        prev1 = current\n    }\n    return current\n}",
            "O(n) zaman, O(1) alan"
        ))
        
        allQuestions.add(InterviewQuestion(
            "Longest Common Subsequence",
            "İki string'in en uzun ortak alt dizisini bulun.",
            "algorithms",
            "hard",
            listOf("2D DP tablosu", "Karakterler eşitse +1", "Eşit değilse max al"),
            "fun lcs(text1: String, text2: String): Int {\n    val m = text1.length\n    val n = text2.length\n    val dp = Array(m + 1) { IntArray(n + 1) }\n    \n    for (i in 1..m) {\n        for (j in 1..n) {\n            if (text1[i - 1] == text2[j - 1]) {\n                dp[i][j] = dp[i - 1][j - 1] + 1\n            } else {\n                dp[i][j] = maxOf(dp[i - 1][j], dp[i][j - 1])\n            }\n        }\n    }\n    return dp[m][n]\n}",
            "O(m*n) zaman ve alan"
        ))
        
        // String
        allQuestions.add(InterviewQuestion(
            "Palindrome Check",
            "Bir string'in palindrome olup olmadığını kontrol edin.",
            "string",
            "easy",
            listOf("İki pointer kullan", "Baştan ve sondan karşılaştır", "Sadece alfanumerik karakterleri dikkate al"),
            "fun isPalindrome(s: String): Boolean {\n    val cleaned = s.lowercase().filter { it.isLetterOrDigit() }\n    return cleaned == cleaned.reversed()\n}",
            "O(n) zaman"
        ))
        
        allQuestions.add(InterviewQuestion(
            "Anagram Check",
            "İki string'in anagram olup olmadığını kontrol edin.",
            "string",
            "easy",
            listOf("Karakter sayılarını karşılaştır", "HashMap veya sorting kullan", "Uzunluklar eşit olmalı"),
            "fun isAnagram(s: String, t: String): Boolean {\n    if (s.length != t.length) return false\n    \n    val count = IntArray(26)\n    for (i in s.indices) {\n        count[s[i] - 'a']++\n        count[t[i] - 'a']--\n    }\n    return count.all { it == 0 }\n}",
            "O(n) zaman, O(1) alan"
        ))
        
        allQuestions.add(InterviewQuestion(
            "Longest Substring Without Repeating",
            "Tekrar eden karakter olmadan en uzun alt string'i bulun.",
            "string",
            "medium",
            listOf("Sliding window", "Set ile tekrarı kontrol et", "Pencereyi genişlet/daralt"),
            "fun lengthOfLongestSubstring(s: String): Int {\n    val seen = HashSet<Char>()\n    var maxLen = 0\n    var left = 0\n    \n    for (right in s.indices) {\n        while (s[right] in seen) {\n            seen.remove(s[left])\n            left++\n        }\n        seen.add(s[right])\n        maxLen = maxOf(maxLen, right - left + 1)\n    }\n    return maxLen\n}",
            "O(n) zaman, O(min(n,m)) alan"
        ))
        
        // Trees
        allQuestions.add(InterviewQuestion(
            "Binary Tree Inorder Traversal",
            "Binary tree'yi inorder olarak traverse edin.",
            "trees",
            "easy",
            listOf("Sol - Kök - Sağ", "Recursive veya iterative", "Stack kullanabilirsiniz"),
            "fun inorderTraversal(root: TreeNode?): List<Int> {\n    val result = mutableListOf<Int>()\n    fun inorder(node: TreeNode?) {\n        if (node == null) return\n        inorder(node.left)\n        result.add(node.value)\n        inorder(node.right)\n    }\n    inorder(root)\n    return result\n}",
            "O(n) zaman"
        ))
        
        allQuestions.add(InterviewQuestion(
            "Maximum Depth of Binary Tree",
            "Binary tree'nin maksimum derinliğini bulun.",
            "trees",
            "easy",
            listOf("DFS veya BFS", "Sol ve sağ alt ağaçların maksimumunu al", "1 ekle"),
            "fun maxDepth(root: TreeNode?): Int {\n    if (root == null) return 0\n    return 1 + maxOf(maxDepth(root.left), maxDepth(root.right))\n}",
            "O(n) zaman"
        ))
        
        allQuestions.add(InterviewQuestion(
            "Validate Binary Search Tree",
            "Bir ağacın geçerli BST olup olmadığını kontrol edin.",
            "trees",
            "medium",
            listOf("Her node için min-max sınırları tut", "Sol alt ağaç < kök < sağ alt ağaç", "Recursive kontrol"),
            "fun isValidBST(root: TreeNode?): Boolean {\n    fun validate(node: TreeNode?, min: Long, max: Long): Boolean {\n        if (node == null) return true\n        if (node.value <= min || node.value >= max) return false\n        \n        return validate(node.left, min, node.value.toLong()) &&\n               validate(node.right, node.value.toLong(), max)\n    }\n    return validate(root, Long.MIN_VALUE, Long.MAX_VALUE)\n}",
            "O(n) zaman"
        ))
        
        // System Design
        allQuestions.add(InterviewQuestion(
            "Design URL Shortener",
            "URL kısaltma servisi tasarlayın (bit.ly gibi).",
            "system_design",
            "hard",
            listOf(
                "Base62 encoding kullan",
                "Unique ID generator gerekli",
                "Cache (Redis) kullan",
                "Database sharding düşün"
            ),
            "Bileşenler:\n1. Web Server\n2. Load Balancer\n3. Application Server\n4. Database (NoSQL)\n5. Cache (Redis)\n\nFlow:\n1. POST /shorten -> Generate ID\n2. GET /{short} -> Redirect",
            "Milyonlarca URL"
        ))
        
        allQuestions.add(InterviewQuestion(
            "Design Rate Limiter",
            "API rate limiter tasarlayın.",
            "system_design",
            "medium",
            listOf(
                "Token Bucket veya Sliding Window",
                "Redis ile distributed",
                "IP veya API key bazlı"
            ),
            "Algoritmalar:\n1. Token Bucket\n2. Leaky Bucket\n3. Fixed Window\n4. Sliding Window\n\nRedis ile atomic operations",
            "Saniyede binlerce istek"
        ))
        
        // Behavioral
        allQuestions.add(InterviewQuestion(
            "Tell me about yourself",
            "Kendinizi tanıtın.",
            "behavioral",
            "easy",
            listOf(
                "2-3 dakika kısa tut",
                "İlgili deneyimlere odaklan",
                "Neden bu rolü istediğini belirt"
            ),
            "Yapı:\n1. Şu anki rolünüz\n2. Önceki deneyimler\n3. Neden bu şirket/rol\n4. Gelecek hedefleri",
            "-"
        ))
        
        allQuestions.add(InterviewQuestion(
            "Tell me about a challenging project",
            "Zorlu bir proje deneyiminizi anlatın.",
            "behavioral",
            "medium",
            listOf(
                "STAR metodunu kullan",
                "Situation - Task - Action - Result",
                "Somut rakamlar ver"
            ),
            "STAR Metodu:\n\nSituation: Durum\nTask: Görev\nAction: Eylem\nResult: Sonuç",
            "-"
        ))
        
        filteredQuestions.addAll(allQuestions)
    }
    
    private fun setupAdapter() {
        adapter = QuestionAdapter(filteredQuestions) { question ->
            showQuestionDetails(question)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }
    
    private fun setupCategoryChips() {
        val categories = listOf(
            "Tümü" to "all",
            "Veri Yapıları" to "data_structures",
            "Algoritmalar" to "algorithms",
            "String" to "string",
            "Ağaçlar" to "trees",
            "Sistem Tasarımı" to "system_design",
            "Davranışsal" to "behavioral"
        )
        
        categories.forEachIndexed { index, (name, category) ->
            val chip = Chip(this).apply {
                text = name
                isCheckable = true
                isChecked = index == 0
                setOnClickListener {
                    selectedCategory = category
                    filterQuestions()
                    for (i in 0 until chipGroupCategories.childCount) {
                        (chipGroupCategories.getChildAt(i) as Chip).isChecked = it == this
                    }
                }
            }
            chipGroupCategories.addView(chip)
        }
    }
    
    private fun setupDifficultyChips() {
        val difficulties = listOf(
            "Tümü" to "all",
            "Kolay" to "easy",
            "Orta" to "medium",
            "Zor" to "hard"
        )
        
        difficulties.forEachIndexed { index, (name, difficulty) ->
            val chip = Chip(this).apply {
                text = name
                isCheckable = true
                isChecked = index == 0
                setOnClickListener {
                    selectedDifficulty = difficulty
                    filterQuestions()
                    for (i in 0 until chipGroupDifficulties.childCount) {
                        (chipGroupDifficulties.getChildAt(i) as Chip).isChecked = it == this
                    }
                }
            }
            chipGroupDifficulties.addView(chip)
        }
    }
    
    private fun filterQuestions() {
        filteredQuestions.clear()
        
        allQuestions.forEach { question ->
            val categoryMatch = selectedCategory == "all" || question.category == selectedCategory
            val difficultyMatch = selectedDifficulty == "all" || question.difficulty == selectedDifficulty
            
            if (categoryMatch && difficultyMatch) {
                filteredQuestions.add(question)
            }
        }
        
        adapter.notifyDataSetChanged()
        updateStats()
    }
    
    private fun updateStats() {
        val easy = allQuestions.count { it.difficulty == "easy" }
        val medium = allQuestions.count { it.difficulty == "medium" }
        val hard = allQuestions.count { it.difficulty == "hard" }
        tvStats.text = getString(R.string.interview_stats_format, allQuestions.size, easy, medium, hard)
    }
    
    private fun showQuestionDetails(question: InterviewQuestion) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_interview_question, null)
        
        dialogView.findViewById<TextView>(R.id.tvTitle).text = question.title
        dialogView.findViewById<TextView>(R.id.tvDescription).text = question.description
        dialogView.findViewById<TextView>(R.id.tvDifficulty).text = getDifficultyLabel(question.difficulty)
        dialogView.findViewById<TextView>(R.id.tvHints).text = question.hints.joinToString("\n• ", prefix = "• ")
        dialogView.findViewById<TextView>(R.id.tvSolution).text = question.solution
        dialogView.findViewById<TextView>(R.id.tvComplexity).text = question.complexity
        
        val difficultyView = dialogView.findViewById<TextView>(R.id.tvDifficulty)
        difficultyView.setTextColor(getDifficultyColor(question.difficulty))
        
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton(R.string.close, null)
            .setNeutralButton(R.string.practice_in_playground) { _, _ ->
                openInPlayground(question)
            }
            .create()
        
        dialog.show()
    }
    
    private fun openInPlayground(question: InterviewQuestion) {
        val intent = Intent(this, CodePlaygroundActivity::class.java)
        intent.putExtra("code", question.solution)
        intent.putExtra("title", question.title)
        startActivity(intent)
    }
    
    private fun getDifficultyLabel(difficulty: String): String {
        return when (difficulty) {
            "easy" -> "🟢 Kolay"
            "medium" -> "🟡 Orta"
            "hard" -> "🔴 Zor"
            else -> difficulty
        }
    }
    
    private fun getDifficultyColor(difficulty: String): Int {
        return when (difficulty) {
            "easy" -> getColor(R.color.difficulty_easy)
            "medium" -> getColor(R.color.difficulty_medium)
            "hard" -> getColor(R.color.difficulty_hard)
            else -> getColor(R.color.secondary_text)
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
    
    data class InterviewQuestion(
        val title: String,
        val description: String,
        val category: String,
        val difficulty: String,
        val hints: List<String>,
        val solution: String,
        val complexity: String
    )
    
    inner class QuestionAdapter(
        private val questions: List<InterviewQuestion>,
        private val onClick: (InterviewQuestion) -> Unit
    ) : RecyclerView.Adapter<QuestionAdapter.ViewHolder>() {
        
        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvTitle: TextView = view.findViewById(R.id.tvTitle)
            val tvDescription: TextView = view.findViewById(R.id.tvDescription)
            val tvDifficulty: TextView = view.findViewById(R.id.tvDifficulty)
            val tvCategory: TextView = view.findViewById(R.id.tvCategory)
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_interview_question, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val question = questions[position]
            holder.tvTitle.text = question.title
            holder.tvDescription.text = question.description
            holder.tvDifficulty.text = getDifficultyLabel(question.difficulty)
            holder.tvDifficulty.setTextColor(getDifficultyColor(question.difficulty))
            holder.tvCategory.text = getCategoryLabel(question.category)
            
            holder.itemView.setOnClickListener { onClick(question) }
        }
        
        override fun getItemCount() = questions.size
        
        private fun getCategoryLabel(category: String): String {
            return when (category) {
                "data_structures" -> "Veri Yapıları"
                "algorithms" -> "Algoritmalar"
                "string" -> "String"
                "trees" -> "Ağaçlar"
                "system_design" -> "Sistem Tasarımı"
                "behavioral" -> "Davranışsal"
                else -> category
            }
        }
    }
}
