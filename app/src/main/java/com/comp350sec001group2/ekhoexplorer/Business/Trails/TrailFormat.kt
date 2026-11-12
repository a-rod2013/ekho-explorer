package com.comp350sec001group2.ekhoexplorer.Business.Trails

import java.util.Locale

/** How trail numbers are shown and typed. Lengths are stored in metres and shown in miles. */
object TrailFormat {

    private const val METERS_PER_MILE = 1609.344

    fun miles(meters: Double): String = String.format(Locale.US, "%.1f mi", meters / METERS_PER_MILE)

    /** The miles a user typed, as metres; null if it is not a usable number. */
    fun metersFromMiles(text: String): Double? {
        val miles = text.trim().toDoubleOrNull() ?: return null
        return if (miles.isFinite()) miles * METERS_PER_MILE else null
    }

    fun ratingText(trail: Trail): String {
        val average = TrailSorting.averageRating(trail) ?: return "No ratings"
        return String.format(Locale.US, "%.1f (%d)", average, trail.ratingCount)
    }
}
