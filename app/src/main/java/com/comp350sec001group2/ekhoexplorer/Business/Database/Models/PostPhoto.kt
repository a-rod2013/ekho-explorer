package com.comp350sec001group2.ekhoexplorer.Business.Database.Models

import com.comp350sec001group2.ekhoexplorer.Business.Database.DocumentData
import com.comp350sec001group2.ekhoexplorer.Business.Database.DocumentReader

class PostPhoto(
    val index: Int,
    val bytes: ByteArray,
    val width: Int,
    val height: Int,
)

/** posts/{postId}/photos/{index}, where the document id is the photo's position: "0" to "4". */
object PhotoFields {
    const val COLLECTION = "photos"
    const val DATA = "data"          // Firestore bytes (Blob), not base64 text
    const val WIDTH = "width"
    const val HEIGHT = "height"
}

object PostPhotoMapper : DocumentReader<PostPhoto> {
    override fun read(doc: DocumentData) = PostPhoto(
        index = doc.id.toIntOrNull() ?: throw doc.mismatch("photo id '${doc.id}' is not a position"),
        bytes = doc.bytes(PhotoFields.DATA),
        width = doc.int(PhotoFields.WIDTH),
        height = doc.int(PhotoFields.HEIGHT),
    )
}
