package com.comp350sec001group2.ekhoexplorer.ui.pages.auth.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.comp350sec001group2.ekhoexplorer.Business.Auth.AuthRepository
import com.comp350sec001group2.ekhoexplorer.Business.Auth.AuthState
import com.comp350sec001group2.ekhoexplorer.EkhoApp
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

class SessionViewModel(private val auth: AuthRepository) : ViewModel() {

    val authState: StateFlow<AuthState> = auth.state

    fun signOut() {
        viewModelScope.launch {
            try {
                auth.signOut()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("SessionViewModel", "Sign-out failed", e)
            }
        }
    }
}

val sessionViewModelFactory = viewModelFactory {
    initializer {
        val app = this[APPLICATION_KEY] as EkhoApp
        SessionViewModel(app.container.authRepository)
    }
}
