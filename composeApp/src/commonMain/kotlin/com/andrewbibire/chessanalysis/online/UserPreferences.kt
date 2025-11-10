package com.andrewbibire.chessanalysis.online

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set

object UserPreferences {
    private val settings: Settings = Settings()
    private const val KEY_LAST_USERNAME = "last_username"
    fun saveLastUsername(username: String) {
        settings[KEY_LAST_USERNAME] = username
    }
    fun getLastUsername(): String? {
        return settings.getStringOrNull(KEY_LAST_USERNAME)
    }
}
