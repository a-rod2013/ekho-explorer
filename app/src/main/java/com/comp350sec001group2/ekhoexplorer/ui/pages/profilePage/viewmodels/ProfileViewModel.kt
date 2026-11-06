
package com.comp350sec001group2.ekhoexplorer.ui.pages.profilePage.viewmodels

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.comp350sec001group2.ekhoexplorer.Business.AppContainer
import com.comp350sec001group2.ekhoexplorer.Business.Cache.EhkoCache
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.Post
import com.comp350sec001group2.ekhoexplorer.EkhoApp
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

data class ProfileUiState(
    val displayName: String = "",
    val username: String = "",
    val avatarUrl: String? = null,
    val bio: String = "",
    val isEditingBio: Boolean = false,
    val bioDraft: String = bio,
    val joinDate: String = "",
    val observations: Int = 0,
    val species: Int = 0,
    val posts: List<Post> = emptyList(),
    val isLoading: Boolean = true,
    val isSavingBio: Boolean = false,
    val errorMessage: String? = null
)

class ProfileViewModel(
    private val container: AppContainer
) : ViewModel() {

    var uiState by mutableStateOf(ProfileUiState())
        private var loadedUserId: String? = null
        private set

    init {
        viewModelScope.launch {
            EhkoCache.activeUserFlow.collectLatest { activeUser ->
                loadedUserId = null

                if (!activeUser.isSignedIn) {
                    uiState = ProfileUiState(isLoading = false)
                    return@collectLatest
                }

                // Show the signed-in user's cached information immediately.
                uiState = ProfileUiState(
                    displayName = activeUser.userName,
                    username = activeUser.userName,
                    avatarUrl = activeUser.avatarUrl,
                    isLoading = true
                )

                try {
                    val profile = container.getUserProfile(activeUser.userId)

                    if (profile == null) {
                        uiState = uiState.copy(
                            isLoading = false,
                            errorMessage = "User profile not found."
                        )
                    } else {
                        loadedUserId = activeUser.userId

                        uiState = uiState.copy(
                            displayName = profile.username,
                            username = profile.username,
                            bio = profile.bio,
                            bioDraft = profile.bio,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w("ProfileViewModel", "Could not load profile", e)
                    uiState = uiState.copy(
                        isLoading = false,
                        errorMessage = "Could not load profile."
                    )
                }
            }
        }
    }

    fun updateBio(newBio: String) {
        uiState = uiState.copy(bio = newBio)
    }

    fun saveBio() {
        if (uiState.isSavingBio || uiState.isLoading || !uiState.isEditingBio) {
            return
        }

        val userId = loadedUserId ?: return
        val activeUser = EhkoCache.activeUser

        if (!activeUser.isSignedIn || activeUser.userId != userId) {
            return
        }

        val newBio = uiState.bioDraft

        if (newBio.length > 160) {
            uiState = uiState.copy(
                errorMessage = "Bio must be 160 characters or fewer."
            )
            return
        }

        uiState = uiState.copy(
            isSavingBio = true,
            errorMessage = null
        )

        viewModelScope.launch {
            try {
                container.updateUserBio(userId, newBio)

                if (loadedUserId == userId && EhkoCache.activeUser.userId == userId) {
                    uiState = uiState.copy(
                        bio = newBio,
                        bioDraft = newBio,
                        isEditingBio = false,
                        isSavingBio = false,
                        errorMessage = null
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("ProfileViewModel", "Could not save bio", e)

                if (loadedUserId == userId && EhkoCache.activeUser.userId == userId) {
                    uiState = uiState.copy(
                        isSavingBio = false,
                        errorMessage = "Could not save bio. Please try again."
                    )
                }
            }
        }
    }

    fun startEditingBio() {

        Log.d(
            "ProfileViewModel",
            "Edit clicker: isLoading =${uiState.isLoading}, loadedUserId=${loadedUserId != null}"
        )

        if (uiState.isLoading || loadedUserId == null) return

        uiState = uiState.copy(
            isEditingBio = true,
            bioDraft = uiState.bio,
            errorMessage = null
        )

        Log.d(
            "ProfileViewModel",
            "Editing started: ${uiState.isEditingBio}"
        )
    }

    fun updateBioDraft(newBio: String) {
        if (uiState.isSavingBio) return

        uiState = uiState.copy(
            bioDraft = newBio,
            errorMessage = null
        )
    }

    fun cancelEditingBio() {
        if (uiState.isSavingBio) return

        uiState = uiState.copy(
            isEditingBio = false,
            bioDraft = uiState.bio,
            errorMessage = null
        )
    }
}

val profileViewModelFactory = viewModelFactory {
    initializer {
        val app = this[APPLICATION_KEY] as EkhoApp
        ProfileViewModel(app.container)
    }
}
