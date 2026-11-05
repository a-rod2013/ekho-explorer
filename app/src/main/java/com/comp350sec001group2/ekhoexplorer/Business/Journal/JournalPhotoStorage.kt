package com.comp350sec001group2.ekhoexplorer.Business.Journal

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

class JournalPhotoStorage(context: Context) {

    private val appContext = context.applicationContext

    suspend fun importPhoto(
        ownerId: String,
        source: Uri,
    ): String = withContext(Dispatchers.IO) {
        require(ownerId.isNotBlank()) { "An owner ID is required." }

        val ownerDirectory = ownerDirectory(ownerId)

        if (!ownerDirectory.isDirectory &&
            !ownerDirectory.mkdirs()
        ) {
            throw IOException("Could not create journal photo directory.")
        }

        val mimeType = appContext.contentResolver.getType(source)

        require(mimeType == null || mimeType.startsWith("image/")) {
            "The selected file must be an image."
        }

        val extension = MimeTypeMap.getSingleton()
            .getExtensionFromMimeType(mimeType)
            ?.takeIf { it.matches(Regex("[A-Za-z0-9]+")) }
            ?: "img"

        val id = UUID.randomUUID().toString()
        val destination = File(ownerDirectory, "$id.$extension")
        val temporary = File(ownerDirectory, "$id.part")

        try {
            val input = appContext.contentResolver.openInputStream(source)
                ?: throw IOException("Could not open selected photo.")

            input.use { sourceStream ->
                FileOutputStream(temporary).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)

                    while (true) {
                        currentCoroutineContext().ensureActive()

                        val count = sourceStream.read(buffer)
                        if (count == -1) break

                        output.write(buffer, 0, count)
                    }

                    output.fd.sync()
                }
            }

            if (temporary.length() == 0L) {
                throw IOException("The selected photo is empty.")
            }

            currentCoroutineContext().ensureActive()

            if (!temporary.renameTo(destination)) {
                throw IOException("Could not finish saving the photo.")
            }

            destination.absolutePath
        } finally {
            temporary.delete()
        }
    }
    private fun ownerDirectory(ownerId: String): File {
        val hash = MessageDigest.getInstance("SHA-256")
            .digest(ownerId.toByteArray(Charsets.UTF_8))
            .joinToString("") {
                (it.toInt() and 0xff).toString(16).padStart(2, '0')
            }

        return File(appContext.filesDir, "journal_photos/$hash")
    }
}
