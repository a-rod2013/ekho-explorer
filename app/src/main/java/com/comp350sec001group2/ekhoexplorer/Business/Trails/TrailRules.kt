package com.comp350sec001group2.ekhoexplorer.Business.Trails

/** Who may see, edit and verify a trail. The Firestore rules enforce the same decisions. */
object TrailRules {

    fun canView(trail: Trail, viewer: Viewer): Boolean = when {
        trail.visibility == TrailVisibility.PUBLIC -> true
        trail.ownerId == viewer.userId -> true
        viewer.isAdmin -> true
        trail.visibility == TrailVisibility.FRIENDS -> trail.ownerId in viewer.following
        else -> false
    }

    fun canEdit(trail: Trail, viewer: Viewer): Boolean = trail.ownerId == viewer.userId || viewer.isAdmin

    fun canRequestVerification(trail: Trail, viewer: Viewer): Boolean =
        trail.ownerId == viewer.userId &&
            trail.visibility == TrailVisibility.PUBLIC &&
            trail.status == TrailStatus.UNVERIFIED &&
            !trail.verificationRequested

    fun statusAfterEdit(old: Trail, new: TrailInput, editor: Viewer, photosReplaced: Boolean = false): TrailStatus = when {
        new.visibility != TrailVisibility.PUBLIC -> TrailStatus.UNVERIFIED
        old.status == TrailStatus.UNVERIFIED -> TrailStatus.UNVERIFIED
        editor.isAdmin -> TrailStatus.VERIFIED
        photosReplaced || contentChanged(old, new) -> TrailStatus.UNVERIFIED
        else -> TrailStatus.VERIFIED
    }

    fun requestAfterEdit(
        old: Trail,
        new: TrailInput,
        editor: Viewer,
        requested: Boolean,
        photosReplaced: Boolean = false,
    ): Boolean =
        requested &&
            new.visibility == TrailVisibility.PUBLIC &&
            statusAfterEdit(old, new, editor, photosReplaced) == TrailStatus.UNVERIFIED

    private fun contentChanged(old: Trail, new: TrailInput): Boolean =
        old.name != new.name ||
            old.description != new.description ||
            old.location != new.location ||
            old.lengthMeters != new.lengthMeters ||
            old.difficulty != new.difficulty ||
            old.visibility != new.visibility ||
            old.photoCount != new.photoCount
}
