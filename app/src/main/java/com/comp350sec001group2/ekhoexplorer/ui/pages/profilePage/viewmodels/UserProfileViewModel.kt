package com.comp350sec001group2.ekhoexplorer.ui.pages.profilePage.viewmodels

import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.comp350sec001group2.ekhoexplorer.Business.AppContainer
import com.comp350sec001group2.ekhoexplorer.Business.Cache.EhkoCache
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.Post
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.Vote
import com.comp350sec001group2.ekhoexplorer.Business.Photos.PhotoLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.coroutines.cancellation.CancellationException

class UserProfileViewModel(
    private val targetUserId: String,
    private val container: AppContainer
) : ViewModel() {

    val photos: PhotoLoader = container.photoRepository

    private val joinDateFormatter = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

    var uiState by mutableStateOf(ProfileUiState())
        private set

    init {
        loadProfile()
    }

    private fun loadProfile() {
        if (targetUserId.isBlank()) {
            uiState = ProfileUiState(isLoading = false, errorMessage = "User not found.")
            return
        }

        uiState = ProfileUiState(isLoading = true)

        viewModelScope.launch {
            try {
                val profile = container.getUserProfile(targetUserId)

                if (profile == null) {
                    uiState = ProfileUiState(isLoading = false, errorMessage = "User profile not found.")
                    return@launch
                }

                uiState = ProfileUiState(
                    displayName = profile.username,
                    username = profile.username,
                    bio = profile.bio,
                    joinDate = profile.createdAt?.let { joinDateFormatter.format(it) } ?: "",
                    isLoading = false
                )

                loadAvatar()
                loadUserPosts()

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("UserProfileViewModel", "Could not load profile for $targetUserId", e)
                uiState = ProfileUiState(isLoading = false, errorMessage = "Could not load profile.")
            }
        }
    }

    fun onPostVote(postId: String, vote: Vote) {
        val currentPosts = uiState.posts
        val targetPost = currentPosts.firstOrNull { it.id == postId } ?: return

        val oldVote = targetPost.myVote
        val newVote = if (oldVote == vote) Vote.NONE else vote

        var upDelta = 0
        var downDelta = 0

        if (oldVote == Vote.UP) upDelta -= 1
        if (oldVote == Vote.DOWN) downDelta -= 1

        if (newVote == Vote.UP) upDelta += 1
        if (newVote == Vote.DOWN) downDelta += 1

        val updatedPosts = currentPosts.map { post ->
            if (post.id == postId) {
                post.copy(
                    myVote = newVote,
                    upCount = (post.upCount + upDelta).coerceAtLeast(0),
                    downCount = (post.downCount + downDelta).coerceAtLeast(0)
                )
            } else post
        }

        uiState = uiState.copy(posts = updatedPosts)

        viewModelScope.launch {
            try {
                container.feedRepository.vote(postId, vote)
            } catch (e: Exception) {
                Log.w("UserProfileViewModel", "Could not vote on post $postId", e)
            }
        }
    }

    private suspend fun loadAvatar() {
        try {
            val avatar = container.getUserAvatar(targetUserId)
            val bitmap = avatar?.let {
                withContext(Dispatchers.Default) {
                    BitmapFactory.decodeByteArray(it.bytes, 0, it.bytes.size)
                }
            }
            uiState = uiState.copy(avatarBitmap = bitmap)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("UserProfileViewModel", "Could not load avatar for $targetUserId", e)
        }
    }

    private suspend fun loadUserPosts() {
        uiState = uiState.copy(isLoadingPosts = true)
        try {
            val userPosts = container.getUserPosts(targetUserId)
            val activeUser = EhkoCache.activeUser
            val votes = if (activeUser.isSignedIn) {
                container.getUserPostVotes(activeUser.userId, userPosts.map { it.id })
            } else emptyMap()

            val postsWithVotes = userPosts.map { post ->
                post.copy(myVote = votes[post.id] ?: Vote.NONE)
            }.sortedWith(
                compareByDescending<Post> { it.createdAtMillis }.thenByDescending { it.id }
            )

            uiState = uiState.copy(
                posts = postsWithVotes,
                observations = postsWithVotes.size,
                isLoadingPosts = false
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("UserProfileViewModel", "Could not load posts for $targetUserId", e)
            uiState = uiState.copy(isLoadingPosts = false)
        }
    }
}

class UserProfileViewModelFactory(
    private val userId: String,
    private val container: AppContainer
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return UserProfileViewModel(userId, container) as T
    }
}
