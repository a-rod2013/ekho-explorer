package com.comp350sec001group2.ekhoexplorer.Business.Database.Commands

import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreCommand
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreContext
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreErrorKind
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreException
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.PostFields
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.tasks.await

class AddFeedPostCommand(
    private val userId: String,
    private val username: String,
    private val avatarUrl: String?,
    private val imageUrls: List<String>,
    private val caption: String,
) : FirestoreCommand<String>() {

    override suspend fun run(db: FirestoreContext): String {
        if (imageUrls.isEmpty() && caption.isBlank()) {
            throw FirestoreException(FirestoreErrorKind.INVALID_ARGUMENT, "A post needs an image or a caption")
        }
        val ref = db.collection(PostFields.COLLECTION).document()
        val fields = mutableMapOf<String, Any?>(
            PostFields.USER_ID to userId,
            PostFields.USERNAME to username,
            PostFields.IMAGE_URLS to imageUrls,
            PostFields.CAPTION to caption.trim(),
            PostFields.UP_COUNT to 0L,
            PostFields.DOWN_COUNT to 0L,
            PostFields.CREATED_AT to FieldValue.serverTimestamp(),   // server time, not the phone's clock
        )
        avatarUrl?.let { fields[PostFields.AVATAR_URL] = it }
        ref.set(fields).await()
        return ref.id
    }
}
