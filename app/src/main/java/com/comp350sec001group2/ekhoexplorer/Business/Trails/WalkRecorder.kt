package com.comp350sec001group2.ekhoexplorer.Business.Trails

/** One reading from the phone's location: where, how sure it is (metres), and when. */
data class Fix(val point: LatLngPoint, val accuracyMeters: Float, val timeMillis: Long)

class WalkRecorder(
    private val minStepMeters: Double = TrailLimits.MIN_STEP_METERS,
    private val maxAccuracyMeters: Float = TrailLimits.MAX_ACCURACY_METERS,
    initial: List<LatLngPoint> = emptyList(),
) {
    private val recorded = initial.toMutableList()

    val points: List<LatLngPoint> get() = recorded.toList()

    var isPaused: Boolean = false
        private set

    fun onFix(fix: Fix): Boolean {
        if (isPaused || fix.accuracyMeters > maxAccuracyMeters) return false
        val last = recorded.lastOrNull()
        if (last != null && TrailGeo.distanceMeters(last, fix.point) < minStepMeters) return false
        recorded += fix.point
        return true
    }

    fun pause() { isPaused = true }
    fun resume() { isPaused = false }
}
