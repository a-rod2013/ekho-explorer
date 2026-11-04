package com.comp350sec001group2.ekhoexplorer.Business.Photos

import android.util.Log
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreService
import com.comp350sec001group2.ekhoexplorer.Business.Database.Queries.GetPostPhotoQuery
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.cancellation.CancellationException

fun interface PhotoLoader {
    /** WebP bytes, or null if the post has no such photo. Throws on a failed read. */
    suspend fun load(postId: String, index: Int): ByteArray?
}

object NoPhotoLoader : PhotoLoader {
    override suspend fun load(postId: String, index: Int): ByteArray? = null
}

fun interface PhotoStore {
    /** The WebP bytes of the photo, or null if the post has no such photo. Throws on a failed read. */
    suspend fun read(postId: String, index: Int): ByteArray?
}

class FirestorePhotoStore(private val service: FirestoreService) : PhotoStore {
    override suspend fun read(postId: String, index: Int): ByteArray? =
        service.execute(GetPostPhotoQuery(postId, index))?.bytes
}

interface PhotoRepository : PhotoLoader {
    /** Drops cached copies of a post's photos, after the post was deleted. */
    suspend fun forget(postId: String)

    /** Drops every cached photo. */
    suspend fun clear()
}

class CachedPhotoRepository(
    private val store: PhotoStore,
    private val cache: PhotoCache = NoPhotoCache,
) : PhotoRepository {

    private val locks = ConcurrentHashMap<String, Mutex>()

    override suspend fun load(postId: String, index: Int): ByteArray? {
        cacheGet(postId, index)?.let { return it }

        val lock = locks.getOrPut("$postId/$index") { Mutex() }
        return lock.withLock {
            cacheGet(postId, index) ?: run {
                store.read(postId, index)?.also { cachePut(postId, index, it) }
            }
        }
    }

    override suspend fun forget(postId: String) {
        try {
            cache.remove(postId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Photo cache remove failed", e)
        }
    }

    override suspend fun clear() {
        try {
            cache.clear()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Photo cache clear failed", e)
        }
    }

    private suspend fun cacheGet(postId: String, index: Int): ByteArray? =
        try {
            cache.get(postId, index)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Photo cache read failed", e)
            null
        }

    private suspend fun cachePut(postId: String, index: Int, bytes: ByteArray) {
        try {
            cache.put(postId, index, bytes)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Photo cache write failed", e)
        }
    }

    private companion object { const val TAG = "PhotoRepository" }
}
