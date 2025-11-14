package com.andrewbibire.chessanalysis.online

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set

object UserPreferences {
    private val settings: Settings = Settings()
    private const val KEY_LAST_USERNAME = "last_username"
    private const val KEY_ANALYSIS_DEPTH = "analysis_depth"

    fun saveLastUsername(username: String) {
        settings[KEY_LAST_USERNAME] = username
    }
    fun getLastUsername(): String? {
        return settings.getStringOrNull(KEY_LAST_USERNAME)
    }

    fun saveAnalysisDepth(depth: Int) {
        settings[KEY_ANALYSIS_DEPTH] = depth
    }
    fun getAnalysisDepth(): Int {
        return settings.getInt(KEY_ANALYSIS_DEPTH, 14) // Default to 14
    }
}
