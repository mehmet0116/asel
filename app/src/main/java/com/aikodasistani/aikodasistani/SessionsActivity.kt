package com.aikodasistani.aikodasistani

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aikodasistani.aikodasistani.data.AppDatabase
import com.aikodasistani.aikodasistani.data.Session
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SessionsActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SessionAdapter
    private lateinit var db: AppDatabase
    private val mainCoroutineScope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sessions)

        db = AppDatabase.getDatabase(this)
        recyclerView = findViewById(R.id.recyclerViewSessions)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // FAB butonuna tıklama olayı
        findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fabNewSession).setOnClickListener {
            createNewSession()
        }

        adapter = SessionAdapter(
            sessions = emptyList(),
            onSessionClick = { session ->
                // Oturum seçildiğinde
                val intent = Intent().apply {
                    putExtra("session_id", session.id)
                }
                setResult(RESULT_OK, intent)
                finish()
            },
            onEditSession = { session ->
                // Oturumu düzenle
                editSession(session)
            },
            onDeleteSession = { session ->
                // Oturumu sil
                deleteSession(session)
            }
        )

        recyclerView.adapter = adapter

        mainCoroutineScope.launch {
            loadSessions()
        }
    }

    private suspend fun loadSessions() {
        withContext(Dispatchers.IO) {
            db.sessionDao().getAllSessions().collect { sessions ->
                withContext(Dispatchers.Main) {
                    adapter.updateSessions(sessions)
                }
            }
        }
    }

    private fun createNewSession() {
        val intent = Intent().apply {
            putExtra("new_session", true)
        }
        setResult(RESULT_OK, intent)
        finish()
    }

    private fun editSession(session: Session) {
        // Oturum adını düzenleme dialog'u göster
        val input = EditText(this)
        input.setText(session.name)
        input.hint = "Oturum adı"

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Oturum Adını Düzenle")
            .setView(input)
            .setPositiveButton("Kaydet") { dialog, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty()) {
                    mainCoroutineScope.launch {
                        withContext(Dispatchers.IO) {
                            // Oturum adını güncelle
                            val updatedSession = session.copy(name = newName)
                            db.sessionDao().updateSession(updatedSession)
                        }
                        // Listeyi yenile
                        loadSessions()
                        Toast.makeText(this@SessionsActivity, "Oturum adı güncellendi: $newName", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@SessionsActivity, "Oturum adı boş olamaz", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("İptal", null)
            .create()

        dialog.show()
    }

    private fun deleteSession(session: Session) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Oturumu Sil")
            .setMessage("'${session.name}' oturumunu silmek istediğinizden emin misiniz?")
            .setPositiveButton("Sil") { dialog, _ ->
                mainCoroutineScope.launch {
                    withContext(Dispatchers.IO) {
                        db.sessionDao().deleteSessionAndMessages(session.id)
                    }
                    // Listeyi yenile
                    loadSessions()
                    Toast.makeText(this@SessionsActivity, "Oturum silindi", Toast.LENGTH_SHORT).show()

                    // Eğer silinen oturum şu anki oturumsa, MainActivity'ye bildir
                    val intent = Intent().apply {
                        putExtra("deleted_session_id", session.id)
                    }
                    setResult(RESULT_OK, intent)
                }
            }
            .setNegativeButton("İptal", null)
            .show()
    }
}