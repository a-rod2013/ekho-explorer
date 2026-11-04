package com.comp350sec001group2.ekhoexplorer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.Post
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.SamplePosts
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.Vote
import com.comp350sec001group2.ekhoexplorer.ui.theme.AppStyle
import com.comp350sec001group2.ekhoexplorer.ui.theme.EkhoExplorerTheme

@Composable
fun PostCard(
    post: Post,
    onVote: (Vote) -> Unit,
    modifier: Modifier = Modifier,
    isOwner: Boolean = false,
    onOpenComments: () -> Unit = {},
    onDelete: () -> Unit = {},
    onHideUser: () -> Unit = {},
    onBlockUser: () -> Unit = {},
) {
    val d = AppStyle.dimens
    Column(modifier = modifier.fillMaxWidth().background(AppStyle.colors.background)) {
        PostHeaderRow(avatarUrl = post.avatarUrl, username = post.username) {
            PostMenu(
                username = post.username,
                isOwner = isOwner,
                onDelete = onDelete,
                onHideUser = onHideUser,
                onBlockUser = onBlockUser,
            )
        }
        if (post.imageUrls.isNotEmpty()) {
            ImageCarousel(imageUrls = post.imageUrls)
        }
        Row(
            modifier = Modifier.padding(horizontal = d.sm, vertical = d.xs),
            horizontalArrangement = Arrangement.spacedBy(d.sm),
        ) {
            VoteBar(
                upCount = post.upCount,
                downCount = post.downCount,
                myVote = post.myVote,
                onVote = onVote,
            )
            CommentButton(count = post.commentCount, onClick = onOpenComments)
        }
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
private fun PostCardPreviewMulti() {
    EkhoExplorerTheme{
        PostCard(SamplePosts.all[1], onVote = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun PostCardPreviewNoCaption() {
    EkhoExplorerTheme {
        PostCard(SamplePosts.all[2], onVote = {})
    }
}
