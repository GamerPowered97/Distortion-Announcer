package com.example.distortiontracker.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.distortiontracker.R

class DistortionAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val distortionName = intent.getStringExtra("DISTORTION_NAME") ?: "Unknown"
        val timeRemaining = intent.getIntExtra("TIME_REMAINING_MINS", 20)
        val targetTimeMillis = intent.getLongExtra("TARGET_TIME_MILLIS", System.currentTimeMillis() + (timeRemaining * 60 * 1000L))

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val targetIndex = intent.getIntExtra("TARGET_INDEX", -1)
        val is5MinuteWarning = intent.getBooleanExtra("IS_5_MINUTE_WARNING", false)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "distortion_channel",
                "Distortion Alerts",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val title = if (is5MinuteWarning) {
            "BREACH DETECTED"
        } else {
            "Distortion Imminent"
        }

        val text = if (is5MinuteWarning) {
            "Distortion opening at $distortionName! Transmat immediately, Guardian!"
        } else {
            "Eyes up, Guardian. $distortionName Distortion starts in 20 minutes!"
        }

        val notificationBuilder = NotificationCompat.Builder(context, "distortion_channel")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setUsesChronometer(true)
            .setWhen(targetTimeMillis)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            notificationBuilder.setChronometerCountDown(true)
        }

        notificationManager.notify(targetIndex + if (is5MinuteWarning) 100 else 0, notificationBuilder.build())

        // Reschedule for the next cycle (7 hours later) if this is still the active target
        val currentTarget = com.example.distortiontracker.data.DistortionManager.getTargetDistortion(context)
        if (currentTarget == targetIndex && targetIndex >= 0) {
            DistortionAlarmScheduler.scheduleAlarm(context, targetIndex, is5MinuteWarning, targetTimeMillis)
        }
    }
}
