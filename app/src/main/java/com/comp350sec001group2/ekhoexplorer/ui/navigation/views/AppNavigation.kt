package com.comp350sec001group2.ekhoexplorer.ui.navigation.views

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import com.comp350sec001group2.ekhoexplorer.Business.Auth.AuthState
import com.comp350sec001group2.ekhoexplorer.ui.map.MapScreen
import com.comp350sec001group2.ekhoexplorer.ui.navigation.models.AUTH_ROUTE
import com.comp350sec001group2.ekhoexplorer.ui.navigation.models.MAIN_ROUTE
import com.comp350sec001group2.ekhoexplorer.ui.navigation.models.Screen
import com.comp350sec001group2.ekhoexplorer.ui.pages.activityFeed.views.FeedScreen
import com.comp350sec001group2.ekhoexplorer.ui.pages.auth.viewmodels.SessionViewModel
import com.comp350sec001group2.ekhoexplorer.ui.pages.auth.viewmodels.sessionViewModelFactory
import com.comp350sec001group2.ekhoexplorer.ui.pages.auth.views.SplashScreen
import com.comp350sec001group2.ekhoexplorer.ui.species.SpeciesSearchScreen
import com.comp350sec001group2.ekhoexplorer.ui.pages.notifications.views.NotificationsScreen
import com.comp350sec001group2.ekhoexplorer.ui.photoTemp.views.NewPostScreen

/**
 * The root of the UI. The first thing it does is find out whether someone is signed in:
 * a splash while that is checked, then the login screens or the main app.
 */
@Composable
fun AppNavigation(
    sessionViewModel: SessionViewModel = viewModel(factory = sessionViewModelFactory),
) {
    val authState by sessionViewModel.authState.collectAsStateWithLifecycle()

    if (authState is AuthState.Checking) {
        SplashScreen()
    } else {
        AppNavHost(authState = authState, onSignOut = sessionViewModel::signOut)
    }
}

@Composable
private fun AppNavHost(authState: AuthState, onSignOut: () -> Unit) {
    val navController = rememberNavController()

    // Decided once, when the host first appears. Later sign-ins and sign-outs are handled by
    // AuthNavigationEffect, which navigates (and pops the old group) instead.
    val startRoute = remember { if (authState is AuthState.SignedIn) MAIN_ROUTE else AUTH_ROUTE }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination =
        backStackEntry?.destination?.route ?: Screen.Map.destination

    // No nav bar on the login and sign-up screens.
    val showBottomBar = backStackEntry?.destination?.hierarchy?.any { it.route == MAIN_ROUTE }
        ?: (startRoute == MAIN_ROUTE)

    AuthNavigationEffect(navController, authState)

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                AppBottomBar(
                    currentDestination = currentDestination,
                    onNavigate = { screen ->
                        navController.navigate(screen.destination) {
                            popUpTo(Screen.Map.destination) {
                                saveState = false
                            }
                            launchSingleTop = true
                            restoreState = false
                        }
                    }
                )
            }
        }
    )  { innerPadding ->

        /**
         * Call screen composable function here when adding a navbar addition
         *
         * i.e. MapScreen() --> [MapScreen]
         *
         * (hold ctrl or command and click on MapScreen to see the reference)
         *
         * New navbar screens go inside the "main" group below.
         */
        NavHost(
            navController = navController,
            startDestination = startRoute,
            modifier = Modifier.padding(innerPadding)
        ) {
            authGraph(navController)

            navigation(startDestination = Screen.Map.destination, route = MAIN_ROUTE) {
                composable(Screen.Map.destination) {
                    MapScreen()
                }

                composable(Screen.Feed.destination) {
                    FeedScreen(
                        onOpenNotifications = { navController.navigate(Screen.Notifications.destination) },
                        onNewPost = { navController.navigate(Screen.NewPost.destination) },
                        onSignOut = onSignOut,
                    )
                }

                composable(Screen.Notifications.destination) {
                    NotificationsScreen(onBack = { navController.popBackStack() })
                }

                composable(Screen.NewPost.destination) {
                    NewPostScreen(onBack = { navController.popBackStack() })
                }

                // COMMENTING OUT FOR SPRINT ONE DEMO PURPOSES, DO NOT REMOVE

//            composable(Screen.Profile.destination) {
//                ProfileScreen()
//            }

                // species search screen
                composable(Screen.Species.destination){
                    SpeciesSearchScreen()
                }
            }

        }

    }
}
