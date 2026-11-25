package com.comp350sec001group2.ekhoexplorer.Business.Photos

import com.comp350sec001group2.ekhoexplorer.Business.Cache.ByteCache
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DiskPhotoCache(
    private val store: ByteCache,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : PhotoCache {

    override suspend fun get(postId: String, index: Int): ByteArray? =
        withContext(io) { store.get("posts", postId, index.toString()) }

    override suspend fun put(postId: String, index: Int, bytes: ByteArray) =
        withContext(io) { store.put(bytes, "posts", postId, index.toString()) }

    override suspend fun remove(postId: String) =
        withContext(io) { store.remove("posts", postId) }

    override suspend fun clear() =
        withContext(io) { store.remove("posts") }
}
