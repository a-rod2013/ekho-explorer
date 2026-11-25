package com.comp350sec001group2.ekhoexplorer.Business.Cache

import android.content.Context

interface FlagStore {
    fun get(key: String): Boolean
    fun set(key: String, value: Boolean)
}

class SharedPrefsFlagStore(context: Context) : FlagStore {
    private val prefs = context.applicationContext.getSharedPreferences("ekho_flags", Context.MODE_PRIVATE)
    override fun get(key: String): Boolean = prefs.getBoolean(key, false)
    override fun set(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }
}

class PendingCacheClear(private val flags: FlagStore) {
    fun request() = flags.set(KEY, true)

    fun consume(): Boolean {
        val pending = flags.get(KEY)
        if (pending) flags.set(KEY, false)
        return pending
    }

    private companion object {
        const val KEY = "firestore_cache_clear_pending"
    }
}
