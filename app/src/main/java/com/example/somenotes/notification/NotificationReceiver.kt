package com.example.somenotes.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val noteId = intent.getLongExtra("note_id", -1)
        val content = intent.getStringExtra("content") ?: ""
        
        if (noteId != -1L) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            
            val mainIntent = Intent(context, com.example.somenotes.MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            
            val pendingIntent = android.app.PendingIntent.getActivity(
                context,
                noteId.toInt(),
                mainIntent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            
            val notification = NotificationCompat.Builder(context, NotificationHelper.getChannelId())
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Note Reminder")
                .setContentText(content.take(100))
                .setStyle(NotificationCompat.BigTextStyle().bigText(content))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI)
                .build()
            
            notificationManager.notify(noteId.toInt(), notification)
        }
    }
}
