package com.comp350sec001group2.ekhoexplorer.Business.Database.Batches

import com.comp350sec001group2.ekhoexplorer.Business.Database.BatchScope
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreBatch
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.Post
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.PostFields
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.SamplePosts
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.Vote
import com.google.firebase.Timestamp
import java.util.Date

/**
 * DEVELOPMENT ONLY. Writes sample posts (and the votes [userId] cast on them) to Firestore in
 * one atomic commit.
 *
 * Document IDs are the sample IDs ("p1", "p2", ...), so running it twice overwrites the same
 * documents instead of duplicating them. It also resets those posts' counts and [userId]'s
 * votes to the sample values, so run it on a fresh database, not alongside real data.
 *
 * Each post gets a createdAt one hour older than the previous, so the feed order is stable.
 */
class SeedFeedPostsBatch(
    private val userId: String,
    private val posts: List<Post> = SamplePosts.all,
) : FirestoreBatch() {

    override fun stage(batch: BatchScope) {
        val now = System.currentTimeMillis()
        posts.forEachIndexed { index, post ->
            val fields = mutableMapOf<String, Any?>(
                PostFields.USER_ID to "seed-${post.username}",
                PostFields.USERNAME to post.username,
                PostFields.IMAGE_URLS to post.imageUrls,
                PostFields.CAPTION to post.caption,
                PostFields.UP_COUNT to post.upCount.toLong(),
                PostFields.DOWN_COUNT to post.downCount.toLong(),
                PostFields.CREATED_AT to Timestamp(Date(now - index * HOUR_MILLIS)),
            )
            post.avatarUrl?.let { fields[PostFields.AVATAR_URL] = it }
            batch.set(batch.db.document(PostFields.COLLECTION, post.id), fields)

            // The samples include a vote for the current user on some posts; store it as a vote document.
            if (post.myVote != Vote.NONE) {
                batch.set(
                    batch.db.document("votes", "${post.id}_$userId"),
                    mapOf("postId" to post.id, "userId" to userId, "value" to post.myVote.name),
                )
            }
        }
    }

    private companion object {
        const val HOUR_MILLIS = 60 * 60 * 1000L
    }
}
