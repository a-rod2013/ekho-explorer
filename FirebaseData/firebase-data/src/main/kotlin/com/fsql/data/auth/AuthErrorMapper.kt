package com.fsql.data.auth

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import java.io.IOException

internal object AuthErrorMapper {

    fun map(error: Throwable): AuthError {
        val dev = "${error::class.simpleName}: ${error.message}"
        return when (error) {
            // FirebaseAuthWeakPasswordException extends FirebaseAuthInvalidCredentialsException: check it first.
            is FirebaseAuthWeakPasswordException -> AuthError.WeakPassword(dev, error)
            is FirebaseAuthInvalidCredentialsException ->
                if (error.errorCode == "ERROR_INVALID_EMAIL") AuthError.InvalidEmail(dev, error)
                else AuthError.InvalidCredentials(dev, error)
            is FirebaseAuthInvalidUserException ->
                if (error.errorCode == "ERROR_USER_DISABLED") AuthError.AccountDisabled(dev, error)
                else AuthError.InvalidCredentials(dev, error)
            is FirebaseAuthUserCollisionException -> AuthError.EmailAlreadyInUse(dev, error)
            is FirebaseTooManyRequestsException -> AuthError.TooManyRequests(dev, error)
            is FirebaseNetworkException -> AuthError.Network(dev, error)
            is IOException -> AuthError.Network(dev, error)
            else -> AuthError.Unknown(dev, error)
        }
    }
}
