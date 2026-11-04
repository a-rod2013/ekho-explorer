package com.comp350sec001group2.ekhoexplorer.Business.Auth

enum class AuthErrorKind {
    INVALID_INPUT,
    WRONG_CREDENTIALS,
    EMAIL_IN_USE,
    WEAK_PASSWORD,
    INVALID_EMAIL,
    ACCOUNT_DISABLED,
    TOO_MANY_REQUESTS,
    NETWORK,
    UNKNOWN,
}

class AuthException(
    val kind: AuthErrorKind,
    val detail: String,
    cause: Throwable? = null,
) : RuntimeException("$kind: $detail", cause)
