package com.comp350sec001group2.ekhoexplorer.ui.pages.trailWizard

import com.comp350sec001group2.ekhoexplorer.Business.Photos.PhotoErrorKind
import com.comp350sec001group2.ekhoexplorer.Business.Trails.TrailFormError
import com.comp350sec001group2.ekhoexplorer.Business.Trails.TrailLimits

fun TrailFormError.message(): String = when (this) {
    TrailFormError.NAME_REQUIRED -> "Give the trail a name."
    TrailFormError.NAME_TOO_LONG -> "The name can be at most ${TrailLimits.NAME_MAX} characters."
    TrailFormError.DESCRIPTION_TOO_LONG -> "The description can be at most ${TrailLimits.DESCRIPTION_MAX} characters."
    TrailFormError.LOCATION_TOO_LONG -> "The location can be at most ${TrailLimits.LOCATION_MAX} characters."
    TrailFormError.LENGTH_NOT_POSITIVE -> "The length must be more than zero. Add pins that are not on the same spot, or type the length."
    TrailFormError.DIFFICULTY_OUT_OF_RANGE -> "Pick a difficulty from 1 to 5."
    TrailFormError.TOO_MANY_PHOTOS -> "A trail can have at most ${TrailLimits.OWNER_PHOTOS} pictures."
    TrailFormError.PATH_TOO_SHORT -> "Drop at least two pins to make a trail."
}

fun PhotoErrorKind.trailMessage(): String = when (this) {
    PhotoErrorKind.UNSUPPORTED_TYPE -> "Only JPEG, PNG and WebP pictures can be used."
    PhotoErrorKind.INVALID_IMAGE -> "One of the pictures could not be read."
    PhotoErrorKind.INPUT_TOO_LARGE -> "One of the pictures is too large."
    PhotoErrorKind.TOO_DETAILED -> "One of the pictures is too detailed to shrink enough. Try another."
    PhotoErrorKind.TOO_MANY_PHOTOS -> "Too many pictures."
}
