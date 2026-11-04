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
}
