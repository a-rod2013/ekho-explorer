package com.comp350sec001group2.ekhoexplorer.ui.navigation.views

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.comp350sec001group2.ekhoexplorer.ProfileScreen
import com.comp350sec001group2.ekhoexplorer.MapScreen
import com.comp350sec001group2.ekhoexplorer.ui.navigation.models.Screen
import com.comp350sec001group2.ekhoexplorer.ui.pages.activityFeed.views.FeedScreen
import com.comp350sec001group2.ekhoexplorer.ui.species.SpeciesSearchScreen
import androidx.compose.material3.Text

@Composable
fun AppNavigation() {
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
    )  { innerPadding ->

        /**
         * Call screen composable function here when adding a navbar addition
         *
         * i.e. MapScreen() --> [MapScreen]
         *
         * (hold ctrl or command and click on MapScreen to see the reference)
         */
        NavHost(
            navController = navController,
            startDestination = Screen.Map.destination,
            modifier = Modifier.padding(innerPadding)
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
            composable(Screen.Species.destination){
                SpeciesSearchScreen()
            }

            composable(Screen.Camera.destination){
                Text("Camera screen")
            }

        }

    }
}
