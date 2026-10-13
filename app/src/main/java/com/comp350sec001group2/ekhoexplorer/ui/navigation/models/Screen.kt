package com.comp350sec001group2.ekhoexplorer.ui.navigation.models

sealed class Screen(
    val destination: String,
    val title: String
) {

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

}
