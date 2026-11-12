package com.example.animator

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

object PalettePrefs {
    const val KEY_PALETTE = "palette"
    const val KEY_RECENT = "recent_colors"
    private const val PREFS = "animator_prefs"

    fun loadColors(context: Context, key: String, default: List<Color>): List<Color> {
        val s = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(key, null)
            ?: return default
        val list = s.split(",").mapNotNull { it.toIntOrNull()?.let { v -> Color(v) } }
        return if (list.isEmpty() && key == KEY_PALETTE) default else list
    }

    fun saveColors(context: Context, key: String, colors: List<Color>) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(key, colors.joinToString(",") { it.toArgb().toString() })
            .apply()
    }
}