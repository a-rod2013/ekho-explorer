package com.comp350sec001group2.ekhoexplorer.Business.Journal

import com.google.firebase.auth.FirebaseAuth

class JournalIdentity(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) {
    fun currentOwnerId(): String? = auth.currentUser?.uid

    fun isCurrentOwner(ownerId: String): Boolean =
        ownerId.isNotBlank() && currentOwnerId() == ownerId
}
