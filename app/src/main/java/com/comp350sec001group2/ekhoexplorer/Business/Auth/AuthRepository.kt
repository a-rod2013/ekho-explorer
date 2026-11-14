package com.comp350sec001group2.ekhoexplorer.Business.Auth

import android.util.Log
import androidx.compose.runtime.State
import com.comp350sec001group2.ekhoexplorer.Business.Cache.ActiveUser
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreErrorKind
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.cancellation.CancellationException

interface AuthRepository {
    val state: StateFlow<AuthState>
    suspend fun signIn(email: String, password: String)
    suspend fun signUp(username: String, email: String, password: String)
    suspend fun signOut()

    suspend fun resendVerification()

    suspend fun refreshVerification(): Boolean

    suspend fun sendPasswordReset(email: String)
}

class FirebaseAuthRepository(
    private val backend: AuthBackend,
    private val profiles: ProfileStore,
    private val hooks: SessionHooks,
    private val scope: CoroutineScope,
    private val profileTimeoutMillis: Long = PROFILE_TIMEOUT_MILLIS,
) : AuthRepository {

    private val mutableState = MutableStateFlow<AuthState>(AuthState.Checking)
    override val state: StateFlow<AuthState> = mutableState.asStateFlow()

    private val busy = Mutex()

    init {
        scope.launch { backend.identity.collect { onBackendChanged() } }
    }

    private suspend fun onBackendChanged() {
        if (!busy.tryLock()) return
        try {
            val identity = backend.currentIdentity
            val current = mutableState.value
            when {
                identity == null -> {
                    if (current is AuthState.SignedIn) hooks.onSignedOut()
                    if (current !is AuthState.SignedOut) mutableState.value = AuthState.SignedOut
                }
                !identity.emailVerified -> {
                    if (current is AuthState.SignedIn) hooks.onSignedOut()
                    holdForVerification(identity)
                }
                current is AuthState.SignedIn && current.userId == identity.uid -> Unit
                else -> {
                    if (current is AuthState.SignedIn) hooks.onSignedOut()
                    beginSession(identity)
                }
            }
        } finally {
            busy.unlock()
        }
    }

    override suspend fun signIn(email: String, password: String) {
        val cleanEmail = email.trim()
        AuthValidator.email(cleanEmail)?.let { throw AuthException(AuthErrorKind.INVALID_EMAIL, "Invalid email: $it") }
        AuthValidator.passwordForSignIn(password)?.let { throw AuthException(AuthErrorKind.INVALID_INPUT, "Invalid password: $it") }

        busy.withLock {
            val identity = backend.signIn(cleanEmail, password)
            if (identity.emailVerified) beginSession(identity) else holdForVerification(identity)
            repairMissingProfile(identity)
        }
    }

    override suspend fun signUp(username: String, email: String, password: String) {
        val cleanName = username.trim()
        val cleanEmail = email.trim()
        AuthValidator.username(cleanName)?.let { throw AuthException(AuthErrorKind.INVALID_INPUT, "Invalid username: $it") }
        AuthValidator.email(cleanEmail)?.let { throw AuthException(AuthErrorKind.INVALID_EMAIL, "Invalid email: $it") }
        AuthValidator.newPassword(password)?.let { throw AuthException(AuthErrorKind.WEAK_PASSWORD, "Invalid password: $it") }

        busy.withLock {
            val identity = backend.createAccount(cleanEmail, password, cleanName)
            try {
                withTimeoutOrNull(profileTimeoutMillis) { profiles.create(identity.uid, cleanName) }
                    ?: throw AuthException(AuthErrorKind.NETWORK, "Saving the profile timed out")
            } catch (e: Exception) {
                undoAccount()
                throw if (e is CancellationException) e else e.asProfileFailure()
            }
            try {
                backend.sendEmailVerification()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // The account exists; the person can use Resend on the next screen.
                Log.w(TAG, "Could not send the verification email", e)
            }
            holdForVerification(identity)
        }
    }

    override suspend fun resendVerification() {
        backend.sendEmailVerification()
    }

    override suspend fun refreshVerification(): Boolean = busy.withLock {
        val identity = backend.reload() ?: return@withLock false
        if (!identity.emailVerified) return@withLock false
        if (mutableState.value !is AuthState.SignedIn) beginSession(identity)
        true
    }

    override suspend fun sendPasswordReset(email: String) {
        val cleanEmail = email.trim()
        AuthValidator.email(cleanEmail)?.let { throw AuthException(AuthErrorKind.INVALID_EMAIL, "Invalid email: $it") }
        try {
            backend.sendPasswordReset(cleanEmail)
        } catch (e: AuthException) {
            // Do not reveal whether an account exists for this address.
            if (e.kind != AuthErrorKind.WRONG_CREDENTIALS) throw e
        }
    }

    override suspend fun signOut() {
        busy.withLock {
            hooks.onSignedOut()
            backend.signOut()
            mutableState.value = AuthState.SignedOut
        }
    }

    private suspend fun beginSession(identity: AuthIdentity) {
        val name = identity.displayName?.takeIf { it.isNotBlank() } ?: AuthValidator.usernameFromEmail(identity.email)
        hooks.onSignedIn(ActiveUser(userId = identity.uid, userName = name))
        mutableState.value = AuthState.SignedIn(identity.uid)
    }

    private fun holdForVerification(identity: AuthIdentity) {
        val held = AuthState.AwaitingVerification(identity.email)
        if (mutableState.value != held) mutableState.value = held
    }

    private suspend fun undoAccount() {
        withContext(NonCancellable) {
            try {
                backend.deleteCurrentAccount()
            } catch (e: Exception) {
                Log.w(TAG, "Could not remove the half-created account; signing out instead", e)
                // They can sign in later; the missing profile is repaired then.
                backend.signOut()
            }
        }
    }

    private fun repairMissingProfile(identity: AuthIdentity) {
        val name = identity.displayName?.takeIf { it.isNotBlank() } ?: AuthValidator.usernameFromEmail(identity.email)
        scope.launch {
            try {
                if (!profiles.exists(identity.uid)) profiles.create(identity.uid, name)
            } catch (e: FirestoreException) {
                Log.w(TAG, "Could not check or create the profile: ${e.kind}", e)
            }
        }
    }

    private fun Exception.asProfileFailure(): AuthException = when (this) {
        is AuthException -> this
        is FirestoreException -> when (kind) {
            FirestoreErrorKind.UNAVAILABLE, FirestoreErrorKind.TIMEOUT ->
                AuthException(AuthErrorKind.NETWORK, "Could not save the profile", this)
            else -> AuthException(AuthErrorKind.UNKNOWN, "Could not save the profile: $kind. Check the Firestore rules for users/{uid}.", this)
        }
        else -> AuthException(AuthErrorKind.UNKNOWN, "Could not save the profile", this)
    }

    private companion object {
        const val TAG = "AuthRepository"
        const val PROFILE_TIMEOUT_MILLIS = 15_000L
    }
}

