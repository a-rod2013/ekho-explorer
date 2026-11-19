package com.comp350sec001group2.ekhoexplorer.Business.Database.Repositories

import android.util.Log
import com.comp350sec001group2.ekhoexplorer.Business.Cache.EhkoCache
import com.comp350sec001group2.ekhoexplorer.Business.Database.Batches.CreateTrailBatch
import com.comp350sec001group2.ekhoexplorer.Business.Database.Commands.DeleteTrailCommand
import com.comp350sec001group2.ekhoexplorer.Business.Database.Commands.PurgeReviewHeartsCommand
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreException
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreIds
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreService
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.TrailReview
import com.comp350sec001group2.ekhoexplorer.Business.Database.Page
import com.comp350sec001group2.ekhoexplorer.Business.Database.PageCursor
import com.comp350sec001group2.ekhoexplorer.Business.Database.Queries.GetTrailReviewPhotoQuery
import com.comp350sec001group2.ekhoexplorer.Business.Database.Queries.GetTrailReviewsPageQuery
import com.comp350sec001group2.ekhoexplorer.Business.Database.Queries.GetTrailPhotoQuery
import com.comp350sec001group2.ekhoexplorer.Business.Database.Queries.GetTrailQuery
import com.comp350sec001group2.ekhoexplorer.Business.Database.Queries.GetTrailViewerStateQuery
import com.comp350sec001group2.ekhoexplorer.Business.Database.Queries.GetVisibleTrailsQuery
import com.comp350sec001group2.ekhoexplorer.Business.Database.Transactions.DeleteTrailReviewTransaction
import com.comp350sec001group2.ekhoexplorer.Business.Database.Transactions.HeartTrailReviewTransaction
import com.comp350sec001group2.ekhoexplorer.Business.Database.Transactions.PromoteReviewPhotoTransaction
import com.comp350sec001group2.ekhoexplorer.Business.Database.Transactions.RequestVerificationTransaction
import com.comp350sec001group2.ekhoexplorer.Business.Database.Transactions.UpdateTrailTransaction
import com.comp350sec001group2.ekhoexplorer.Business.Database.Transactions.SaveTrailReviewTransaction
import com.comp350sec001group2.ekhoexplorer.Business.Photos.EncodedPhoto
import com.comp350sec001group2.ekhoexplorer.Business.Trails.LatLngPoint
import com.comp350sec001group2.ekhoexplorer.Business.Trails.Trail
import com.comp350sec001group2.ekhoexplorer.Business.Trails.TrailInput
import com.comp350sec001group2.ekhoexplorer.Business.Trails.TrailRepository
import com.comp350sec001group2.ekhoexplorer.Business.Trails.TrailViewerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class FirestoreTrailRepository(private val service: FirestoreService) : TrailRepository {

    private val state = MutableStateFlow<List<Trail>>(emptyList())
    override val trails: StateFlow<List<Trail>> = state.asStateFlow()

    private val user get() = EhkoCache.activeUser

    override suspend fun refresh() {
        state.value = service.execute(GetVisibleTrailsQuery(user.userId, user.following, user.isAdmin))
    }

    override suspend fun create(
        input: TrailInput,
        path: List<LatLngPoint>,
        photos: List<EncodedPhoto>,
        requestVerification: Boolean,
    ): String {
        val id = FirestoreIds.newId()
        service.execute(CreateTrailBatch(id, user, input, path, photos, requestVerification))
        reload(id)
        return id
    }

    override suspend fun update(trailId: String, input: TrailInput, requestVerification: Boolean, photos: List<EncodedPhoto>?) {
        service.execute(UpdateTrailTransaction(trailId, user, input, requestVerification, photos))
        reload(trailId)
    }

    override suspend fun requestVerification(trailId: String) {
        service.execute(RequestVerificationTransaction(trailId, user))
        reload(trailId)
    }

    override suspend fun delete(trailId: String) {
        service.execute(DeleteTrailCommand(trailId, user))
        state.update { list -> list.filterNot { it.id == trailId } }
    }

    override suspend fun get(trailId: String): Trail? = service.execute(GetTrailQuery(trailId))

    override suspend fun saveReview(trailId: String, rating: Int, caption: String, photos: List<EncodedPhoto>?) {
        service.execute(SaveTrailReviewTransaction(trailId, user.userId, user.userName, rating, caption, photos))
        reload(trailId)
    }

    override suspend fun deleteReview(trailId: String, reviewId: String) {
        service.execute(DeleteTrailReviewTransaction(trailId, reviewId, user))
        // The review is gone; clearing its hearts is tidying, so a failure here does not undo the delete.
        try {
            service.execute(PurgeReviewHeartsCommand(trailId, reviewId))
        } catch (e: FirestoreException) {
            Log.w("TrailRepository", "Could not clear the hearts of review $reviewId: ${e.message}")
        }
        reload(trailId)
    }

    override suspend fun toggleHeart(trailId: String, reviewId: String): Boolean =
        service.execute(HeartTrailReviewTransaction(trailId, reviewId, user.userId))

    override suspend fun setPromoted(trailId: String, reviewId: String, index: Int, promote: Boolean) {
        service.execute(PromoteReviewPhotoTransaction(trailId, reviewId, index, promote, user.userId))
        reload(trailId)
    }

    override suspend fun reviews(trailId: String, after: PageCursor?): Page<TrailReview> =
        service.execute(GetTrailReviewsPageQuery(trailId, after = after))

    override suspend fun viewerState(trailId: String): TrailViewerState =
        service.execute(GetTrailViewerStateQuery(trailId, user.userId))

    override suspend fun loadTrailPhoto(trailId: String, index: Int): ByteArray? =
        service.execute(GetTrailPhotoQuery(trailId, index))?.bytes

    override suspend fun loadReviewPhoto(trailId: String, reviewId: String, index: Int): ByteArray? =
        service.execute(GetTrailReviewPhotoQuery(trailId, reviewId, index))?.bytes

    override suspend fun clear() {
        state.value = emptyList()
    }

    private suspend fun reload(trailId: String) {
        val fresh = service.execute(GetTrailQuery(trailId))
        state.update { list ->
            when {
                fresh == null -> list.filterNot { it.id == trailId }
                list.any { it.id == trailId } -> list.map { if (it.id == trailId) fresh else it }
                else -> listOf(fresh) + list
            }
        }
    }
}
