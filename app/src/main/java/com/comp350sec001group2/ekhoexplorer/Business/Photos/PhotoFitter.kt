package com.comp350sec001group2.ekhoexplorer.Business.Photos

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** The outcome of fitting a photo to its budget. */
class FitResult(val edgePx: Int, val quality: Int, val bytes: ByteArray)
object PhotoFitter {

    fun fit(
        limits: PhotoLimits,
        sourceLongestEdge: Int,
        encode: (edgePx: Int, quality: Int) -> ByteArray,
    ): FitResult {
        require(sourceLongestEdge > 0) { "sourceLongestEdge must be positive" }

        // A photo smaller than a rung is not enlarged, so those rungs would repeat each other.
        val rungs = limits.rungs
            .map { PhotoRung(min(it.edgePx, sourceLongestEdge), it.minQuality) }
            .distinct()

        var smallest: FitResult? = null
        for (rung in rungs) {
            var low = rung.minQuality
            var high = limits.maxQuality
            var fitting: FitResult? = null
            while (low <= high) {
                val quality = (low + high) / 2
                val bytes = encode(rung.edgePx, quality)
                val candidate = FitResult(rung.edgePx, quality, bytes)
                if (smallest == null || bytes.size < smallest.bytes.size) smallest = candidate
                if (bytes.size <= limits.targetBytes) {
                    fitting = candidate
                    low = quality + 1      // fits: try for better quality
                } else {
                    high = quality - 1     // too big: lower quality
                }
            }
            if (fitting != null) return fitting
        }

        val fallback = smallest!!
        if (fallback.bytes.size <= limits.hardCapBytes) return fallback
        throw PhotoException(
            PhotoErrorKind.TOO_DETAILED,
            "Smallest version is ${fallback.bytes.size} bytes, over the ${limits.hardCapBytes} byte cap",
        )
    }
}

object PhotoSizing {

    /** Scales so the longest side is at most [maxEdge], keeping the aspect ratio. Never upscales. */
    fun fitWithin(width: Int, height: Int, maxEdge: Int): Pair<Int, Int> {
        require(width > 0 && height > 0 && maxEdge > 0) { "dimensions must be positive" }
        val longest = max(width, height)
        if (longest <= maxEdge) return width to height
        val scale = maxEdge.toDouble() / longest
        return max(1, (width * scale).roundToInt()) to max(1, (height * scale).roundToInt())
    }

    fun sampleSize(width: Int, height: Int, maxEdge: Int): Int {
        require(width > 0 && height > 0 && maxEdge > 0) { "dimensions must be positive" }
        val longest = max(width, height)
        var sample = 1
        while (longest / (sample * 2) >= maxEdge) sample *= 2
        return sample
    }
}
