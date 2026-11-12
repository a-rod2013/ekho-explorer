package com.comp350sec001group2.ekhoexplorer.Business.Trails

enum class TrailFormError {
    NAME_REQUIRED,
    NAME_TOO_LONG,
    DESCRIPTION_TOO_LONG,
    LOCATION_TOO_LONG,
    LENGTH_NOT_POSITIVE,
    DIFFICULTY_OUT_OF_RANGE,
    TOO_MANY_PHOTOS,
    PATH_TOO_SHORT,
}

object TrailValidator {

    fun validate(input: TrailInput, pointCount: Int): List<TrailFormError> = buildList {
        if (input.name.isBlank()) add(TrailFormError.NAME_REQUIRED)
        else if (input.name.length > TrailLimits.NAME_MAX) add(TrailFormError.NAME_TOO_LONG)
        if (input.description.length > TrailLimits.DESCRIPTION_MAX) add(TrailFormError.DESCRIPTION_TOO_LONG)
        if (input.location.length > TrailLimits.LOCATION_MAX) add(TrailFormError.LOCATION_TOO_LONG)
        if (!(input.lengthMeters > 0.0)) add(TrailFormError.LENGTH_NOT_POSITIVE) // also catches NaN
        if (input.difficulty !in TrailLimits.DIFFICULTY_MIN..TrailLimits.DIFFICULTY_MAX) add(TrailFormError.DIFFICULTY_OUT_OF_RANGE)
        if (input.photoCount > TrailLimits.OWNER_PHOTOS) add(TrailFormError.TOO_MANY_PHOTOS)
        if (pointCount < 2) add(TrailFormError.PATH_TOO_SHORT)
    }
}
