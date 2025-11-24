package com.aikodasistani.aikodasistani

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aikodasistani.aikodasistani.data.Session
import java.text.SimpleDateFormat
import java.util.*

class SessionAdapter(
    private var sessions: List<Session>,
    private val onSessionClick: (Session) -> Unit,
    private val onEditSession: (Session) -> Unit,
    private val onDeleteSession: (Session) -> Unit
) : RecyclerView.Adapter<SessionAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textViewName: TextView = view.findViewById(R.id.textViewSessionName)
        val textViewDate: TextView = view.findViewById(R.id.textViewSessionDate)
        val editButton: ImageButton = view.findViewById(R.id.editButton)
        val deleteButton: ImageButton = view.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_session, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val session = sessions[position]
        holder.textViewName.text = session.name

        // Timestamp'i formatla
        val date = Date(session.createdAt)
        val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        holder.textViewDate.text = formatter.format(date)

        // Tıklama olayları
        holder.itemView.setOnClickListener {
            onSessionClick(session)
        }

        holder.editButton.setOnClickListener {
            onEditSession(session)
        }

        holder.deleteButton.setOnClickListener {
            onDeleteSession(session)
        }
    }

    override fun getItemCount() = sessions.size

    fun updateSessions(newSessions: List<Session>) {
        sessions = newSessions
        notifyDataSetChanged()
    }
}