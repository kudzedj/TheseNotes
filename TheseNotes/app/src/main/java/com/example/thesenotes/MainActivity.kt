package com.example.thesenotes

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    private lateinit var editTextNote: EditText
    private lateinit var buttonSave: Button
    private lateinit var textViewSavedNote: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        // Находим элементы интерфейса по их id
        editTextNote = findViewById(R.id.editTextNote)
        buttonSave = findViewById(R.id.buttonSave)
        textViewSavedNote = findViewById(R.id.textViewSavedNote)

        // Устанавливаем обработчик нажатия на кнопку
        buttonSave.setOnClickListener {
            saveNote()
        }

        // Этот код оставляем для обработки системных панелей
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    // Функция для сохранения заметки
    private fun saveNote() {
        // Получаем текст из поля ввода
        val noteText = editTextNote.text.toString()

        // Проверяем, не пустой ли текст
        if (noteText.trim().isEmpty()) {
            // Показываем сообщение, если текст пустой
            Toast.makeText(this, "Введите текст заметки!", Toast.LENGTH_SHORT).show()
            return
        }

        // Отображаем сохраненную заметку в TextView
        textViewSavedNote.text = "Сохранено: $noteText"

        // Показываем всплывающее сообщение
        Toast.makeText(this, "Заметка сохранена!", Toast.LENGTH_SHORT).show()

        // Очищаем поле ввода
        editTextNote.text.clear()
    }
}