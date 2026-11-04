package com.comp350sec001group2.ekhoexplorer.Business.ImageService

/**
 * Where uploaded images are kept.
 */
interface ImageStore {
    /**
     * Stores [bytes] under a new unique name inside [folder] and returns the permanent URL.
     * Never overwrites an existing image.
     */
    suspend fun upload(folder: String, bytes: ByteArray, contentType: String): String

    /** Fetches the bytes behind one of this store's own URLs. Rejects any other URL up front. */
    suspend fun download(url: String, maxBytes: Long): ByteArray

    /** True if [url] points at an image in this store. */
    fun owns(url: String): Boolean
}
