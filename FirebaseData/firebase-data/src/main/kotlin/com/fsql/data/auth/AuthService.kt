package com.fsql.data.auth

import kotlinx.coroutines.flow.Flow

/** A signed-in user. */
data class AuthUser(val uid: String, val email: String?)

/** Why sign-in or sign-up failed. [userMessage] is safe to show; [devMessage] is for logs. */
sealed class AuthError(
    val userMessage: String,
    val devMessage: String,
    val cause: Throwable? = null,
) {
    /** Wrong password OR no such account (deliberately not distinguished, so accounts cannot be probed). */
    class InvalidCredentials(devMessage: String, cause: Throwable? = null) :
        AuthError("The email or password is not correct.", devMessage, cause)

    class InvalidEmail(devMessage: String, cause: Throwable? = null) :
        AuthError("That email address is not valid.", devMessage, cause)

    class WeakPassword(devMessage: String, cause: Throwable? = null) :
        AuthError("Choose a stronger password (at least 6 characters).", devMessage, cause)

    class EmailAlreadyInUse(devMessage: String, cause: Throwable? = null) :
        AuthError("An account with that email already exists.", devMessage, cause)

    class AccountDisabled(devMessage: String, cause: Throwable? = null) :
        AuthError("This account has been disabled.", devMessage, cause)

    class TooManyRequests(devMessage: String, cause: Throwable? = null) :
        AuthError("Too many attempts. Please wait a moment and try again.", devMessage, cause)

    class Network(devMessage: String, cause: Throwable? = null) :
        AuthError("Check your internet connection and try again.", devMessage, cause)

    class Timeout(devMessage: String, cause: Throwable? = null) :
        AuthError("The request took too long. Please try again.", devMessage, cause)

    class Unknown(devMessage: String, cause: Throwable? = null) :
        AuthError("Something went wrong. Please try again.", devMessage, cause)

    override fun toString(): String = "${this::class.simpleName}: $devMessage"
}

/**
 * The outcome of sign-in or sign-up. Always returned, never thrown.
 * (This is not `com.google.firebase.auth.AuthResult`; import the one from `com.fsql.data.auth`.)
 */
sealed interface AuthResult {
    data class Success(val user: AuthUser?) : AuthResult
    data class Failure(val error: AuthError) : AuthResult

    val isSuccess: Boolean get() = this is Success
    val isFailure: Boolean get() = this is Failure
    fun errorOrNull(): AuthError? = (this as? Failure)?.error
}

/** Email and password authentication. */
interface AuthService {
    /** The signed-in user right now, or null. */
    val currentUser: AuthUser?

    /** Emits the current user (or null) immediately and again on every sign-in or sign-out. */
    val authState: Flow<AuthUser?>

    suspend fun signUp(email: String, password: String): AuthResult

    suspend fun signIn(email: String, password: String): AuthResult

    fun signOut()
}
