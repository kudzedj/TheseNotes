package com.example.somenotes

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.somenotes.adapter.NoteAdapter
import com.example.somenotes.data.Note
import com.example.somenotes.databinding.ActivityMainBinding
import com.example.somenotes.notification.NotificationHelper
import com.example.somenotes.viewmodel.NoteViewModel
import com.example.somenotes.viewmodel.NoteViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: NoteViewModel
    private lateinit var noteAdapter: NoteAdapter
    private val PERMISSION_REQUEST_CODE = 100
    private var isCalendarFiltering = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Initialize ViewModel
        viewModel = ViewModelProvider(this, NoteViewModelFactory(application))[NoteViewModel::class.java]
        
        // Request notification permission
        requestNotificationPermission()
        
        // Create notification channel
        NotificationHelper.createNotificationChannel(this)
        
        setupRecyclerView()
        setupCalendar()
        setupFab()
        
        observeNotes()
    }
    
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    PERMISSION_REQUEST_CODE
                )
            }
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
            } else {
                Toast.makeText(this, "Notification permission is required for reminders", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun setupRecyclerView() {
        noteAdapter = NoteAdapter { note ->
            // Open note for editing
            val intent = android.content.Intent(this, NoteEditActivity::class.java)
            intent.putExtra("note_id", note.id)
            startActivity(intent)
        }
        
        binding.notesRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.notesRecyclerView.adapter = noteAdapter
    }
    
    private fun setupCalendar() {
        binding.calendarToggleButton.setOnClickListener {
            if (binding.calendarView.visibility == View.VISIBLE) {
                binding.calendarView.visibility = View.GONE
                isCalendarFiltering = false
                // Show all notes when calendar is closed
                notesJob?.cancel()
                observeNotes()
            } else {
                binding.calendarView.visibility = View.VISIBLE
            }
        }
        
        binding.calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val selectedDate = Calendar.getInstance().apply {
                set(year, month, dayOfMonth)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            
            // Filter notes by selected date
            isCalendarFiltering = true
            notesJob?.cancel()
            notesJob = lifecycleScope.launch {
                viewModel.notesWithReminders.collectLatest { notes ->
                    val filteredNotes = notes.filter { note ->
                        note.reminderTime != null && 
                        note.reminderTime >= selectedDate && 
                        note.reminderTime < selectedDate + 86400000 // Next day
                    }
                    noteAdapter.submitList(filteredNotes)
                }
            }
        }
    }
    
    private fun setupFab() {
        binding.fabAddNote.setOnClickListener {
            val intent = android.content.Intent(this, NoteEditActivity::class.java)
            startActivity(intent)
        }
    }
    
    private var notesJob: kotlinx.coroutines.Job? = null
    
    private fun observeNotes() {
        notesJob?.cancel()
        notesJob = lifecycleScope.launch {
            viewModel.allNotes.collectLatest { notes ->
                if (!isCalendarFiltering) {
                    noteAdapter.submitList(notes)
                }
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh notes when returning to the activity
        if (!isCalendarFiltering) {
            observeNotes()
        }
    }
}
