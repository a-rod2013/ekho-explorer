package com.comp350sec001group2.ekhoexplorer.ui.pages.trailWizard.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.comp350sec001group2.ekhoexplorer.Business.Trails.WalkPhase
import com.comp350sec001group2.ekhoexplorer.ui.pages.trailWizard.viewmodels.MapMode
import com.comp350sec001group2.ekhoexplorer.ui.pages.trailWizard.viewmodels.TrailWizardUiState
import com.comp350sec001group2.ekhoexplorer.ui.pages.trailWizard.viewmodels.TrailWizardViewModel
import com.comp350sec001group2.ekhoexplorer.ui.theme.AppStyle

@Composable
internal fun MapStepControls(
    state: TrailWizardUiState,
    viewModel: TrailWizardViewModel,
    onOpenSettings: () -> Unit,
    onAllowLocation: () -> Unit,
) {
    val d = AppStyle.dimens
    val recording = state.walkPhase != WalkPhase.IDLE
    val walking = state.mode == MapMode.WALK
    val count = state.points.size

    Column(
        modifier = Modifier.fillMaxWidth().background(AppStyle.colors.background).padding(d.lg),
        verticalArrangement = Arrangement.spacedBy(d.sm),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(d.sm)) {
            FilterChip(
                selected = !walking,
                onClick = { viewModel.onModeChange(MapMode.PINS) },
                enabled = !recording,
                label = { Text("Drop pins") },
                modifier = Modifier.testTag("mode-pins"),
            )
            FilterChip(
                selected = walking,
                onClick = { viewModel.onModeChange(MapMode.WALK) },
                enabled = !recording,
                label = { Text("Walk it") },
                modifier = Modifier.testTag("mode-walk"),
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(d.lg)) {
            val noun = if (walking || state.pathFromWalk) "point" else "pin"
            Text(
                text = if (count == 1) "1 $noun" else "$count ${noun}s",
                style = AppStyle.type.username,
                color = AppStyle.colors.textPrimary,
                modifier = Modifier.testTag("pin-count"),
            )
            Text("${state.lengthText} mi", style = AppStyle.type.username, color = AppStyle.colors.textSecondary, modifier = Modifier.testTag("live-length"))
        }

        if (walking) {
            WalkPanel(state, viewModel, onOpenSettings, onAllowLocation)
        } else {
            Text(
                text = when {
                    state.pathFromWalk -> "This path was walked. Clear it to start over."
                    state.selectedPin != null -> "Tap the map to move the selected pin."
                    else -> "Tap the map to drop a pin. Tap a pin to move it."
                },
                style = AppStyle.type.caption,
                color = AppStyle.colors.textSecondary,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(d.sm)) {
                OutlinedButton(onClick = viewModel::onUndoPin, enabled = count > 0 && !state.pathFromWalk, modifier = Modifier.testTag("undo-pin")) { Text("Undo") }
                OutlinedButton(onClick = viewModel::onClearPins, enabled = count > 0, modifier = Modifier.testTag("clear-pins")) { Text("Clear") }
                Button(onClick = viewModel::onNext, modifier = Modifier.weight(1f).testTag("next-step")) { Text("Next") }
            }
        }
    }
}

@Composable
private fun WalkPanel(
    state: TrailWizardUiState,
    viewModel: TrailWizardViewModel,
    onOpenSettings: () -> Unit,
    onAllowLocation: () -> Unit,
) {
    val d = AppStyle.dimens
    val options = state.walkOptions

    options.message?.let {
        Text(
            text = it,
            style = AppStyle.type.caption,
            color = if (options.availability.enabled) AppStyle.colors.textSecondary else AppStyle.colors.downvote,
            modifier = Modifier.testTag("walk-warning"),
        )
    }

    when (state.walkPhase) {
        WalkPhase.IDLE -> {
            if (state.pathFromWalk) {
                Row(horizontalArrangement = Arrangement.spacedBy(d.sm)) {
                    OutlinedButton(onClick = viewModel::onClearPins, modifier = Modifier.testTag("clear-pins")) { Text("Clear") }
                    Button(onClick = viewModel::onNext, modifier = Modifier.weight(1f).testTag("next-step")) { Text("Next") }
                }
            } else {
                Button(
                    onClick = viewModel::onStartWalk,
                    enabled = options.availability.enabled,
                    modifier = Modifier.fillMaxWidth().testTag("walk-start"),
                ) { Text("Start walking") }
                if (!options.availability.enabled) {
                    Row(horizontalArrangement = Arrangement.spacedBy(d.sm)) {
                        OutlinedButton(onClick = onAllowLocation, modifier = Modifier.testTag("allow-location")) { Text("Allow location") }
                        OutlinedButton(onClick = onOpenSettings, modifier = Modifier.testTag("open-settings")) { Text("Open settings") }
                    }
                }
            }
        }

        WalkPhase.RECORDING -> Row(horizontalArrangement = Arrangement.spacedBy(d.sm)) {
            OutlinedButton(onClick = viewModel::onPauseWalk, modifier = Modifier.testTag("walk-pause")) { Text("Pause") }
            Button(onClick = viewModel::onFinishWalk, modifier = Modifier.weight(1f).testTag("walk-finish")) { Text("Finish") }
        }

        WalkPhase.PAUSED -> Row(horizontalArrangement = Arrangement.spacedBy(d.sm)) {
            OutlinedButton(onClick = viewModel::onResumeWalk, modifier = Modifier.testTag("walk-resume")) { Text("Resume") }
            OutlinedButton(onClick = viewModel::onDiscardWalk, modifier = Modifier.testTag("walk-discard")) { Text("Discard") }
            Button(onClick = viewModel::onFinishWalk, modifier = Modifier.weight(1f).testTag("walk-finish")) { Text("Finish") }
        }

        WalkPhase.INTERRUPTED -> {
            Text(
                text = "Recording stopped. Your path so far is kept.",
                style = AppStyle.type.caption,
                color = AppStyle.colors.textPrimary,
                modifier = Modifier.testTag("walk-interrupted"),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(d.sm)) {
                OutlinedButton(onClick = viewModel::onResumeWalk, enabled = options.availability.enabled, modifier = Modifier.testTag("walk-resume")) { Text("Resume") }
                OutlinedButton(onClick = viewModel::onDiscardWalk, modifier = Modifier.testTag("walk-discard")) { Text("Discard") }
                Button(onClick = viewModel::onFinishWalk, modifier = Modifier.weight(1f).testTag("walk-finish")) { Text("Finish") }
            }
        }
    }
}
