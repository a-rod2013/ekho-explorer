package com.comp350sec001group2.ekhoexplorer.Business.Photos

/**
 * One iter of the downsizing pipeline. try this longest side, with quality no lower than [minQuality].
 */
data class PhotoRung(val edgePx: Int, val minQuality: Int)

data class PhotoLimits(
    /** Most photos one post may hold. */
    val maxPhotos: Int = 5,

    /** Longest side of a stored photo. Never upscales a smaller original. */
    val maxEdgePx: Int = 800,

    /** The size each photo aims for. */
    val targetBytes: Int = 60_000,

    /** No stored photo may exceed this, even when it cannot reach [targetBytes]. */
    val hardCapBytes: Int = 100_000,

    /** Quality ceiling. */
    val maxQuality: Int = 85,

    /** Quality floor on the normal rungs. */
    val minQuality: Int = 50,

    /** Largest original accepted, checked before anything is decoded. */
    val maxInputBytes: Long = 25L * 1024 * 1024,
) {
    init {
        require(maxPhotos in 1..10) { "maxPhotos must be in 1..10" }
        require(maxEdgePx in 320..2048) { "maxEdgePx must be in 320..2048" }
        require(targetBytes in 5_000..hardCapBytes) { "targetBytes must be between 5 KB and the hard cap" }
        require(hardCapBytes <= 400_000) { "hardCapBytes above 400 KB defeats the purpose" }
        require(minQuality in 1..maxQuality && maxQuality <= 100) { "quality range invalid" }
        require(maxInputBytes > 0) { "maxInputBytes must be positive" }
    }

    val rungs: List<PhotoRung> = listOf(
        PhotoRung(maxEdgePx, minQuality),
        PhotoRung((maxEdgePx * 0.9).toInt(), minQuality),
        PhotoRung((maxEdgePx * 0.8).toInt(), minQuality),
        PhotoRung((maxEdgePx * 0.8).toInt(), 30),
    )
}
