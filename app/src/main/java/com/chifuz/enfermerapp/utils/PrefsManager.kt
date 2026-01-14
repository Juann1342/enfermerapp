package com.chifuz.enfermerapp.utils

import android.content.Context

object PrefsManager {
    private const val PREFS_NAME = "enfermerapp_prefs"
    private const val KEY_CALC_COUNT = "calculation_count"

    fun incrementCalculationCount(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentCount = prefs.getInt(KEY_CALC_COUNT, 0) + 1
        prefs.edit().putInt(KEY_CALC_COUNT, currentCount).apply()
        return currentCount
    }
}
