package com.comp350sec001group2.ekhoexplorer.ui.pages.activity.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.comp350sec001group2.ekhoexplorer.Business.Cache.EhkoCache
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreException
import com.comp350sec001group2.ekhoexplorer.Business.Location.AndroidLocationAccessReader
import com.comp350sec001group2.ekhoexplorer.Business.Location.FusedLocationProvider
import com.comp350sec001group2.ekhoexplorer.Business.Location.LocationAccess
import com.comp350sec001group2.ekhoexplorer.Business.Location.LocationAccessReader
import com.comp350sec001group2.ekhoexplorer.Business.Location.LocationProvider
import com.comp350sec001group2.ekhoexplorer.Business.Trails.LatLngPoint
import com.comp350sec001group2.ekhoexplorer.Business.Trails.Trail
import com.comp350sec001group2.ekhoexplorer.Business.Trails.TrailGeo
import com.comp350sec001group2.ekhoexplorer.Business.Trails.TrailPhotoLoader
import com.comp350sec001group2.ekhoexplorer.Business.Trails.TrailRepository
import com.comp350sec001group2.ekhoexplorer.Business.Trails.TrailRules
import com.comp350sec001group2.ekhoexplorer.Business.Trails.TrailSort
import com.comp350sec001group2.ekhoexplorer.Business.Trails.TrailSorting
import com.comp350sec001group2.ekhoexplorer.Business.Trails.toViewer
import com.comp350sec001group2.ekhoexplorer.EkhoApp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TrailListUiState(
    val trails: List<Trail> = emptyList(),
    val popular: List<Trail> = emptyList(),
    val distances: Map<String, Double> = emptyMap(),
    val sort: TrailSort = TrailSort.POPULARITY,
    val ascending: Boolean = false,
    val locationNote: String? = null,
    val currentUserId: String = "",
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val hasError: Boolean = false,
)

class TrailListViewModel(
    private val repository: TrailRepository,
    private val location: LocationProvider,
    private val access: LocationAccessReader,
) : ViewModel() {

    val photos: TrailPhotoLoader get() = repository

    private data class Status(
        val loading: Boolean = true,
        val refreshing: Boolean = false,
        val error: Boolean = false,
        val located: Boolean = false,
    )

    private val accessState = MutableStateFlow(access.read())
    private val origin = MutableStateFlow<LatLngPoint?>(null)
    private val chosen = MutableStateFlow<Pair<TrailSort, Boolean>?>(null)
    private val status = MutableStateFlow(Status())

    val uiState: StateFlow<TrailListUiState> =
        combine(repository.trails, accessState, origin, chosen, EhkoCache.activeUserFlow) { loaded, accessNow, from, picked, user ->
            val viewer = user.toViewer()
            val visible = loaded.filter { TrailRules.canView(it, viewer) }
            val sort = picked?.first ?: TrailSorting.defaultSort(hasLocation = from != null)
            val ascending = picked?.second ?: TrailSorting.defaultAscending(sort)
            Snapshot(
                trails = TrailSorting.sort(visible, sort, ascending, from),
                popular = TrailSorting.sort(visible, TrailSort.POPULARITY, ascending = false, origin = null).take(POPULAR_COUNT),
                distances = from?.let { o -> visible.mapNotNull { t -> t.start?.let { t.id to TrailGeo.distanceMeters(o, it) } }.toMap() }.orEmpty(),
                sort = sort,
                ascending = ascending,
                access = accessNow,
                hasOrigin = from != null,
                userId = user.userId,
            )
        }.combine(status) { s, st ->
            TrailListUiState(
                trails = s.trails,
                popular = s.popular,
                distances = s.distances,
                sort = s.sort,
                ascending = s.ascending,
                locationNote = locationNote(s.access, s.hasOrigin, st.located),
                currentUserId = s.userId,
                isLoading = st.loading,
                isRefreshing = st.refreshing,
                hasError = st.error,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TrailListUiState())

    private class Snapshot(
        val trails: List<Trail>,
        val popular: List<Trail>,
        val distances: Map<String, Double>,
        val sort: TrailSort,
        val ascending: Boolean,
        val access: LocationAccess,
        val hasOrigin: Boolean,
        val userId: String,
    )

    init {
        viewModelScope.launch { load(isRefresh = false) }
        viewModelScope.launch {
            EhkoCache.activeUserFlow
                .map { Triple(it.userId, it.isAdmin, it.following) }
                .distinctUntilChanged()
                .drop(1)
                .collect { load(isRefresh = true) }
        }
    }

    fun onSort(sort: TrailSort) {
        val current = chosen.value ?: (uiState.value.sort to uiState.value.ascending)
        chosen.value = if (current.first == sort) sort to !current.second else sort to TrailSorting.defaultAscending(sort)
    }

    fun onRefresh() {
        viewModelScope.launch { load(isRefresh = true) }
    }

    fun onRequestVerification(trailId: String) {
        viewModelScope.launch {
            try {
                repository.requestVerification(trailId)
                status.update { it.copy(error = false) }
            } catch (e: FirestoreException) {
                status.update { it.copy(error = true) }
            }
        }
    }

    private suspend fun load(isRefresh: Boolean) {
        status.update { it.copy(loading = !isRefresh && it.loading, refreshing = isRefresh, error = false) }
        val now = access.read()
        accessState.value = now
        origin.value = if (now.canLocate) {
            try {
                location.lastKnown()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
        status.update { it.copy(located = true) }
        val failed = try {
            repository.refresh()
            false
        } catch (e: FirestoreException) {
            true
        }
        status.update { it.copy(loading = false, refreshing = false, error = failed) }
    }

    private fun locationNote(access: LocationAccess, hasOrigin: Boolean, located: Boolean): String? = when {
        !located -> null
        !access.canLocate -> "Location is off or not allowed, so trails are sorted by popularity. Turn location on to sort by distance."
        !hasOrigin -> "Could not find your location, so trails are sorted by popularity."
        else -> null
    }
}

const val POPULAR_COUNT = 3

val trailListViewModelFactory = viewModelFactory {
    initializer {
        val app = this[APPLICATION_KEY] as EkhoApp
        TrailListViewModel(app.container.trailRepository, FusedLocationProvider(app), AndroidLocationAccessReader(app))
    }
}
