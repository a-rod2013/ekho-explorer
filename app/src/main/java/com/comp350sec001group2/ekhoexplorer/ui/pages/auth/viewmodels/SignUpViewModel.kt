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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

data class SignUpUiState(
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val usernameProblem: InputProblem? = null,
    val emailProblem: InputProblem? = null,
    val passwordProblem: InputProblem? = null,
    val confirmProblem: InputProblem? = null,
    val isLoading: Boolean = false,
    val error: AuthErrorKind? = null,
)

class SignUpViewModel(private val auth: AuthRepository) : ViewModel() {

    private val mutableState = MutableStateFlow(SignUpUiState())
    val uiState: StateFlow<SignUpUiState> = mutableState.asStateFlow()

    fun onUsernameChange(value: String) =
        mutableState.update { it.copy(username = value, usernameProblem = null, error = null) }

    fun onEmailChange(value: String) =
        mutableState.update { it.copy(email = value, emailProblem = null, error = null) }

    fun onPasswordChange(value: String) =
        mutableState.update { it.copy(password = value, passwordProblem = null, confirmProblem = null, error = null) }

    fun onConfirmPasswordChange(value: String) =
        mutableState.update { it.copy(confirmPassword = value, confirmProblem = null, error = null) }

    fun onSubmit() {
        val current = mutableState.value
        if (current.isLoading) return

        val usernameProblem = AuthValidator.username(current.username)
        val emailProblem = AuthValidator.email(current.email)
        val passwordProblem = AuthValidator.newPassword(current.password)
        val confirmProblem = AuthValidator.confirmPassword(current.password, current.confirmPassword)
        if (listOf(usernameProblem, emailProblem, passwordProblem, confirmProblem).any { it != null }) {
            mutableState.update {
                it.copy(
                    usernameProblem = usernameProblem,
                    emailProblem = emailProblem,
                    passwordProblem = passwordProblem,
                    confirmProblem = confirmProblem,
                )
            }
            return
        }

        mutableState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                auth.signUp(current.username, current.email, current.password)
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

val signUpViewModelFactory = viewModelFactory {
    initializer {
        val app = this[APPLICATION_KEY] as EkhoApp
        SignUpViewModel(app.container.authRepository)
    }
}
