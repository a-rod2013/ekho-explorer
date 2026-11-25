package com.comp350sec001group2.ekhoexplorer.Business.Cache

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun ByteCache.cached(
    vararg key: String,
    io: CoroutineDispatcher = Dispatchers.IO,
    load: suspend () -> ByteArray?,
): ByteArray? {
    withContext(io) { get(*key) }?.let { return it }
    val loaded = load() ?: return null
    withContext(io) { put(loaded, *key) }
    return loaded
}

