package com.aikodasistani.aikodasistani

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

/**
 * Keyboard Shortcuts Activity
 * IDE ve geliştirme araçları kısayolları referansı
 */
class KeyboardShortcutsActivity : AppCompatActivity() {

    private lateinit var searchInput: EditText
    private lateinit var ideChipGroup: ChipGroup
    private lateinit var categoryChipGroup: ChipGroup
    private lateinit var shortcutsRecyclerView: RecyclerView

    private var selectedIDE = "android_studio"
    private var selectedCategory = "all"

    private val allShortcuts = listOf(
        // Android Studio - Navigation
        KeyboardShortcut("android_studio", "navigation", "Ctrl+Shift+N", "Dosyaya git", "Dosya adıyla arama"),
        KeyboardShortcut("android_studio", "navigation", "Ctrl+N", "Sınıfa git", "Class adıyla arama"),
        KeyboardShortcut("android_studio", "navigation", "Ctrl+Shift+Alt+N", "Sembole git", "Metod/değişken arama"),
        KeyboardShortcut("android_studio", "navigation", "Ctrl+E", "Son dosyalar", "Son açılan dosyaları göster"),
        KeyboardShortcut("android_studio", "navigation", "Ctrl+B", "Tanıma git", "Değişken/metod tanımına git"),
        KeyboardShortcut("android_studio", "navigation", "Alt+F7", "Kullanımları bul", "Tüm kullanımları listele"),
        KeyboardShortcut("android_studio", "navigation", "Ctrl+F12", "Dosya yapısı", "Metod listesini göster"),
        KeyboardShortcut("android_studio", "navigation", "Ctrl+H", "Tür hiyerarşisi", "Class inheritance göster"),
        KeyboardShortcut("android_studio", "navigation", "Ctrl+G", "Satıra git", "Belirli satır numarasına git"),
        KeyboardShortcut("android_studio", "navigation", "Ctrl+[/]", "Kod bloğu başı/sonu", "Parantez eşleştirme"),

        // Android Studio - Editing
        KeyboardShortcut("android_studio", "editing", "Ctrl+Space", "Kod tamamlama", "Otomatik tamamlama"),
        KeyboardShortcut("android_studio", "editing", "Ctrl+Shift+Space", "Akıllı tamamlama", "Tip bazlı tamamlama"),
        KeyboardShortcut("android_studio", "editing", "Alt+Enter", "Hızlı düzeltme", "Hata/uyarı düzeltme önerileri"),
        KeyboardShortcut("android_studio", "editing", "Ctrl+Alt+L", "Kodu formatla", "Otomatik kod formatlama"),
        KeyboardShortcut("android_studio", "editing", "Ctrl+Alt+O", "Import'ları düzenle", "Kullanılmayan import'ları kaldır"),
        KeyboardShortcut("android_studio", "editing", "Ctrl+D", "Satırı kopyala", "Mevcut satırı çoğalt"),
        KeyboardShortcut("android_studio", "editing", "Ctrl+Y", "Satırı sil", "Mevcut satırı sil"),
        KeyboardShortcut("android_studio", "editing", "Ctrl+/", "Satır yorum", "Tek satır yorum ekle/kaldır"),
        KeyboardShortcut("android_studio", "editing", "Ctrl+Shift+/", "Blok yorum", "/* */ yorum ekle"),
        KeyboardShortcut("android_studio", "editing", "Ctrl+W", "Seçimi genişlet", "Kelime → satır → blok seç"),
        KeyboardShortcut("android_studio", "editing", "Ctrl+Shift+W", "Seçimi daralt", "Seçimi küçült"),
        KeyboardShortcut("android_studio", "editing", "Shift+F6", "Yeniden adlandır", "Rename refactoring"),
        KeyboardShortcut("android_studio", "editing", "Ctrl+Alt+M", "Metod çıkar", "Seçili kodu metoda çıkar"),
        KeyboardShortcut("android_studio", "editing", "Ctrl+Alt+V", "Değişken çıkar", "Expression'ı değişkene çıkar"),

        // Android Studio - Debugging
        KeyboardShortcut("android_studio", "debugging", "Shift+F9", "Debug başlat", "Debug modda çalıştır"),
        KeyboardShortcut("android_studio", "debugging", "Shift+F10", "Run başlat", "Normal modda çalıştır"),
        KeyboardShortcut("android_studio", "debugging", "Ctrl+F8", "Breakpoint ekle/kaldır", "Toggle breakpoint"),
        KeyboardShortcut("android_studio", "debugging", "F8", "Adım atla", "Step over"),
        KeyboardShortcut("android_studio", "debugging", "F7", "İçine gir", "Step into"),
        KeyboardShortcut("android_studio", "debugging", "Shift+F8", "Dışına çık", "Step out"),
        KeyboardShortcut("android_studio", "debugging", "F9", "Devam et", "Resume program"),
        KeyboardShortcut("android_studio", "debugging", "Alt+F8", "İfade değerlendir", "Evaluate expression"),

        // Android Studio - Build
        KeyboardShortcut("android_studio", "build", "Ctrl+F9", "Projeyi derle", "Make project"),
        KeyboardShortcut("android_studio", "build", "Ctrl+Shift+F9", "Modülü derle", "Make module"),
        KeyboardShortcut("android_studio", "build", "Ctrl+Alt+Shift+S", "Proje yapısı", "Project structure"),

        // VS Code - Navigation
        KeyboardShortcut("vscode", "navigation", "Ctrl+P", "Dosyaya git", "Hızlı dosya açma"),
        KeyboardShortcut("vscode", "navigation", "Ctrl+Shift+P", "Komut paleti", "Tüm komutlara erişim"),
        KeyboardShortcut("vscode", "navigation", "Ctrl+G", "Satıra git", "Belirli satıra git"),
        KeyboardShortcut("vscode", "navigation", "Ctrl+Shift+O", "Sembole git", "Dosyadaki sembollere git"),
        KeyboardShortcut("vscode", "navigation", "F12", "Tanıma git", "Go to definition"),
        KeyboardShortcut("vscode", "navigation", "Alt+F12", "Tanımı göster", "Peek definition"),
        KeyboardShortcut("vscode", "navigation", "Shift+F12", "Referansları göster", "Show references"),
        KeyboardShortcut("vscode", "navigation", "Ctrl+Tab", "Tab değiştir", "Açık dosyalar arasında geç"),
        KeyboardShortcut("vscode", "navigation", "Ctrl+\\", "Editörü böl", "Split editor"),

        // VS Code - Editing
        KeyboardShortcut("vscode", "editing", "Ctrl+Space", "Öneriler", "Trigger suggestions"),
        KeyboardShortcut("vscode", "editing", "Ctrl+Shift+K", "Satırı sil", "Delete line"),
        KeyboardShortcut("vscode", "editing", "Alt+Up/Down", "Satırı taşı", "Move line up/down"),
        KeyboardShortcut("vscode", "editing", "Shift+Alt+Up/Down", "Satırı kopyala", "Copy line up/down"),
        KeyboardShortcut("vscode", "editing", "Ctrl+/", "Satır yorum", "Toggle line comment"),
        KeyboardShortcut("vscode", "editing", "Shift+Alt+A", "Blok yorum", "Toggle block comment"),
        KeyboardShortcut("vscode", "editing", "Ctrl+]", "Girintiyi artır", "Indent line"),
        KeyboardShortcut("vscode", "editing", "Ctrl+[", "Girintiyi azalt", "Outdent line"),
        KeyboardShortcut("vscode", "editing", "Ctrl+Shift+\\", "Eşleşen paranteze git", "Jump to bracket"),
        KeyboardShortcut("vscode", "editing", "F2", "Yeniden adlandır", "Rename symbol"),
        KeyboardShortcut("vscode", "editing", "Ctrl+.", "Hızlı düzeltme", "Quick fix"),

        // VS Code - Multi-cursor
        KeyboardShortcut("vscode", "multicursor", "Alt+Click", "İmleç ekle", "Insert cursor"),
        KeyboardShortcut("vscode", "multicursor", "Ctrl+Alt+Up/Down", "Üste/alta imleç", "Add cursor above/below"),
        KeyboardShortcut("vscode", "multicursor", "Ctrl+D", "Sonraki eşleşmeyi seç", "Select next occurrence"),
        KeyboardShortcut("vscode", "multicursor", "Ctrl+Shift+L", "Tüm eşleşmeleri seç", "Select all occurrences"),

        // IntelliJ IDEA
        KeyboardShortcut("intellij", "navigation", "Double Shift", "Her yerde ara", "Search everywhere"),
        KeyboardShortcut("intellij", "navigation", "Ctrl+Shift+A", "Eylem bul", "Find action"),
        KeyboardShortcut("intellij", "navigation", "Ctrl+E", "Son dosyalar", "Recent files"),
        KeyboardShortcut("intellij", "editing", "Ctrl+Alt+L", "Kodu formatla", "Reformat code"),
        KeyboardShortcut("intellij", "editing", "Ctrl+Shift+Enter", "İfadeyi tamamla", "Complete statement"),
        KeyboardShortcut("intellij", "refactoring", "Ctrl+Alt+Shift+T", "Refactor menüsü", "Refactor this"),
        KeyboardShortcut("intellij", "refactoring", "Ctrl+Alt+M", "Metod çıkar", "Extract method"),
        KeyboardShortcut("intellij", "refactoring", "Ctrl+Alt+V", "Değişken çıkar", "Introduce variable"),

        // Terminal / Git
        KeyboardShortcut("terminal", "navigation", "Ctrl+C", "İşlemi iptal et", "Çalışan komutu durdur"),
        KeyboardShortcut("terminal", "navigation", "Ctrl+L", "Ekranı temizle", "Clear terminal"),
        KeyboardShortcut("terminal", "navigation", "Ctrl+R", "Geçmişte ara", "Reverse search history"),
        KeyboardShortcut("terminal", "navigation", "Tab", "Otomatik tamamla", "Auto-complete"),
        KeyboardShortcut("terminal", "navigation", "Up/Down", "Geçmiş komutlar", "Navigate history"),
        KeyboardShortcut("terminal", "git", "git status", "Durum", "Değişiklikleri göster"),
        KeyboardShortcut("terminal", "git", "git add .", "Tümünü stage'le", "Tüm değişiklikleri ekle"),
        KeyboardShortcut("terminal", "git", "git commit -m", "Commit", "Değişiklikleri kaydet"),
        KeyboardShortcut("terminal", "git", "git push", "Gönder", "Remote'a gönder"),
        KeyboardShortcut("terminal", "git", "git pull", "Çek", "Remote'dan çek"),
        KeyboardShortcut("terminal", "git", "git branch", "Branch listele", "Tüm branch'leri göster"),
        KeyboardShortcut("terminal", "git", "git checkout -b", "Yeni branch", "Yeni branch oluştur")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_keyboard_shortcuts)

        setupViews()
        setupIDEChips()
        setupCategoryChips()
        loadShortcuts()
    }

    private fun setupViews() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.keyboard_shortcuts_title)

        searchInput = findViewById(R.id.searchInput)
        ideChipGroup = findViewById(R.id.ideChipGroup)
        categoryChipGroup = findViewById(R.id.categoryChipGroup)
        shortcutsRecyclerView = findViewById(R.id.shortcutsRecyclerView)

        shortcutsRecyclerView.layoutManager = LinearLayoutManager(this)

        searchInput.addTextChangedListener {
            loadShortcuts()
        }
    }

    private fun setupIDEChips() {
        val ides = listOf(
            "android_studio" to "Android Studio",
            "vscode" to "VS Code",
            "intellij" to "IntelliJ",
            "terminal" to "Terminal/Git"
        )

        ides.forEach { (id, name) ->
            val chip = Chip(this).apply {
                text = name
                isCheckable = true
                isChecked = id == selectedIDE
                setOnClickListener {
                    selectedIDE = id
                    updateCategoryChips()
                    loadShortcuts()
                }
            }
            ideChipGroup.addView(chip)
        }
    }

    private fun setupCategoryChips() {
        updateCategoryChips()
    }

    private fun updateCategoryChips() {
        categoryChipGroup.removeAllViews()
        selectedCategory = "all"

        val categories = allShortcuts
            .filter { it.ide == selectedIDE }
            .map { it.category }
            .distinct()

        val allChip = Chip(this).apply {
            text = "Tümü"
            isCheckable = true
            isChecked = true
            setOnClickListener {
                selectedCategory = "all"
                loadShortcuts()
            }
        }
        categoryChipGroup.addView(allChip)

        val categoryNames = mapOf(
            "navigation" to "Navigasyon",
            "editing" to "Düzenleme",
            "debugging" to "Debug",
            "build" to "Derleme",
            "multicursor" to "Çoklu İmleç",
            "refactoring" to "Refactoring",
            "git" to "Git"
        )

        categories.forEach { category ->
            val chip = Chip(this).apply {
                text = categoryNames[category] ?: category.replaceFirstChar { it.uppercase() }
                isCheckable = true
                isChecked = false
                setOnClickListener {
                    selectedCategory = category
                    loadShortcuts()
                }
            }
            categoryChipGroup.addView(chip)
        }
    }

    private fun loadShortcuts() {
        val searchQuery = searchInput.text.toString().lowercase()

        val filtered = allShortcuts.filter { shortcut ->
            val matchesIDE = shortcut.ide == selectedIDE
            val matchesCategory = selectedCategory == "all" || shortcut.category == selectedCategory
            val matchesSearch = searchQuery.isEmpty() ||
                shortcut.shortcut.lowercase().contains(searchQuery) ||
                shortcut.action.lowercase().contains(searchQuery) ||
                shortcut.description.lowercase().contains(searchQuery)

            matchesIDE && matchesCategory && matchesSearch
        }

        shortcutsRecyclerView.adapter = ShortcutsAdapter(filtered) { shortcut ->
            copyToClipboard(shortcut.shortcut)
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("shortcut", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Kopyalandı: $text", Toast.LENGTH_SHORT).show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    data class KeyboardShortcut(
        val ide: String,
        val category: String,
        val shortcut: String,
        val action: String,
        val description: String
    )

    inner class ShortcutsAdapter(
        private val shortcuts: List<KeyboardShortcut>,
        private val onCopy: (KeyboardShortcut) -> Unit
    ) : RecyclerView.Adapter<ShortcutsAdapter.ShortcutViewHolder>() {

        inner class ShortcutViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val shortcutText: TextView = view.findViewById(R.id.shortcutText)
            val actionText: TextView = view.findViewById(R.id.actionText)
            val descriptionText: TextView = view.findViewById(R.id.descriptionText)
            val copyButton: ImageButton = view.findViewById(R.id.copyButton)
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ShortcutViewHolder {
            val view = layoutInflater.inflate(R.layout.item_keyboard_shortcut, parent, false)
            return ShortcutViewHolder(view)
        }

        override fun onBindViewHolder(holder: ShortcutViewHolder, position: Int) {
            val shortcut = shortcuts[position]
            
            holder.shortcutText.text = shortcut.shortcut
            holder.actionText.text = shortcut.action
            holder.descriptionText.text = shortcut.description
            
            holder.copyButton.setOnClickListener {
                onCopy(shortcut)
            }
            
            holder.itemView.setOnClickListener {
                onCopy(shortcut)
            }
        }

        override fun getItemCount() = shortcuts.size
    }
}
