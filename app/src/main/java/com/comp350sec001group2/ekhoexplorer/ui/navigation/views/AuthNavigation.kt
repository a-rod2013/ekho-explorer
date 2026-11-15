package com.comp350sec001group2.ekhoexplorer.ui.navigation.views

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navigation
import com.comp350sec001group2.ekhoexplorer.Business.Auth.AuthState
import com.comp350sec001group2.ekhoexplorer.ui.navigation.models.AUTH_ROUTE
import com.comp350sec001group2.ekhoexplorer.ui.navigation.models.AuthScreen
import com.comp350sec001group2.ekhoexplorer.ui.navigation.models.MAIN_ROUTE
import com.comp350sec001group2.ekhoexplorer.ui.pages.auth.views.ForgotPasswordScreen
import com.comp350sec001group2.ekhoexplorer.ui.pages.auth.views.LoginScreen
import com.comp350sec001group2.ekhoexplorer.ui.pages.auth.views.SignUpScreen
import com.comp350sec001group2.ekhoexplorer.ui.pages.auth.views.VerifyEmailScreen

fun NavGraphBuilder.authGraph(navController: NavController) {
    navigation(startDestination = AuthScreen.Login.destination, route = AUTH_ROUTE) {
        composable(AuthScreen.Login.destination) {
            LoginScreen(
                onCreateAccount = { navController.navigate(AuthScreen.SignUp.destination) },
                onForgotPassword = { navController.navigate(AuthScreen.ForgotPassword.destination) },
            )
        }
        composable(AuthScreen.SignUp.destination) {
            SignUpScreen(onBackToLogin = { navController.popBackStack() })
        }
        composable(AuthScreen.ForgotPassword.destination) {
            ForgotPasswordScreen(onBackToLogin = { navController.popBackStack() })
        }
        composable(AuthScreen.VerifyEmail.destination) {
            VerifyEmailScreen()
        }
    }
}

@Composable
fun AuthNavigationEffect(navController: NavController, authState: AuthState) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    LaunchedEffect(authState, backStackEntry) {
        val destination = backStackEntry?.destination ?: return@LaunchedEffect
        val inMain = destination.hierarchy.any { it.route == MAIN_ROUTE }
        val onVerify = destination.route == AuthScreen.VerifyEmail.destination
        when (authState) {
            is AuthState.SignedIn -> if (!inMain) {
                navController.navigate(MAIN_ROUTE) {
                    popUpTo(AUTH_ROUTE) { inclusive = true }
                    launchSingleTop = true
                }
            }
            is AuthState.AwaitingVerification -> if (!onVerify) {
                // The app stays closed until the email address is confirmed.
                navController.navigate(AuthScreen.VerifyEmail.destination) {
                    popUpTo(if (inMain) MAIN_ROUTE else AuthScreen.Login.destination) { inclusive = true }
                    launchSingleTop = true
                }
            }
            AuthState.SignedOut -> when {
                inMain -> navController.navigate(AUTH_ROUTE) {
                    popUpTo(MAIN_ROUTE) { inclusive = true }
                    launchSingleTop = true
                }
                onVerify -> navController.navigate(AuthScreen.Login.destination) {
                    popUpTo(AuthScreen.VerifyEmail.destination) { inclusive = true }
                    launchSingleTop = true
                }
            }
            AuthState.Checking -> Unit
        }
    }
}
