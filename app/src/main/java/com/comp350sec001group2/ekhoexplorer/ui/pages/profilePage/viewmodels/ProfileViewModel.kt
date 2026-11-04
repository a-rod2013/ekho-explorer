package com.comp350sec001group2.ekhoexplorer.ui.pages.profilePage.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.Post
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.SamplePosts
import androidx.lifecycle.ViewModel

data class ProfileUiState(

    val displayName: String = "Maya",
    val username: String = "maya.hikes",
    val avatarUrl: String? = SamplePosts.all.first { it.username == "maya.hikes" }.avatarUrl,
    val bio: String = "This is a test bio for the profile screen.",
    val isEditingBio: Boolean = false,
    val bioDraft: String = bio,
    val joinDate: String = "November 2026",
    val observations: Int = 0,
    val species: Int = 0,
    val posts: List<Post> = SamplePosts.all.filter {
        it.username == "maya.hikes"
    }


)

class ProfileViewModel : ViewModel() {
    var uiState by mutableStateOf(ProfileUiState())
        private set

    fun updateBio(newBio: String) {
        uiState = uiState.copy(bio = newBio)
    }

    fun saveBio() {
        uiState = uiState.copy(
            bio = uiState.bioDraft,
            isEditingBio = false
        )
    }

    fun startEditingBio() {
        uiState = uiState.copy(
            isEditingBio = true,
            bioDraft = uiState.bio
        )
    }

    fun updateBioDraft(newBio: String) {
        uiState = uiState.copy(bioDraft = newBio)
    }

    fun cancelEditingBio() {
        uiState = uiState.copy(
            isEditingBio = false,
            bioDraft = uiState.bio
        )
    }
}
