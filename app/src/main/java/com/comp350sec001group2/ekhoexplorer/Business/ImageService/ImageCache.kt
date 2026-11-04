package com.comp350sec001group2.ekhoexplorer.Business.ImageService

import java.io.File
import java.io.IOException
import java.security.MessageDigest

/**
 * Disk cache of downloaded images, one file per URL.
 *
 * Methods block on disk I/O; call them off the main thread.
 */
class ImageCache(
    private val dir: File,
    private val maxBytes: Long = 50L * 1024 * 1024,
) {
    private val lock = Any()

    init {
        require(maxBytes > 0) { "maxBytes must be positive" }
        dir.mkdirs()
        // Leftovers from a crash in the middle of a write.
        dir.listFiles { f -> f.name.endsWith(TEMP_SUFFIX) }?.forEach { it.delete() }
    }

    /** The cached file for [key]. */
    fun get(key: String): File? = synchronized(lock) {
        val file = fileFor(key)
        if (file.isFile) {
            file.setLastModified(System.currentTimeMillis())
            file
        } else {
            null
        }
    }

    /** Stores [bytes] under [key] and returns the file. May evict older entries. */
    fun put(key: String, bytes: ByteArray): File = synchronized(lock) {
        val target = fileFor(key)
        val temp = File(dir, "$key$TEMP_SUFFIX")
        temp.writeBytes(bytes)
        if (!temp.renameTo(target)) {
            target.delete()
            if (!temp.renameTo(target)) {
                temp.delete()
                throw IOException("Could not store the image in the cache")
            }
        }
        trim(keep = target)
        target
    }

    /** Total bytes in cache */
    fun sizeBytes(): Long = synchronized(lock) { entries().sumOf { it.length() } }

    fun clear() = synchronized(lock) { entries().forEach { it.delete() } }

    private fun fileFor(key: String) = File(dir, "$key$ENTRY_SUFFIX")

    private fun entries(): List<File> =
        dir.listFiles { f -> f.isFile && f.name.endsWith(ENTRY_SUFFIX) }?.toList().orEmpty()

    private fun trim(keep: File) {
        val files = entries().sortedBy { it.lastModified() }
        var total = files.sumOf { it.length() }
        for (file in files) {
            if (total <= maxBytes) break
            if (file == keep) continue
            total -= file.length()
            file.delete()
        }
    }

    companion object {
        private const val ENTRY_SUFFIX = ".img"
        private const val TEMP_SUFFIX = ".tmp"

        /** cache key for a url */
        fun keyFor(url: String): String =
            MessageDigest.getInstance("SHA-256")
                .digest(url.trim().toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }
    }
}
