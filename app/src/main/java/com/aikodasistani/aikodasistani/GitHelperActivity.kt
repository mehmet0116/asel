package com.aikodasistani.aikodasistani

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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

class GitHelperActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var chipGroupCategories: ChipGroup
    private lateinit var searchView: EditText
    private lateinit var btnSearch: ImageButton
    
    private val allCommands = mutableListOf<GitCommand>()
    private val filteredCommands = mutableListOf<GitCommand>()
    private lateinit var adapter: GitCommandAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_git_helper)
        
        supportActionBar?.apply {
            title = getString(R.string.git_helper_title)
            setDisplayHomeAsUpEnabled(true)
        }
        
        initializeViews()
        loadGitCommands()
        setupAdapter()
        setupCategoryChips()
        setupSearch()
    }
    
    private fun initializeViews() {
        recyclerView = findViewById(R.id.recyclerViewCommands)
        chipGroupCategories = findViewById(R.id.chipGroupCategories)
        searchView = findViewById(R.id.editTextSearch)
        btnSearch = findViewById(R.id.btnSearch)
    }
    
    private fun loadGitCommands() {
        allCommands.clear()
        
        // Basic Commands
        allCommands.add(GitCommand(
            "git init",
            "Yeni Git deposu başlatır",
            "basic",
            "git init",
            "Mevcut dizinde yeni bir .git klasörü oluşturur ve versiyon kontrolünü başlatır.",
            listOf("git init my-project", "git init --bare")
        ))
        
        allCommands.add(GitCommand(
            "git clone",
            "Uzak depoyu kopyalar",
            "basic",
            "git clone <url>",
            "Uzak bir Git deposunu lokal bilgisayarınıza kopyalar.",
            listOf("git clone https://github.com/user/repo.git", "git clone --depth 1 <url>")
        ))
        
        allCommands.add(GitCommand(
            "git status",
            "Çalışma dizini durumunu gösterir",
            "basic",
            "git status",
            "Değiştirilmiş, staged ve untracked dosyaları listeler.",
            listOf("git status -s", "git status --short")
        ))
        
        allCommands.add(GitCommand(
            "git add",
            "Dosyaları staging'e ekler",
            "basic",
            "git add <dosya>",
            "Dosyaları commit için hazırlık alanına (staging) ekler.",
            listOf("git add .", "git add -A", "git add *.kt")
        ))
        
        allCommands.add(GitCommand(
            "git commit",
            "Değişiklikleri kaydeder",
            "basic",
            "git commit -m \"mesaj\"",
            "Staging alanındaki değişiklikleri kalıcı olarak depoya kaydeder.",
            listOf("git commit -m \"Fix bug\"", "git commit -am \"Update\"", "git commit --amend")
        ))
        
        allCommands.add(GitCommand(
            "git push",
            "Değişiklikleri uzak depoya gönderir",
            "basic",
            "git push <remote> <branch>",
            "Lokal commit'leri uzak depoya yükler.",
            listOf("git push origin main", "git push -u origin feature", "git push --force")
        ))
        
        allCommands.add(GitCommand(
            "git pull",
            "Uzak depodaki değişiklikleri çeker",
            "basic",
            "git pull <remote> <branch>",
            "Uzak depodaki değişiklikleri indirir ve mevcut branch ile birleştirir.",
            listOf("git pull origin main", "git pull --rebase")
        ))
        
        // Branch Commands
        allCommands.add(GitCommand(
            "git branch",
            "Branch işlemleri",
            "branch",
            "git branch <isim>",
            "Yeni branch oluşturur, listeler veya siler.",
            listOf("git branch feature", "git branch -d old-branch", "git branch -a")
        ))
        
        allCommands.add(GitCommand(
            "git checkout",
            "Branch değiştirir veya dosya geri yükler",
            "branch",
            "git checkout <branch>",
            "Farklı bir branch'e geçiş yapar veya dosyaları önceki durumuna döndürür.",
            listOf("git checkout main", "git checkout -b new-feature", "git checkout -- file.txt")
        ))
        
        allCommands.add(GitCommand(
            "git switch",
            "Branch değiştirir (modern)",
            "branch",
            "git switch <branch>",
            "Checkout'un modern alternatifi. Sadece branch değiştirmek için kullanılır.",
            listOf("git switch main", "git switch -c new-feature")
        ))
        
        allCommands.add(GitCommand(
            "git merge",
            "Branch'leri birleştirir",
            "branch",
            "git merge <branch>",
            "Belirtilen branch'i mevcut branch ile birleştirir.",
            listOf("git merge feature", "git merge --no-ff feature", "git merge --squash feature")
        ))
        
        allCommands.add(GitCommand(
            "git rebase",
            "Commit'leri yeniden düzenler",
            "branch",
            "git rebase <branch>",
            "Mevcut branch'in commit'lerini başka bir branch'in üzerine taşır.",
            listOf("git rebase main", "git rebase -i HEAD~3")
        ))
        
        // History Commands
        allCommands.add(GitCommand(
            "git log",
            "Commit geçmişini gösterir",
            "history",
            "git log",
            "Tüm commit geçmişini detaylı olarak listeler.",
            listOf("git log --oneline", "git log --graph", "git log -5")
        ))
        
        allCommands.add(GitCommand(
            "git diff",
            "Değişiklikleri karşılaştırır",
            "history",
            "git diff",
            "Çalışma dizini, staging veya commit'ler arasındaki farkları gösterir.",
            listOf("git diff", "git diff --staged", "git diff HEAD~1")
        ))
        
        allCommands.add(GitCommand(
            "git show",
            "Commit detaylarını gösterir",
            "history",
            "git show <commit>",
            "Belirli bir commit'in detaylarını ve değişikliklerini gösterir.",
            listOf("git show HEAD", "git show abc123")
        ))
        
        allCommands.add(GitCommand(
            "git blame",
            "Dosya satırlarının kim tarafından değiştirildiğini gösterir",
            "history",
            "git blame <dosya>",
            "Her satırın son kez kim tarafından değiştirildiğini gösterir.",
            listOf("git blame file.kt", "git blame -L 10,20 file.kt")
        ))
        
        // Undo Commands
        allCommands.add(GitCommand(
            "git reset",
            "Commit'leri geri alır",
            "undo",
            "git reset <commit>",
            "HEAD'i belirli bir commit'e taşır, değişiklikleri korur veya siler.",
            listOf("git reset HEAD~1", "git reset --soft HEAD~1", "git reset --hard HEAD~1")
        ))
        
        allCommands.add(GitCommand(
            "git revert",
            "Commit'i tersine çevirir",
            "undo",
            "git revert <commit>",
            "Belirli bir commit'in yaptığı değişiklikleri geri alan yeni bir commit oluşturur.",
            listOf("git revert abc123", "git revert HEAD")
        ))
        
        allCommands.add(GitCommand(
            "git stash",
            "Değişiklikleri geçici olarak saklar",
            "undo",
            "git stash",
            "Commit edilmemiş değişiklikleri geçici olarak saklar.",
            listOf("git stash", "git stash pop", "git stash list", "git stash drop")
        ))
        
        allCommands.add(GitCommand(
            "git restore",
            "Dosyaları geri yükler",
            "undo",
            "git restore <dosya>",
            "Dosyaları önceki durumuna geri yükler.",
            listOf("git restore file.kt", "git restore --staged file.kt")
        ))
        
        // Remote Commands
        allCommands.add(GitCommand(
            "git remote",
            "Uzak depo ayarları",
            "remote",
            "git remote <komut>",
            "Uzak depo bağlantılarını yönetir.",
            listOf("git remote -v", "git remote add origin <url>", "git remote remove origin")
        ))
        
        allCommands.add(GitCommand(
            "git fetch",
            "Uzak değişiklikleri indirir (birleştirmez)",
            "remote",
            "git fetch <remote>",
            "Uzak depodaki değişiklikleri indirir ama birleştirmez.",
            listOf("git fetch origin", "git fetch --all")
        ))
        
        // Tags
        allCommands.add(GitCommand(
            "git tag",
            "Tag (etiket) işlemleri",
            "tag",
            "git tag <isim>",
            "Belirli commit'lere isim vermek için tag kullanılır (genellikle sürümler için).",
            listOf("git tag v1.0.0", "git tag -a v1.0.0 -m \"Version 1\"", "git push --tags")
        ))
        
        // Advanced
        allCommands.add(GitCommand(
            "git cherry-pick",
            "Belirli commit'i kopyalar",
            "advanced",
            "git cherry-pick <commit>",
            "Başka bir branch'ten belirli bir commit'i mevcut branch'e uygular.",
            listOf("git cherry-pick abc123", "git cherry-pick --no-commit abc123")
        ))
        
        allCommands.add(GitCommand(
            "git bisect",
            "Bug aramak için ikili arama",
            "advanced",
            "git bisect <start|good|bad>",
            "Bir hatanın hangi commit'te ortaya çıktığını bulmak için ikili arama yapar.",
            listOf("git bisect start", "git bisect good abc123", "git bisect bad")
        ))
        
        allCommands.add(GitCommand(
            "git reflog",
            "HEAD geçmişini gösterir",
            "advanced",
            "git reflog",
            "HEAD'in tüm hareketlerini gösterir, kayıp commit'leri bulmaya yardımcı olur.",
            listOf("git reflog", "git reflog show HEAD")
        ))
        
        filteredCommands.addAll(allCommands)
    }
    
    private fun setupAdapter() {
        adapter = GitCommandAdapter(filteredCommands) { command ->
            showCommandDetails(command)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }
    
    private fun setupCategoryChips() {
        val categories = listOf(
            "Tümü" to "all",
            "Temel" to "basic",
            "Branch" to "branch",
            "Geçmiş" to "history",
            "Geri Al" to "undo",
            "Uzak" to "remote",
            "Tag" to "tag",
            "İleri" to "advanced"
        )
        
        categories.forEachIndexed { index, (name, category) ->
            val chip = Chip(this).apply {
                text = name
                isCheckable = true
                isChecked = index == 0
                setOnClickListener {
                    filterByCategory(category)
                    // Uncheck others
                    for (i in 0 until chipGroupCategories.childCount) {
                        val c = chipGroupCategories.getChildAt(i) as Chip
                        c.isChecked = c == this
                    }
                }
            }
            chipGroupCategories.addView(chip)
        }
    }
    
    private fun setupSearch() {
        btnSearch.setOnClickListener {
            val query = searchView.text.toString().trim()
            filterBySearch(query)
        }
        
        searchView.setOnEditorActionListener { _, _, _ ->
            val query = searchView.text.toString().trim()
            filterBySearch(query)
            true
        }
    }
    
    private fun filterByCategory(category: String) {
        filteredCommands.clear()
        if (category == "all") {
            filteredCommands.addAll(allCommands)
        } else {
            filteredCommands.addAll(allCommands.filter { it.category == category })
        }
        adapter.notifyDataSetChanged()
    }
    
    private fun filterBySearch(query: String) {
        filteredCommands.clear()
        if (query.isBlank()) {
            filteredCommands.addAll(allCommands)
        } else {
            filteredCommands.addAll(allCommands.filter { 
                it.name.contains(query, ignoreCase = true) ||
                it.shortDescription.contains(query, ignoreCase = true) ||
                it.longDescription.contains(query, ignoreCase = true)
            })
        }
        adapter.notifyDataSetChanged()
    }
    
    private fun showCommandDetails(command: GitCommand) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_git_command, null)
        
        dialogView.findViewById<TextView>(R.id.tvCommandName).text = command.name
        dialogView.findViewById<TextView>(R.id.tvSyntax).text = command.syntax
        dialogView.findViewById<TextView>(R.id.tvDescription).text = command.longDescription
        dialogView.findViewById<TextView>(R.id.tvExamples).text = command.examples.joinToString("\n")
        
        dialogView.findViewById<Button>(R.id.btnCopy).setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Git Command", command.syntax)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
        }
        
        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton(R.string.close, null)
            .show()
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
    
    // Data class
    data class GitCommand(
        val name: String,
        val shortDescription: String,
        val category: String,
        val syntax: String,
        val longDescription: String,
        val examples: List<String>
    )
    
    // Adapter
    inner class GitCommandAdapter(
        private val commands: List<GitCommand>,
        private val onClick: (GitCommand) -> Unit
    ) : RecyclerView.Adapter<GitCommandAdapter.ViewHolder>() {
        
        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvName: TextView = view.findViewById(R.id.tvCommandName)
            val tvDescription: TextView = view.findViewById(R.id.tvDescription)
            val tvCategory: TextView = view.findViewById(R.id.tvCategory)
            val btnCopy: ImageButton = view.findViewById(R.id.btnCopy)
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_git_command, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val command = commands[position]
            holder.tvName.text = command.name
            holder.tvDescription.text = command.shortDescription
            holder.tvCategory.text = getCategoryLabel(command.category)
            
            holder.itemView.setOnClickListener { onClick(command) }
            
            holder.btnCopy.setOnClickListener {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Git Command", command.syntax)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this@GitHelperActivity, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
            }
        }
        
        override fun getItemCount() = commands.size
        
        private fun getCategoryLabel(category: String): String {
            return when (category) {
                "basic" -> "Temel"
                "branch" -> "Branch"
                "history" -> "Geçmiş"
                "undo" -> "Geri Al"
                "remote" -> "Uzak"
                "tag" -> "Tag"
                "advanced" -> "İleri"
                else -> category
            }
        }
    }
}
