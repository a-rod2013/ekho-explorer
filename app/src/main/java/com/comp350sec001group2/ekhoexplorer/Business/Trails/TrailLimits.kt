package com.comp350sec001group2.ekhoexplorer.Business.Trails

/** Every limit in one place. The Firestore rules enforce the same numbers. */
object TrailLimits {
    const val NAME_MAX = 60
    const val DESCRIPTION_MAX = 1_000
    const val LOCATION_MAX = 80
    const val REVIEW_CAPTION_MAX = 500

    const val DIFFICULTY_MIN = 1
    const val DIFFICULTY_MAX = 5

    const val OWNER_PHOTOS = 5
    const val REVIEW_PHOTOS = 3
    const val PROMOTED_PHOTOS = 10

    const val MAX_PINS = 200
    const val MAX_WALK_POINTS = 2_000

    const val MIN_STEP_METERS = 5.0
    const val MAX_ACCURACY_METERS = 30f
    const val PER_QUERY_LIMIT = 100
}
