package com.comp350sec001group2.ekhoexplorer.ui.pages.journalHub.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.comp350sec001group2.ekhoexplorer.Business.Cache.EhkoCache
import com.comp350sec001group2.ekhoexplorer.Business.Journal.JournalDraft
import com.comp350sec001group2.ekhoexplorer.Business.Journal.JournalRepository
import com.comp350sec001group2.ekhoexplorer.EkhoApp
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine

@OptIn(ExperimentalCoroutinesApi::class)
class JournalViewModel(
    private val repository: JournalRepository
) : ViewModel() {

    // Holds the text currently entered in the search field.
    private val _searchQuery = MutableStateFlow("")

    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Allows the interface to observe the query without changing it directly
    val entries: StateFlow<List<JournalDraft>> =
        EhkoCache.activeUserFlow.flatMapLatest { user ->
            if (user.isSignedIn) {
                repository.observeDrafts(user.userId)
            } else {
                flowOf(emptyList())
            }
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList(),
            )

    // Recalculates matches whenever the entries or query change
    val filteredEntries: StateFlow<List<JournalDraft>> =
        combine(entries, searchQuery) { allEntries, query ->

            // Splits input into keywords, ignores extra spaces.
            val keywords = query.trim().split(Regex("\\s+")).filter { it.isNotBlank() }

            if (keywords.isEmpty()) {
                allEntries
            } else {
                allEntries.filter { entry ->

                    // Every keyword must appear in the title or notes.
                    keywords.all { keyword ->
                        entry.title.contains(
                            other = keyword,
                            ignoreCase = true,
                        ) || entry.notes.contains(
                            other = keyword,
                            ignoreCase = true,
                        )
                    }
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    // Suggests existing observation titles beginning with the query.
    val prefixSuggestions: StateFlow<List<String>> =
        combine(entries, searchQuery) { allEntries, query ->
            val prefix = query.trim()

            if (prefix.length < 2) {
                emptyList<String>()
            } else {
                allEntries
                    .flatMap { entry ->
                        val noteKeywords = entry.notes
                            .split(Regex("[^\\p{L}\\p{N}]+"))
                            .filter { it.length >= 2 }

                        listOf(entry.title.trim()) + noteKeywords
                    }
                    .filter { suggestion ->
                        suggestion.isNotBlank() &&
                            suggestion.startsWith(
                                prefix = prefix,
                                ignoreCase = true,
                            ) && !suggestion.equals(
                                other = prefix,
                                ignoreCase = true,
                            )
                    }
                    .distinctBy { it.lowercase() }
                    .sortedBy { it.lowercase()  }
                    .take(5)
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    // Called when the user types or whe search is cleared
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

}
val journalViewModelFactory = viewModelFactory {
    initializer {
        val app = this[APPLICATION_KEY] as EkhoApp
        JournalViewModel(app.container.journalRepository)
    }
}
sealed interface ObservationDetailsState {
    data object Loading : ObservationDetailsState

    data class Loaded(
        val entry: JournalDraft?) : ObservationDetailsState

    data object Error: ObservationDetailsState
}
@OptIn(ExperimentalCoroutinesApi::class)
class ObservationDetailsViewModel(
    repository: JournalRepository,
    entryId: String,
) : ViewModel() {

    val state: StateFlow<ObservationDetailsState> =
        EhkoCache.activeUserFlow.flatMapLatest { user ->
            if (user.isSignedIn) {
                repository.observeDrafts(user.userId)
                    .map<List<JournalDraft>, ObservationDetailsState> {
                        entries -> ObservationDetailsState.Loaded(
                            entry = entries.firstOrNull {
                                it.id == entryId
                            },
                        )
                    }
                    .onStart { emit(ObservationDetailsState.Loading)
                    }
                    .catch { emit(ObservationDetailsState.Error)
                    }
            } else {
                flowOf<ObservationDetailsState>(
                    ObservationDetailsState.Loaded(
                        entry = null,
                    )
                )
            }
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = ObservationDetailsState.Loading,
            )
}

fun observationDetailsViewModelFactory(entryId: String) =
    viewModelFactory {
        initializer {
            val app = this[APPLICATION_KEY] as EkhoApp

            ObservationDetailsViewModel(
                repository = app.container.journalRepository,
                entryId = entryId,
            )
        }
    }
