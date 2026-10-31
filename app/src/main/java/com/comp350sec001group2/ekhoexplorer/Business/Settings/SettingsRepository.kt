package com.comp350sec001group2.ekhoexplorer.Business.Settings

import android.content.Context
import com.comp350sec001group2.ekhoexplorer.Business.Cache.EhkoCache

/**
 * Reads and changes the active user's settings. The values live in [EhkoCache.activeUser].settings,
 * so screens read them from the cache and call this to change them.
 */
interface SettingsRepository {
    /** Fills the cache from storage. Call right after the active user is set (sign-in). */
    fun load()

    fun setNotificationsEnabled(enabled: Boolean)
}

/** Stores settings on the device, one entry per user. */
class SharedPrefsSettingsRepository(context: Context) : SettingsRepository {

    private val prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    override fun load() {
        val user = EhkoCache.activeUser
        if (!user.isSignedIn) return
        val enabled = prefs.getBoolean(notificationsKey(user.userId), true)
        EhkoCache.updateActiveUser { it.copy(settings = it.settings.copy(notificationsEnabled = enabled)) }
    }

    override fun setNotificationsEnabled(enabled: Boolean) {
        val user = EhkoCache.activeUser
        if (!user.isSignedIn) return
        EhkoCache.updateActiveUser { it.copy(settings = it.settings.copy(notificationsEnabled = enabled)) }
        prefs.edit().putBoolean(notificationsKey(user.userId), enabled).apply()
    }

    private fun notificationsKey(userId: String) = "notificationsEnabled_$userId"

    private companion object { const val FILE = "ekho_settings" }
}
