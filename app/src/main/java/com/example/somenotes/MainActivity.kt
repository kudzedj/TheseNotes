package com.example.somenotes

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.somenotes.adapter.NoteAdapter
import com.example.somenotes.data.Note
import com.example.somenotes.databinding.ActivityMainBinding
import com.example.somenotes.notification.NotificationHelper
import com.example.somenotes.viewmodel.NoteViewModel
import com.example.somenotes.viewmodel.NoteViewModelFactory
import com.example.somenotes.CalendarDialog
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
        
        // Enable edge-to-edge and draw behind system bars
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            )
        }
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Setup status bar spacer
        setupStatusBarSpacer()
        
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
    
    private fun setupStatusBarSpacer() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val statusBarHeight = systemBars.top
            
            binding.statusBarSpacer.layoutParams.height = statusBarHeight
            binding.statusBarSpacer.requestLayout()
            
            insets
        }
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
        
        // Setup swipe to delete
        setupSwipeToDelete()
    }
    
    private fun setupSwipeToDelete() {
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(
            0,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }
            
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val note = noteAdapter.getNoteAt(position)
                
                if (note != null) {
                    // Cancel reminder if exists
                    if (note.reminderTime != null) {
                        NotificationHelper.cancelNotification(this@MainActivity, note.id)
                    }
                    
                    // Delete note
                    viewModel.deleteNote(note)
                    
                    Toast.makeText(
                        this@MainActivity,
                        "Note deleted",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
        
        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(binding.notesRecyclerView)
    }
    
    private fun setupCalendar() {
        binding.calendarToggleButton.setOnClickListener {
            showCalendarDialog()
        }
    }
    
    private fun showCalendarDialog() {
        val calendarDialog = CalendarDialog(
            this,
            viewModel,
            this,
            onDateSelected = { selectedDate ->
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
        )
        calendarDialog.show()
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
