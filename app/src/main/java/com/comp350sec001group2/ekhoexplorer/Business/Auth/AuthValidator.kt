package com.comp350sec001group2.ekhoexplorer.Business.Auth

enum class InputProblem {
    EMPTY,
    EMAIL_FORMAT,
    PASSWORD_SHORT,
    PASSWORD_MISMATCH,
    USERNAME_SHORT,
    USERNAME_LONG,
    USERNAME_CHARS,
}

object AuthValidator {
    const val MIN_PASSWORD_LENGTH = 8
    const val MAX_PASSWORD_LENGTH = 128
    const val MIN_USERNAME_LENGTH = 3
    const val MAX_USERNAME_LENGTH = 20
    const val MAX_EMAIL_LENGTH = 254

    private val EMAIL = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]{2,}$")
    private val USERNAME_CHARACTERS = Regex("[A-Za-z0-9_.]+")

    fun email(value: String): InputProblem? {
        val email = value.trim()
        return when {
            email.isEmpty() -> InputProblem.EMPTY
            email.length > MAX_EMAIL_LENGTH || !EMAIL.matches(email) -> InputProblem.EMAIL_FORMAT
            else -> null
        }
    }

    fun passwordForSignIn(value: String): InputProblem? =
        if(value.isEmpty()) InputProblem.EMPTY else null

    fun newPassword(value: String): InputProblem? = when {
        value.isEmpty() -> InputProblem.EMPTY
        value.length < MIN_PASSWORD_LENGTH -> InputProblem.PASSWORD_SHORT
        value.length > MAX_PASSWORD_LENGTH -> InputProblem.PASSWORD_SHORT
        else -> null
    }

    fun confirmPassword(password: String, confirm: String): InputProblem? = when {
        confirm.isEmpty() -> InputProblem.EMPTY
        confirm != password -> InputProblem.PASSWORD_MISMATCH
        else -> null
    }

    fun username(value: String): InputProblem? {
        val name = value.trim()
        return when {
            name.isEmpty() -> InputProblem.EMPTY
            name.length < MIN_USERNAME_LENGTH -> InputProblem.USERNAME_SHORT
            name.length > MAX_USERNAME_LENGTH -> InputProblem.USERNAME_LONG
            !USERNAME_CHARACTERS.matches(name) -> InputProblem.USERNAME_CHARS
            else -> null
        }
    }

    fun usernameFromEmail(email: String?): String {
        val local = email.orEmpty().substringBefore('@')
        val cleaned = local.filter { it.isLetterOrDigit() && it.code < 128 || it == '_' || it == '.' }
            .take(MAX_USERNAME_LENGTH)
        return if (cleaned.length >= MIN_USERNAME_LENGTH) cleaned else "explorer"
    }
}
