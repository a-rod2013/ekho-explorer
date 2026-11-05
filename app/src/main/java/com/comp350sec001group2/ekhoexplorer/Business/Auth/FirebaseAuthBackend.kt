package com.comp350sec001group2.ekhoexplorer.Business.Auth

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

class FirebaseAuthBackend(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) : AuthBackend {

    override val identity: Flow<AuthIdentity?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.toIdentity()) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }.distinctUntilChanged()

    override val currentIdentity: AuthIdentity? get() = auth.currentUser?.toIdentity()

    override suspend fun signIn(email: String, password: String): AuthIdentity =
        guarded {
            val user = auth.signInWithEmailAndPassword(email, password).await().user
                ?: throw AuthException(AuthErrorKind.UNKNOWN, "Sign-in returned no user")
            user.toIdentity()
        }

    override suspend fun createAccount(email: String, password: String, displayName: String): AuthIdentity =
        guarded {
            val user = auth.createUserWithEmailAndPassword(email, password).await().user
                ?: throw AuthException(AuthErrorKind.UNKNOWN, "Account creation returned no user")
            try {
                val change = UserProfileChangeRequest.Builder().setDisplayName(displayName).build()
                user.updateProfile(change).await()
            } catch (e: Exception) {
                bestEffort { user.delete().await() }
                throw e
            }
            AuthIdentity(user.uid, user.email, displayName)
        }

    override suspend fun deleteCurrentAccount() {
        guarded { auth.currentUser?.delete()?.await() }
    }

    override fun signOut() = auth.signOut()

    private fun FirebaseUser.toIdentity() = AuthIdentity(uid = uid, email = email, displayName = displayName)

    private suspend fun bestEffort(block: suspend () -> Unit) {
        withContext(NonCancellable) {
            try {
                block()
            } catch (ignored: Exception) {
                // Nothing more can be done here
            }
        }
    }

    private suspend fun <T> guarded(block: suspend () -> T): T =
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw e.toAuthException()
        }
}

internal fun Throwable.toAuthException(): AuthException {
    val code = (this as? FirebaseAuthException)?.errorCode
    return when (this) {
        is AuthException -> this
        is FirebaseAuthWeakPasswordException -> AuthException(AuthErrorKind.WEAK_PASSWORD, "Password rejected as too weak", this)
        is FirebaseAuthUserCollisionException -> AuthException(AuthErrorKind.EMAIL_IN_USE, "Email already registered", this)
        is FirebaseAuthInvalidUserException ->
            if (code == "ERROR_USER_DISABLED") AuthException(AuthErrorKind.ACCOUNT_DISABLED, "Account disabled", this)
            else AuthException(AuthErrorKind.WRONG_CREDENTIALS, "No such account", this)
        is FirebaseAuthInvalidCredentialsException ->
            if (code == "ERROR_INVALID_EMAIL") AuthException(AuthErrorKind.INVALID_EMAIL, "Malformed email", this)
            else AuthException(AuthErrorKind.WRONG_CREDENTIALS, "Wrong email or password", this)
        is FirebaseTooManyRequestsException -> AuthException(AuthErrorKind.TOO_MANY_REQUESTS, "Too many attempts", this)
        is FirebaseNetworkException -> AuthException(AuthErrorKind.NETWORK, "Network unavailable", this)
        is FirebaseAuthException -> when (code) {
            "ERROR_NETWORK_REQUEST_FAILED" -> AuthException(AuthErrorKind.NETWORK, "Network unavailable", this)
            "ERROR_TOO_MANY_REQUESTS" -> AuthException(AuthErrorKind.TOO_MANY_REQUESTS, "Too many attempts", this)
            "ERROR_USER_DISABLED" -> AuthException(AuthErrorKind.ACCOUNT_DISABLED, "Account disabled", this)
            "ERROR_OPERATION_NOT_ALLOWED" -> AuthException(
                AuthErrorKind.UNKNOWN,
                "Email/Password sign-in is not enabled. Turn it on in the Firebase console under Authentication.",
                this,
            )
            else -> AuthException(AuthErrorKind.UNKNOWN, "Firebase Auth error $code", this)
        }
        is IOException -> AuthException(AuthErrorKind.NETWORK, "Network unavailable", this)
        else -> AuthException(AuthErrorKind.UNKNOWN, "Unexpected ${this::class.java.simpleName}", this)
    }
}
