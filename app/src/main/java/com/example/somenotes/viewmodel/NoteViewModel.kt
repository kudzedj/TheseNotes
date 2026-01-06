package com.example.somenotes.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.somenotes.data.Note
import com.example.somenotes.data.NoteDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class NoteViewModel(application: Application) : AndroidViewModel(application) {
    private val noteDao = NoteDatabase.getDatabase(application).noteDao()
    
    val allNotes: Flow<List<Note>> = noteDao.getAllNotes()
    val notesWithReminders: Flow<List<Note>> = noteDao.getNotesWithReminders()
    
    fun insertNote(note: Note, onInserted: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = noteDao.insertNote(note)
            onInserted(id)
        }
    }
    
    fun updateNote(note: Note) {
        viewModelScope.launch {
            noteDao.updateNote(note)
        }
    }
    
    fun deleteNote(note: Note) {
        viewModelScope.launch {
            noteDao.deleteNote(note)
        }
    }
    
    suspend fun getNoteById(id: Long): Note? {
        return noteDao.getNoteById(id)
    }
}
