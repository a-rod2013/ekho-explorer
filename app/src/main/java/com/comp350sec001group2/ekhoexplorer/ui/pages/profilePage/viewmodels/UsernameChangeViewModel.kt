package com.comp350sec001group2.ekhoexplorer.ui.pages.profilePage.viewmodels


import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.comp350sec001group2.ekhoexplorer.Business.Auth.AuthValidator
import com.comp350sec001group2.ekhoexplorer.Business.Auth.InputProblem
import com.comp350sec001group2.ekhoexplorer.Business.Auth.SupportRepository
import com.comp350sec001group2.ekhoexplorer.Business.Cache.EhkoCache
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.SupportRequestStatus
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.UsernameChangeRequest
import com.comp350sec001group2.ekhoexplorer.EkhoApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

data class UsernameChangeUiState(
    val isLoading: Boolean = true,
    val currentUsername: String = "",
    /** The person's latest request, shown above the form (pending, or how the last one ended). */
    val existing: UsernameChangeRequest? = null,
    val requested: String = "",
    val reason: String = "",
    val requestedProblem: InputProblem? = null,
    val reasonProblem: InputProblem? = null,
    val isSubmitting: Boolean = false,
    val submitted: Boolean = false,
    val error: AccountError? = null,
) {
    val hasPending: Boolean get() = existing?.status == SupportRequestStatus.PENDING || submitted
    val canShowForm: Boolean get() = !isLoading && !hasPending && currentUsername.isNotEmpty()
}

class UsernameChangeViewModel(
    private val support: SupportRepository,
    private val userId: String,
) : ViewModel() {

    private val mutableState = MutableStateFlow(UsernameChangeUiState())
    val uiState: StateFlow<UsernameChangeUiState> = mutableState.asStateFlow()

    init {
        load()
    }

    fun load() {
        mutableState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                // The name on the users document is the one the security rules compare against.
                val username = support.currentUsername(userId).orEmpty()
                val existing = support.usernameChangeRequest(userId)
                mutableState.update { it.copy(isLoading = false, currentUsername = username, existing = existing) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                mutableState.update { it.copy(isLoading = false, error = e.toAccountError()) }
            }
        }
    }

    fun onRequestedChange(value: String) =
        mutableState.update { it.copy(requested = value, requestedProblem = null, error = null) }

    fun onReasonChange(value: String) =
        mutableState.update { it.copy(reason = value, reasonProblem = AuthValidator.reason(value), error = null) }

    fun submit() {
        val current = mutableState.value
        if (current.isSubmitting || !current.canShowForm) return

        val requestedProblem = AuthValidator.requestedUsername(current.requested, current.currentUsername)
        val reasonProblem = AuthValidator.reason(current.reason)
        if (requestedProblem != null || reasonProblem != null) {
            mutableState.update { it.copy(requestedProblem = requestedProblem, reasonProblem = reasonProblem) }
            return
        }

        mutableState.update { it.copy(isSubmitting = true, error = null) }
        viewModelScope.launch {
            try {
                support.requestUsernameChange(userId, current.currentUsername, current.requested.trim(), current.reason.trim())
                mutableState.update { it.copy(submitted = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                mutableState.update { it.copy(error = e.toAccountError()) }
            } finally {
                mutableState.update { it.copy(isSubmitting = false) }
            }
        }
    }
}

val usernameChangeViewModelFactory = viewModelFactory {
    initializer {
        val app = this[APPLICATION_KEY] as EkhoApp
        UsernameChangeViewModel(
            support = app.container.supportRepository,
            userId = EhkoCache.activeUser.userId,
        )
    }
}
