package com.comp350sec001group2.ekhoexplorer.Business.ImageService

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap

/**
 * The image service the rest of the app uses.
 *
 * ```
 * val url  = imageService.upload(photoFile)   // shrink, send to Supabase, get a URL
 * // ...save `url` in the post's imageUrls in Firestore...
 * val file = imageService.get(url)            // any user, any device: the image as a File
 * ```
 *
 * - [upload] validates the input, resizes and compresses it, uploads it, and returns the
 *   permanent URL. The processed copy is also cached, so the uploader's own feed shows it
 *   with no download.
 * - [get] returns a local file for one of our URLs, downloading it at most once. Concurrent
 *   calls for the same URL share one download.
 *
 * Every failure is an [ImageException]; cancellation propagates untouched. The service holds
 * no coroutine scope: work must run in the caller's coroutine.
 */
class ImageService(
    private val store: ImageStore,
    private val processor: ImageProcessor,
    private val cache: ImageCache,
    private val limits: ImageLimits = ImageLimits(),
    private val cpuDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val downloadLocks = ConcurrentHashMap<String, Mutex>()

    /** Uploads the image in [file] and returns its URL. */
    suspend fun upload(file: File, folder: String = DEFAULT_FOLDER): String {
        val bytes = withContext(ioDispatcher) {
            if (!file.isFile) throw ImageException(ImageErrorKind.INVALID_IMAGE, "Not a readable file")
            if (file.length() > limits.maxInputBytes) {
                throw ImageException(ImageErrorKind.TOO_LARGE, "Image is larger than ${limits.maxInputBytes} bytes")
            }
            try {
                file.readBytes()
            } catch (e: IOException) {
                throw ImageException(ImageErrorKind.INVALID_IMAGE, "Could not read the file", e)
            }
        }
        return upload(bytes, folder)
    }

    /**
     * Uploads the image read from [input] and returns its URL; the stream is closed afterwards.
     * Reading stops with `TOO_LARGE` at [ImageLimits.maxInputBytes], so a huge file cannot
     * exhaust memory.
     */
    suspend fun upload(input: InputStream, folder: String = DEFAULT_FOLDER): String {
        val bytes = withContext(ioDispatcher) { readAtMost(input, limits.maxInputBytes) }
        return upload(bytes, folder)
    }

    /** Uploads the image in [bytes] (JPEG, PNG or WebP) and returns its URL. */
    suspend fun upload(bytes: ByteArray, folder: String = DEFAULT_FOLDER): String {
        if (bytes.size > limits.maxInputBytes) {
            throw ImageException(ImageErrorKind.TOO_LARGE, "Image is larger than ${limits.maxInputBytes} bytes")
        }
        val processed = withContext(cpuDispatcher) { processor.process(bytes) }
        val url = store.upload(folder, processed.bytes, processed.contentType)
        cacheQuietly(url, processed.bytes)
        return url
    }

    /** Returns the image behind [url] as a local file, downloading it only if not cached. */
    suspend fun get(url: String): File {
        if (!store.owns(url)) {
            throw ImageException(ImageErrorKind.INVALID_URL, "URL is not an image from this project's bucket")
        }
        val key = ImageCache.keyFor(url)
        cachedFile(key)?.let { return it }

        val lock = downloadLocks.getOrPut(key) { Mutex() }
        return lock.withLock {
            cachedFile(key) ?: run {
                val bytes = store.download(url, limits.maxDownloadBytes)
                if (ImageSniffer.detect(bytes) == null) {
                    throw ImageException(ImageErrorKind.INVALID_IMAGE, "The downloaded data is not a supported image")
                }
                withContext(ioDispatcher) {
                    try {
                        cache.put(key, bytes)
                    } catch (e: IOException) {
                        throw ImageException(ImageErrorKind.UNKNOWN, "Could not save the image to the cache", e)
                    }
                }
            }
        }
    }

    /** True if [url] is one of ours, without any network access. Useful before calling [get]. */
    fun isOwnUrl(url: String): Boolean = store.owns(url)

    private suspend fun cachedFile(key: String): File? = withContext(ioDispatcher) { cache.get(key) }

    private fun readAtMost(input: InputStream, maxBytes: Long): ByteArray {
        val out = ByteArrayOutputStream()
        val chunk = ByteArray(8192)
        var total = 0L
        try {
            input.use { stream ->
                while (true) {
                    val read = stream.read(chunk)
                    if (read == -1) break
                    total += read
                    if (total > maxBytes) {
                        throw ImageException(ImageErrorKind.TOO_LARGE, "Image is larger than $maxBytes bytes")
                    }
                    out.write(chunk, 0, read)
                }
            }
        } catch (e: IOException) {
            throw ImageException(ImageErrorKind.INVALID_IMAGE, "Could not read the image", e)
        }
        return out.toByteArray()
    }

    private suspend fun cacheQuietly(url: String, bytes: ByteArray) {
        withContext(ioDispatcher) {
            try {
                cache.put(ImageCache.keyFor(url), bytes)
            } catch (e: IOException) {
                // Not cached; the next get() will download it instead.
            }
        }
    }

    companion object {
        const val DEFAULT_FOLDER = "posts"


        fun create(
            cacheDir: File,
            config: SupabaseConfig,
            limits: ImageLimits = ImageLimits(),
        ): ImageService = ImageService(
            store = SupabaseImageStore(config),
            processor = AndroidImageProcessor(limits),
            cache = ImageCache(cacheDir, limits.cacheMaxBytes),
            limits = limits,
        )
    }
}
