package com.comp350sec001group2.ekhoexplorer.Business.Cache

import java.io.File
import java.io.IOException
import java.net.URLEncoder

interface ByteCache {
    fun get(vararg key: String): ByteArray?
    fun put(bytes: ByteArray, vararg key: String)
    fun remove(vararg prefix: String)
    fun clear()
    fun sizeBytes(): Long
}

object NoByteCache : ByteCache {
    override fun get(vararg key: String): ByteArray? = null
    override fun put(bytes: ByteArray, vararg key: String) = Unit
    override fun remove(vararg prefix: String) = Unit
    override fun clear() = Unit
    override fun sizeBytes(): Long = 0
}

class DiskLruByteCache(
    private val root: File,
    private val maxBytes: Long,
    private val clock: () -> Long = System::currentTimeMillis,
) : ByteCache {

    private val lock = Any()

    init {
        require(maxBytes > 0) { "maxBytes must be positive" }
    }

    override fun get(vararg key: String): ByteArray? = synchronized(lock) {
        val file = target(key)
        if (!file.isFile) return null
        try {
            file.setLastModified(clock())
            file.readBytes()
        } catch (e: IOException) {
            null
        }
    }

    override fun put(bytes: ByteArray, vararg key: String) {
        if (bytes.size > maxBytes) return
        synchronized(lock) {
            val file = target(key)
            try {
                file.parentFile?.mkdirs()
                val temp = File(file.parentFile, file.name + ".tmp")
                temp.writeBytes(bytes)
                if (!temp.renameTo(file)) {
                    file.delete()
                    temp.renameTo(file)
                }
                file.setLastModified(clock())
                evict()
            } catch (e: IOException) {
                // A cache that cannot write is just a miss.
            }
        }
    }

    override fun remove(vararg prefix: String) {
        synchronized(lock) { target(prefix).deleteRecursively() }
    }

    override fun clear() {
        synchronized(lock) { root.listFiles()?.forEach { it.deleteRecursively() } }
    }

    override fun sizeBytes(): Long = synchronized(lock) { files().sumOf { it.length() } }

    private fun files() = root.walkTopDown().filter { it.isFile && !it.name.endsWith(".tmp") }.toList()

    private fun evict() {
        val all = files()
        var total = all.sumOf { it.length() }
        if (total <= maxBytes) return
        for (f in all.sortedWith(compareBy({ it.lastModified() }, { it.name }))) {
            if (total <= maxBytes) break
            total -= f.length()
            f.delete()
        }
    }

    private fun target(key: Array<out String>): File {
        require(key.isNotEmpty()) { "A key needs at least one part" }
        return key.fold(root) { dir, part -> File(dir, safe(part)) }
    }

    private fun safe(part: String): String {
        require(part.isNotEmpty()) { "A key part cannot be empty" }
        // Every dot is encoded: ".." must not be a path step, and Windows trims trailing dots from names.
        val s = URLEncoder.encode(part, "UTF-8").replace("*", "%2A").replace(".", "%2E")
        return if (s.length <= 80) s else s.take(60) + "~" + Integer.toHexString(part.hashCode())
    }
}
