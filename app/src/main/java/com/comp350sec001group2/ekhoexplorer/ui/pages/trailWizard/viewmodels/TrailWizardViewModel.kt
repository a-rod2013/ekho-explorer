package com.comp350sec001group2.ekhoexplorer.ui.pages.trailWizard.viewmodels


import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreException
import com.comp350sec001group2.ekhoexplorer.Business.Location.FusedLocationProvider
import com.comp350sec001group2.ekhoexplorer.Business.Location.LocationAccess
import com.comp350sec001group2.ekhoexplorer.Business.Location.LocationAccessReader
import com.comp350sec001group2.ekhoexplorer.Business.Location.AndroidLocationAccessReader
import com.comp350sec001group2.ekhoexplorer.Business.Location.LocationProvider
import com.comp350sec001group2.ekhoexplorer.Business.Photos.PhotoErrorKind
import com.comp350sec001group2.ekhoexplorer.Business.Photos.PhotoException
import com.comp350sec001group2.ekhoexplorer.Business.Photos.PhotoPreparer
import com.comp350sec001group2.ekhoexplorer.Business.Trails.Fix
import com.comp350sec001group2.ekhoexplorer.Business.Trails.LatLngPoint
import com.comp350sec001group2.ekhoexplorer.Business.Cache.EhkoCache
import com.comp350sec001group2.ekhoexplorer.Business.Trails.Trail
import com.comp350sec001group2.ekhoexplorer.Business.Trails.TrailFormError
import com.comp350sec001group2.ekhoexplorer.Business.Trails.TrailFormat
import com.comp350sec001group2.ekhoexplorer.Business.Trails.TrailGeo
import com.comp350sec001group2.ekhoexplorer.Business.Trails.TrailInput
import com.comp350sec001group2.ekhoexplorer.Business.Trails.TrailLimits
import com.comp350sec001group2.ekhoexplorer.Business.Trails.TrailRepository
import com.comp350sec001group2.ekhoexplorer.Business.Trails.TrailStatus
import com.comp350sec001group2.ekhoexplorer.Business.Trails.TrailValidator
import com.comp350sec001group2.ekhoexplorer.Business.Trails.TrailVisibility
import com.comp350sec001group2.ekhoexplorer.Business.Trails.WalkOptions
import com.comp350sec001group2.ekhoexplorer.Business.Trails.WalkPhase
import com.comp350sec001group2.ekhoexplorer.Business.Trails.WalkSession
import com.comp350sec001group2.ekhoexplorer.Business.Trails.WalkUiState
import com.comp350sec001group2.ekhoexplorer.Business.Trails.walkOptions
import com.comp350sec001group2.ekhoexplorer.EkhoApp
import com.comp350sec001group2.ekhoexplorer.ui.pages.trailWizard.TrailDrawState
import com.comp350sec001group2.ekhoexplorer.ui.pages.trailWizard.message
import com.comp350sec001group2.ekhoexplorer.ui.pages.trailWizard.trailMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

enum class WizardStep { MAP, FORM }
enum class MapMode { PINS, WALK }

data class TrailWizardUiState(
    val step: WizardStep = WizardStep.MAP,
    val points: List<LatLngPoint> = emptyList(),
    val selectedPin: Int? = null,
    val name: String = "",
    val description: String = "",
    val location: String = "",
    val lengthText: String = "0.00",
    val lengthEdited: Boolean = false,
    val difficulty: Int = 3,
    val visibility: TrailVisibility = TrailVisibility.PUBLIC,
    val requestVerification: Boolean = false,
    val photos: List<Uri> = emptyList(),
    val errors: List<TrailFormError> = emptyList(),
    val message: String? = null,
    val isSaving: Boolean = false,
    val savedTrailId: String? = null,
    val userLocation: LatLngPoint? = null,
    val mode: MapMode = MapMode.PINS,
    val walkPhase: WalkPhase = WalkPhase.IDLE,
    val walkOptions: WalkOptions = walkOptions(LocationAccess.DENIED),
    val pathFromWalk: Boolean = false,
    val editingTrailId: String? = null,
    val existingPhotoCount: Int = 0,
    val editingStatus: TrailStatus? = null,
    val editorIsAdmin: Boolean = false,
    val originalLengthText: String? = null,
    val originalLengthMeters: Double = 0.0,
) {
    val isEdit: Boolean get() = editingTrailId != null

    val showUnverifyWarning: Boolean
        get() = editingStatus == TrailStatus.VERIFIED && (!editorIsAdmin || visibility != TrailVisibility.PUBLIC)

    val nameRemaining: Int get() = TrailLimits.NAME_MAX - name.length
    val descriptionRemaining: Int get() = TrailLimits.DESCRIPTION_MAX - description.length
    val locationRemaining: Int get() = TrailLimits.LOCATION_MAX - location.length

    val canRequestReview: Boolean get() = visibility == TrailVisibility.PUBLIC

    val lengthMeters: Double
        get() = if (originalLengthText != null && lengthText == originalLengthText) originalLengthMeters else TrailFormat.metersFromMiles(lengthText) ?: 0.0
}

class TrailWizardViewModel(
    private val repository: TrailRepository,
    private val preparer: PhotoPreparer,
    private val resolver: ContentResolver,
    location: LocationProvider? = null,
    private val walk: WalkSession? = null,
    private val access: LocationAccessReader? = null,
    editing: Trail? = null,
) : ViewModel() {

    private val draw = TrailDrawState()
    private val _state = MutableStateFlow(initialState(editing))
    val state: StateFlow<TrailWizardUiState> = _state

    private var lastWalkMessage: String? = null

    init {
        refreshWalkOptions()
        if (walk != null) {
            viewModelScope.launch { walk.state.collect { applyWalk(it) } }
        }
        if (location != null) {
            viewModelScope.launch {
                val here = try {
                    location.lastKnown()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    null // no permission or no fix: the map just starts where it starts
                }
                _state.update { it.copy(userLocation = here) }
            }
        }
    }

    fun onPinAdded(point: LatLngPoint) = withPins {
        when {
            _state.value.pathFromWalk -> "This path was walked. Clear it to drop pins instead."
            _state.value.mode == MapMode.WALK -> "Switch to Drop pins to place pins."
            draw.add(point) -> null
            else -> "A trail can have at most ${TrailLimits.MAX_PINS} pins."
        }
    }

    fun onMapTapped(point: LatLngPoint) {
        val selected = draw.selected
        if (selected != null) {
            withPins { draw.move(selected, point); draw.select(null); null }
        } else {
            onPinAdded(point)
        }
    }

    fun onPinSelected(index: Int) = withPins { draw.select(if (draw.selected == index) null else index); null }
    fun onUndoPin() = withPins { draw.undo(); null }
    fun onClearPins() {
        if (walk != null && _state.value.walkPhase != WalkPhase.IDLE) walk.discard()
        withPins { draw.clear(); null }
        _state.update { it.copy(pathFromWalk = false) }
    }

    fun onModeChange(mode: MapMode) {
        if (_state.value.walkPhase == WalkPhase.IDLE) _state.update { it.copy(mode = mode) }
    }

    fun onStartWalk() {
        val session = walk ?: return
        if (_state.value.points.isNotEmpty()) {
            _state.update { it.copy(message = "Clear the map first, then walk the trail.") }
            return
        }
        val now = currentAccess()
        refreshWalkOptions(now)
        if (!session.start(now)) {
            _state.update { it.copy(message = session.state.value.message ?: walkOptions(now).message) }
        }
    }

    fun onWalkFix(fix: Fix) { walk?.onFix(fix) }

    fun onPauseWalk() { walk?.pause() }

    fun onResumeWalk() {
        val session = walk ?: return
        if (!session.resume(currentAccess())) {
            _state.update { it.copy(message = session.state.value.message) }
        }
    }

    fun onFinishWalk() {
        val session = walk ?: return
        val path = session.finish()
        draw.replaceAll(path)
        _state.update {
            it.copy(
                points = path,
                selectedPin = null,
                pathFromWalk = path.isNotEmpty(),
                mode = MapMode.PINS,
                walkPhase = WalkPhase.IDLE,
                lengthText = if (it.lengthEdited) it.lengthText else calculatedLength(path),
            )
        }
    }

    fun onDiscardWalk() {
        walk?.discard()
        draw.clear()
        _state.update { it.copy(points = emptyList(), pathFromWalk = false, walkPhase = WalkPhase.IDLE, lengthText = if (it.lengthEdited) it.lengthText else calculatedLength(emptyList())) }
    }

    fun onScreenStopped() { walk?.onAppLeft(currentAccess()) }

    fun onScreenStarted() {
        walk?.onAppReturned()
        onAccessMayHaveChanged()
    }

    fun onAccessMayHaveChanged() {
        val now = currentAccess()
        refreshWalkOptions(now)
        walk?.onAccessChanged(now)
    }

    private fun currentAccess(): LocationAccess = access?.read() ?: LocationAccess.DENIED

    private fun refreshWalkOptions(now: LocationAccess = currentAccess()) {
        _state.update { it.copy(walkOptions = walkOptions(now)) }
    }

    private fun applyWalk(w: WalkUiState) {
        val newMessage = w.message.takeIf { it != lastWalkMessage }
        lastWalkMessage = w.message
        _state.update { s ->
            val active = w.phase != WalkPhase.IDLE
            val points = if (active) w.points else s.points
            s.copy(
                walkPhase = w.phase,
                points = points,
                mode = if (active) MapMode.WALK else s.mode,
                lengthText = if (active && !s.lengthEdited) calculatedLength(points) else s.lengthText,
                message = newMessage ?: s.message,
            )
        }
    }

    fun onNext() {
        if (_state.value.points.size < 2) {
            _state.update { it.copy(message = TrailFormError.PATH_TOO_SHORT.message()) }
        } else {
            _state.update { it.copy(step = WizardStep.FORM, message = null) }
        }
    }

    fun onStepBack() {
        // An edited trail has no map step to go back to.
        if (!_state.value.isEdit) _state.update { it.copy(step = WizardStep.MAP, message = null) }
    }

    fun onNameChange(text: String) = _state.update { it.copy(name = text.take(TrailLimits.NAME_MAX), errors = it.errors - TrailFormError.NAME_REQUIRED) }
    fun onDescriptionChange(text: String) = _state.update { it.copy(description = text.take(TrailLimits.DESCRIPTION_MAX)) }
    fun onLocationChange(text: String) = _state.update { it.copy(location = text.take(TrailLimits.LOCATION_MAX)) }

    fun onLengthChange(text: String) = _state.update { it.copy(lengthText = text, lengthEdited = true, errors = it.errors - TrailFormError.LENGTH_NOT_POSITIVE) }

    fun onResetLength() = _state.update { it.copy(lengthText = calculatedLength(it.points), lengthEdited = false) }

    fun onDifficultyChange(value: Int) {
        if (value in TrailLimits.DIFFICULTY_MIN..TrailLimits.DIFFICULTY_MAX) _state.update { it.copy(difficulty = value) }
    }

    fun onVisibilityChange(value: TrailVisibility) = _state.update {
        it.copy(visibility = value, requestVerification = it.requestVerification && value == TrailVisibility.PUBLIC)
    }

    fun onRequestVerificationChange(value: Boolean) = _state.update { it.copy(requestVerification = value && it.canRequestReview) }

    fun onPhotosPicked(uris: List<Uri>) = _state.update {
        val merged = (it.photos + uris).distinct()
        it.copy(
            photos = merged.take(TrailLimits.OWNER_PHOTOS),
            message = if (merged.size > TrailLimits.OWNER_PHOTOS) "A trail can have at most ${TrailLimits.OWNER_PHOTOS} pictures." else null,
        )
    }

    fun onRemovePhoto(uri: Uri) = _state.update { it.copy(photos = it.photos - uri, message = null) }

    fun onMessageShown() = _state.update { it.copy(message = null) }

    fun onSave() {
        val current = _state.value
        if (current.isSaving) return
        val replacingPictures = current.photos.isNotEmpty()
        val input = TrailInput(
            name = current.name.trim(),
            description = current.description.trim(),
            location = current.location.trim(),
            lengthMeters = current.lengthMeters,
            difficulty = current.difficulty,
            visibility = current.visibility,
            photoCount = if (current.isEdit && !replacingPictures) current.existingPhotoCount else current.photos.size,
        )
        val problems = TrailValidator.validate(input, if (current.isEdit) 2 else current.points.size)
        if (problems.isNotEmpty()) {
            _state.update { it.copy(errors = problems, message = problems.first().message()) }
            return
        }

        _state.update { it.copy(isSaving = true, errors = emptyList(), message = null) }
        viewModelScope.launch {
            try {
                val sources = current.photos.map { uri ->
                    {
                        resolver.openInputStream(uri)
                            ?: throw PhotoException(PhotoErrorKind.INVALID_IMAGE, "Could not open the picture")
                    }
                }
                val prepared = preparer.prepare(sources)
                val wantsReview = current.requestVerification && current.canRequestReview
                val id = if (current.editingTrailId != null) {
                    repository.update(current.editingTrailId, input, wantsReview, prepared.takeIf { replacingPictures })
                    current.editingTrailId
                } else {
                    repository.create(input, current.points, prepared, wantsReview)
                }
                _state.update { it.copy(isSaving = false, savedTrailId = id) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: PhotoException) {
                _state.update { it.copy(isSaving = false, message = e.kind.trailMessage()) }
            } catch (e: FirestoreException) {
                _state.update { it.copy(isSaving = false, message = "Could not save the trail (${e.kind}). Please try again.") }
            }
        }
    }

    private fun withPins(change: () -> String?) {
        val message = change()
        _state.update {
            val points = draw.points
            it.copy(
                points = points,
                selectedPin = draw.selected,
                lengthText = if (it.lengthEdited) it.lengthText else calculatedLength(points),
                message = message,
            )
        }
    }

    private fun calculatedLength(points: List<LatLngPoint>): String =
        String.format(Locale.US, "%.2f", TrailGeo.lengthMeters(points) / METERS_PER_MILE)

    private companion object {
        const val METERS_PER_MILE = 1609.344

        fun initialState(editing: Trail?): TrailWizardUiState {
            if (editing == null) return TrailWizardUiState()
            return TrailWizardUiState(
                step = WizardStep.FORM,
                name = editing.name,
                description = editing.description,
                location = editing.location,
                lengthText = String.format(Locale.US, "%.2f", editing.lengthMeters / METERS_PER_MILE),
                lengthEdited = true,
                originalLengthText = String.format(Locale.US, "%.2f", editing.lengthMeters / METERS_PER_MILE),
                originalLengthMeters = editing.lengthMeters,
                difficulty = editing.difficulty,
                visibility = editing.visibility,
                requestVerification = editing.verificationRequested,
                editingTrailId = editing.id,
                existingPhotoCount = editing.photoCount,
                editingStatus = editing.status,
                editorIsAdmin = EhkoCache.activeUser.isAdmin,
            )
        }
    }
}

val trailWizardViewModelFactory = viewModelFactory {
    initializer {
        val app = this[APPLICATION_KEY] as EkhoApp
        TrailWizardViewModel(
            app.container.trailRepository,
            app.container.photoPreparer,
            app.contentResolver,
            FusedLocationProvider(app),
            app.container.walkSession,
            AndroidLocationAccessReader(app),
        )
    }
}
