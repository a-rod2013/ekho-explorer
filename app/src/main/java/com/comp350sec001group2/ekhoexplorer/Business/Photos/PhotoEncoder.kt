package com.comp350sec001group2.ekhoexplorer.Business.Photos


import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.media.ExifInterface
import android.os.Build
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

/** Aggressively downsizes and encodes a pic */
interface PhotoEncoder {
    /**
     * CPU-heavy and blocking: call it off the main thread. Throws [PhotoException].
     * Cancelling the calling coroutine does not interrupt an encode already running.
     */
    fun encode(input: ByteArray): EncodedPhoto

    /** Reads at most [PhotoLimits.maxInputBytes] from [input], then encodes. Closes the stream. */
    fun encode(input: InputStream): EncodedPhoto
}

class AndroidPhotoEncoder(private val limits: PhotoLimits = PhotoLimits()) : PhotoEncoder {

    override fun encode(input: InputStream): EncodedPhoto = encode(readAtMost(input, limits.maxInputBytes))

    override fun encode(input: ByteArray): EncodedPhoto {
        if (input.size > limits.maxInputBytes) {
            throw PhotoException(PhotoErrorKind.INPUT_TOO_LARGE, "Original is larger than ${limits.maxInputBytes} bytes")
        }
        if (PhotoFormat.detect(input) == null) {
            throw PhotoException(PhotoErrorKind.UNSUPPORTED_TYPE, "Only JPEG, PNG and WebP photos are supported")
        }

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(input, 0, input.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw PhotoException(PhotoErrorKind.INVALID_IMAGE, "Could not read the photo's dimensions")
        }

        val options = BitmapFactory.Options().apply {
            inSampleSize = PhotoSizing.sampleSize(bounds.outWidth, bounds.outHeight, limits.maxEdgePx)
        }
        val decoded = try {
            BitmapFactory.decodeByteArray(input, 0, input.size, options)
        } catch (e: OutOfMemoryError) {
            throw PhotoException(PhotoErrorKind.INPUT_TOO_LARGE, "Photo is too large to process", e)
        } ?: throw PhotoException(PhotoErrorKind.INVALID_IMAGE, "Could not decode the photo")

        val upright = replace(decoded, rotated(decoded, exifOrientation(input)))
        val source = replace(upright, flattened(upright))
        try {
            val scaled = HashMap<Int, Bitmap>()
            fun scaledTo(edge: Int): Bitmap = scaled.getOrPut(edge) {
                val (w, h) = PhotoSizing.fitWithin(source.width, source.height, edge)
                if (w == source.width && h == source.height) source else Bitmap.createScaledBitmap(source, w, h, true)
            }

            val result = PhotoFitter.fit(limits, maxOf(source.width, source.height)) { edge, quality ->
                compress(scaledTo(edge), quality)
            }
            val picture = scaledTo(result.edgePx)
            return EncodedPhoto(result.bytes, picture.width, picture.height)
        } finally {
            // (the scaled copies are small and garbage collected; only the big source is freed eagerly)
            if (!source.isRecycled) source.recycle()
        }
    }

    private fun compress(bitmap: Bitmap, quality: Int): ByteArray {
        val out = ByteArrayOutputStream(limits.targetBytes)
        val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Bitmap.CompressFormat.WEBP_LOSSY
        else @Suppress("DEPRECATION") Bitmap.CompressFormat.WEBP
        if (!bitmap.compress(format, quality, out)) {
            throw PhotoException(PhotoErrorKind.INVALID_IMAGE, "Could not encode the photo")
        }
        return out.toByteArray()
    }

    /** Recycles [old] when [new] is a different bitmap, and returns [new]. */
    private fun replace(old: Bitmap, new: Bitmap): Bitmap {
        if (new !== old && !old.isRecycled) old.recycle()
        return new
    }

    private fun exifOrientation(input: ByteArray): Int =
        try {
            ExifInterface(ByteArrayInputStream(input))
                .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } catch (e: IOException) {
            ExifInterface.ORIENTATION_NORMAL
        }

    private fun rotated(source: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { matrix.postRotate(90f); matrix.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> { matrix.postRotate(-90f); matrix.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(-90f)
            else -> return source
        }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    /** Transparent pixels would turn black; paint them white. Skips the copy when there is no alpha. */
    private fun flattened(source: Bitmap): Bitmap {
        if (!source.hasAlpha()) return source
        val out = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        Canvas(out).apply {
            drawColor(Color.WHITE)
            drawBitmap(source, 0f, 0f, null)
        }
        return out
    }

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
                        throw PhotoException(PhotoErrorKind.INPUT_TOO_LARGE, "Original is larger than $maxBytes bytes")
                    }
                    out.write(chunk, 0, read)
                }
            }
        } catch (e: IOException) {
            throw PhotoException(PhotoErrorKind.INVALID_IMAGE, "Could not read the photo", e)
        }
        return out.toByteArray()
    }
}
