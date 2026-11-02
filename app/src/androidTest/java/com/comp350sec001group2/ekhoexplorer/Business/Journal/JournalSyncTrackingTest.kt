package com.comp350sec001group2.ekhoexplorer.Business.Journal

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class JournalSyncTrackingTest {

    private lateinit var context: Context
    private lateinit var databaseName: String
    private lateinit var database: JournalDatabase

    @Before
    fun setUp() {
        context = InstrumentationRegistry
            .getInstrumentation()
            .targetContext

        databaseName = "journal-sync-test-${UUID.randomUUID()}.db"
        database = openDatabase()
    }

    @After
    fun tearDown() {
        database.close()
        context.deleteDatabase(databaseName)
    }

    private fun openDatabase(): JournalDatabase =
        Room.databaseBuilder(
            context,
            JournalDatabase::class.java,
            databaseName,
        )
            .build()

    private fun draft() = JournalDraft(
        id = "entry-1",
        ownerId = "owner-a",
        title = "Trail observation",
        notes = "Original note",
        photoPaths = listOf("/local/photo.jpg"),
        createdAtMillis = 100L,
        updatedAtMillis = 100L,
    )

    @Test
    fun olderUploadCannotAcknowledgeNewerEdit() = runBlocking {
        val dao = database.journalDraftDao()
        val repository = LocalJournalRepository(dao)
        val original = draft()

        repository.saveDraft(original)

        val uploaded = dao.getPendingChanges("owner-a", 50).single()
        assertEquals(1L, uploaded.revision)
        assertEquals(0L, uploaded.syncedRevision)

        repository.saveDraft(
            original.copy(
                notes = "Edited during upload",
                updatedAtMillis = 200L,
            )
        )

        assertEquals(
            0,
            dao.acknowledgeUpload(
                "owner-a",
                original.id,
                uploaded.revision,
            ),
        )

        val latest = dao.getPendingChanges("owner-a", 50).single()
        assertEquals(2L, latest.revision)
        assertEquals("Edited during upload", latest.notes)

        assertEquals(
            1,
            dao.acknowledgeUpload(
                "owner-a",
                original.id,
                latest.revision,
            ),
        )

        assertTrue(dao.getPendingChanges("owner-a", 50).isEmpty())

        repository.saveDraft(
            original.copy(
                notes = "Another edit",
                updatedAtMillis = 300L,
            )
        )

        val editedAgain = dao.getPendingChanges("owner-a", 50).single()
        assertEquals(3L, editedAgain.revision)
        assertEquals(2L, editedAgain.syncedRevision)
    }

    @Test
    fun deletionIsHiddenButRemainsPending() = runBlocking {
        val dao = database.journalDraftDao()
        val repository = LocalJournalRepository(dao)
        val original = draft()

        repository.saveDraft(original)
        dao.acknowledgeUpload("owner-a", original.id, 1L)

        repository.deleteDraft("owner-a", original.id)

        assertNull(repository.getDraft("owner-a", original.id))
        assertTrue(
            repository.observeDrafts("owner-a").first().isEmpty()
        )

        val deletion = dao.getPendingChanges("owner-a", 50).single()
        assertNotNull(deletion.deletedAtMillis)
        assertEquals(2L, deletion.revision)

        assertEquals(
            0,
            dao.acknowledgeUpload("owner-a", original.id, 1L),
        )

        assertEquals(
            0,
            dao.acknowledgeUpload("owner-b", original.id, 2L),
        )

        repository.deleteDraft("owner-a", original.id)

        assertEquals(
            2L,
            dao.getPendingChanges("owner-a", 50).single().revision,
        )

        assertEquals(
            1,
            dao.acknowledgeUpload("owner-a", original.id, 2L),
        )

        assertTrue(dao.getPendingChanges("owner-a", 50).isEmpty())

        var rejected = false
        try {
            repository.saveDraft(original)
        } catch (_: IllegalStateException) {
            rejected = true
        }

        assertTrue(rejected)
    }

    @Test
    fun pendingChangesSurviveDatabaseReopening() = runBlocking {
        val original = draft()

        LocalJournalRepository(database.journalDraftDao())
            .saveDraft(original)

        database.close()
        database = openDatabase()

        val dao = database.journalDraftDao()
        val pending = dao.getPendingChanges("owner-a", 50).single()

        assertEquals(original, pending.toDraft())
        assertEquals(1L, pending.revision)
        assertEquals(0L, pending.syncedRevision)
        assertTrue(dao.getPendingChanges("owner-b", 50).isEmpty())
    }
}
