package com.comp350sec001group2.ekhoexplorer.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.comp350sec001group2.ekhoexplorer.ui.theme.AppStyle

@Composable
fun AccountMenu(
    username: String,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    var confirming by remember { mutableStateOf(false) }

    Box(modifier) {
        IconButton(onClick = { expanded = true }, modifier = Modifier.testTag("account-button")) {
            Icon(
                imageVector = Icons.Filled.AccountCircle,
                contentDescription = "Account",
                tint = AppStyle.colors.textSecondary,
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (username.isNotBlank()) {
                DropdownMenuItem(
                    text = { Text("Signed in as $username") },
                    onClick = {},
                    enabled = false,
                )
            }
            DropdownMenuItem(
                text = { Text("Sign out") },
                onClick = { expanded = false; confirming = true },
                modifier = Modifier.testTag("sign-out-item"),
            )
        }
    }

    if (confirming) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            title = { Text("Sign out?") },
            text = { Text("You will need to sign in again to use Ekho Explorer.") },
            confirmButton = {
                TextButton(
                    onClick = { confirming = false; onSignOut() },
                    modifier = Modifier.testTag("sign-out-confirm"),
                ) { Text("Sign out") }
            },
            dismissButton = {
                TextButton(onClick = { confirming = false }) { Text("Cancel") }
            },
        )
    }
}
