package com.example.distortiontracker.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.distortiontracker.data.DistortionManager

object DistortionAlarmScheduler {

    fun scheduleAlarm(context: Context, targetIndex: Int, is5MinuteWarning: Boolean = false, fromTriggerTime: Long? = null) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        // Offset by 20 or 5 mins
        val offsetMillis = if (is5MinuteWarning) 5 * 60 * 1000L else 20 * 60 * 1000L
        
        val targetTimeMillis = if (fromTriggerTime != null) {
            fromTriggerTime + DistortionManager.DESTINATIONS.size * 60 * 60 * 1000L
        } else {
            // Calculate time until target distortion
            val currentIndex = DistortionManager.getCurrentDistortionIndex(context)
            val timeRemainingMillis = DistortionManager.getTimeRemainingMillis()
            
            var hourDiff = targetIndex - currentIndex
            if (hourDiff <= 0) hourDiff += DistortionManager.DESTINATIONS.size
            
            // If it's currently active, the NEXT one is N hours away
            if (targetIndex == currentIndex && timeRemainingMillis > 0) {
                hourDiff = DistortionManager.DESTINATIONS.size
            }

            // Total time until target starts
            val calculatedTargetTime = System.currentTimeMillis() + timeRemainingMillis + ((hourDiff - 1) * 60 * 60 * 1000L)
            
            // Safety check: if trigger time is in the past, shift to the next cycle (7 hours later)
            if (calculatedTargetTime - offsetMillis <= System.currentTimeMillis()) {
                calculatedTargetTime + DistortionManager.DESTINATIONS.size * 60 * 60 * 1000L
            } else {
                calculatedTargetTime
            }
        }
        
        val triggerAtMillis = targetTimeMillis - offsetMillis

        val intent = Intent(context, DistortionAlarmReceiver::class.java).apply {
            putExtra("DISTORTION_NAME", DistortionManager.DESTINATIONS[targetIndex])
            putExtra("TIME_REMAINING_MINS", if (is5MinuteWarning) 5 else 20)
            putExtra("TARGET_TIME_MILLIS", targetTimeMillis)
            putExtra("TARGET_INDEX", targetIndex)
            putExtra("IS_5_MINUTE_WARNING", is5MinuteWarning)
        }
        
        val requestCode = targetIndex + if (is5MinuteWarning) 100 else 0
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }
    
    fun cancelAlarm(context: Context, targetIndex: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, DistortionAlarmReceiver::class.java)
        
        // Cancel 20-minute alarm
        val pending20 = PendingIntent.getBroadcast(
            context,
            targetIndex,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pending20 != null) {
            alarmManager.cancel(pending20)
            pending20.cancel()
        }

        // Cancel 5-minute alarm
        val pending5 = PendingIntent.getBroadcast(
            context,
            targetIndex + 100,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pending5 != null) {
            alarmManager.cancel(pending5)
            pending5.cancel()
        }
    }
}
