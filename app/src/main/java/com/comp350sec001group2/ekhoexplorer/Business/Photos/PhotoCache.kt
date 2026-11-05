package com.comp350sec001group2.ekhoexplorer.Business.Photos

interface PhotoCache {
    /** The cached WebP bytes of photo [index] of post [postId], or null if not cached. */
    suspend fun get(postId: String, index: Int): ByteArray?

    /** Stores the bytes of a photo that was just read. Replacing an existing entry is fine. */
    suspend fun put(postId: String, index: Int, bytes: ByteArray)

    /** Forgets all photos of [postId]. */
    suspend fun remove(postId: String)

    /** Forgets everything. */
    suspend fun clear()
}

/** The default: caches nothing. */
object NoPhotoCache : PhotoCache {
    override suspend fun get(postId: String, index: Int): ByteArray? = null
    override suspend fun put(postId: String, index: Int, bytes: ByteArray) = Unit
    override suspend fun remove(postId: String) = Unit
    override suspend fun clear() = Unit
}
