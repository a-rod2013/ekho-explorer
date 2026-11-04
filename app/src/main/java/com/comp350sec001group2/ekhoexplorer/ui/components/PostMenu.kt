package com.comp350sec001group2.ekhoexplorer.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.comp350sec001group2.ekhoexplorer.ui.theme.AppStyle

private enum class PendingAction { DELETE, BLOCK }

/**
 * Only owner gets Delete.
 * Everyone else gets "Don't show posts from this user" and Block.
 * Delete and Block ask for confirmation first.
 */
@Composable
fun PostMenu(
    username: String,
    isOwner: Boolean,
    onDelete: () -> Unit,
    onHideUser: () -> Unit,
    onBlockUser: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf<PendingAction?>(null) }

    Box(modifier) {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = "Post options",
                tint = AppStyle.colors.textSecondary,
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (isOwner) {
                DropdownMenuItem(
                    text = { Text("Delete post") },
                    onClick = { expanded = false; pending = PendingAction.DELETE },
                )
            } else {
                DropdownMenuItem(
                    text = { Text("Don't show posts from $username") },
                    onClick = { expanded = false; onHideUser() },
                )
                DropdownMenuItem(
                    text = { Text("Block $username") },
                    onClick = { expanded = false; pending = PendingAction.BLOCK },
                )
            }
        }
    }

    pending?.let { action ->
        val isDelete = action == PendingAction.DELETE
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text(if (isDelete) "Delete this post?" else "Block $username?") },
            text = {
                Text(
                    if (isDelete) "This also removes its comments and votes. It cannot be undone."
                    else "You will no longer see their posts or comments.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    pending = null
                    if (isDelete) onDelete() else onBlockUser()
                }) {
                    Text(if (isDelete) "Delete" else "Block", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { pending = null }) { Text("Cancel") } },
        )
    }
}
