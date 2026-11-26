package com.comp350sec001group2.ekhoexplorer.ui.pages.settings.views

import com.comp350sec001group2.ekhoexplorer.ui.pages.settings.views.components.SettingsItem
import com.comp350sec001group2.ekhoexplorer.ui.pages.settings.views.components.SettingsSectionTitle
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
// import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import com.comp350sec001group2.ekhoexplorer.ui.navigation.models.Screen
import com.comp350sec001group2.ekhoexplorer.ui.theme.AppStyle

@Composable
fun SettingsScreen(
    username: String,
    onChangePassword: () -> Unit,
    onRequestUsernameChange: () -> Unit,
    onSignOut: () -> Unit
) {
    var showSignOutConfirmation by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    val contributors = listOf(
        "Luis Corral",
        "Ethan Hook",
        "Connor Green",
        "Kieron Rank",
        "Anthony Rodriguez",
        "Alejandra Salazar",
        "Alex Walker",
        "Drew Wishengrad"
    )

    val dimensions = AppStyle.dimens
    val colors = AppStyle.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {

        // Settings content
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = dimensions.lg, vertical = dimensions.md),
            verticalArrangement = Arrangement.spacedBy(dimensions.sm)
        ) {
            SettingsSectionTitle("Your account")

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = dimensions.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dimensions.md)
            ) {
                Icon(
                    imageVector = Icons.Filled.AccountCircle,
                    contentDescription = null,
                    tint = colors.textSecondary
                )

                Column {
                    Text(
                        text = "Account Information",
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.textPrimary
                    )

                    Text(
                        text = if (username.isNotBlank()) {
                            "Signed in as @$username"
                        } else {
                            "Signed-in account"
                        },
                        style = AppStyle.type.caption,
                        color = colors.textSecondary
                    )
                }
            }

            SettingsItem(
                title = "Change password",
                icon = Icons.Filled.Lock,
                onClick = onChangePassword
            )

            SettingsItem(
                title = "Request username change",
                icon = Icons.Filled.Person,
                onClick = onRequestUsernameChange
            )

            HorizontalDivider(color = colors.divider)

            SettingsSectionTitle("About")

            SettingsItem(
                title = "About Ekho Explorer",
                icon = Icons.Filled.Info,
                onClick = { showAboutDialog = true },
                showChevron = false
            )
        }

        // Separating sign-out button from rest of settings list, making it a fixed button at the bottom of the page
        HorizontalDivider(color = colors.divider)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    showSignOutConfirmation = true
                }
                .padding(
                    horizontal = dimensions.lg,
                    vertical = dimensions.lg
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimensions.md)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )

            Text(
                text = "Sign out",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error
            )
        }

    }

    // Displays sign-out confirmation behavior
    if (showSignOutConfirmation) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirmation = false },
            title = { Text("Sign out?") },
            text = {
                Text("You will need to sign in again to use Ekho Explorer.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSignOutConfirmation = false
                        onSignOut()
                    }
                ) {
                    Text("Sign out")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showSignOutConfirmation = false }
                ) {
                    Text("Cancel")
                }
            }

        )
    }

    // Displays the app information

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = {
                showAboutDialog = false
            },
            title = {
                Text("About Ekho Explorer")
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(
                        AppStyle.dimens.md
                    )
                ) {
                    // Application description.
                    Text(
                        text = "Explore the natural world, record observations, " +
                            "and share discoveries with your community.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppStyle.colors.textPrimary
                    )

                    HorizontalDivider(
                        color = AppStyle.colors.divider
                    )

                    // Contributors section heading.
                    Text(
                        text = "Contributors",
                        style = MaterialTheme.typography.titleSmall,
                        color = AppStyle.colors.textPrimary
                    )

                    // Displays contributor names in two columns.
                    Column(
                        verticalArrangement = Arrangement.spacedBy(
                            AppStyle.dimens.sm
                        )
                    ) {
                        contributors.chunked(2).forEach { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(
                                    AppStyle.dimens.md
                                )
                            ) {
                                pair.forEach { contributor ->
                                    Text(
                                        text = contributor,
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = AppStyle.colors.textSecondary
                                    )
                                }

                                if (pair.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showAboutDialog = false
                    }
                ) {
                    Text("Close")
                }
            }
        )
    }




}
