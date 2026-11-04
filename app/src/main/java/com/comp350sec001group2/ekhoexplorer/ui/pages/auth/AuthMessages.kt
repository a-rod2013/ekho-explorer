package com.comp350sec001group2.ekhoexplorer.ui.pages.auth

import com.comp350sec001group2.ekhoexplorer.Business.Auth.AuthErrorKind
import com.comp350sec001group2.ekhoexplorer.Business.Auth.InputProblem

object AuthMessages {

    fun email(problem: InputProblem): String = when (problem) {
        InputProblem.EMPTY -> "Enter your email"
        else -> "Enter a valid email address"
    }

    fun password(problem: InputProblem): String = when (problem) {
        InputProblem.EMPTY -> "Enter your password"
        InputProblem.PASSWORD_SHORT -> "Use at least 8 characters"
        else -> "Check your password"
    }

    fun newPassword(problem: InputProblem): String = when (problem) {
        InputProblem.EMPTY -> "Choose a password"
        InputProblem.PASSWORD_SHORT -> "Use 8 to 128 characters"
        else -> "Check your password"
    }

    fun confirmPassword(problem: InputProblem): String = when (problem) {
        InputProblem.EMPTY -> "Re-enter your password"
        InputProblem.PASSWORD_MISMATCH -> "Passwords don't match"
        else -> "Check your password"
    }

    fun username(problem: InputProblem): String = when (problem) {
        InputProblem.EMPTY -> "Choose a username"
        InputProblem.USERNAME_SHORT -> "Use at least 3 characters"
        InputProblem.USERNAME_LONG -> "Use 20 characters or fewer"
        InputProblem.USERNAME_CHARS -> "Use only letters, numbers, dots and underscores"
        else -> "Check your username"
    }

    /** Shown above the button when a request fails. */
    fun error(kind: AuthErrorKind): String = when (kind) {
        AuthErrorKind.WRONG_CREDENTIALS -> "Incorrect email or password."
        AuthErrorKind.EMAIL_IN_USE -> "An account with this email already exists. Try signing in."
        AuthErrorKind.WEAK_PASSWORD -> "That password is too weak. Try a longer one."
        AuthErrorKind.INVALID_EMAIL -> "That email address doesn't look right."
        AuthErrorKind.ACCOUNT_DISABLED -> "This account has been disabled."
        AuthErrorKind.TOO_MANY_REQUESTS -> "Too many attempts. Wait a few minutes and try again."
        AuthErrorKind.NETWORK -> "Can't reach the server. Check your connection and try again."
        AuthErrorKind.INVALID_INPUT,
        AuthErrorKind.UNKNOWN -> "Something went wrong. Please try again."
    }
}
