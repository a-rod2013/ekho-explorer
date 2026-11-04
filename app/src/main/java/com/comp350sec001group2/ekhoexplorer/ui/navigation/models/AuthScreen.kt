package com.comp350sec001group2.ekhoexplorer.ui.navigation.models

/** The two top-level groups of screens. Everything the nav bar shows lives in [MAIN_ROUTE]. */
const val AUTH_ROUTE = "auth"
const val MAIN_ROUTE = "main"

/** Screens shown before sign-in. They never appear in the nav bar. */
sealed class AuthScreen(val destination: String) {
    data object Login : AuthScreen("login")
    data object SignUp : AuthScreen("signup")
}
