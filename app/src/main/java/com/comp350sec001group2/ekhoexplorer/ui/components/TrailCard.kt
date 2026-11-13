package com.comp350sec001group2.ekhoexplorer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import com.comp350sec001group2.ekhoexplorer.Business.Trails.Trail
import com.comp350sec001group2.ekhoexplorer.Business.Trails.TrailFormat
import com.comp350sec001group2.ekhoexplorer.Business.Trails.TrailStatus
import com.comp350sec001group2.ekhoexplorer.Business.Trails.TrailVisibility
import com.comp350sec001group2.ekhoexplorer.ui.theme.AppStyle

fun TrailVisibility.label(): String = when (this) {
    TrailVisibility.PUBLIC -> "Public"
    TrailVisibility.FRIENDS -> "Friends only"
    TrailVisibility.PRIVATE -> "Private"
}

@Composable
fun TrailCard(
    trail: Trail,
    distanceMeters: Double?,
    isOwner: Boolean,
    canRequestVerification: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onRequestVerification: () -> Unit = {},
) {
    val d = AppStyle.dimens
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(AppStyle.colors.background)
            .clickable(onClick = onClick)
            .testTag("trail-card-${trail.id}"),
    ) {
        TrailCover(trailId = trail.id, photoCount = trail.photoCount, modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f))

        Column(modifier = Modifier.padding(horizontal = d.lg, vertical = d.md), verticalArrangement = Arrangement.spacedBy(d.xs)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(d.sm)) {
                Text(
                    text = trail.name,
                    style = AppStyle.type.username,
                    color = AppStyle.colors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (trail.status == TrailStatus.VERIFIED) {
                    Icon(
                        imageVector = Icons.Filled.Verified,
                        contentDescription = "Verified trail",
                        tint = AppStyle.colors.accent,
                        modifier = Modifier.size(d.icon).testTag("verified-badge-${trail.id}"),
                    )
                } else if (trail.visibility == TrailVisibility.PUBLIC) {
                    Text(
                        text = "Unverified",
                        style = AppStyle.type.caption,
                        color = AppStyle.colors.textSecondary,
                        modifier = Modifier.testTag("unverified-label-${trail.id}"),
                    )
                }
            }

            val place = listOfNotNull(trail.location.takeIf { it.isNotBlank() })
            if (place.isNotEmpty() || distanceMeters != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(d.sm)) {
                    place.forEach { Text(it, style = AppStyle.type.caption, color = AppStyle.colors.textSecondary) }
                    if (distanceMeters != null) {
                        Text(
                            text = "${TrailFormat.miles(distanceMeters)} away",
                            style = AppStyle.type.caption,
                            color = AppStyle.colors.textSecondary,
                            modifier = Modifier.testTag("trail-distance-${trail.id}"),
                        )
                    }
                }
            }

            Text(
                text = "${TrailFormat.miles(trail.lengthMeters)}  -  Difficulty ${trail.difficulty} of 5  -  ${trail.visibility.label()}",
                style = AppStyle.type.caption,
                color = AppStyle.colors.textPrimary,
            )

            Text(
                text = TrailFormat.ratingText(trail),
                style = AppStyle.type.caption,
                color = AppStyle.colors.textSecondary,
                modifier = Modifier.testTag("trail-rating-${trail.id}"),
            )

            if (canRequestVerification) {
                OutlinedButton(
                    onClick = onRequestVerification,
                    modifier = Modifier.testTag("request-verification-${trail.id}"),
                ) { Text("Request verification") }
            } else if (isOwner && trail.verificationRequested && trail.status == TrailStatus.UNVERIFIED) {
                // No label any more, but the line keeps its place so the card does not change height.
                Text(text = " ", style = AppStyle.type.caption)
            }
        }
        HorizontalDivider(color = AppStyle.colors.divider)
    }
}
