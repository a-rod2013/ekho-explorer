package com.comp350sec001group2.ekhoexplorer.Business.ImageService

import kotlin.math.max
import kotlin.math.roundToInt

object ImageSizing {

    /** Scales so the longest side is at most [maxEdge], keeping the aspect ratio. Never upscales. */
    fun fitWithin(width: Int, height: Int, maxEdge: Int): Pair<Int, Int> {
        require(width > 0 && height > 0 && maxEdge > 0) { "dimensions must be positive" }
        val longest = max(width, height)
        if (longest <= maxEdge) return width to height
        val scale = maxEdge.toDouble() / longest
        return max(1, (width * scale).roundToInt()) to max(1, (height * scale).roundToInt())
    }

    /**
     * Largest power-of-two `inSampleSize` that still leaves the longest side at or above [maxEdge].
     */
    fun sampleSize(width: Int, height: Int, maxEdge: Int): Int {
        require(width > 0 && height > 0 && maxEdge > 0) { "dimensions must be positive" }
        val longest = max(width, height)
        var sample = 1
        while (longest / (sample * 2) >= maxEdge) sample *= 2
        return sample
    }
}
