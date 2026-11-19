package com.comp350sec001group2.ekhoexplorer.Business.Database.Queries

import com.comp350sec001group2.ekhoexplorer.Business.Database.DocumentReader
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreContext
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestorePagedQuery
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreQuery
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.LenientTrailMapper
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.PhotoFields
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.PostPhoto
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.PostPhotoMapper
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.PresenceReader
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.TrailAccessMapper
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.TrailReview
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.TrailReviewFields
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.TrailReviewHeartFields
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.TrailReviewHeartMapper
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.TrailReviewMapper
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.TrailFields
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.TrailMapper
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.UserProfileFields
import com.comp350sec001group2.ekhoexplorer.Business.Database.PageCursor
import com.comp350sec001group2.ekhoexplorer.Business.Trails.Trail
import com.comp350sec001group2.ekhoexplorer.Business.Trails.TrailAccess
import com.comp350sec001group2.ekhoexplorer.Business.Trails.TrailLimits
import com.comp350sec001group2.ekhoexplorer.Business.Trails.TrailViewerState
import com.comp350sec001group2.ekhoexplorer.Business.Trails.TrailVisibility
import com.google.firebase.firestore.Query

internal const val MAX_IN_VALUES = 30

fun chunkFollowing(following: Set<String>): List<List<String>> = following.toList().chunked(MAX_IN_VALUES)

fun mergeTrails(vararg lists: List<Trail>): List<Trail> = lists.flatMap { it }.distinctBy { it.id }

class GetVisibleTrailsQuery(
    private val userId: String,
    private val following: Set<String>,
    private val isAdmin: Boolean,
    private val perQueryLimit: Int = TrailLimits.PER_QUERY_LIMIT,
) : FirestoreQuery<List<Trail>>() {

    override suspend fun run(db: FirestoreContext): List<Trail> {
        val trails = db.collection(TrailFields.COLLECTION)
        if (isAdmin) return db.getDocuments(trails, perQueryLimit, LenientTrailMapper).filterNotNull()

        val results = mutableListOf<List<Trail>>()
        results += db.getDocuments(trails.whereEqualTo(TrailFields.VISIBILITY, TrailVisibility.PUBLIC.key), perQueryLimit, LenientTrailMapper).filterNotNull()
        if (userId.isNotEmpty()) {
            results += db.getDocuments(trails.whereEqualTo(TrailFields.OWNER_ID, userId), perQueryLimit, LenientTrailMapper).filterNotNull()
        }
        for (group in chunkFollowing(following)) {
            results += db.getDocuments(
                trails.whereEqualTo(TrailFields.VISIBILITY, TrailVisibility.FRIENDS.key).whereIn(TrailFields.OWNER_ID, group),
                perQueryLimit,
                LenientTrailMapper,
            ).filterNotNull()
        }
        return mergeTrails(*results.toTypedArray())
    }
}

class GetTrailQuery(private val trailId: String) : FirestoreQuery<Trail?>() {
    override suspend fun run(db: FirestoreContext): Trail? =
        db.getDocument(db.document(TrailFields.COLLECTION, trailId), TrailMapper)
}

/** Picture [index] (0 to 4) of the trail's owner pictures, or null if there is none. */
class GetTrailPhotoQuery(private val trailId: String, private val index: Int) : FirestoreQuery<PostPhoto?>() {
    override suspend fun run(db: FirestoreContext): PostPhoto? =
        db.getDocument(db.document(TrailFields.COLLECTION, trailId, PhotoFields.COLLECTION, index.toString()), PostPhotoMapper)
}

/** Picture [index] (0 to 2) of one review, or null if there is none. */
class GetTrailReviewPhotoQuery(
    private val trailId: String,
    private val reviewId: String,
    private val index: Int,
) : FirestoreQuery<PostPhoto?>() {
    override suspend fun run(db: FirestoreContext): PostPhoto? = db.getDocument(
        db.document(TrailFields.COLLECTION, trailId, TrailReviewFields.COLLECTION, reviewId, PhotoFields.COLLECTION, index.toString()),
        PostPhotoMapper,
    )
}

/** One page of a trail's reviews, most liked first. */
class GetTrailReviewsPageQuery(
    private val trailId: String,
    pageSize: Int = 20,
    after: PageCursor? = null,
) : FirestorePagedQuery<TrailReview>(pageSize, after) {

    override val reader: DocumentReader<TrailReview> get() = TrailReviewMapper

    override fun baseQuery(db: FirestoreContext): Query =
        db.collection(TrailFields.COLLECTION, trailId, TrailReviewFields.COLLECTION)
            .orderBy(TrailReviewFields.HEART_COUNT, Query.Direction.DESCENDING)
}

/** Admin status and the following list from the users document. */
class GetTrailAccessQuery(private val userId: String) : FirestoreQuery<TrailAccess>() {
    override suspend fun run(db: FirestoreContext): TrailAccess =
        db.getDocument(db.document(UserProfileFields.COLLECTION, userId), TrailAccessMapper) ?: TrailAccess.None
}

/** This user's own review of one trail, and which reviews they have liked. */
class GetTrailViewerStateQuery(private val trailId: String, private val userId: String) : FirestoreQuery<TrailViewerState>() {
    override suspend fun run(db: FirestoreContext): TrailViewerState {
        val mine = db.getDocument(db.document(TrailFields.COLLECTION, trailId, TrailReviewFields.COLLECTION, userId), TrailReviewMapper)
        val hearts = db.getDocuments(
            db.collection(TrailFields.COLLECTION, trailId, TrailFields.REVIEW_HEARTS).whereEqualTo(TrailReviewHeartFields.USER_ID, userId),
            TrailLimits.PER_QUERY_LIMIT,
            TrailReviewHeartMapper,
        ).toSet()
        return TrailViewerState(myReview = mine, myHearts = hearts)
    }
}
