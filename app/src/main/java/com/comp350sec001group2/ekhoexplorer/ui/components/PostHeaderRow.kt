package com.comp350sec001group2.ekhoexplorer.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import com.comp350sec001group2.ekhoexplorer.EkhoApp
import com.comp350sec001group2.ekhoexplorer.ui.theme.AppStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

@Composable
fun PostHeaderRow(
    avatarUrl: String?,
    username: String,
    modifier: Modifier = Modifier,
    userId: String = "",
    onProfileClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit = {},
) {
    val d = AppStyle.dimens
    val context = LocalContext.current
    val app = context.applicationContext as EkhoApp

    // Retrieve the current profile picture belonging to the post author.
    val avatarBitmap by produceState<Bitmap?>(
        initialValue = null,
        userId
    ) {
        value = null

        if (userId.isNotBlank()) {
            value = try {
                val avatar = app.container.getUserAvatar(userId)

                avatar?.let {
                    withContext(Dispatchers.Default) {
                        BitmapFactory.decodeByteArray(
                            it.bytes,
                            0,
                            it.bytes.size
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(
                    "PostHeaderRow",
                    "Could not load post author avatar",
                    e
                )
                null
            }
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = d.lg,
                vertical = d.sm
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(d.md)
    ) {

        val openProfile = onProfileClick.takeIf { userId.isNotBlank() }
        val profileClick = if (openProfile != null) {
            Modifier.clickable(onClickLabel = "Open profile of $username", role = Role.Button, onClick = openProfile)
        } else {
            Modifier
        }

        // Display a saved avatar, legacy image, or default silhouette.
        Box(
            modifier = Modifier
                .size(d.avatar)
                .clip(CircleShape)
                .background(AppStyle.colors.surface)
                .then(profileClick)   // BUG FIX 02
                .testTag("post-avatar"),
            contentAlignment = Alignment.Center
        ) {
            when {
                avatarBitmap != null -> {
                    Image(
                        bitmap = avatarBitmap!!.asImageBitmap(),
                        contentDescription = "Profile picture of $username",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                userId.isBlank() && !avatarUrl.isNullOrBlank() -> {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = "Profile picture of $username",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                else -> {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Default profile picture",
                        modifier = Modifier.fillMaxSize(),
                        tint = AppStyle.colors.textSecondary
                    )
                }
            }
        }

        Text(
            text = username,
            style = AppStyle.type.username,
            color = AppStyle.colors.textPrimary,
            modifier = Modifier.weight(1f).then(profileClick)
        )
        trailing()
    }
}
