package com.chifuz.enfermerapp.utils

import android.content.Context

object PrefsManager {
    private const val PREFS_NAME = "enfermerapp_prefs"
    private const val KEY_CALC_COUNT = "calculation_count"
    private const val KEY_DARK_MODE = "dark_mode"
    private const val KEY_LANG = "language"

    fun incrementCalculationCount(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentCount = prefs.getInt(KEY_CALC_COUNT, 0) + 1
        prefs.edit().putInt(KEY_CALC_COUNT, currentCount).apply()
        return currentCount
    }

    fun isDarkMode(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_DARK_MODE, false) // Falso por defecto (Siempre claro)
    }

    fun setDarkMode(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_DARK_MODE, enabled).apply()
    }

    fun getLang(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LANG, "es") ?: "es"
    }

    fun setLang(context: Context, lang: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_LANG, lang).apply()
    }
}