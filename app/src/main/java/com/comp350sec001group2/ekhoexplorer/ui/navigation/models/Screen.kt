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
    data object Activity : Screen(
        destination = "activity",
        title = "Activity"
    )
    data object Action : Screen(
        destination = "action",
        title = "Camera"
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
    data object SpeciesDetails : Screen(
        destination = "species/{speciesId}",
        title = "Species Details"
    ) {
        fun route(speciesId: Int) = "species/$speciesId"
    }

    data object Notifications : Screen(
        destination = "notifications",
        title = "Notifications"
    )

    // Feed post creation
    data object NewPost : Screen(
        destination = "newPost",
        title = "New post"
    )

    // Trails
    data object AllTrails : Screen(
        destination = "trails",
        title = "Trails"
    )

    data object TrailWizard : Screen(
        destination = "trailWizard",
        title = "Create a trail"
    )

    data object TrailDetail : Screen(
        destination = "trail/{trailId}",
        title = "Trail"
    ) {
        fun route(trailId: String) = "trail/$trailId"
    }

    data object TrailEdit : Screen(
        destination = "trail/{trailId}/edit",
        title = "Edit trail"
    ) {
        fun route(trailId: String) = "trail/$trailId/edit"
    }

    data object Camera : Screen(
        destination = "camera",
        title = "Camera"
    )
}
