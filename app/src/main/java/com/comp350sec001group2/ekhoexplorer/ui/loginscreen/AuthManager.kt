package com.comp350sec001group2.ekhoexplorer.ui.loginscreen

import com.google.firebase.auth.FirebaseAuth

object AuthManager {
    private val auth = FirebaseAuth.getInstance()

    fun currentUser() = auth.currentUser

    fun signUp(email: String, password: String, onResult: (String?) -> Unit) {
        if (email.isBlank() || password.isBlank()) {
            onResult("Please enter an email and password")
            return
        }
        auth.createUserWithEmailAndPassword(email.trim(), password)
            .addOnCompleteListener { task ->
                onResult(if (task.isSuccessful) null else task.exception?.message)
            }
    }

    fun signIn(email: String, password: String, onResult: (String?) -> Unit) {
        if (email.isBlank() || password.isBlank()) {
            onResult("Please enter an email and password")
            return
        }
        auth.signInWithEmailAndPassword(email.trim(), password)
            .addOnCompleteListener { task ->
                onResult(if (task.isSuccessful) null else task.exception?.message)
            }
    }

    fun signOut() = auth.signOut()
}
