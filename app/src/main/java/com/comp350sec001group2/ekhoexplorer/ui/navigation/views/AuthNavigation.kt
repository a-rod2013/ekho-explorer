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
import com.comp350sec001group2.ekhoexplorer.ui.pages.auth.views.LoginScreen
import com.comp350sec001group2.ekhoexplorer.ui.pages.auth.views.SignUpScreen

fun NavGraphBuilder.authGraph(navController: NavController) {
    navigation(startDestination = AuthScreen.Login.destination, route = AUTH_ROUTE) {
        composable(AuthScreen.Login.destination) {
            LoginScreen(onCreateAccount = { navController.navigate(AuthScreen.SignUp.destination) })
        }
        composable(AuthScreen.SignUp.destination) {
            SignUpScreen(onBackToLogin = { navController.popBackStack() })
        }
    }
}

@Composable
fun AuthNavigationEffect(navController: NavController, authState: AuthState) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    LaunchedEffect(authState, backStackEntry) {
        val destination = backStackEntry?.destination ?: return@LaunchedEffect
        val inMain = destination.hierarchy.any { it.route == MAIN_ROUTE }
        when (authState) {
            is AuthState.SignedIn -> if (!inMain) {
                navController.navigate(MAIN_ROUTE) {
                    popUpTo(AUTH_ROUTE) { inclusive = true }
                    launchSingleTop = true
                }
            }
            AuthState.SignedOut -> if (inMain) {
                navController.navigate(AUTH_ROUTE) {
                    popUpTo(MAIN_ROUTE) { inclusive = true }
                    launchSingleTop = true
                }
            }
            AuthState.Checking -> Unit
        }
    }
}
