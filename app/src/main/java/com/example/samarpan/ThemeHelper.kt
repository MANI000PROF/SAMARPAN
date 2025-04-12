package com.example.samarpan

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

object ThemeHelper {
    const val PREF_NAME = "theme_pref"
    const val KEY_THEME = "app_theme"

    const val THEME_LIGHT = 1
    const val THEME_DARK = 2
    const val THEME_SYSTEM = 0

    fun applyTheme(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        when (prefs.getInt(KEY_THEME, THEME_SYSTEM)) {
            THEME_LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            THEME_DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            THEME_SYSTEM -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    fun setTheme(context: Context, themeMode: Int) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putInt(KEY_THEME, themeMode).apply()
        applyTheme(context)
    }

    fun getCurrentTheme(context: Context): Int {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_THEME, THEME_SYSTEM)
    }
}
