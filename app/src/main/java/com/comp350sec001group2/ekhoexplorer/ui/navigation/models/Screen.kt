package com.comp350sec001group2.ekhoexplorer.ui.navigation.models

sealed class Screen(
    val destination: String,
    val title: String
) {

    // Add a new object here when wanting to create a new navbar addition
    // be sure to include both a destination and title value
    data object Map : Screen(
        destination = "map",
        title = "Map"
    )

    data object Feed : Screen(
        destination = "feed",
        title = "Feed"
    )

    data object Profile : Screen(
        destination = "profile",
        title = "Profile"
    )

    data object Species : Screen(
        destination = "Species",
        title = "Species"
    )

    data object Camera: Screen(
        destination = "camera",
        title = "Camera"
    )

}
