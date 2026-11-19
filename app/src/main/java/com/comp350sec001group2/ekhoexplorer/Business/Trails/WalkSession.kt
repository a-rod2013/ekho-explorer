package com.comp350sec001group2.ekhoexplorer.Business.Trails

import com.comp350sec001group2.ekhoexplorer.Business.Location.LocationAccess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class WalkAvailability(val enabled: Boolean) {
    ENABLED_BACKGROUND(true),
    ENABLED_FOREGROUND_ONLY(true),
    DISABLED_APPROXIMATE(false),
    DISABLED_OFF(false),
}

data class WalkOptions(val availability: WalkAvailability, val message: String?)

fun walkOptions(access: LocationAccess): WalkOptions = when (access) {
    LocationAccess.PRECISE_BACKGROUND -> WalkOptions(WalkAvailability.ENABLED_BACKGROUND, null)
    LocationAccess.PRECISE_FOREGROUND -> WalkOptions(
        WalkAvailability.ENABLED_FOREGROUND_ONLY,
        "Recording stops when you leave the app. Allow location all the time in settings to keep recording with the screen off.",
    )
    LocationAccess.APPROXIMATE -> WalkOptions(
        WalkAvailability.DISABLED_APPROXIMATE,
        "Walking a trail needs precise location. Turn on precise location for this app in settings. You can still drop pins.",
    )
    LocationAccess.DENIED -> WalkOptions(
        WalkAvailability.DISABLED_OFF,
        "Location is off or not allowed for this app, so walking a trail is unavailable. Turn location on in settings. You can still drop pins.",
    )
}

enum class WalkPhase { IDLE, RECORDING, PAUSED, INTERRUPTED }

data class WalkUiState(
    val phase: WalkPhase = WalkPhase.IDLE,
    val points: List<LatLngPoint> = emptyList(),
    val message: String? = null,
)

class WalkSession(private val store: WalkStore) {

    private val lock = Any()
    private val _state = MutableStateFlow(WalkUiState())
    val state: StateFlow<WalkUiState> = _state.asStateFlow()

    private var recorder = WalkRecorder()
    private var appVisible = true

    init {
        val saved = store.load()
        if (saved.isNotEmpty()) {
            recorder = WalkRecorder(initial = saved).also { it.pause() }
            _state.value = WalkUiState(WalkPhase.INTERRUPTED, saved, "A walk you were recording was found. Resume it, finish it, or discard it.")
        }
    }

    fun start(access: LocationAccess): Boolean = synchronized(lock) {
        if (!access.canRecordWalk) {
            _state.value = _state.value.copy(message = walkOptions(access).message ?: "Walking is not available.")
            return false
        }
        store.clear()
        recorder = WalkRecorder()
        appVisible = true
        _state.value = WalkUiState(WalkPhase.RECORDING, emptyList(), null)
        true
    }

    fun onFix(fix: Fix): Unit = synchronized(lock) {
        if (_state.value.phase != WalkPhase.RECORDING) return
        if (recorder.onFix(fix)) {
            store.append(fix.point)
            _state.value = _state.value.copy(points = recorder.points)
        }
    }

    fun pause(): Unit = synchronized(lock) {
        if (_state.value.phase != WalkPhase.RECORDING) return
        recorder.pause()
        _state.value = _state.value.copy(phase = WalkPhase.PAUSED, message = null)
    }

    fun resume(access: LocationAccess): Boolean = synchronized(lock) {
        val phase = _state.value.phase
        if (phase != WalkPhase.PAUSED && phase != WalkPhase.INTERRUPTED) return false
        if (!canRecordNow(access)) {
            _state.value = _state.value.copy(message = walkOptions(access).message ?: "Walking is not available.")
            return false
        }
        recorder.resume()
        _state.value = _state.value.copy(phase = WalkPhase.RECORDING, message = null)
        true
    }

    fun onAppLeft(access: LocationAccess) = synchronized(lock) {
        appVisible = false
        if (_state.value.phase == WalkPhase.RECORDING && !access.recordsInBackground) {
            interrupt("Recording stopped because you left the app. Your path so far is kept.")
        }
    }

    fun onAppReturned() = synchronized(lock) { appVisible = true }

    fun onAccessChanged(access: LocationAccess) = synchronized(lock) {
        if (_state.value.phase == WalkPhase.RECORDING && !canRecordNow(access)) {
            interrupt("Recording stopped because location is no longer available. Your path so far is kept.")
        }
    }

    fun finish(): List<LatLngPoint> = synchronized(lock) {
        val path = recorder.points
        val result = if (path.size > TrailLimits.MAX_WALK_POINTS) TrailGeo.simplify(path, TrailLimits.MAX_WALK_POINTS) else path
        reset()
        result
    }

    fun discard() = synchronized(lock) { reset() }

    private fun canRecordNow(access: LocationAccess) = access.canRecordWalk && (appVisible || access.recordsInBackground)

    private fun interrupt(message: String) {
        recorder.pause()
        _state.value = _state.value.copy(phase = WalkPhase.INTERRUPTED, message = message)
    }

    private fun reset() {
        store.clear()
        recorder = WalkRecorder()
        _state.value = WalkUiState()
    }
}
