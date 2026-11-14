package com.comp350sec001group2.ekhoexplorer.ui.pages.profilePage.viewmodels

import com.comp350sec001group2.ekhoexplorer.Business.Auth.AuthErrorKind
import com.comp350sec001group2.ekhoexplorer.Business.Auth.AuthException
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreErrorKind
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreException

enum class AccountError {
    NETWORK,
    DENIED,
    TOO_MANY_REQUESTS,
    UNKNOWN,
}

internal fun Throwable.toAccountError(): AccountError = when (this) {
    is FirestoreException -> when (kind) {
        FirestoreErrorKind.UNAVAILABLE, FirestoreErrorKind.TIMEOUT -> AccountError.NETWORK
        FirestoreErrorKind.PERMISSION_DENIED -> AccountError.DENIED
        else -> AccountError.UNKNOWN
    }
    is AuthException -> when (kind) {
        AuthErrorKind.NETWORK -> AccountError.NETWORK
        AuthErrorKind.TOO_MANY_REQUESTS -> AccountError.TOO_MANY_REQUESTS
        else -> AccountError.UNKNOWN
    }
    else -> AccountError.UNKNOWN
}
