package com.comp350sec001group2.ekhoexplorer.ui.pages.trailWizard.views


import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.comp350sec001group2.ekhoexplorer.Business.Trails.Fix
import com.comp350sec001group2.ekhoexplorer.Business.Trails.LatLngPoint
import com.comp350sec001group2.ekhoexplorer.Business.Trails.TrailFormError
import com.comp350sec001group2.ekhoexplorer.Business.Trails.TrailLimits
import com.comp350sec001group2.ekhoexplorer.Business.Trails.TrailVisibility
import com.comp350sec001group2.ekhoexplorer.Business.Trails.WalkAvailability
import com.comp350sec001group2.ekhoexplorer.Business.Trails.WalkPhase
import com.comp350sec001group2.ekhoexplorer.ui.components.label
import com.comp350sec001group2.ekhoexplorer.ui.pages.trailWizard.viewmodels.TrailWizardViewModel
import com.comp350sec001group2.ekhoexplorer.ui.pages.trailWizard.viewmodels.WizardStep
import com.comp350sec001group2.ekhoexplorer.ui.pages.trailWizard.viewmodels.trailWizardViewModelFactory
import com.comp350sec001group2.ekhoexplorer.ui.components.rememberLocationPermissionRequester
import com.comp350sec001group2.ekhoexplorer.ui.theme.AppStyle

private fun Context.isChangingConfigurations(): Boolean {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current.isChangingConfigurations
        current = current.baseContext
    }
    return false
}

class MapSlot(
    val points: List<LatLngPoint>,
    val selectedPin: Int?,
    val userLocation: LatLngPoint?,
    val onMapTap: (LatLngPoint) -> Unit,
    val onPinTap: (Int) -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrailWizardScreen(
    onSaved: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TrailWizardViewModel = viewModel(factory = trailWizardViewModelFactory),
    map: @Composable (MapSlot) -> Unit = { slot ->
        TrailMapView(slot.points, slot.selectedPin, slot.onMapTap, slot.onPinTap, userLocation = slot.userLocation)
    },
    walkDriver: @Composable (phase: WalkPhase, background: Boolean, onFix: (Fix) -> Unit) -> Unit = { phase, background, onFix ->
        WalkRecordingDriver(phase, background, onFix)
    },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val d = AppStyle.dimens

    LaunchedEffect(state.savedTrailId) { state.savedTrailId?.let(onSaved) }
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.onMessageShown()
        }
    }
    BackHandler(enabled = state.step == WizardStep.FORM && !state.isEdit && !state.isSaving) { viewModel.onStepBack() }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val hostContext = LocalContext.current
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP ->
                    if (!hostContext.isChangingConfigurations()) viewModel.onScreenStopped()
                Lifecycle.Event.ON_START -> viewModel.onScreenStarted()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    val context = LocalContext.current
    val locationPermission = rememberLocationPermissionRequester { viewModel.onAccessMayHaveChanged() }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val background = state.walkOptions.availability == WalkAvailability.ENABLED_BACKGROUND
    LaunchedEffect(state.walkPhase, background) {
        val needsAsking = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        if (state.walkPhase == WalkPhase.RECORDING && background && needsAsking) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    walkDriver(state.walkPhase, background, viewModel::onWalkFix)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = AppStyle.colors.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbar) },
        // The map controls live in the bottom bar, so messages (the snackbar) appear above them
        // instead of covering the buttons.
        bottomBar = {
            if (state.step == WizardStep.MAP) {
                MapStepControls(
                    state = state,
                    viewModel = viewModel,
                    onOpenSettings = locationPermission::openSettings,
                    onAllowLocation = locationPermission::request,
                )
            }
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // The app's shared header is above this screen; the step name is a plain heading. Going
            // back is the system back (from the details step it returns to the map).
            Text(
                text = when {
                    state.isEdit -> "Edit trail"
                    state.step == WizardStep.MAP -> "Create a trail: map it"
                    else -> "Create a trail: details"
                },
                style = AppStyle.type.username,
                color = AppStyle.colors.textPrimary,
                modifier = Modifier.fillMaxWidth().padding(horizontal = d.lg, vertical = d.xs).testTag("wizard-title"),
            )
            when (state.step) {
                WizardStep.MAP -> {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        map(MapSlot(state.points, state.selectedPin, state.userLocation, viewModel::onMapTapped, viewModel::onPinSelected))
                    }
                }

                WizardStep.FORM -> TrailForm(state = state, viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrailForm(
    state: com.comp350sec001group2.ekhoexplorer.ui.pages.trailWizard.viewmodels.TrailWizardUiState,
    viewModel: TrailWizardViewModel,
) {
    val d = AppStyle.dimens
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(TrailLimits.OWNER_PHOTOS)) { uris ->
        viewModel.onPhotosPicked(uris)
    }
    val busy = state.isSaving

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(d.lg),
        verticalArrangement = Arrangement.spacedBy(d.md),
    ) {
        OutlinedTextField(
            value = state.name,
            onValueChange = viewModel::onNameChange,
            label = { Text("Name") },
            singleLine = true,
            enabled = !busy,
            isError = TrailFormError.NAME_REQUIRED in state.errors,
            supportingText = { Text("${state.nameRemaining} left", modifier = Modifier.testTag("name-counter")) },
            modifier = Modifier.fillMaxWidth().testTag("trail-name"),
        )
        OutlinedTextField(
            value = state.description,
            onValueChange = viewModel::onDescriptionChange,
            label = { Text("Description") },
            minLines = 3,
            enabled = !busy,
            supportingText = { Text("${state.descriptionRemaining} left", modifier = Modifier.testTag("description-counter")) },
            modifier = Modifier.fillMaxWidth().testTag("trail-description"),
        )
        OutlinedTextField(
            value = state.location,
            onValueChange = viewModel::onLocationChange,
            label = { Text("General location (for example: Boulder, CO)") },
            singleLine = true,
            enabled = !busy,
            supportingText = { Text("${state.locationRemaining} left", modifier = Modifier.testTag("location-counter")) },
            modifier = Modifier.fillMaxWidth().testTag("trail-location"),
        )

        OutlinedTextField(
            value = state.lengthText,
            onValueChange = viewModel::onLengthChange,
            label = { Text("Length (miles)") },
            singleLine = true,
            enabled = !busy,
            isError = TrailFormError.LENGTH_NOT_POSITIVE in state.errors,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            supportingText = {
                Text(
                    when {
                        state.isEdit && state.lengthText == state.originalLengthText -> "As saved"
                        state.lengthEdited -> "Typed by you"
                        else -> "Worked out from your pins"
                    },
                )
            },
            modifier = Modifier.fillMaxWidth().testTag("trail-length"),
        )
        if (state.lengthEdited && !state.isEdit) {
            TextButton(onClick = viewModel::onResetLength, modifier = Modifier.testTag("reset-length")) { Text("Use the length from my pins") }
        }

        Text("Difficulty (1 is easy, 5 is hard)", style = AppStyle.type.caption, color = AppStyle.colors.textSecondary)
        Row(horizontalArrangement = Arrangement.spacedBy(d.sm)) {
            for (level in TrailLimits.DIFFICULTY_MIN..TrailLimits.DIFFICULTY_MAX) {
                FilterChip(
                    selected = state.difficulty == level,
                    onClick = { viewModel.onDifficultyChange(level) },
                    enabled = !busy,
                    label = { Text("$level") },
                    modifier = Modifier.testTag("difficulty-$level"),
                )
            }
        }

        var menuOpen by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = menuOpen, onExpandedChange = { if (!busy) menuOpen = it }) {
            OutlinedTextField(
                value = state.visibility.label(),
                onValueChange = {},
                readOnly = true,
                label = { Text("Who can see this trail") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuOpen) },
                modifier = Modifier.fillMaxWidth().menuAnchor().testTag("visibility-dropdown"),
            )
            ExposedDropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                TrailVisibility.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label()) },
                        onClick = { viewModel.onVisibilityChange(option); menuOpen = false },
                        modifier = Modifier.testTag("visibility-option-${option.name}"),
                    )
                }
            }
        }

        if (state.canRequestReview) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(value = state.requestVerification, enabled = !busy, role = Role.Switch, onValueChange = viewModel::onRequestVerificationChange)
                    .testTag("auto-review"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Auto submit for review", color = AppStyle.colors.textPrimary)
                    Text("An admin checks public trails and can mark them verified.", style = AppStyle.type.caption, color = AppStyle.colors.textSecondary)
                }
                Switch(checked = state.requestVerification, onCheckedChange = null, enabled = !busy)
            }
        }

        if (state.isEdit && state.photos.isEmpty()) {
            val saved = state.existingPhotoCount
            Text(
                text = if (saved == 0) "No pictures saved. Add some below." else "$saved ${if (saved == 1) "picture" else "pictures"} saved. Choose pictures to replace them all.",
                style = AppStyle.type.caption,
                color = AppStyle.colors.textSecondary,
                modifier = Modifier.testTag("existing-pictures"),
            )
        }
        OutlinedButton(
            onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            enabled = !busy && state.photos.size < TrailLimits.OWNER_PHOTOS,
            modifier = Modifier.fillMaxWidth().testTag("choose-pictures"),
        ) { Text(if (state.isEdit) "Replace pictures (${state.photos.size} of ${TrailLimits.OWNER_PHOTOS})" else "Add pictures (${state.photos.size} of ${TrailLimits.OWNER_PHOTOS})") }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(d.sm)) {
            items(state.photos, key = { it.toString() }) { uri ->
                Box(modifier = Modifier.size(96.dp).clip(RoundedCornerShape(d.radiusSm))) {
                    AsyncImage(model = uri, contentDescription = "Chosen picture", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    IconButton(
                        onClick = { viewModel.onRemovePhoto(uri) },
                        enabled = !busy,
                        modifier = Modifier.align(Alignment.TopEnd).size(32.dp),
                    ) { Icon(Icons.Filled.Close, "Remove picture", tint = AppStyle.colors.onAccent) }
                }
            }
        }

        if (state.showUnverifyWarning) {
            Text(
                text = "Saving changes will remove this trail's verification until an admin reviews it again.",
                style = AppStyle.type.caption,
                color = AppStyle.colors.downvote,
                modifier = Modifier.testTag("edit-unverify-warning"),
            )
        }

        Button(onClick = viewModel::onSave, enabled = !busy, modifier = Modifier.fillMaxWidth().testTag("save-trail")) {
            if (busy) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp) else Text(if (state.isEdit) "Save changes" else "Save trail")
        }
    }
}
