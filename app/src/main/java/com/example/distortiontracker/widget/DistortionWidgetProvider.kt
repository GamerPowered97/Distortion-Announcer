package com.example.distortiontracker.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import com.example.distortiontracker.MainActivity
import com.example.distortiontracker.R
import com.example.distortiontracker.data.DistortionManager

class DistortionWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
        scheduleNextUpdate(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_SCHEDULED_UPDATE || 
            intent.action == Intent.ACTION_BOOT_COMPLETED || 
            intent.action == Intent.ACTION_TIME_CHANGED || 
            intent.action == Intent.ACTION_TIMEZONE_CHANGED) {
            
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, DistortionWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            if (appWidgetIds.isNotEmpty()) {
                onUpdate(context, appWidgetManager, appWidgetIds)
            }
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_layout)

        val isCalibrated = DistortionManager.isCalibrated(context)
        if (isCalibrated) {
            val currentIndex = DistortionManager.getCurrentDistortionIndex(context)
            val destination = DistortionManager.DESTINATIONS[currentIndex]
            views.setTextViewText(R.id.widget_destination_name, destination)

            // Setup real-time countdown using Chronometer
            val currentTime = System.currentTimeMillis()
            val timeRemainingMillis = DistortionManager.getTimeRemainingMillis()
            val nextHourMillis = currentTime + timeRemainingMillis

            views.setChronometer(R.id.widget_countdown, nextHourMillis, null, true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                views.setChronometerCountDown(R.id.widget_countdown, true)
            }
            
            // If the active distortion matches the target distortion, highlight it
            val targetIndex = DistortionManager.getTargetDistortion(context)
            if (currentIndex == targetIndex) {
                // Glow accent bar in amber/orange if it's the target distortion
                views.setInt(R.id.widget_accent_bar, "setBackgroundColor", android.graphics.Color.parseColor("#FFB300"))
                views.setTextColor(R.id.widget_countdown_label, android.graphics.Color.parseColor("#FF5500"))
                views.setTextViewText(R.id.widget_countdown_label, "TARGET BREACHED")
            } else {
                // Cyan accent color for standard
                views.setInt(R.id.widget_accent_bar, "setBackgroundColor", android.graphics.Color.parseColor("#FF00E5FF"))
                views.setTextColor(R.id.widget_countdown_label, android.graphics.Color.parseColor("#FFB300"))
                views.setTextViewText(R.id.widget_countdown_label, "ROTATION IN")
            }

            views.setViewVisibility(R.id.widget_countdown_label, View.VISIBLE)
            views.setViewVisibility(R.id.widget_countdown, View.VISIBLE)
        } else {
            views.setTextViewText(R.id.widget_destination_name, "Tap to Calibrate")
            views.setViewVisibility(R.id.widget_countdown_label, View.GONE)
            views.setViewVisibility(R.id.widget_countdown, View.GONE)
            // Default cyan accent bar
            views.setInt(R.id.widget_accent_bar, "setBackgroundColor", android.graphics.Color.parseColor("#FF00E5FF"))
        }

        // Open MainActivity when clicked
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun scheduleNextUpdate(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, DistortionWidgetProvider::class.java).apply {
            action = ACTION_SCHEDULED_UPDATE
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Calculate next top of the hour (rotation time)
        val currentTime = System.currentTimeMillis()
        val nextHourMillis = currentTime + DistortionManager.getTimeRemainingMillis()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, nextHourMillis, pendingIntent)
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextHourMillis, pendingIntent)
        }
    }

    companion object {
        private const val ACTION_SCHEDULED_UPDATE = "com.example.distortiontracker.widget.ACTION_SCHEDULED_UPDATE"

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, DistortionWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            if (appWidgetIds.isNotEmpty()) {
                val intent = Intent(context, DistortionWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                }
                context.sendBroadcast(intent)
            }
        }
    }
}
