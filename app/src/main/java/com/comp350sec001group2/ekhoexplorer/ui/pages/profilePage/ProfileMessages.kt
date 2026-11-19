package com.comp350sec001group2.ekhoexplorer.ui.pages.profilePage

import com.comp350sec001group2.ekhoexplorer.Business.Auth.AuthValidator
import com.comp350sec001group2.ekhoexplorer.Business.Auth.InputProblem
import com.comp350sec001group2.ekhoexplorer.ui.pages.profilePage.viewmodels.AccountError

object ProfileMessages {

    fun error(error: AccountError): String = when (error) {
        AccountError.NETWORK -> "Can't reach the server. Check your connection and try again."
        AccountError.DENIED -> "That was not allowed. If you already have a request waiting, it needs a decision first."
        AccountError.TOO_MANY_REQUESTS -> "Too many attempts. Wait a few minutes and try again."
        AccountError.UNKNOWN -> "Something went wrong. Please try again."
    }

    fun reason(problem: InputProblem): String = when (problem) {
        InputProblem.TOO_LONG -> "Keep it to ${AuthValidator.MAX_REASON_LENGTH} characters or fewer"
        else -> "Check your reason"
    }

    fun requestedUsername(problem: InputProblem): String = when (problem) {
        InputProblem.EMPTY -> "Enter the username you would like"
        InputProblem.USERNAME_SHORT -> "Use at least ${AuthValidator.MIN_USERNAME_LENGTH} characters"
        InputProblem.USERNAME_LONG -> "Use ${AuthValidator.MAX_USERNAME_LENGTH} characters or fewer"
        InputProblem.USERNAME_CHARS -> "Use only letters, numbers, dots and underscores"
        InputProblem.SAME_AS_CURRENT -> "That is already your username"
        else -> "Check the username"
    }
}
