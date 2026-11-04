package com.comp350sec001group2.ekhoexplorer.Business.ImageService

/**
 * Defaults are a 1080 px JPEG at quality 80.
 */
data class ImageLimits(
    /** Longest side after resizing. */
    val maxEdgePx: Int = 1080,

    /** Upload size the processor aims for; it lowers quality, then size, until under this. */
    val targetMaxBytes: Int = 350_000,
    val startQuality: Int = 80,
    val minQuality: Int = 50,

    /** Largest original accepted for upload, checked before decoding anything. */
    val maxInputBytes: Long = 25L * 1024 * 1024,

    /** Largest download accepted by `get`. Our own uploads are far below this. */
    val maxDownloadBytes: Long = 8L * 1024 * 1024,

    /** Disk cache budget; the oldest files are deleted first. */
    val cacheMaxBytes: Long = 50L * 1024 * 1024,
) {
    init {
        require(maxEdgePx in 64..8192) { "maxEdgePx must be in 64..8192" }
        require(targetMaxBytes > 0) { "targetMaxBytes must be positive" }
        require(minQuality in 1..100 && startQuality in minQuality..100) { "quality range invalid" }
        require(maxInputBytes > 0 && maxDownloadBytes > 0 && cacheMaxBytes > 0) { "byte limits must be positive" }
    }
}
