package com.comp350sec001group2.ekhoexplorer.ui.pages.auth.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.comp350sec001group2.ekhoexplorer.Business.Auth.AuthErrorKind
import com.comp350sec001group2.ekhoexplorer.Business.Auth.AuthException
import com.comp350sec001group2.ekhoexplorer.Business.Auth.AuthRepository
import com.comp350sec001group2.ekhoexplorer.Business.Auth.AuthValidator
import com.comp350sec001group2.ekhoexplorer.Business.Auth.InputProblem
import com.comp350sec001group2.ekhoexplorer.EkhoApp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

data class ForgotPasswordUiState(
    val email: String = "",
    val emailProblem: InputProblem? = null,
    val isLoading: Boolean = false,
    val sent: Boolean = false,
    val error: AuthErrorKind? = null,
)

class ForgotPasswordViewModel(private val auth: AuthRepository, initialEmail: String = "") : ViewModel() {

    private val mutableState = MutableStateFlow(ForgotPasswordUiState(email = initialEmail))
    val uiState: StateFlow<ForgotPasswordUiState> = mutableState.asStateFlow()

    fun onEmailChange(value: String) =
        mutableState.update { it.copy(email = value, emailProblem = null, error = null, sent = false) }

    fun onSubmit() {
        val current = mutableState.value
        if (current.isLoading) return

        val problem = AuthValidator.email(current.email)
        if (problem != null) {
            mutableState.update { it.copy(emailProblem = problem) }
            return
        }

        mutableState.update { it.copy(isLoading = true, error = null, sent = false) }
        viewModelScope.launch {
            try {
                auth.sendPasswordReset(current.email)
                mutableState.update { it.copy(sent = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val kind = (e as? AuthException)?.kind ?: AuthErrorKind.UNKNOWN
                mutableState.update { it.copy(error = kind) }
            } finally {
                mutableState.update { it.copy(isLoading = false) }
            }
        }
    }
}

val changePasswordViewModelFactory = viewModelFactory {
    initializer {
        val app = this[APPLICATION_KEY] as EkhoApp
        ForgotPasswordViewModel(
            app.container.authRepository,
            initialEmail = FirebaseAuth.getInstance().currentUser?.email.orEmpty(),
        )
    }
}

val forgotPasswordViewModelFactory = viewModelFactory {
    initializer {
        val app = this[APPLICATION_KEY] as EkhoApp
        ForgotPasswordViewModel(app.container.authRepository)
    }
}
