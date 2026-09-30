package com.fsql.data

/**
 * Why a query failed. Every error has two messages:
 *  - [userMessage] is safe to show in the UI (no technical detail).
 *  - [devMessage] names the procedure, parameter and cause; log it, never show it to users.
 */
sealed class DbError(
    val userMessage: String,
    val devMessage: String,
    val cause: Throwable? = null,
) {
    /** The query does not match its procedure (unknown name, missing/extra/wrong-type parameter, or SQL Firestore rejects). A bug in the calling code or the .fsql file. */
    class InvalidQuery(devMessage: String, cause: Throwable? = null) :
        DbError("Something went wrong with this request. Please try again, and contact support if it keeps happening.", devMessage, cause)

    /** A single-document UPDATE targeted a document that does not exist. */
    class NotFound(devMessage: String, cause: Throwable? = null) :
        DbError("That item could not be found.", devMessage, cause)

    class PermissionDenied(devMessage: String, cause: Throwable? = null) :
        DbError("You do not have permission to do that.", devMessage, cause)

    class Unauthenticated(devMessage: String, cause: Throwable? = null) :
        DbError("Please sign in and try again.", devMessage, cause)

    /** The query needs a composite index; [indexLink] opens the Firebase console page that creates it. */
    class MissingIndex(val indexLink: String?, devMessage: String, cause: Throwable? = null) :
        DbError("This request is not available right now. Please try again later.", devMessage, cause)

    /** The free-tier quota (or a rate limit) is used up. */
    class QuotaExceeded(devMessage: String, cause: Throwable? = null) :
        DbError("The service is busy right now. Please try again later.", devMessage, cause)

    class Network(devMessage: String, cause: Throwable? = null) :
        DbError("Check your internet connection and try again.", devMessage, cause)

    /** The database did not answer in time. A write that timed out MAY still be applied later, when the device reconnects. */
    class Timeout(devMessage: String, cause: Throwable? = null) :
        DbError("The request took too long. Please try again.", devMessage, cause)

    /** A document already existed for an INSERT. */
    class Conflict(devMessage: String, cause: Throwable? = null) :
        DbError("That item already exists or was changed by someone else. Please try again.", devMessage, cause)

    class Unknown(devMessage: String, cause: Throwable? = null) :
        DbError("Something went wrong. Please try again.", devMessage, cause)

    override fun toString(): String = "${this::class.simpleName}: $devMessage"
}

/** Thrown only by [DbResult.getOrThrow], for callers who prefer exceptions. */
class DbException(val error: DbError) : RuntimeException(error.toString(), error.cause)