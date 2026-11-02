package com.comp350sec001group2.ekhoexplorer.Business.Authentication

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

object AuthManager {
    private val auth = FirebaseAuth.getInstance()

    fun currentUser() = auth.currentUser

    suspend fun signUp(email: String, password: String): String? {
        if (email.isBlank() || password.isBlank()) {
            return "Please enter an email and password"
        }
        return try {
            auth.createUserWithEmailAndPassword(email.trim(), password).await()
            null
        } catch (e: Exception) {
            e.localizedMessage ?: "Sign up failed"
        }
    }

    suspend fun signIn(email: String, password: String): String? {
        if (email.isBlank() || password.isBlank()) {
            return "Please enter an email and password"
        }
        return try {
            auth.signInWithEmailAndPassword(email.trim(), password).await()
            null
        } catch (e: Exception) {
            e.localizedMessage ?: "Sign in failed"
        }
    }

    fun signOut() = auth.signOut()
}
