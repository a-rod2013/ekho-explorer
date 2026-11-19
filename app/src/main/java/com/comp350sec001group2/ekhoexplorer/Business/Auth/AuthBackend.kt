package com.comp350sec001group2.ekhoexplorer.Business.Auth

import kotlinx.coroutines.flow.Flow

interface AuthBackend {
    val identity: Flow<AuthIdentity?>

    val currentIdentity: AuthIdentity?

    suspend fun signIn(email: String, password: String): AuthIdentity

    suspend fun createAccount(email: String, password: String, displayName: String): AuthIdentity

    suspend fun deleteCurrentAccount()

    suspend fun sendEmailVerification()

    suspend fun reload(): AuthIdentity?

    suspend fun sendPasswordReset(email: String)

    fun signOut()
}
