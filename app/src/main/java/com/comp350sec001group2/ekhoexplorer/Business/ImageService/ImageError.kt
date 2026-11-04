package com.comp350sec001group2.ekhoexplorer.Business.ImageService


enum class ImageErrorKind {
    /** The bytes are not a readable image (corrupt, truncated, or not an image at all). */
    INVALID_IMAGE,

    /** A real file, but not JPEG, PNG or WebP. */
    UNSUPPORTED_TYPE,

    /** The input or the download is bigger than the configured limit, or the server returned 413. */
    TOO_LARGE,

    /** The URL is malformed or nothing was requested. */
    INVALID_URL,
    NOT_FOUND,

    /** Supabase rejected the key or a storage policy blocked the request. */
    UNAUTHORIZED,
    ALREADY_EXISTS,

    /** Could not reach Supabase, or it answered 5xx / 429. */
    UNAVAILABLE,
    TIMEOUT,
    UNKNOWN,
}

/**
 * The single exception this package throws. [detail] is developer-facing and never contains
 * the API key or image contents. Coroutine cancellation is never converted to this.
 */
class ImageException(
    val kind: ImageErrorKind,
    val detail: String,
    cause: Throwable? = null,
) : RuntimeException("$kind: $detail", cause)
