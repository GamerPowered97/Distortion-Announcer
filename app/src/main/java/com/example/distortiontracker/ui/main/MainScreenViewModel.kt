package com.example.distortiontracker.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.distortiontracker.data.DistortionManager
import com.example.distortiontracker.notification.DistortionAlarmScheduler
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainScreenViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext

    private val _uiState = MutableStateFlow(DistortionUiState())
    val uiState: StateFlow<DistortionUiState> = _uiState.asStateFlow()

    private val _timeRemaining = MutableStateFlow(0L)
    val timeRemaining: StateFlow<Long> = _timeRemaining.asStateFlow()

    init {
        updateState()
        viewModelScope.launch {
            var lastHour = -1L
            while (true) {
                val timeRemainingMillis = DistortionManager.getTimeRemainingMillis()
                _timeRemaining.value = timeRemainingMillis
                
                // Only update the static UI state when the hour changes (rotation happens)
                val currentHour = (System.currentTimeMillis() / (1000 * 60 * 60))
                if (currentHour != lastHour) {
                    lastHour = currentHour
                    updateState()
                }
                delay(1000)
            }
        }
    }

    private fun updateState() {
        val isCalibrated = DistortionManager.isCalibrated(context)
        val currentIndex = DistortionManager.getCurrentDistortionIndex(context)
        val nextIndex = DistortionManager.getNextDistortionIndex(context)
        val targetIndex = DistortionManager.getTargetDistortion(context)
        val is5MinWarning = DistortionManager.is5MinWarning(context)

        _uiState.value = DistortionUiState(
            isCalibrated = isCalibrated,
            currentDistortion = DistortionManager.DESTINATIONS[currentIndex],
            currentDistortionIndex = currentIndex,
            nextDistortion = DistortionManager.DESTINATIONS[nextIndex],
            nextDistortionIndex = nextIndex,
            targetDistortionIndex = targetIndex,
            is5MinWarning = is5MinWarning
        )
    }

    fun calibrate(index: Int) {
        DistortionManager.calibrate(context, index)
        val targetIndex = DistortionManager.getTargetDistortion(context)
        val is5Min = DistortionManager.is5MinWarning(context)
        if (targetIndex >= 0) {
            for (i in 0 until DistortionManager.DESTINATIONS.size) {
                DistortionAlarmScheduler.cancelAlarm(context, i)
            }
            if (is5Min) {
                DistortionAlarmScheduler.scheduleAlarm(context, targetIndex, true)
            } else {
                DistortionAlarmScheduler.scheduleAlarm(context, targetIndex, false)
                DistortionAlarmScheduler.scheduleAlarm(context, targetIndex, true)
            }
        }
        updateState()
    }

    fun setTarget(index: Int, is5MinWarning: Boolean = false) {
        for (i in 0 until DistortionManager.DESTINATIONS.size) {
            DistortionAlarmScheduler.cancelAlarm(context, i)
        }
        DistortionManager.setTargetDistortion(context, index, is5MinWarning)
        updateState()
        
        if (index >= 0) {
            if (is5MinWarning) {
                DistortionAlarmScheduler.scheduleAlarm(context, index, true)
            } else {
                DistortionAlarmScheduler.scheduleAlarm(context, index, false)
                DistortionAlarmScheduler.scheduleAlarm(context, index, true)
            }
        }
    }
}

data class DistortionUiState(
    val isCalibrated: Boolean = false,
    val currentDistortion: String = "",
    val currentDistortionIndex: Int = 0,
    val nextDistortion: String = "",
    val nextDistortionIndex: Int = 1,
    val targetDistortionIndex: Int = -1,
    val is5MinWarning: Boolean = false
)
