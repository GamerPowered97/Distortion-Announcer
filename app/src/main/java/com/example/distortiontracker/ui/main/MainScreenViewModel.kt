package com.example.distortiontracker.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.distortiontracker.data.DistortionManager
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

        _uiState.value = DistortionUiState(
            isCalibrated = isCalibrated,
            currentDistortion = DistortionManager.DESTINATIONS[currentIndex],
            currentDistortionIndex = currentIndex,
            nextDistortion = DistortionManager.DESTINATIONS[nextIndex],
            nextDistortionIndex = nextIndex,
            targetDistortionIndex = targetIndex
        )
    }

    fun calibrate(index: Int) {
        DistortionManager.calibrate(context, index)
        val targetIndex = DistortionManager.getTargetDistortion(context)
        val is5Min = DistortionManager.is5MinWarning(context)
        if (targetIndex >= 0) {
            for (i in 0 until 7) {
                com.example.distortiontracker.notification.DistortionAlarmScheduler.cancelAlarm(context, i)
            }
            com.example.distortiontracker.notification.DistortionAlarmScheduler.scheduleAlarm(context, targetIndex, is5Min)
        }
        updateState()
    }

    fun setTarget(index: Int, is5MinWarning: Boolean = false) {
        for (i in 0 until 7) {
            com.example.distortiontracker.notification.DistortionAlarmScheduler.cancelAlarm(context, i)
        }
        DistortionManager.setTargetDistortion(context, index, is5MinWarning)
        updateState()
        
        if (index >= 0) {
            com.example.distortiontracker.notification.DistortionAlarmScheduler.scheduleAlarm(context, index, is5MinWarning)
        }
    }
}

data class DistortionUiState(
    val isCalibrated: Boolean = false,
    val currentDistortion: String = "",
    val currentDistortionIndex: Int = 0,
    val nextDistortion: String = "",
    val nextDistortionIndex: Int = 1,
    val targetDistortionIndex: Int = -1
)
