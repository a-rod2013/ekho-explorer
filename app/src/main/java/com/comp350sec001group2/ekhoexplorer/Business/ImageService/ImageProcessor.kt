package com.comp350sec001group2.ekhoexplorer.Business.ImageService

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.media.ExifInterface
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException

/** The result of shrinking an image for upload. Always JPEG. */
class ProcessedImage(
    val bytes: ByteArray,
    val contentType: String,
    val width: Int,
    val height: Int,
)

/** Turns whatever the user picked into something small enough to upload. */
interface ImageProcessor {
    /** CPU-heavy and blocking; call it off the main thread. Throws [ImageException]. */
    fun process(input: ByteArray): ProcessedImage
}

/**
 * Decodes, fixes camera rotation, resizes to [ImageLimits.maxEdgePx], flattens transparency
 * onto white, and re-encodes as JPEG, lowering quality (then size) until the result is under
 * [ImageLimits.targetMaxBytes].
 *
 * Location and other EXIF metadata are dropped, because re-encoding writes a fresh file.
 */
class AndroidImageProcessor(private val limits: ImageLimits = ImageLimits()) : ImageProcessor {

    override fun process(input: ByteArray): ProcessedImage {
        if (ImageSniffer.detect(input) == null) {
            throw ImageException(ImageErrorKind.UNSUPPORTED_TYPE, "Only JPEG, PNG and WebP images are supported")
        }

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(input, 0, input.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw ImageException(ImageErrorKind.INVALID_IMAGE, "Could not read the image dimensions")
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = ImageSizing.sampleSize(bounds.outWidth, bounds.outHeight, limits.maxEdgePx)
        }
        val decoded = try {
            BitmapFactory.decodeByteArray(input, 0, input.size, decodeOptions)
        } catch (e: OutOfMemoryError) {
            throw ImageException(ImageErrorKind.TOO_LARGE, "Image is too large to process", e)
        } ?: throw ImageException(ImageErrorKind.INVALID_IMAGE, "Could not decode the image")

        var working: Bitmap = decoded
        try {
            working = replace(working, rotated(working, exifOrientation(input)))
            working = replace(working, scaledToFit(working, limits.maxEdgePx))
            working = replace(working, flattened(working))
            return encodeUnderBudget(working)
        } finally {
            if (!working.isRecycled) working.recycle()
        }
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

    private fun scaledToFit(source: Bitmap, maxEdge: Int): Bitmap {
        val (w, h) = ImageSizing.fitWithin(source.width, source.height, maxEdge)
        return if (w == source.width && h == source.height) source
        else Bitmap.createScaledBitmap(source, w, h, true)
    }

    /** JPEG has no alpha, so transparent pixels would turn black; paint them white instead. */
    private fun flattened(source: Bitmap): Bitmap {
        if (!source.hasAlpha()) return source
        val out = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        Canvas(out).apply {
            drawColor(Color.WHITE)
            drawBitmap(source, 0f, 0f, null)
        }
        return out
    }

    private fun encodeUnderBudget(start: Bitmap): ProcessedImage {
        var bitmap = start
        var quality = limits.startQuality
        var attempts = 0
        while (true) {
            val bytes = compress(bitmap, quality)
            val withinBudget = bytes.size <= limits.targetMaxBytes
            if (withinBudget || attempts >= MAX_ATTEMPTS) {
                val result = ProcessedImage(bytes, ImageFormat.JPEG.mimeType, bitmap.width, bitmap.height)
                if (bitmap !== start && !bitmap.isRecycled) bitmap.recycle()
                return result
            }
            attempts++
            if (quality > limits.minQuality) {
                quality = maxOf(limits.minQuality, quality - QUALITY_STEP)
            } else {
                // Quality is at the floor; shrink the picture instead.
                val smaller = Bitmap.createScaledBitmap(
                    bitmap,
                    maxOf(1, (bitmap.width * SHRINK_FACTOR).toInt()),
                    maxOf(1, (bitmap.height * SHRINK_FACTOR).toInt()),
                    true,
                )
                if (bitmap !== start && !bitmap.isRecycled) bitmap.recycle()
                bitmap = smaller
            }
        }
    }

    private fun compress(bitmap: Bitmap, quality: Int): ByteArray {
        val out = ByteArrayOutputStream()
        if (!bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)) {
            throw ImageException(ImageErrorKind.INVALID_IMAGE, "Could not encode the image")
        }
        return out.toByteArray()
    }

    private companion object {
        const val QUALITY_STEP = 10
        const val SHRINK_FACTOR = 0.85f
        const val MAX_ATTEMPTS = 10
    }
}
