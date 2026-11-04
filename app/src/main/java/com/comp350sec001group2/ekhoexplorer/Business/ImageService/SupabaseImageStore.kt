package com.comp350sec001group2.ekhoexplorer.Business.ImageService

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.Buffer
import java.io.IOException
import java.io.InterruptedIOException
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * [ImageStore] backed by Supabase Storage, over its plain HTTP API.
 *
 * - Uploads POST to `/storage/v1/object/<bucket>/<folder>/<uuid>.<ext>` with the project key.
 *   `x-upsert: false` means an existing object is never replaced.
 * - Downloads GET the object's public URL.
 */
class SupabaseImageStore(
    private val config: SupabaseConfig,
    client: OkHttpClient = defaultClient(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) : ImageStore {

    // A redirect could carry a request (and its headers) to a different host.
    private val client: OkHttpClient = client.newBuilder()
        .followRedirects(false)
        .followSslRedirects(false)
        .build()

    override fun owns(url: String): Boolean = config.owns(url)

    override suspend fun upload(folder: String, bytes: ByteArray, contentType: String): String {
        requireValidFolder(folder)
        val extension = extensionFor(contentType)
        val path = "$folder/${newId()}.$extension"

        val request = Request.Builder()
            .url(config.uploadUrl(path))
            .post(bytes.toRequestBody(contentType.toMediaType()))
            .header("apikey", config.apiKey)
            .header("x-upsert", "false")
            .apply {
                if (config.apiKey.startsWith("eyJ")) header("Authorization", "Bearer ${config.apiKey}")
            }
            .build()

        send(request) { response ->
            if (!response.isSuccessful) throw httpError(response)
        }
        return config.publicUrl(path)
    }

    override suspend fun download(url: String, maxBytes: Long): ByteArray {
        if (!config.owns(url)) {
            throw ImageException(ImageErrorKind.INVALID_URL, "URL is not an image from this project's bucket")
        }
        val request = Request.Builder().url(url.trim()).get().build()
        return send(request) { response ->
            if (!response.isSuccessful) throw httpError(response)
            val body = response.body ?: throw ImageException(ImageErrorKind.UNKNOWN, "Empty response")
            if (body.contentLength() > maxBytes) {
                throw ImageException(ImageErrorKind.TOO_LARGE, "Image is larger than $maxBytes bytes")
            }
            readAtMost(body.source(), maxBytes)
        }
    }

    /** Runs the call, then [handle] on the IO dispatcher. */
    private suspend fun <T> send(request: Request, handle: (Response) -> T): T {
        val response = try {
            client.newCall(request).await()
        } catch (e: IOException) {
            throw networkError(e)
        }
        return response.use { r ->
            withContext(ioDispatcher) {
                try {
                    handle(r)
                } catch (e: IOException) {
                    throw networkError(e)
                }
            }
        }
    }

    private fun readAtMost(source: okio.BufferedSource, maxBytes: Long): ByteArray {
        val buffer = Buffer()
        var total = 0L
        while (true) {
            val read = source.read(buffer, CHUNK_BYTES)
            if (read == -1L) break
            total += read
            if (total > maxBytes) throw ImageException(ImageErrorKind.TOO_LARGE, "Image is larger than $maxBytes bytes")
        }
        return buffer.readByteArray()
    }

    private fun networkError(e: IOException): ImageException =
        if (e is InterruptedIOException) ImageException(ImageErrorKind.TIMEOUT, "The request timed out", e)
        else ImageException(ImageErrorKind.UNAVAILABLE, "Could not reach the image server", e)

    /** Maps a failed response to an [ImageException]. */
    private fun httpError(response: Response): ImageException {
        val text = try {
            response.body?.source()?.let { s ->
                s.request(ERROR_BODY_BYTES)
                s.buffer.snapshot(minOf(s.buffer.size, ERROR_BODY_BYTES).toInt()).utf8()
            }.orEmpty()
        } catch (e: IOException) {
            ""
        }
        // Supabase reports its own code in the JSON body, which can differ from the HTTP status.
        val code = STATUS_CODE.find(text)?.groupValues?.get(1)?.toIntOrNull() ?: response.code
        val message = MESSAGE.find(text)?.groupValues?.get(1)?.take(200)
        val detail = "HTTP $code" + (message?.let { ": $it" } ?: "")
        val kind = when {
            code == 401 || code == 403 -> ImageErrorKind.UNAUTHORIZED
            code == 404 -> ImageErrorKind.NOT_FOUND
            code == 409 -> ImageErrorKind.ALREADY_EXISTS
            code == 413 -> ImageErrorKind.TOO_LARGE
            code == 415 -> ImageErrorKind.UNSUPPORTED_TYPE
            code == 429 || code in 500..599 -> ImageErrorKind.UNAVAILABLE
            else -> ImageErrorKind.UNKNOWN
        }
        return ImageException(kind, detail)
    }

    private fun requireValidFolder(folder: String) {
        if (!FOLDER.matches(folder)) {
            throw ImageException(ImageErrorKind.INVALID_URL, "Folder must be 1-32 characters of a-z, 0-9, '-' or '_'")
        }
    }

    private fun extensionFor(contentType: String): String = when (contentType) {
        ImageFormat.JPEG.mimeType -> "jpg"
        ImageFormat.PNG.mimeType -> "png"
        ImageFormat.WEBP.mimeType -> "webp"
        else -> throw ImageException(ImageErrorKind.UNSUPPORTED_TYPE, "Unsupported content type")
    }

    private suspend fun Call.await(): Response = suspendCancellableCoroutine { cont ->
        cont.invokeOnCancellation { cancel() }
        enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (cont.isActive) cont.resume(response) else response.close()
            }

            override fun onFailure(call: Call, e: IOException) {
                if (cont.isActive) cont.resumeWithException(e)
            }
        })
    }

    companion object {
        private const val CHUNK_BYTES = 8192L
        private const val ERROR_BODY_BYTES = 4096L
        private val FOLDER = Regex("[a-z0-9_-]{1,32}")
        private val STATUS_CODE = Regex("\"statusCode\"\\s*:\\s*\"?(\\d{3})\"?")
        private val MESSAGE = Regex("\"message\"\\s*:\\s*\"([^\"]*)\"")

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .callTimeout(60, TimeUnit.SECONDS)
            .build()
    }
}
