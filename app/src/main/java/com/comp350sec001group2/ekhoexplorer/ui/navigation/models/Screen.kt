package com.comp350sec001group2.ekhoexplorer.ui.navigation.models

sealed class Screen(
    val destination: String,
    val title: String
) {
    // add new navigation screens with a destination and title
    // main navigation tabs
    data object Journal : Screen(
        destination = "journal",
        title = "Journal"
    )
    data object Map : Screen(
        destination = "map",
        title = "Map"
    )
    data object Action : Screen(
        destination = "action",
        title = "Action"
    )
    data object Feed : Screen(
        destination = "feed",
        title = "Feed"
    )
    data object Profile : Screen(
        destination = "profile",
        title = "Profile"
    )
    // additional screens outside the main navigation bar
    data object Species : Screen(
        destination = "Species",
        title = "Species"
    )
    data object Notifications : Screen(
        destination = "notifications",
        title = "Notifications"
    )
}
