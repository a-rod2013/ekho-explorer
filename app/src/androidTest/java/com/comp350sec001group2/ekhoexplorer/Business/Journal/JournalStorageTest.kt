package com.comp350sec001group2.ekhoexplorer.Business.Journal

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class JournalStorageTest {

    private val context: Context
        get() = InstrumentationRegistry
            .getInstrumentation()
            .targetContext

    @Test
    fun draftsSurviveReopeningAndStayScopedToOwner() = runBlocking {
        val name = "journal-test-${UUID.randomUUID()}.db"

        fun openDatabase(): JournalDatabase =
            Room.databaseBuilder(
                context,
                JournalDatabase::class.java,
                name,
            ).build()

        var database = openDatabase()

        try {
            var repository = LocalJournalRepository(
                database.journalDraftDao()
            )

            val original = JournalDraft(
                id = "same-id",
                ownerId = "owner-a",
                notes = "Original",
                createdAtMillis = 100L,
                updatedAtMillis = 100L,
            )

            val edited = original.copy(
                notes = "Edited offline",
                photoPaths = listOf("/example/photo,one.jpg"),
                updatedAtMillis = 200L,
            )

            val otherOwner = original.copy(
                ownerId = "owner-b",
                notes = "Other owner's entry",
            )

            repository.saveDraft(original)
            repository.saveDraft(edited)
            repository.saveDraft(otherOwner)

            database.close()
            database = openDatabase()

            repository = LocalJournalRepository(
                database.journalDraftDao()
            )

            assertEquals(
                edited,
                repository.getDraft("owner-a", "same-id"),
            )

            assertEquals(
                listOf(edited),
                repository.observeDrafts("owner-a").first(),
            )

            repository.deleteDraft("owner-a", "same-id")

            assertNull(repository.getDraft("owner-a", "same-id"))

            assertEquals(
                otherOwner,
                repository.getDraft("owner-b", "same-id"),
            )
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }

    @Test
    fun importedPhotoSurvivesRemovalOfOriginal() = runBlocking {
        val source = File.createTempFile(
            "journal-source-",
            ".img",
            context.cacheDir,
        )

        var retained: File? = null

        try {
            val bytes = byteArrayOf(10, 20, 30, 40)
            source.writeBytes(bytes)

            val path = JournalPhotoStorage(context).importPhoto(
                ownerId = "test-${UUID.randomUUID()}",
                source = Uri.fromFile(source),
            )

            val copy = File(path)
            retained = copy

            assertTrue(source.delete())
            assertTrue(copy.isFile)
            assertArrayEquals(bytes, copy.readBytes())
        } finally {
            source.delete()
            retained?.delete()
            retained?.parentFile?.delete()
        }
    }
}
