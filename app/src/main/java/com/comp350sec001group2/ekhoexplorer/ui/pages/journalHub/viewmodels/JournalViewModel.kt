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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class JournalViewModel(
    private val repository: JournalRepository
) : ViewModel() {
    val entries: StateFlow<List<JournalDraft>> =
        EhkoCache.activeUserFlow
            .flatMapLatest { user ->
                if (user.isSignedIn) {
                    repository.observeDrafts(user.userId)
                } else {
                    flowOf(emptyList())
                }
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                emptyList()
            )
}
val journalViewModelFactory = viewModelFactory {
    initializer {
        val app = this[APPLICATION_KEY] as EkhoApp
        JournalViewModel(app.container.journalRepository)
    }
}
