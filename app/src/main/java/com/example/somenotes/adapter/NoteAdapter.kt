package com.example.somenotes.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.somenotes.R
import com.example.somenotes.data.Note
import java.text.SimpleDateFormat
import java.util.*

class NoteAdapter(private val onNoteClick: (Note) -> Unit) :
    ListAdapter<Note, NoteAdapter.NoteViewHolder>(NoteDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_note, parent, false)
        return NoteViewHolder(view, onNoteClick)
    }
    
    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class NoteViewHolder(
        itemView: View,
        private val onNoteClick: (Note) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val contentText: TextView = itemView.findViewById(R.id.noteContent)
        private val dateText: TextView = itemView.findViewById(R.id.noteDate)
        private val reminderIndicator: android.widget.ImageView = itemView.findViewById(R.id.reminderIndicator)
        
        fun bind(note: Note) {
            contentText.text = note.content.take(100) + if (note.content.length > 100) "..." else ""
            
            val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            dateText.text = dateFormat.format(Date(note.updatedAt))
            
            reminderIndicator.visibility = if (note.reminderTime != null) View.VISIBLE else View.GONE
            
            itemView.setOnClickListener {
                onNoteClick(note)
            }
        }
    }
    
    class NoteDiffCallback : DiffUtil.ItemCallback<Note>() {
        override fun areItemsTheSame(oldItem: Note, newItem: Note): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: Note, newItem: Note): Boolean {
            return oldItem == newItem
        }
    }
}
