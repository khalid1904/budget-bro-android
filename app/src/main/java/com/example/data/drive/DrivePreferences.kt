package com.example.data.drive

import android.content.Context
import android.content.SharedPreferences

class DrivePreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("drive_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_USER_EMAIL = "drive_user_email"
        private const val KEY_LAST_BACKUP_TIME = "drive_last_backup_time"
        private const val KEY_PERIODIC_BACKUP_ENABLED = "drive_periodic_backup_enabled"
        private const val KEY_PERIODIC_FREQUENCY = "drive_periodic_frequency" // DAILY, WEEKLY, MONTHLY
    }

    var userEmail: String?
        get() = prefs.getString(KEY_USER_EMAIL, null)
        set(value) = prefs.edit().putString(KEY_USER_EMAIL, value).apply()

    var lastBackupTime: Long
        get() = prefs.getLong(KEY_LAST_BACKUP_TIME, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_BACKUP_TIME, value).apply()

    var isPeriodicBackupEnabled: Boolean
        get() = prefs.getBoolean(KEY_PERIODIC_BACKUP_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_PERIODIC_BACKUP_ENABLED, value).apply()

    var periodicFrequency: String
        get() = prefs.getString(KEY_PERIODIC_FREQUENCY, "DAILY") ?: "DAILY"
        set(value) = prefs.edit().putString(KEY_PERIODIC_FREQUENCY, value).apply()

    fun clear() {
        prefs.edit().clear().apply()
    }
}
