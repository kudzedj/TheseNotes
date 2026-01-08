package com.example.somenotes

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.somenotes.data.Note
import com.example.somenotes.databinding.ActivityNoteEditBinding
import com.example.somenotes.notification.NotificationHelper
import com.example.somenotes.viewmodel.NoteViewModel
import com.example.somenotes.viewmodel.NoteViewModelFactory
import kotlinx.coroutines.launch
import java.util.*

class NoteEditActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNoteEditBinding
    private lateinit var viewModel: NoteViewModel
    private var noteId: Long? = null
    private var hasChanges = false
    private var reminderTime: Long? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge and draw behind system bars
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            )
        }
        
        binding = ActivityNoteEditBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Setup status bar spacer
        setupStatusBarSpacer()
        
        // Initialize ViewModel
        viewModel = ViewModelProvider(this, NoteViewModelFactory(application))[NoteViewModel::class.java]
        
        noteId = intent.getLongExtra("note_id", -1).takeIf { it != -1L }
        
        setupToolbar()
        setupReminderButton()
        setupSaveButton()
        setupTextWatcher()
        
        if (noteId != null) {
            loadNote()
        }
    }
    
    private fun setupStatusBarSpacer() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val statusBarHeight = systemBars.top
            
            binding.statusBarSpacer.layoutParams.height = statusBarHeight
            binding.statusBarSpacer.requestLayout()
            
            insets
        }
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = if (noteId != null) "Edit Note" else "New Note"
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
    
    private fun setupTextWatcher() {
        binding.noteEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                hasChanges = s?.isNotEmpty() == true
            }
        })
    }
    
    private fun setupReminderButton() {
        updateReminderButtonIcon()
        binding.reminderButton.setOnClickListener {
            if (reminderTime != null) {
                // Show option to change or clear reminder
                android.app.AlertDialog.Builder(this)
                    .setTitle("Reminder")
                    .setMessage("Change or remove reminder?")
                    .setPositiveButton("Change") { _, _ ->
                        showReminderPicker()
                    }
                    .setNeutralButton("Remove") { _, _ ->
                        reminderTime = null
                        updateReminderButtonIcon()
                        Toast.makeText(this, "Reminder removed", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            } else {
                showReminderPicker()
            }
        }
    }
    
    private fun updateReminderButtonIcon() {
        if (reminderTime != null) {
            binding.reminderButton.setImageResource(android.R.drawable.ic_lock_idle_alarm)
        } else {
            binding.reminderButton.setImageResource(android.R.drawable.ic_menu_recent_history)
        }
    }
    
    private fun showReminderPicker() {
        val calendar = Calendar.getInstance()
        if (reminderTime != null) {
            calendar.timeInMillis = reminderTime!!
        }
        
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                
                TimePickerDialog(
                    this,
                    { _, hourOfDay, minute ->
                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        calendar.set(Calendar.MINUTE, minute)
                        calendar.set(Calendar.SECOND, 0)
                        calendar.set(Calendar.MILLISECOND, 0)
                        
                        reminderTime = calendar.timeInMillis
                        
                        if (reminderTime!! < System.currentTimeMillis()) {
                            Toast.makeText(this, "Please select a future date and time", Toast.LENGTH_SHORT).show()
                            reminderTime = null
                            updateReminderButtonIcon()
                        } else {
                            updateReminderButtonIcon()
                            Toast.makeText(this, "Reminder set", Toast.LENGTH_SHORT).show()
                        }
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true
                ).show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
    
    private fun setupSaveButton() {
        binding.saveButton.setOnClickListener {
            saveNote()
        }
    }
    
    private fun loadNote() {
        lifecycleScope.launch {
            noteId?.let { id ->
                val note = viewModel.getNoteById(id)
                note?.let {
                    binding.noteEditText.setText(it.content)
                    reminderTime = it.reminderTime
                    updateReminderButtonIcon()
                    hasChanges = true
                }
            }
        }
    }
    
    private fun saveNote() {
        val content = binding.noteEditText.text.toString().trim()
        
        if (content.isEmpty()) {
            Toast.makeText(this, "Note cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }
        
        lifecycleScope.launch {
            if (noteId != null) {
                // Update existing note
                val existingNote = viewModel.getNoteById(noteId!!)
                if (existingNote != null) {
                    // Cancel old reminder if it existed
                    if (existingNote.reminderTime != null) {
                        NotificationHelper.cancelNotification(this@NoteEditActivity, noteId!!)
                    }
                    
                    val updatedNote = existingNote.copy(
                        content = content,
                        updatedAt = System.currentTimeMillis(),
                        reminderTime = reminderTime
                    )
                    viewModel.updateNote(updatedNote)
                    
                    // Schedule new reminder if set
                    if (reminderTime != null && reminderTime!! > System.currentTimeMillis()) {
                        NotificationHelper.scheduleNotification(
                            this@NoteEditActivity,
                            noteId!!,
                            content,
                            reminderTime!!
                        )
                    }
                    
                    runOnUiThread {
                        Toast.makeText(this@NoteEditActivity, "Note saved", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            } else {
                // Create new note
                val newNote = Note(
                    content = content,
                    reminderTime = reminderTime
                )
                
                viewModel.insertNote(newNote) { id ->
                    // Schedule new reminder if set
                    if (reminderTime != null && reminderTime!! > System.currentTimeMillis()) {
                        NotificationHelper.scheduleNotification(
                            this@NoteEditActivity,
                            id,
                            content,
                            reminderTime!!
                        )
                    }
                    
                    runOnUiThread {
                        Toast.makeText(this@NoteEditActivity, "Note saved", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            }
        }
    }
    
    override fun onBackPressed() {
        val content = binding.noteEditText.text.toString().trim()
        
        if (hasChanges && content.isNotEmpty()) {
            saveNote()
        } else {
            super.onBackPressed()
        }
    }
}
