package com.comp350sec001group2.ekhoexplorer.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.comp350sec001group2.ekhoexplorer.ui.theme.AppStyle
import androidx.compose.material.icons.filled.Tune

@Composable
fun AppHeader(
    onSpeciesClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onSignOut: () -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier,
    isJournal: Boolean = false,
    isFeed: Boolean = false,
    pageTitle: String? = null,
    onBackClick: (() -> Unit)? = null
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(AppStyle.dimens.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (pageTitle != null) {
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
                } else if (
                    pageTitle == "Field Journal" ||
                    pageTitle == "Map" ||
                    pageTitle == "Activity" ||
                    pageTitle == "Feed") {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Filled.Menu,
                            contentDescription = "Open navigation menu",
                            tint = AppStyle.colors.textPrimary
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded && (isJournal || isFeed),
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        if (isJournal) {
                            DropdownMenuItem(
                                text = { Text("Species Search") },
                                onClick = {
                                    menuExpanded = false
                                    onSpeciesClick()
                                }
                            )
                        }

                        if (isFeed) {
                            DropdownMenuItem(
                                text = { Text("Notifications") },
                                onClick = {
                                    menuExpanded = false
                                    onNotificationsClick()
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Log Out") },
                                onClick = {
                                    menuExpanded = false
                                    onSignOut()
                                }
                            )
                        }
                    }
                }

                Text(
                    text = pageTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = AppStyle.colors.textPrimary
                )
            }
        } else {
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    enabled = isJournal || isFeed
                ) {
                    Icon(
                        imageVector = Icons.Filled.Menu,
                        contentDescription = "Open navigation menu",
                        tint = AppStyle.colors.textPrimary
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded && (isJournal || isFeed),
                    onDismissRequest = { menuExpanded = false }
                ) {
                    if (isJournal) {
                        DropdownMenuItem(
                            text = { Text("Species Search") },
                            onClick = {
                                menuExpanded = false
                                onSpeciesClick()
                            }
                        )
                    }

                    if (isFeed) {
                        DropdownMenuItem(
                            text = { Text("Notifications") },
                            onClick = {
                                menuExpanded = false
                                onNotificationsClick()
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("Log Out") },
                            onClick = {
                                menuExpanded = false
                                onSignOut()
                            }
                        )
                    }
                }
            }
        }
        IconButton(onClick = onSearchClick) {
            Icon(
                imageVector = if (
                    pageTitle == "Field Journal" ||
                    pageTitle == "Map" ||
                    pageTitle == "Activity" ||
                    pageTitle == "Feed"
                ) {
                    Icons.Filled.Tune
                } else {
                    Icons.Filled.Search
                },
                contentDescription = if (
                    pageTitle == "Field Journal" ||
                    pageTitle == "Map" ||
                    pageTitle == "Activity" ||
                    pageTitle == "Feed"
                ) {
                    "Filter this page"
                } else {
                    "Search this page"
                },
                tint = AppStyle.colors.textPrimary
            )
        }
    }
}
