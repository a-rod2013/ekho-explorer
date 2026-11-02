package com.comp350sec001group2.ekhoexplorer.ui.navigation.views

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.comp350sec001group2.ekhoexplorer.MapScreen
import com.comp350sec001group2.ekhoexplorer.Business.Authentication.AuthManager
import com.comp350sec001group2.ekhoexplorer.ui.loginscreen.LoginScreen
import com.comp350sec001group2.ekhoexplorer.ui.navigation.models.Screen
import com.comp350sec001group2.ekhoexplorer.ui.pages.activityFeed.views.FeedScreen
import com.comp350sec001group2.ekhoexplorer.ui.species.SpeciesSearchScreen

@Composable
fun AppNavigation() {
    var isLoggedIn by remember { mutableStateOf(AuthManager.currentUser() != null) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            isLoggedIn = auth.currentUser != null
        }
        FirebaseAuth.getInstance().addAuthStateListener(listener)
        onDispose {
            FirebaseAuth.getInstance().removeAuthStateListener(listener)
        }
    }

    if (!isLoggedIn) {
        LoginScreen(
            onLoggedIn = {
                isLoggedIn = true
            }
        )
    } else {
        val navController = rememberNavController()

        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination =
            backStackEntry?.destination?.route ?: Screen.Map.destination

        Scaffold(
            bottomBar = {
                AppBottomBar(
                    currentDestination = currentDestination,
                    onNavigate = { screen ->
                        navController.navigate(screen.destination) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = false
                            }
                            launchSingleTop = true
                            restoreState = false
                        }
                    }
                )
            }
        ) { innerPadding ->

            Box(modifier = Modifier.padding(innerPadding)) {
                /**
                 * Call screen composable function here when adding a navbar addition
                 *
                 * i.e. MapScreen() --> [MapScreen]
                 *
                 * (hold ctrl or command and click on MapScreen to see the reference)
                 */
                NavHost(
                    navController = navController,
                    startDestination = Screen.Map.destination
                ) {
                    composable(Screen.Map.destination) {
                        MapScreen()
                    }

                    composable(Screen.Feed.destination) {
                        FeedScreen()
                    }

                    // COMMENTING OUT FOR SPRINT ONE DEMO PURPOSES, DO NOT REMOVE

//            composable(Screen.Profile.destination) {
//                ProfileScreen()
//            }

                    // species search screen
                    composable(Screen.Species.destination) {
                        SpeciesSearchScreen()
                    }

                }

                // DEV ONLY - Sign out button in top right, do not keep in production
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                ) {
                    IconButton(onClick = { AuthManager.signOut() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Sign Out"
                        )
                    }
                }
            }

        }
    }
}
