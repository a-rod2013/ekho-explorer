package com.comp350sec001group2.ekhoexplorer.ui.pages.activityFeed.views.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

import com.comp350sec001group2.ekhoexplorer.ui.pages.activityFeed.models.Post
import com.comp350sec001group2.ekhoexplorer.ui.pages.activityFeed.models.Vote
import com.comp350sec001group2.ekhoexplorer.ui.pages.activityFeed.models.SamplePosts
import com.comp350sec001group2.ekhoexplorer.ui.theme.AppStyle

@Composable
fun PostCard(post: Post, onVote: (Vote) -> Unit, modifier: Modifier = Modifier) {
    val d = AppStyle.dimens
    Column(modifier = modifier.fillMaxWidth().background(AppStyle.colors.background)) {
        PostHeaderRow(avatarUrl = post.avatarUrl, username = post.username)
        if (post.imageUrls.isNotEmpty()) {
            ImageCarousel(imageUrls = post.imageUrls)
        }
        VoteBar(
            upCount = post.upCount,
            downCount = post.downCount,
            myVote = post.myVote,
            onVote = onVote,
            modifier = Modifier.padding(horizontal = d.sm, vertical = d.xs),
        )
        if (post.caption.isNotBlank()) {
            ExpandableCaption(
                text = post.caption,
                modifier = Modifier.padding(start = d.lg, end = d.lg, bottom = d.md),
            )
        }
        HorizontalDivider(color = AppStyle.colors.divider)
    }
}

@Preview(showBackground = true)
@Composable
private fun PostCardPreviewMulti() = PostCard(SamplePosts.all[1], onVote = {})

@Preview(showBackground = true)
@Composable
private fun PostCardPreviewNoCaption() = PostCard(SamplePosts.all[2], onVote = {})
