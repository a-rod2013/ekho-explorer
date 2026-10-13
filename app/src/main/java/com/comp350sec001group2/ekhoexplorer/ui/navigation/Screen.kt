package com.comp350sec001group2.ekhoexplorer.ui.navigation

sealed class Screen(
    val destination: String,
    val title: String
) {
    data object Feed : Screen(
        destination = "feed",
        title = "Feed"
    )

    data object Profile : Screen(
        destination = "profile",
        title = "Profile"
    )

}
