package com.comp350sec001group2.ekhoexplorer.Business.Trails

import com.comp350sec001group2.ekhoexplorer.Business.Cache.ActiveUser
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.TrailReview

/** What the users document says about the signed-in account, beyond its name. */
data class TrailAccess(val isAdmin: Boolean, val following: Set<String>) {
    companion object {
        val None = TrailAccess(isAdmin = false, following = emptySet())
    }
}

fun ActiveUser.toViewer(): Viewer = Viewer(userId = userId, isAdmin = isAdmin, following = following)

/** What the signed-in user has already done on one trail. */
data class TrailViewerState(
    /** My own review of the trail, if I wrote one. */
    val myReview: TrailReview? = null,
    /** The reviews I have given a heart to, by review id. */
    val myHearts: Set<String> = emptySet(),
)

/** How a rating (or a changed rating) moves the trail's two rating counters. */
object RatingMath {
    data class Change(val sumDelta: Long, val countDelta: Long)

    fun change(previous: Int?, stars: Int): Change =
        if (previous == null) Change(sumDelta = stars.toLong(), countDelta = 1) else Change((stars - previous).toLong(), 0)
}
