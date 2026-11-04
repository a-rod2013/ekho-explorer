package com.comp350sec001group2.ekhoexplorer.Business.Photos

enum class PhotoErrorKind {
    /** A real file, but not a JPEG, PNG or WebP. */
    UNSUPPORTED_TYPE,

    /** Looks like an image but cannot be read. */
    INVALID_IMAGE,

    /** The original is bigger than [PhotoLimits.maxInputBytes]. */
    INPUT_TOO_LARGE,

    /** Even the smallest allowed version is bigger than [PhotoLimits.hardCapBytes]. */
    TOO_DETAILED,

    /** More photos than [PhotoLimits.maxPhotos] in one post. */
    TOO_MANY_PHOTOS,
}

class PhotoException(
    val kind: PhotoErrorKind,
    val detail: String,
    cause: Throwable? = null,
) : RuntimeException("$kind: $detail", cause)
