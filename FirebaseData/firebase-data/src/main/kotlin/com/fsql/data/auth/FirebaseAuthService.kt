package com.fsql.data.auth

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

internal class FirebaseAuthService(
    private val auth: FirebaseAuth,
    private val timeoutMs: Long,
) : AuthService {

    private companion object {
        const val TAG = "FirebaseData"
    }

    private class Holder(val user: FirebaseUser?)

    override val currentUser: AuthUser? get() = auth.currentUser?.toAuthUser()

    override val authState: Flow<AuthUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.toAuthUser()) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override suspend fun signUp(email: String, password: String): AuthResult =
        call { auth.createUserWithEmailAndPassword(email, password).await().user }

    override suspend fun signIn(email: String, password: String): AuthResult =
        call { auth.signInWithEmailAndPassword(email, password).await().user }

    override fun signOut() = auth.signOut()

    private suspend fun call(block: suspend () -> FirebaseUser?): AuthResult {
        val error: AuthError = try {
            val holder = withTimeoutOrNull(timeoutMs) { Holder(block()) }
            if (holder != null) return AuthResult.Success(holder.user?.toAuthUser())
            AuthError.Timeout("The request did not finish within $timeoutMs ms.")
        } catch (e: Throwable) {
            if (e is CancellationException) currentCoroutineContext().ensureActive()
            AuthErrorMapper.map(e)
        }
        return failure(error)
    }

    private fun failure(error: AuthError): AuthResult.Failure {
        Log.w(TAG, "auth failed: $error", error.cause)
        return AuthResult.Failure(error)
    }

    private fun FirebaseUser.toAuthUser() = AuthUser(uid, email)
}
