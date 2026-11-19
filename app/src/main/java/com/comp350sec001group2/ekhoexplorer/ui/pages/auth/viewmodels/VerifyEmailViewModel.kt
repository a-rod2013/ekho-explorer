package com.comp350sec001group2.ekhoexplorer.ui.pages.auth.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.comp350sec001group2.ekhoexplorer.Business.Auth.AuthErrorKind
import com.comp350sec001group2.ekhoexplorer.Business.Auth.AuthException
import com.comp350sec001group2.ekhoexplorer.Business.Auth.AuthRepository
import com.comp350sec001group2.ekhoexplorer.Business.Auth.AuthState
import com.comp350sec001group2.ekhoexplorer.EkhoApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

enum class VerifyNotice { RESENT, NOT_YET }

data class VerifyEmailUiState(
    val email: String = "",
    val isChecking: Boolean = false,
    val isResending: Boolean = false,
    val notice: VerifyNotice? = null,
    val error: AuthErrorKind? = null,
)

class VerifyEmailViewModel(private val auth: AuthRepository) : ViewModel() {

    private val mutableState = MutableStateFlow(
        VerifyEmailUiState(email = (auth.state.value as? AuthState.AwaitingVerification)?.email.orEmpty()),
    )
    val uiState: StateFlow<VerifyEmailUiState> = mutableState.asStateFlow()

    fun onCheck() {
        if (mutableState.value.isChecking) return
        mutableState.update { it.copy(isChecking = true, notice = null, error = null) }
        viewModelScope.launch {
            try {
                val verified = auth.refreshVerification()
                if (!verified) mutableState.update { it.copy(notice = VerifyNotice.NOT_YET) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                mutableState.update { it.copy(error = (e as? AuthException)?.kind ?: AuthErrorKind.UNKNOWN) }
            } finally {
                mutableState.update { it.copy(isChecking = false) }
            }
        }
    }

    fun onResend() {
        if (mutableState.value.isResending) return
        mutableState.update { it.copy(isResending = true, notice = null, error = null) }
        viewModelScope.launch {
            try {
                auth.resendVerification()
                mutableState.update { it.copy(notice = VerifyNotice.RESENT) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                mutableState.update { it.copy(error = (e as? AuthException)?.kind ?: AuthErrorKind.UNKNOWN) }
            } finally {
                mutableState.update { it.copy(isResending = false) }
            }
        }
    }

    fun onSignOut() {
        viewModelScope.launch {
            try {
                auth.signOut()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Nothing more to do; the screen stays where it is.
            }
        }
    }
}

val verifyEmailViewModelFactory = viewModelFactory {
    initializer {
        val app = this[APPLICATION_KEY] as EkhoApp
        VerifyEmailViewModel(app.container.authRepository)
    }
}
