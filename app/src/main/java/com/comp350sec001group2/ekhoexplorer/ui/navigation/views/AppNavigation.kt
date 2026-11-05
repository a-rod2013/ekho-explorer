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
import com.comp350sec001group2.ekhoexplorer.ui.pages.journalHub.views.FieldJournalScreen
import com.comp350sec001group2.ekhoexplorer.ui.pages.notifications.views.NotificationsScreen
import com.comp350sec001group2.ekhoexplorer.ui.pages.createPost.views.NewPostScreen
import com.comp350sec001group2.ekhoexplorer.ui.species.SpeciesSearchScreen
import com.comp350sec001group2.ekhoexplorer.ui.pages.camera.CameraScreen

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

    // temp authentication navigation
    val startRoute = remember {
        if (authState is AuthState.SignedIn) MAIN_ROUTE else AUTH_ROUTE
    }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination =
        backStackEntry?.destination?.route ?: Screen.Map.destination
    // show the bottom bar only inside the main app
    val showBottomBar =
        backStackEntry?.destination?.hierarchy?.any { it.route == MAIN_ROUTE }
            ?: (startRoute == MAIN_ROUTE)
    AuthNavigationEffect(navController, authState)
    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                AppBottomBar(
                    currentDestination = currentDestination,
                    onNavigate = { screen ->
                        if (screen == Screen.Action) {
                            navController.navigate(Screen.Camera.destination) {
                                launchSingleTop = true
                            }
                        } else {
                            navController.navigate(screen.destination) {
                                popUpTo(Screen.Map.destination) {
                                    saveState = false
                                }
                                launchSingleTop = true
                                restoreState = false
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startRoute,
            modifier = Modifier.padding(innerPadding)
        ) {
            // temp login and account creation screens
            authGraph(navController)

            // main application screens
            navigation(
                startDestination = Screen.Map.destination,
                route = MAIN_ROUTE
            ) {
                composable(Screen.Journal.destination) {
                    FieldJournalScreen(
                        onOpenSpecies = {
                            navController.navigate(Screen.Species.destination)
                        }
                    )
                }

                composable(Screen.Map.destination) {
                    MapScreen()
                }

                composable(Screen.Feed.destination) {
                    FeedScreen(
                        onOpenNotifications = {
                            navController.navigate(Screen.Notifications.destination)
                        },
                        onSignOut = onSignOut,
                    )
                }

                composable(Screen.Notifications.destination) {
                    NotificationsScreen(
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.Camera.destination) {
                    CameraScreen(
                        onPosted = {
                            navController.navigate(Screen.Feed.destination) {
                                popUpTo(Screen.Camera.destination) {
                                    inclusive = true
                                }
                                launchSingleTop = true
                            }
                        }
                    )
                }

                composable(Screen.NewPost.destination) {
                    NewPostScreen(
                        onBack = { navController.popBackStack() }
                    )
                }

                // profile is temporarily disabled pending a crash fix

                composable(Screen.Species.destination) {
                    SpeciesSearchScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
