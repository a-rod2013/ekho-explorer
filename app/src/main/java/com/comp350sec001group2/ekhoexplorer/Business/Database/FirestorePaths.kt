package com.comp350sec001group2.ekhoexplorer.Business.Database

object FirestorePaths {
    private const val MAX_SEGMENT_BYTES = 1500

    fun requireValidSegment(segment: String, label: String = "path segment"): String {
        fun fail(reason: String): Nothing =
            throw FirestoreException(FirestoreErrorKind.INVALID_ARGUMENT, "Invalid $label: $reason")

        if (segment.isEmpty()) fail("empty")
        if (segment.contains('/')) fail("contains '/'")
        if (segment == "." || segment == "..") fail("reserved name")
        if (segment.length > 4 && segment.startsWith("__") && segment.endsWith("__")) fail("reserved __name__ pattern")
        if (segment.toByteArray(Charsets.UTF_8).size > MAX_SEGMENT_BYTES) fail("longer than $MAX_SEGMENT_BYTES bytes")
        return segment
    }

    internal fun join(segments: Array<out String>, expectDocument: Boolean): String {
        if (segments.isEmpty()) {
            throw FirestoreException(FirestoreErrorKind.INVALID_ARGUMENT, "Empty path")
        }
        val isDocument = segments.size % 2 == 0
        if (isDocument != expectDocument) {
            val want = if (expectDocument) "document (even number of segments)" else "collection (odd number of segments)"
            throw FirestoreException(FirestoreErrorKind.INVALID_ARGUMENT, "Path has ${segments.size} segments but a $want was requested")
        }
        return segments.joinToString("/") { requireValidSegment(it) }
    }
}
