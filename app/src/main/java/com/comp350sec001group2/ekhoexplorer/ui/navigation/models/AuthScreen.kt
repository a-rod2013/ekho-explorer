package com.comp350sec001group2.ekhoexplorer.ui.navigation.models

const val AUTH_ROUTE = "auth"
const val MAIN_ROUTE = "main"

sealed class AuthScreen(val destination: String) {
    data object Login : AuthScreen("login")
    data object SignUp : AuthScreen("signup")
    data object ForgotPassword : AuthScreen("forgotPassword")
    data object VerifyEmail : AuthScreen("verifyEmail")
}
