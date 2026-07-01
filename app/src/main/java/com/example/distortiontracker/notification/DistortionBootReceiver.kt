package com.example.distortiontracker.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.distortiontracker.data.DistortionManager

class DistortionBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val targetDistortion = DistortionManager.getTargetDistortion(context)
            if (targetDistortion >= 0) {
                DistortionAlarmScheduler.scheduleAlarm(
                    context,
                    targetDistortion,
                    DistortionManager.is5MinWarning(context)
                )
            }
        }
    }
}
