package com.aikodasistani.aikodasistani.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

/**
 * Entity for storing daily coding challenges and user progress.
 */
@Entity(tableName = "coding_challenges")
data class CodingChallenge(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "description")
    val description: String,

    @ColumnInfo(name = "difficulty")
    val difficulty: String, // "easy", "medium", "hard"

    @ColumnInfo(name = "category")
    val category: String, // "algorithm", "string", "array", "math", "data-structure"

    @ColumnInfo(name = "starterCode")
    val starterCode: String,

    @ColumnInfo(name = "language")
    val language: String = "kotlin",

    @ColumnInfo(name = "hints")
    val hints: String? = null, // JSON array of hints

    @ColumnInfo(name = "solution")
    val solution: String? = null,

    @ColumnInfo(name = "isCompleted")
    val isCompleted: Boolean = false,

    @ColumnInfo(name = "userSolution")
    val userSolution: String? = null,

    @ColumnInfo(name = "completedAt")
    val completedAt: Long? = null,

    @ColumnInfo(name = "dateShown")
    val dateShown: String, // Format: "yyyy-MM-dd"

    @ColumnInfo(name = "createdAt")
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        // Sample challenges for the app
        fun getDefaultChallenges(): List<CodingChallenge> {
            return listOf(
                CodingChallenge(
                    title = "İki Sayının Toplamı",
                    description = "Verilen bir dizide, toplamı hedef sayıya eşit olan iki elemanın indekslerini bulun.\n\nÖrnek:\nGiriş: nums = [2, 7, 11, 15], hedef = 9\nÇıkış: [0, 1]\nAçıklama: nums[0] + nums[1] = 2 + 7 = 9",
                    difficulty = "easy",
                    category = "array",
                    starterCode = """fun twoSum(nums: IntArray, target: Int): IntArray {
    // Kodunuzu buraya yazın
    
}""",
                    language = "kotlin",
                    hints = """["HashMap kullanmayı düşünün", "Her eleman için hedef - eleman değerini arayın"]""",
                    solution = """fun twoSum(nums: IntArray, target: Int): IntArray {
    val map = mutableMapOf<Int, Int>()
    nums.forEachIndexed { index, num ->
        val complement = target - num
        if (map.containsKey(complement)) {
            return intArrayOf(map[complement]!!, index)
        }
        map[num] = index
    }
    return intArrayOf()
}""",
                    dateShown = "2024-01-01"
                ),
                CodingChallenge(
                    title = "Palindrom Kontrolü",
                    description = "Verilen bir string'in palindrom olup olmadığını kontrol edin. Sadece alfanumerik karakterleri dikkate alın ve büyük/küçük harf duyarsız olun.\n\nÖrnek:\nGiriş: \"A man, a plan, a canal: Panama\"\nÇıkış: true",
                    difficulty = "easy",
                    category = "string",
                    starterCode = """fun isPalindrome(s: String): Boolean {
    // Kodunuzu buraya yazın
    
}""",
                    language = "kotlin",
                    hints = """["String'i temizleyip küçük harfe çevirin", "İki pointer kullanabilirsiniz"]""",
                    solution = """fun isPalindrome(s: String): Boolean {
    val cleaned = s.filter { it.isLetterOrDigit() }.lowercase()
    return cleaned == cleaned.reversed()
}""",
                    dateShown = "2024-01-02"
                ),
                CodingChallenge(
                    title = "FizzBuzz",
                    description = "1'den n'e kadar sayılar için:\n- 3'e bölünebiliyorsa \"Fizz\"\n- 5'e bölünebiliyorsa \"Buzz\"\n- Her ikisine de bölünebiliyorsa \"FizzBuzz\"\n- Değilse sayının kendisini döndürün.\n\nÖrnek: n=15 için son 3 eleman: [\"13\", \"14\", \"FizzBuzz\"]",
                    difficulty = "easy",
                    category = "math",
                    starterCode = """fun fizzBuzz(n: Int): List<String> {
    // Kodunuzu buraya yazın
    
}""",
                    language = "kotlin",
                    hints = """["Modulo (%) operatörünü kullanın", "Önce her iki durumu kontrol edin"]""",
                    solution = """fun fizzBuzz(n: Int): List<String> {
    return (1..n).map { i ->
        when {
            i % 15 == 0 -> "FizzBuzz"
            i % 3 == 0 -> "Fizz"
            i % 5 == 0 -> "Buzz"
            else -> i.toString()
        }
    }
}""",
                    dateShown = "2024-01-03"
                ),
                CodingChallenge(
                    title = "Fibonacci Sayısı",
                    description = "n. Fibonacci sayısını hesaplayın.\n\nFibonacci dizisi: F(0) = 0, F(1) = 1, F(n) = F(n-1) + F(n-2)\n\nÖrnek:\nGiriş: n = 10\nÇıkış: 55",
                    difficulty = "medium",
                    category = "algorithm",
                    starterCode = """fun fibonacci(n: Int): Int {
    // Kodunuzu buraya yazın
    
}""",
                    language = "kotlin",
                    hints = """["Dinamik programlama kullanın", "Önceki iki değeri saklayın"]""",
                    solution = """fun fibonacci(n: Int): Int {
    if (n <= 1) return n
    var prev = 0
    var curr = 1
    repeat(n - 1) {
        val next = prev + curr
        prev = curr
        curr = next
    }
    return curr
}""",
                    dateShown = "2024-01-04"
                ),
                CodingChallenge(
                    title = "Tekrarsız En Uzun Alt String",
                    description = "Verilen bir string'de tekrarlanan karakter içermeyen en uzun alt string'in uzunluğunu bulun.\n\nÖrnek:\nGiriş: \"abcabcbb\"\nÇıkış: 3 (\"abc\")",
                    difficulty = "medium",
                    category = "string",
                    starterCode = """fun lengthOfLongestSubstring(s: String): Int {
    // Kodunuzu buraya yazın
    
}""",
                    language = "kotlin",
                    hints = """["Sliding window tekniği kullanın", "Karakterlerin son pozisyonlarını bir map'te tutun"]""",
                    solution = """fun lengthOfLongestSubstring(s: String): Int {
    val charIndex = mutableMapOf<Char, Int>()
    var maxLength = 0
    var start = 0
    
    s.forEachIndexed { index, char ->
        if (charIndex.containsKey(char) && charIndex[char]!! >= start) {
            start = charIndex[char]!! + 1
        }
        charIndex[char] = index
        maxLength = maxOf(maxLength, index - start + 1)
    }
    return maxLength
}""",
                    dateShown = "2024-01-05"
                ),
                CodingChallenge(
                    title = "Dizi Tersine Çevirme",
                    description = "Verilen bir integer dizisini yerinde (in-place) tersine çevirin.\n\nÖrnek:\nGiriş: [1, 2, 3, 4, 5]\nÇıkış: [5, 4, 3, 2, 1]",
                    difficulty = "easy",
                    category = "array",
                    starterCode = """fun reverseArray(nums: IntArray): Unit {
    // Kodunuzu buraya yazın
    
}""",
                    language = "kotlin",
                    hints = """["İki pointer kullanın: baştan ve sondan", "Elemanları swap edin"]""",
                    solution = """fun reverseArray(nums: IntArray): Unit {
    var left = 0
    var right = nums.size - 1
    while (left < right) {
        val temp = nums[left]
        nums[left] = nums[right]
        nums[right] = temp
        left++
        right--
    }
}""",
                    dateShown = "2024-01-06"
                ),
                CodingChallenge(
                    title = "Binary Search",
                    description = "Sıralı bir dizide hedef değerin indeksini bulun. Yoksa -1 döndürün.\n\nÖrnek:\nGiriş: nums = [-1, 0, 3, 5, 9, 12], hedef = 9\nÇıkış: 4",
                    difficulty = "easy",
                    category = "algorithm",
                    starterCode = """fun binarySearch(nums: IntArray, target: Int): Int {
    // Kodunuzu buraya yazın
    
}""",
                    language = "kotlin",
                    hints = """["İki pointer kullan: left ve right", "Ortayı hesapla ve karşılaştır"]""",
                    solution = """fun binarySearch(nums: IntArray, target: Int): Int {
    var left = 0
    var right = nums.size - 1
    while (left <= right) {
        val mid = left + (right - left) / 2
        when {
            nums[mid] == target -> return mid
            nums[mid] < target -> left = mid + 1
            else -> right = mid - 1
        }
    }
    return -1
}""",
                    dateShown = "2024-01-07"
                )
            )
        }
    }
}
