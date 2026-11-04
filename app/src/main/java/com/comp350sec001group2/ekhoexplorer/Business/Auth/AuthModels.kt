package com.comp350sec001group2.ekhoexplorer.Business.Auth

data class AuthIdentity(
    val uid: String,
    val email: String?,
    val displayName: String?,
)

sealed interface AuthState {
    data object Checking : AuthState
    data object SignedOut : AuthState
    data class SignedIn(val userId: String) : AuthState
}
