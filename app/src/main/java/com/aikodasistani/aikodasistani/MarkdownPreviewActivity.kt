package com.aikodasistani.aikodasistani

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.text.Html
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputEditText

class MarkdownPreviewActivity : AppCompatActivity() {

    private lateinit var markdownInput: TextInputEditText
    private lateinit var previewText: TextView
    private lateinit var tabLayout: TabLayout
    private lateinit var editorContainer: ScrollView
    private lateinit var previewContainer: ScrollView
    private lateinit var copyHtmlButton: Button
    private lateinit var insertChipGroup: com.google.android.material.chip.ChipGroup
    private lateinit var charCountLabel: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_markdown_preview)

        supportActionBar?.apply {
            title = getString(R.string.markdown_preview)
            setDisplayHomeAsUpEnabled(true)
        }

        initViews()
        setupListeners()
        setupInsertChips()
        loadSampleMarkdown()
    }

    private fun initViews() {
        markdownInput = findViewById(R.id.markdownInput)
        previewText = findViewById(R.id.previewText)
        tabLayout = findViewById(R.id.tabLayout)
        editorContainer = findViewById(R.id.editorContainer)
        previewContainer = findViewById(R.id.previewContainer)
        copyHtmlButton = findViewById(R.id.copyHtmlButton)
        insertChipGroup = findViewById(R.id.insertChipGroup)
        charCountLabel = findViewById(R.id.charCountLabel)
    }

    private fun setupListeners() {
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> { // Editor
                        editorContainer.visibility = android.view.View.VISIBLE
                        previewContainer.visibility = android.view.View.GONE
                    }
                    1 -> { // Preview
                        editorContainer.visibility = android.view.View.GONE
                        previewContainer.visibility = android.view.View.VISIBLE
                        updatePreview()
                    }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        markdownInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val text = s?.toString() ?: ""
                val charCount = text.length
                val wordCount = if (text.isBlank()) 0 else text.trim().split("\\s+".toRegex()).size
                charCountLabel.text = "$charCount karakter, $wordCount kelime"
            }
        })

        copyHtmlButton.setOnClickListener {
            val markdown = markdownInput.text?.toString() ?: ""
            val html = convertMarkdownToHtml(markdown)
            copyToClipboard(html)
        }
    }

    private fun setupInsertChips() {
        val insertItems = listOf(
            "# Başlık" to "# ",
            "**Kalın**" to "**metin**",
            "*İtalik*" to "*metin*",
            "~~Üstü Çizili~~" to "~~metin~~",
            "`Kod`" to "`kod`",
            "```Kod Bloğu```" to "```\nkod\n```",
            "[Link](url)" to "[metin](url)",
            "![Resim](url)" to "![açıklama](url)",
            "- Liste" to "- öğe\n- öğe",
            "1. Sıralı Liste" to "1. öğe\n2. öğe",
            "> Alıntı" to "> alıntı",
            "---" to "\n---\n",
            "| Tablo |" to "| Başlık 1 | Başlık 2 |\n|---|---|\n| Değer 1 | Değer 2 |"
        )

        insertItems.forEach { (label, insertText) ->
            val chip = com.google.android.material.chip.Chip(this).apply {
                text = label
                isCheckable = false
                isClickable = true
                setOnClickListener {
                    insertTextAtCursor(insertText)
                }
            }
            insertChipGroup.addView(chip)
        }
    }

    private fun insertTextAtCursor(text: String) {
        val start = markdownInput.selectionStart
        val end = markdownInput.selectionEnd
        val editable = markdownInput.editableText
        editable?.replace(start, end, text)
    }

    private fun loadSampleMarkdown() {
        val sample = """
# Markdown Önizleme

Bu bir **Markdown** önizleme aracıdır.

## Özellikler

- *İtalik* metin
- **Kalın** metin
- ~~Üstü çizili~~ metin
- `Satır içi kod`

### Kod Bloğu

```kotlin
fun main() {
    println("Merhaba Dünya!")
}
```

### Liste

1. Birinci öğe
2. İkinci öğe
3. Üçüncü öğe

### Alıntı

> Bu bir alıntı metnidir.

### Link

[GitHub](https://github.com)

---

*Alt çizgi ile ayırıcı*
        """.trimIndent()
        
        markdownInput.setText(sample)
    }

    private fun updatePreview() {
        val markdown = markdownInput.text?.toString() ?: ""
        val html = convertMarkdownToHtml(markdown)
        previewText.text = Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
    }

    private fun convertMarkdownToHtml(markdown: String): String {
        var html = markdown
        
        // Headers
        html = html.replace(Regex("^### (.+)$", RegexOption.MULTILINE), "<h3>$1</h3>")
        html = html.replace(Regex("^## (.+)$", RegexOption.MULTILINE), "<h2>$1</h2>")
        html = html.replace(Regex("^# (.+)$", RegexOption.MULTILINE), "<h1>$1</h1>")
        
        // Bold and Italic
        html = html.replace(Regex("\\*\\*\\*(.+?)\\*\\*\\*"), "<b><i>$1</i></b>")
        html = html.replace(Regex("\\*\\*(.+?)\\*\\*"), "<b>$1</b>")
        html = html.replace(Regex("\\*(.+?)\\*"), "<i>$1</i>")
        html = html.replace(Regex("__(.+?)__"), "<b>$1</b>")
        html = html.replace(Regex("_(.+?)_"), "<i>$1</i>")
        
        // Strikethrough
        html = html.replace(Regex("~~(.+?)~~"), "<s>$1</s>")
        
        // Code blocks
        html = html.replace(Regex("```([\\s\\S]*?)```"), "<pre><code>$1</code></pre>")
        html = html.replace(Regex("`(.+?)`"), "<code>$1</code>")
        
        // Links
        html = html.replace(Regex("\\[(.+?)\\]\\((.+?)\\)"), "<a href=\"$2\">$1</a>")
        
        // Images
        html = html.replace(Regex("!\\[(.+?)\\]\\((.+?)\\)"), "[Resim: $1]")
        
        // Blockquote
        html = html.replace(Regex("^> (.+)$", RegexOption.MULTILINE), "<blockquote>$1</blockquote>")
        
        // Unordered list
        html = html.replace(Regex("^- (.+)$", RegexOption.MULTILINE), "• $1")
        html = html.replace(Regex("^\\* (.+)$", RegexOption.MULTILINE), "• $1")
        
        // Ordered list (simple)
        html = html.replace(Regex("^(\\d+)\\. (.+)$", RegexOption.MULTILINE), "$1. $2")
        
        // Horizontal rule
        html = html.replace(Regex("^---$", RegexOption.MULTILINE), "<hr>")
        html = html.replace(Regex("^\\*\\*\\*$", RegexOption.MULTILINE), "<hr>")
        
        // Line breaks
        html = html.replace("\n\n", "<br><br>")
        html = html.replace("\n", "<br>")
        
        return html
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("HTML", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
