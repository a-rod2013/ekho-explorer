package com.comp350sec001group2.ekhoexplorer.Business.Trails

enum class TrailSort { DISTANCE, DIFFICULTY, VISIBILITY, POPULARITY }

object TrailSorting {

    fun sort(trails: List<Trail>, by: TrailSort, ascending: Boolean, origin: LatLngPoint?): List<Trail> {
        val byName = compareBy<Trail> { it.name.lowercase() }.thenBy { it.id }
        return when (by) {
            TrailSort.DISTANCE -> sortByDistance(trails, ascending, origin, byName)
            TrailSort.DIFFICULTY -> trails.sortedWith(directed(compareBy<Trail> { it.difficulty }.then(byName), ascending))
            TrailSort.VISIBILITY -> trails.sortedWith(directed(compareBy<Trail> { it.visibility.ordinal }.then(byName), ascending))
            TrailSort.POPULARITY -> trails.sortedWith(directed(compareBy<Trail> { popularity(it) }.then(byName), ascending))
        }
    }

    fun defaultSort(hasLocation: Boolean): TrailSort = if (hasLocation) TrailSort.DISTANCE else TrailSort.POPULARITY

    fun defaultAscending(sort: TrailSort): Boolean = sort != TrailSort.POPULARITY

    fun popularity(trail: Trail): Long = trail.ratingCount

    fun averageRating(trail: Trail): Double? =
        if (trail.ratingCount <= 0) null else trail.ratingSum.toDouble() / trail.ratingCount

    private fun sortByDistance(
        trails: List<Trail>,
        ascending: Boolean,
        origin: LatLngPoint?,
        byName: Comparator<Trail>,
    ): List<Trail> {
        if (origin == null) return trails
        val (located, unlocated) = trails.partition { it.start != null }
        val distance = located.associateWith { TrailGeo.distanceMeters(origin, it.start!!) }
        val sorted = located.sortedWith(directed(compareBy<Trail> { distance.getValue(it) }.then(byName), ascending))
        return sorted + unlocated
    }

    private fun directed(comparator: Comparator<Trail>, ascending: Boolean): Comparator<Trail> =
        if (ascending) comparator else comparator.reversed()
}
