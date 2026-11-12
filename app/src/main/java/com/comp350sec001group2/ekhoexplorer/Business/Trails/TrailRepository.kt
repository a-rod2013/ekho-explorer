package com.comp350sec001group2.ekhoexplorer.Business.Trails

import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.TrailReview
import com.comp350sec001group2.ekhoexplorer.Business.Database.Page
import com.comp350sec001group2.ekhoexplorer.Business.Database.PageCursor
import com.comp350sec001group2.ekhoexplorer.Business.Photos.EncodedPhoto
import kotlinx.coroutines.flow.StateFlow


interface TrailPhotoLoader {
    suspend fun loadTrailPhoto(trailId: String, index: Int): ByteArray?
    suspend fun loadReviewPhoto(trailId: String, reviewId: String, index: Int): ByteArray?
}

object NoTrailPhotoLoader : TrailPhotoLoader {
    override suspend fun loadTrailPhoto(trailId: String, index: Int): ByteArray? = null
    override suspend fun loadReviewPhoto(trailId: String, reviewId: String, index: Int): ByteArray? = null
}


interface TrailRepository : TrailPhotoLoader {
    val trails: StateFlow<List<Trail>>

    suspend fun refresh()

    /** Returns the new trail's id. */
    suspend fun create(input: TrailInput, path: List<LatLngPoint>, photos: List<EncodedPhoto>, requestVerification: Boolean): String

    /** [photos] null keeps the pictures as they are. */
    suspend fun update(trailId: String, input: TrailInput, requestVerification: Boolean, photos: List<EncodedPhoto>? = null)

    suspend fun requestVerification(trailId: String)
    suspend fun delete(trailId: String)
    suspend fun get(trailId: String): Trail?

    suspend fun saveReview(trailId: String, rating: Int, caption: String, photos: List<EncodedPhoto>?)

    /** The reviewer, the trail's owner and admins may delete a review. */
    suspend fun deleteReview(trailId: String, reviewId: String)

    /** Returns true if the review is now liked by the user. */
    suspend fun toggleHeart(trailId: String, reviewId: String): Boolean

    suspend fun setPromoted(trailId: String, reviewId: String, index: Int, promote: Boolean)

    /** One page of reviews, most liked first. */
    suspend fun reviews(trailId: String, after: PageCursor?): Page<TrailReview>
    suspend fun viewerState(trailId: String): TrailViewerState

    /** Forget loaded trails, after sign-out. */
    suspend fun clear()
}
