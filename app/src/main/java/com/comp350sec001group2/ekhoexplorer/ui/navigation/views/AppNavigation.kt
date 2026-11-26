package com.comp350sec001group2.ekhoexplorer.ui.navigation.views

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.comp350sec001group2.ekhoexplorer.Business.Auth.AuthState
import com.comp350sec001group2.ekhoexplorer.Business.Cache.EhkoCache
import com.comp350sec001group2.ekhoexplorer.ui.components.AppHeader
import com.comp350sec001group2.ekhoexplorer.ui.map.MapScreen
import com.comp350sec001group2.ekhoexplorer.ui.navigation.models.AUTH_ROUTE
import com.comp350sec001group2.ekhoexplorer.ui.navigation.models.MAIN_ROUTE
import com.comp350sec001group2.ekhoexplorer.ui.navigation.models.Screen
import com.comp350sec001group2.ekhoexplorer.ui.pages.activity.views.ActivityScreen
import com.comp350sec001group2.ekhoexplorer.ui.pages.activity.views.AllTrailsScreen
import com.comp350sec001group2.ekhoexplorer.ui.pages.activityFeed.views.FeedScreen
import com.comp350sec001group2.ekhoexplorer.ui.pages.auth.viewmodels.SessionViewModel
import com.comp350sec001group2.ekhoexplorer.ui.pages.auth.viewmodels.sessionViewModelFactory
import com.comp350sec001group2.ekhoexplorer.ui.pages.auth.views.SplashScreen
import com.comp350sec001group2.ekhoexplorer.ui.pages.camera.CameraScreen
import com.comp350sec001group2.ekhoexplorer.ui.pages.createPost.views.NewPostScreen
import com.comp350sec001group2.ekhoexplorer.ui.pages.journalHub.views.FieldJournalScreen
import com.comp350sec001group2.ekhoexplorer.ui.pages.notifications.views.NotificationsScreen
import com.comp350sec001group2.ekhoexplorer.ui.pages.trailDetail.views.TrailDetailScreen
import com.comp350sec001group2.ekhoexplorer.ui.pages.trailWizard.views.TrailEditScreen
import com.comp350sec001group2.ekhoexplorer.ui.pages.trailWizard.views.TrailWizardScreen
import com.comp350sec001group2.ekhoexplorer.ui.species.SpeciesDetailsScreen
import com.comp350sec001group2.ekhoexplorer.ui.species.SpeciesSearchScreen
import com.comp350sec001group2.ekhoexplorer.ui.pages.auth.viewmodels.changePasswordViewModelFactory
import com.comp350sec001group2.ekhoexplorer.ui.pages.auth.views.ForgotPasswordScreen
import com.comp350sec001group2.ekhoexplorer.ui.pages.profilePage.views.UsernameChangeScreen
import com.comp350sec001group2.ekhoexplorer.ui.pages.profilePage.views.ProfileScreen
import com.comp350sec001group2.ekhoexplorer.ui.pages.profilePage.views.UserProfileScreen
import com.comp350sec001group2.ekhoexplorer.ui.pages.journalHub.views.ObservationDetailsScreen
import com.comp350sec001group2.ekhoexplorer.ui.pages.activityFeed.viewmodels.FeedViewModel
import com.comp350sec001group2.ekhoexplorer.ui.pages.activityFeed.viewmodels.feedViewModelFactory
import com.comp350sec001group2.ekhoexplorer.ui.pages.notifications.viewmodels.NotificationsViewModel
import com.comp350sec001group2.ekhoexplorer.ui.pages.notifications.viewmodels.notificationsViewModelFactory
import com.comp350sec001group2.ekhoexplorer.ui.components.AccountMenu
import com.comp350sec001group2.ekhoexplorer.ui.pages.profilePage.viewmodels.ProfileViewModel
import com.comp350sec001group2.ekhoexplorer.ui.pages.profilePage.viewmodels.profileViewModelFactory
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import com.comp350sec001group2.ekhoexplorer.ui.pages.settings.views.SettingsScreen
import com.comp350sec001group2.ekhoexplorer.ui.theme.AppStyle


@Composable
fun AppNavigation(
    sessionViewModel: SessionViewModel = viewModel(factory = sessionViewModelFactory),
) {
    val authState by sessionViewModel.authState.collectAsStateWithLifecycle()

    if (authState is AuthState.Checking) {
        SplashScreen()
    } else {
        AppNavHost(
            authState = authState,
            onSignOut = sessionViewModel::signOut
        )
    }
}

@Composable
private fun AppNavHost(
    authState: AuthState,
    onSignOut: () -> Unit
) {
    val navController = rememberNavController()

    val profileViewModel: ProfileViewModel = viewModel(
        factory = profileViewModelFactory
    )

    val profileUiState = profileViewModel.uiState

    val notificationsViewModel: NotificationsViewModel =
        viewModel(factory = notificationsViewModelFactory)

    val notificationsState by notificationsViewModel.uiState
        .collectAsStateWithLifecycle()

    val feedViewModel: FeedViewModel = viewModel(factory = feedViewModelFactory)
    val unreadNotifications by feedViewModel.unreadNotifications
        .collectAsStateWithLifecycle()

    var showSpeciesSearch by remember { mutableStateOf(false) }
    var showMapSearch by remember { mutableStateOf(false) }
    var showMapFilters by remember { mutableStateOf(false) }

    // Temporary authentication navigation
    val startRoute = remember {
        if (authState is AuthState.SignedIn) {
            MAIN_ROUTE
        } else {
            AUTH_ROUTE
        }
    }

    val backStackEntry by navController.currentBackStackEntryAsState()

    val currentDestination =
        backStackEntry?.destination?.route ?: Screen.Map.destination

    // Show the bottom bar only inside the main app
    val showBottomBar =
        backStackEntry?.destination?.hierarchy?.any {
            it.route == MAIN_ROUTE
        } ?: (startRoute == MAIN_ROUTE)

    AuthNavigationEffect(navController, authState)

    Scaffold(
        topBar = {
            if (showBottomBar) {
                AppHeader(
                    unreadNotifications = unreadNotifications,
                    onMarkAllRead = notificationsViewModel::onMarkAllRead,
                    canMarkAllRead = notificationsState.unreadCount > 0,
                    onCreateTrailClick = {
                        navController.navigate(Screen.TrailWizard.destination)
                    },
                    onViewTrailsClick = {
                        navController.navigate(Screen.AllTrails.destination) {
                            launchSingleTop = true
                        }
                    },
                    onSpeciesClick = {
                        navController.navigate(Screen.Species.destination) {
                            launchSingleTop = true
                        }
                    },
                    accountMenu = if (currentDestination == Screen.Profile.destination) {
                        {
                            IconButton(
                                onClick = {
                                    navController.navigate(Screen.Settings.destination) {
                                        launchSingleTop = true
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Settings,
                                    contentDescription = "Open settings",
                                    tint = AppStyle.colors.textSecondary
                                )
                            }
                        }
                    } else {
                        null
                    },

                    onSearchClick = {
                        when (currentDestination) {
                            Screen.ObservationDetails.destination -> {
                                navController.popBackStack(
                                    route = Screen.Journal.destination,
                                    inclusive = false,
                                )
                            }

                            Screen.Species.destination -> {
                                showSpeciesSearch = !showSpeciesSearch
                            }
                        }
                    },
                    onNotificationsClick = {
                        navController.navigate(Screen.Notifications.destination) {
                            launchSingleTop = true
                        }
                    },
                    onSignOut = onSignOut,
                    onMapSearchClick = {
                        showMapSearch = !showMapSearch
                        showMapFilters = false
                    },
                    onMapFilterClick = {
                        showMapFilters = !showMapFilters
                        showMapSearch = false
                    },
                    isJournal = currentDestination == Screen.Journal.destination,
                    isFeed = currentDestination == Screen.Feed.destination,
                    pageTitle = when (currentDestination) {
                        Screen.Journal.destination -> "Field Journal"
                        Screen.ObservationDetails.destination -> "Observation Details"
                        Screen.Species.destination -> "Species Search"
                        Screen.SpeciesDetails.destination -> "Species Details"
                        Screen.Map.destination -> "Map"
                        Screen.Feed.destination -> "Feed"
                        Screen.Notifications.destination -> "Notifications"
                        Screen.Activity.destination -> "Activity"
                        Screen.AllTrails.destination -> "All Trails"
                        Screen.TrailDetail.destination -> "Trail Details"
                        Screen.Profile.destination -> "Profile"
                        Screen.Settings.destination -> "Settings"
                        else -> null
                    },
                    onBackClick = if (
                        currentDestination == Screen.Settings.destination ||
                        currentDestination == Screen.Species.destination ||
                        currentDestination == Screen.ObservationDetails.destination ||
                        currentDestination == Screen.Notifications.destination ||
                        currentDestination == Screen.SpeciesDetails.destination ||
                        currentDestination == Screen.AllTrails.destination ||
                        currentDestination == Screen.TrailDetail.destination
                    ) {
                        { navController.popBackStack() }
                    } else {
                        null
                    }
                )
            }
        },
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

            // Authentication screens
            authGraph(navController)

            // Main application screens
            navigation(
                startDestination = Screen.Map.destination,
                route = MAIN_ROUTE
            ) {

                // Field Journal
                composable(Screen.Journal.destination) {
                    FieldJournalScreen(
                        onOpenObservation = { entryId ->
                            navController.navigate(
                                Screen.ObservationDetails.createRoute(entryId)
                            )
                        },
                        onOpenSpecies = {
                            navController.navigate(Screen.Species.destination)
                        }
                    )
                }

                composable(
                    route = Screen.ObservationDetails.destination,
                    arguments = listOf(
                        navArgument("entryId") {
                            type = NavType.StringType
                        }
                    ),
                ) { detailsEntry ->
                    val entryId = requireNotNull(
                        detailsEntry.arguments?.getString("entryId")
                    )

                    ObservationDetailsScreen(
                        entryId =  entryId,
                        onBack = {
                            navController.popBackStack()
                        },
                    )
                }

                // Map
                composable(Screen.Map.destination) {
                    MapScreen(
                        showSearch = showMapSearch,
                        showFilters = showMapFilters
                    )
                }

                // Activity
                composable(Screen.Activity.destination) {
                    ActivityScreen(
                        onViewTrails = {
                            navController.navigate(Screen.AllTrails.destination)
                        },
                        onOpenTrail = { id ->
                            navController.navigate(
                                Screen.TrailDetail.route(id)
                            )
                        }
                    )
                }

                // Species Search
                composable(Screen.Species.destination) {
                    SpeciesSearchScreen(
                        onBack = {
                            navController.popBackStack()
                        },
                        showSearchField = showSpeciesSearch,
                        onSpeciesClick = { speciesId ->
                            navController.navigate(
                                Screen.SpeciesDetails.route(speciesId)
                            )
                        }
                    )
                }

                // Species Details
                composable(Screen.SpeciesDetails.destination) { entry ->
                    val speciesId = entry.arguments
                        ?.getString("speciesId")
                        ?.toIntOrNull() ?: 0

                    SpeciesDetailsScreen(
                        speciesId = speciesId,
                        onBack = {
                            navController.popBackStack()
                        }
                    )
                }

                // All Trails
                composable(Screen.AllTrails.destination) {
                    AllTrailsScreen(
                        onCreateTrail = {
                            navController.navigate(
                                Screen.TrailWizard.destination
                            )
                        },
                        onOpenTrail = { id ->
                            navController.navigate(
                                Screen.TrailDetail.route(id)
                            )
                        }
                    )
                }

                // Trail Wizard
                composable(Screen.TrailWizard.destination) {
                    TrailWizardScreen(
                        onSaved = { id ->
                            navController.navigate(
                                Screen.TrailDetail.route(id)
                            ) {
                                popUpTo(Screen.TrailWizard.destination) {
                                    inclusive = true
                                }
                            }
                        }
                    )
                }

                // Trail Details
                composable(Screen.TrailDetail.destination) { entry ->
                    val trailId =
                        entry.arguments?.getString("trailId").orEmpty()

                    TrailDetailScreen(
                        trailId = trailId,
                        onEdit = { id ->
                            navController.navigate(
                                Screen.TrailEdit.route(id)
                            )
                        },
                        onDeleted = {
                            navController.popBackStack()
                        }
                    )
                }

                // Feed
                composable(Screen.Feed.destination) {
                    FeedScreen(
                        onOpenNotifications = {
                            navController.navigate(
                                Screen.Notifications.destination
                            )
                        },
                        onSignOut = onSignOut,
                        onUserClick = { userId ->
                            val activeUserId = EhkoCache.activeUser.userId
                            if (activeUserId.isNotBlank() && userId == activeUserId) {
                                navController.navigate(Screen.Profile.destination) {
                                    launchSingleTop = true
                                }
                            } else {
                                navController.navigate(
                                    Screen.UserProfile.route(userId)
                                )
                            }
                        }
                    )
                }

                // Trail Editing
                composable(Screen.TrailEdit.destination) { entry ->
                    TrailEditScreen(
                        trailId = entry.arguments
                            ?.getString("trailId")
                            .orEmpty(),
                        onSaved = {
                            navController.popBackStack()
                        }
                    )
                }

                // Notifications
                composable(Screen.Notifications.destination) {
                    NotificationsScreen(
                        onBack = {
                            navController.popBackStack()
                        }
                    )
                }

                // Camera
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

                // New Post
                composable(Screen.NewPost.destination) {
                    NewPostScreen(
                        onBack = {
                            navController.popBackStack()
                        }
                    )
                }

                composable(Screen.Profile.destination) {
                    ProfileScreen(
                        onSignOut = onSignOut,
                        onChangePassword = {
                            navController.navigate(Screen.ChangePassword.destination) {
                                launchSingleTop = true
                            }
                        },
                        onRequestUsernameChange = {
                            navController.navigate(Screen.UsernameChange.destination) {
                                launchSingleTop = true
                            }
                        },
                    )
                }

                composable(Screen.Settings.destination) {
                    SettingsScreen(
                        username = profileUiState.username,
                        onChangePassword = {
                            navController.navigate(Screen.ChangePassword.destination) {
                                launchSingleTop = true
                            }
                        },
                        onRequestUsernameChange = {
                            navController.navigate(Screen.UsernameChange.destination) {
                                launchSingleTop = true
                            }
                        },
                        onSignOut = onSignOut
                    )
                }

                composable(Screen.UserProfile.destination) { entry ->
                    val userId = entry.arguments?.getString("userId").orEmpty()
                    UserProfileScreen(
                        userId = userId,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.UsernameChange.destination) {
                    UsernameChangeScreen(onBack = { navController.popBackStack() })
                }

                composable(Screen.ChangePassword.destination) {
                    ForgotPasswordScreen(
                        onBackToLogin = { navController.popBackStack() },
                        viewModel = viewModel(factory = changePasswordViewModelFactory),
                        title = "Change your password",
                        subtitle = "We will email a link to your account address so you can choose a new password.",
                        backLabel = "Back to settings",
                    )
                }
            }
        }
    }
}

