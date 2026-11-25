package com.comp350sec001group2.ekhoexplorer.Business.Cache

class TtlTextCache(
    private val store: ByteCache,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    fun get(ttlMillis: Long, vararg key: String): String? {
        val raw = store.get(*key)?.toString(Charsets.UTF_8) ?: return null
        val newline = raw.indexOf('\n')
        if (newline < 0) return null
        val stamp = raw.substring(0, newline).toLongOrNull() ?: return null
        return if (clock() - stamp <= ttlMillis) raw.substring(newline + 1) else null
    }

    fun put(text: String, vararg key: String) {
        store.put("${clock()}\n$text".toByteArray(Charsets.UTF_8), *key)
    }
}
