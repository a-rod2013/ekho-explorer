package com.comp350sec001group2.ekhoexplorer.Business.Database.Models

object TrailFields {
    const val COLLECTION = "trails"

    const val OWNER_ID = "ownerId"
    const val OWNER_NAME = "ownerName"
    const val NAME = "name"
    const val DESCRIPTION = "description"
    const val LOCATION = "location"
    const val LENGTH_METERS = "lengthMeters"
    const val DIFFICULTY = "difficulty"
    const val VISIBILITY = "visibility"
    const val STATUS = "status"
    const val VERIFICATION_REQUESTED = "verificationRequested"
    const val PATH = "path"
    const val START_LAT = "startLat"
    const val START_LNG = "startLng"
    const val PHOTO_COUNT = "photoCount"
    const val GALLERY_PHOTOS = "galleryPhotos"
    const val RATING_SUM = "ratingSum"
    const val RATING_COUNT = "ratingCount"
    const val CREATED_AT = "createdAt"
    const val UPDATED_AT = "updatedAt"
    const val REVIEW_HEARTS = "reviewHearts"
}

object TrailReviewFields {
    const val COLLECTION = "reviews"
    const val USER_ID = "userId"
    const val USER_NAME = "userName"
    const val CAPTION = "caption"
    const val RATING = "rating"
    const val PHOTO_COUNT = "photoCount"
    const val HEART_COUNT = "heartCount"
    const val CREATED_AT = "createdAt"
    const val UPDATED_AT = "updatedAt"
}

object TrailReviewHeartFields {
    const val REVIEW_ID = "reviewId"
    const val USER_ID = "userId"
}

object UserTrailFields {
    const val PERMISSION = "permission"
    const val ADMIN = "admin"
    const val FOLLOWING = "following"
}
