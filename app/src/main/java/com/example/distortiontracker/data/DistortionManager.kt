package com.example.distortiontracker.data

import android.content.Context
import android.content.SharedPreferences

object DistortionManager {
    val DESTINATIONS = listOf(
        "DREAMING CITY",
        "SAVATHUN'S THRONE WORLD",
        "MOON",
        "EUROPA",
        "NESSUS",
        "COSMODROME",
        "EDZ"
    )

    private const val PREFS_NAME = "DistortionPrefs"
    private const val KEY_ANCHOR_TIME = "anchorTimeMillis"
    private const val KEY_ANCHOR_INDEX = "anchorIndex"
    private const val KEY_TARGET_DISTORTION = "targetDistortionIndex"
    private const val KEY_IS_5_MIN_WARNING = "is5MinWarning"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // Call this when the user calibrates the app
    fun calibrate(context: Context, currentDistortionIndex: Int) {
        val prefs = getPrefs(context)
        val currentTime = System.currentTimeMillis()
        // Round anchor time down to the top of the current hour to align with timer transitions
        val topOfHourMillis = (currentTime / (1000 * 60 * 60)) * (1000 * 60 * 60)
        prefs.edit()
            .putLong(KEY_ANCHOR_TIME, topOfHourMillis)
            .putInt(KEY_ANCHOR_INDEX, currentDistortionIndex)
            .apply()
    }

    fun isCalibrated(context: Context): Boolean {
        return getPrefs(context).contains(KEY_ANCHOR_TIME)
    }

    // Returns current distortion index based on time elapsed since anchor
    fun getCurrentDistortionIndex(context: Context): Int {
        val prefs = getPrefs(context)
        if (!prefs.contains(KEY_ANCHOR_TIME)) return 0 // Default fallback

        val anchorTime = prefs.getLong(KEY_ANCHOR_TIME, 0)
        val anchorIndex = prefs.getInt(KEY_ANCHOR_INDEX, 0)
        
        val currentTime = System.currentTimeMillis()
        val elapsedMillis = currentTime - anchorTime
        
        // Use Math.floorDiv to handle negative elapsed times just in case
        val elapsedHours = Math.floorDiv(elapsedMillis, 1000L * 60 * 60)
        
        var currentIndex = (anchorIndex + elapsedHours) % DESTINATIONS.size
        if (currentIndex < 0) currentIndex += DESTINATIONS.size
        
        return currentIndex.toInt()
    }

    fun getNextDistortionIndex(context: Context): Int {
        return (getCurrentDistortionIndex(context) + 1) % DESTINATIONS.size
    }

    // Gets the remaining time for the current distortion in milliseconds
    fun getTimeRemainingMillis(): Long {
        val currentTime = System.currentTimeMillis()
        val currentHourMillis = (currentTime / (1000 * 60 * 60)) * (1000 * 60 * 60)
        val nextHourMillis = currentHourMillis + (1000 * 60 * 60)
        return nextHourMillis - currentTime
    }

    fun setTargetDistortion(context: Context, index: Int, is5MinWarning: Boolean = false) {
        getPrefs(context).edit()
            .putInt(KEY_TARGET_DISTORTION, index)
            .putBoolean(KEY_IS_5_MIN_WARNING, is5MinWarning)
            .apply()
    }

    fun getTargetDistortion(context: Context): Int {
        return getPrefs(context).getInt(KEY_TARGET_DISTORTION, -1)
    }

    fun is5MinWarning(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_IS_5_MIN_WARNING, false)
    }
}
