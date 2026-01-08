package com.example.somenotes

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.Window
import android.widget.CalendarView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.somenotes.data.Note
import com.example.somenotes.databinding.DialogCalendarBinding
import com.example.somenotes.viewmodel.NoteViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class CalendarDialog(
    context: Context,
    private val viewModel: NoteViewModel,
    private val lifecycleOwner: LifecycleOwner,
    private val onDateSelected: (Long) -> Unit
) : Dialog(context) {

    private lateinit var binding: DialogCalendarBinding
    private val reminderDates = mutableSetOf<Long>()
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        binding = DialogCalendarBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.9).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
        
        setupCalendar()
        observeReminders()
    }

    private fun setupCalendar() {
        // Customize calendar appearance - white background
        binding.dialogCalendarView.setBackgroundColor(Color.WHITE)
        
        // Set calendar background to white
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            binding.dialogCalendarView.background = ColorDrawable(Color.WHITE)
        }
        
        binding.dialogCalendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val selectedDate = Calendar.getInstance().apply {
                set(year, month, dayOfMonth)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            
            onDateSelected(selectedDate)
            dismiss()
        }
    }

    private fun observeReminders() {
        lifecycleOwner.lifecycleScope.launch {
            viewModel.notesWithReminders.collectLatest { notes ->
                reminderDates.clear()
                notes.forEach { note ->
                    note.reminderTime?.let { reminderTime ->
                        val calendar = Calendar.getInstance().apply {
                            timeInMillis = reminderTime
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        reminderDates.add(calendar.timeInMillis)
                    }
                }
                updateReminderDatesList(notes)
            }
        }
    }

    private fun updateReminderDatesList(notes: List<Note>) {
        val datesWithReminders = notes
            .filter { it.reminderTime != null }
            .map { note ->
                val calendar = Calendar.getInstance().apply {
                    timeInMillis = note.reminderTime!!
                }
                Pair(calendar.timeInMillis, dateFormat.format(calendar.time))
            }
            .distinctBy { it.first }
            .sortedBy { it.first }
            .map { it.second }

        val adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val view = android.view.LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_reminder_date, parent, false)
                return object : RecyclerView.ViewHolder(view) {}
            }

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val textView = holder.itemView as android.widget.TextView
                textView.text = datesWithReminders[position]
                textView.setBackgroundColor(Color.parseColor("#FFFF00")) // Yellow background
            }

            override fun getItemCount() = datesWithReminders.size
        }

        binding.reminderDatesRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.reminderDatesRecyclerView.adapter = adapter
        
        // Show/hide the list and label based on whether there are reminders
        val hasReminders = datesWithReminders.isNotEmpty()
        binding.reminderDatesRecyclerView.visibility = 
            if (hasReminders) android.view.View.VISIBLE else android.view.View.GONE
        binding.reminderDatesLabel.visibility = 
            if (hasReminders) android.view.View.VISIBLE else android.view.View.GONE
    }
}
