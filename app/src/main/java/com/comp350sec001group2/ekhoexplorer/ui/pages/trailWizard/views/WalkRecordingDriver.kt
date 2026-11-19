package com.comp350sec001group2.ekhoexplorer.ui.pages.trailWizard.views

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.comp350sec001group2.ekhoexplorer.Business.Trails.Fix
import com.comp350sec001group2.ekhoexplorer.Business.Trails.TrailRecordingService
import com.comp350sec001group2.ekhoexplorer.Business.Trails.WalkLocationUpdates
import com.comp350sec001group2.ekhoexplorer.Business.Trails.WalkPhase

@Composable
fun WalkRecordingDriver(phase: WalkPhase, background: Boolean, onFix: (Fix) -> Unit) {
    val context = LocalContext.current
    val recording = phase == WalkPhase.RECORDING
    val latestOnFix = rememberUpdatedState(onFix)

    // Background recording: a service.
    LaunchedEffect(recording, background) {
        if (recording && background) TrailRecordingService.start(context)
    }

    // Foreground-only recording: updates tied to this screen.
    DisposableEffect(recording, background) {
        val updates = if (recording && !background) WalkLocationUpdates(context) else null
        updates?.start { latestOnFix.value(it) }
        onDispose { updates?.stop() }
    }
}
