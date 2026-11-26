package com.comp350sec001group2.ekhoexplorer.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.comp350sec001group2.ekhoexplorer.ui.theme.AppStyle
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button

@Composable
fun AppHeader(
    onSpeciesClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onSignOut: () -> Unit,
    onSearchClick: () -> Unit,
    onMapSearchClick: () -> Unit,
    onMapFilterClick: () -> Unit,
    modifier: Modifier = Modifier,
    isJournal: Boolean = false,
    isFeed: Boolean = false,
    pageTitle: String? = null,
    onBackClick: (() -> Unit)? = null,
    unreadNotifications: Int = 0,
    onMarkAllRead: () -> Unit = {},
    canMarkAllRead: Boolean = false,
    accountMenu: (@Composable () -> Unit)? = null,
    onCreateTrailClick: () -> Unit = {},
    onViewTrailsClick: () -> Unit = {}
) {
    if (pageTitle == null) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(AppStyle.dimens.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBackClick != null) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = AppStyle.colors.textPrimary
                    )
                }
            }

            Text(
                text = pageTitle,
                style = MaterialTheme.typography.titleLarge,
                color = AppStyle.colors.textPrimary
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (pageTitle) {
                "Field Journal" -> {
                    Button(onClick = onSpeciesClick) {
                        Text("Species Search")
                    }
                }

                "All Trails" -> {
                    Button(
                        onClick = onCreateTrailClick
                    ) {
                        Text("Create a trail")
                    }
                }


                "Map" -> {
                    IconButton(onClick = onMapSearchClick) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Search observations",
                            tint = AppStyle.colors.textPrimary
                        )
                    }

                    IconButton(onClick = onMapFilterClick) {
                        Icon(
                            imageVector = Icons.Filled.FilterList,
                            contentDescription = "Filter observations",
                            tint = AppStyle.colors.textPrimary
                        )
                    }
                }

                "Feed" -> {
                    NotificationBell(
                        unreadCount = unreadNotifications,
                        onClick = onNotificationsClick
                    )
                }

                "Notifications" -> {
                    TextButton(
                        onClick = onMarkAllRead,
                        enabled = canMarkAllRead
                    ) {
                        Text("Mark all read")
                    }
                }

                "Activity" -> {
                    Button(onClick = onViewTrailsClick) {
                        Text("View Trails")
                    }
                }

                "Profile" -> {
                    accountMenu?.invoke()
                }
            }
        }
    }
}
