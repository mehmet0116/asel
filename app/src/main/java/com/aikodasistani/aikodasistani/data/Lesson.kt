package com.aikodasistani.aikodasistani.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

/**
 * Entity for storing programming lessons and tutorials.
 */
@Entity(tableName = "lessons")
data class Lesson(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "description")
    val description: String,

    @ColumnInfo(name = "category")
    val category: String, // "kotlin", "python", "javascript", "algorithms", "data-structures"

    @ColumnInfo(name = "difficulty")
    val difficulty: String, // "beginner", "intermediate", "advanced"

    @ColumnInfo(name = "content")
    val content: String, // Markdown content

    @ColumnInfo(name = "codeExamples")
    val codeExamples: String? = null, // JSON array of code examples

    @ColumnInfo(name = "exercises")
    val exercises: String? = null, // JSON array of exercises

    @ColumnInfo(name = "duration")
    val duration: Int = 10, // Estimated minutes to complete

    @ColumnInfo(name = "orderIndex")
    val orderIndex: Int = 0,

    @ColumnInfo(name = "isCompleted")
    val isCompleted: Boolean = false,

    @ColumnInfo(name = "progress")
    val progress: Int = 0, // 0-100

    @ColumnInfo(name = "completedAt")
    val completedAt: Long? = null,

    @ColumnInfo(name = "createdAt")
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        fun getDefaultLessons(): List<Lesson> {
            return listOf(
                // Kotlin Temelleri
                Lesson(
                    title = "Kotlin'e Giriş",
                    description = "Kotlin programlama dilinin temelleri ve ilk programınız",
                    category = "kotlin",
                    difficulty = "beginner",
                    content = """
# Kotlin'e Giriş

Kotlin, JetBrains tarafından geliştirilen modern bir programlama dilidir. Android geliştirme için resmi olarak desteklenir.

## Neden Kotlin?

- **Null Safety**: NullPointerException'ları önler
- **Kısa ve Öz**: Java'ya göre %40 daha az kod
- **Java Uyumluluğu**: Mevcut Java kodlarıyla çalışır
- **Fonksiyonel Programlama**: Lambda ve higher-order functions

## İlk Programınız

```kotlin
fun main() {
    println("Merhaba Dünya!")
}
```

## Değişkenler

Kotlin'de iki tür değişken vardır:

```kotlin
val sabit = "Değiştirilemez"  // Immutable
var degisken = "Değiştirilebilir"  // Mutable
degisken = "Yeni değer"
```

## Veri Tipleri

```kotlin
val isim: String = "Ahmet"
val yas: Int = 25
val boy: Double = 1.75
val aktif: Boolean = true
```
                    """.trimIndent(),
                    codeExamples = """["fun main() { println(\"Merhaba!\") }", "val x = 10\nval y = 20\nprintln(x + y)"]""",
                    exercises = """["Konsola kendi adınızı yazdıran bir program yazın", "İki sayıyı toplayan bir program yazın"]""",
                    duration = 15,
                    orderIndex = 1
                ),
                Lesson(
                    title = "Kotlin Fonksiyonları",
                    description = "Fonksiyon tanımlama, parametreler ve dönüş değerleri",
                    category = "kotlin",
                    difficulty = "beginner",
                    content = """
# Kotlin Fonksiyonları

Fonksiyonlar, belirli bir görevi yerine getiren kod bloklarıdır.

## Temel Fonksiyon

```kotlin
fun selamla() {
    println("Merhaba!")
}
```

## Parametreli Fonksiyon

```kotlin
fun selamla(isim: String) {
    println("Merhaba, ${'$'}isim!")
}
```

## Dönüş Değerli Fonksiyon

```kotlin
fun topla(a: Int, b: Int): Int {
    return a + b
}

// Tek satırlık fonksiyon
fun carp(a: Int, b: Int) = a * b
```

## Varsayılan Parametreler

```kotlin
fun selamla(isim: String = "Dünya") {
    println("Merhaba, ${'$'}isim!")
}

selamla()  // "Merhaba, Dünya!"
selamla("Ahmet")  // "Merhaba, Ahmet!"
```

## Named Arguments

```kotlin
fun kullaniciBilgisi(isim: String, yas: Int, sehir: String) {
    println("${'$'}isim, ${'$'}yas yaşında, ${'$'}sehir")
}

kullaniciBilgisi(yas = 25, sehir = "İstanbul", isim = "Ali")
```
                    """.trimIndent(),
                    codeExamples = """["fun topla(a: Int, b: Int) = a + b\nprintln(topla(5, 3))"]""",
                    exercises = """["Bir sayının karesini hesaplayan fonksiyon yazın", "İki string'i birleştiren fonksiyon yazın"]""",
                    duration = 20,
                    orderIndex = 2
                ),
                Lesson(
                    title = "Koşullu İfadeler",
                    description = "if-else, when ifadeleri ve koşullu programlama",
                    category = "kotlin",
                    difficulty = "beginner",
                    content = """
# Koşullu İfadeler

## if-else İfadesi

```kotlin
val yas = 18

if (yas >= 18) {
    println("Yetişkin")
} else {
    println("Çocuk")
}
```

## if Expression

Kotlin'de if bir ifadedir (expression) ve değer döndürür:

```kotlin
val sonuc = if (yas >= 18) "Yetişkin" else "Çocuk"
```

## when İfadesi

when, switch'in güçlü versiyonudur:

```kotlin
val gun = 3

when (gun) {
    1 -> println("Pazartesi")
    2 -> println("Salı")
    3 -> println("Çarşamba")
    4 -> println("Perşembe")
    5 -> println("Cuma")
    6, 7 -> println("Hafta sonu")
    else -> println("Geçersiz gün")
}
```

## when as Expression

```kotlin
val gunAdi = when (gun) {
    1 -> "Pazartesi"
    2 -> "Salı"
    in 3..5 -> "Hafta ortası"
    else -> "Diğer"
}
```

## when with Ranges

```kotlin
val not = 85

val harf = when (not) {
    in 90..100 -> "A"
    in 80..89 -> "B"
    in 70..79 -> "C"
    in 60..69 -> "D"
    else -> "F"
}
```
                    """.trimIndent(),
                    codeExamples = """["val x = 10\nif (x > 5) println(\"Büyük\") else println(\"Küçük\")"]""",
                    exercises = """["Bir sayının pozitif, negatif veya sıfır olduğunu bulan program yazın", "Harf notunu sayısal nota çeviren when kullanın"]""",
                    duration = 15,
                    orderIndex = 3
                ),
                Lesson(
                    title = "Döngüler",
                    description = "for, while, do-while döngüleri ve koleksiyonlarda iterasyon",
                    category = "kotlin",
                    difficulty = "beginner",
                    content = """
# Döngüler

## for Döngüsü

```kotlin
// Range ile
for (i in 1..5) {
    println(i)  // 1, 2, 3, 4, 5
}

// until ile (son hariç)
for (i in 1 until 5) {
    println(i)  // 1, 2, 3, 4
}

// step ile
for (i in 1..10 step 2) {
    println(i)  // 1, 3, 5, 7, 9
}

// downTo ile
for (i in 5 downTo 1) {
    println(i)  // 5, 4, 3, 2, 1
}
```

## Koleksiyonlarda for

```kotlin
val meyveler = listOf("Elma", "Armut", "Muz")

for (meyve in meyveler) {
    println(meyve)
}

// Index ile
for ((index, meyve) in meyveler.withIndex()) {
    println("${'$'}index: ${'$'}meyve")
}
```

## while Döngüsü

```kotlin
var sayac = 5
while (sayac > 0) {
    println(sayac)
    sayac--
}
```

## do-while Döngüsü

```kotlin
var x = 0
do {
    println(x)
    x++
} while (x < 5)
```

## break ve continue

```kotlin
for (i in 1..10) {
    if (i == 5) break  // Döngüden çık
    if (i % 2 == 0) continue  // Sonraki iterasyona geç
    println(i)
}
```
                    """.trimIndent(),
                    codeExamples = """["for (i in 1..5) println(i * i)"]""",
                    exercises = """["1'den 100'e kadar sayıların toplamını hesaplayın", "Bir listenin elemanlarını tersten yazdırın"]""",
                    duration = 20,
                    orderIndex = 4
                ),
                Lesson(
                    title = "Listeler ve Koleksiyonlar",
                    description = "List, Set, Map veri yapıları ve işlemleri",
                    category = "kotlin",
                    difficulty = "intermediate",
                    content = """
# Koleksiyonlar

## List

```kotlin
// Immutable List
val sayilar = listOf(1, 2, 3, 4, 5)

// Mutable List
val meyveler = mutableListOf("Elma", "Armut")
meyveler.add("Muz")
meyveler.remove("Elma")
```

## Set

Benzersiz elemanlar içerir:

```kotlin
val kume = setOf(1, 2, 2, 3, 3, 3)
println(kume)  // [1, 2, 3]

val mutableKume = mutableSetOf<String>()
mutableKume.add("A")
mutableKume.add("B")
```

## Map

Anahtar-değer çiftleri:

```kotlin
val sehirler = mapOf(
    "TR" to "Türkiye",
    "US" to "Amerika",
    "DE" to "Almanya"
)

println(sehirler["TR"])  // Türkiye

val mutableMap = mutableMapOf<String, Int>()
mutableMap["Ali"] = 25
mutableMap["Veli"] = 30
```

## Koleksiyon İşlemleri

```kotlin
val sayilar = listOf(1, 2, 3, 4, 5)

// filter
val ciftler = sayilar.filter { it % 2 == 0 }

// map
val kareler = sayilar.map { it * it }

// reduce
val toplam = sayilar.reduce { acc, i -> acc + i }

// find
val ilkCift = sayilar.find { it % 2 == 0 }

// any, all, none
sayilar.any { it > 3 }  // true
sayilar.all { it > 0 }  // true
sayilar.none { it < 0 }  // true
```
                    """.trimIndent(),
                    codeExamples = """["val list = listOf(1, 2, 3, 4, 5)\nval doubled = list.map { it * 2 }\nprintln(doubled)"]""",
                    exercises = """["Bir listedeki çift sayıları filtreleyin", "İki listenin kesişimini bulun"]""",
                    duration = 25,
                    orderIndex = 5
                ),
                // Algoritma Dersleri
                Lesson(
                    title = "Algoritmalara Giriş",
                    description = "Algoritma nedir, zaman/uzay karmaşıklığı (Big O)",
                    category = "algorithms",
                    difficulty = "beginner",
                    content = """
# Algoritmalara Giriş

## Algoritma Nedir?

Algoritma, bir problemi çözmek için izlenen adımlar dizisidir.

## Örnek: Çay Demleme Algoritması

1. Su kaynat
2. Çaydanlığa çay koy
3. Kaynar suyu dök
4. 10 dakika bekle
5. Bardağa dök

## Big O Notasyonu

Algoritmanın performansını ölçer:

- **O(1)**: Sabit zaman - Array'de index ile erişim
- **O(log n)**: Logaritmik - Binary Search
- **O(n)**: Lineer - Basit arama
- **O(n log n)**: Linearitmik - Merge Sort
- **O(n²)**: Karesel - Nested loops
- **O(2ⁿ)**: Üstel - Fibonacci (naive)

## Örnek: O(n) vs O(n²)

```kotlin
// O(n) - Lineer
fun topla(liste: List<Int>): Int {
    var toplam = 0
    for (sayi in liste) {
        toplam += sayi
    }
    return toplam
}

// O(n²) - Karesel
fun bubbleSort(arr: IntArray) {
    for (i in arr.indices) {
        for (j in 0 until arr.size - 1) {
            if (arr[j] > arr[j + 1]) {
                val temp = arr[j]
                arr[j] = arr[j + 1]
                arr[j + 1] = temp
            }
        }
    }
}
```
                    """.trimIndent(),
                    codeExamples = """["// O(n) örneği\nfun ara(liste: List<Int>, hedef: Int): Int {\n    for (i in liste.indices) {\n        if (liste[i] == hedef) return i\n    }\n    return -1\n}"]""",
                    exercises = """["Verilen bir algoritmanın Big O karmaşıklığını belirleyin", "O(n) ile çalışan bir arama fonksiyonu yazın"]""",
                    duration = 30,
                    orderIndex = 6
                )
            )
        }
    }
}
