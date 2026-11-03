package com.comp350sec001group2.ekhoexplorer.Business.Journal

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.comp350sec001group2.ekhoexplorer.Business.Database.Commands.JournalUploadResult
import com.comp350sec001group2.ekhoexplorer.Business.Database.Commands.UploadJournalDraftTransaction
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreService
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings
import com.google.firebase.firestore.Source
import java.util.UUID
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class JournalCloudIntegrationTest {

    private data class Client(
        val app: FirebaseApp,
        val auth: FirebaseAuth,
        val firestore: FirebaseFirestore,
    ) {
        val identity = JournalIdentity(auth)
        val service = FirestoreService(firestore)
    }

    private val clients = mutableListOf<Client>()

    private lateinit var owner: Client

    @Before
    fun setUp() = runBlocking {
        assumeTrue(
            "Start Firebase emulators and enable journalCloudTests.",
            InstrumentationRegistry.getArguments()
                .getString(CLOUD_TEST_ARGUMENT) == "true",
        )

        withTimeout(SETUP_TIMEOUT) {
            owner = createClient()
        }
    }

    @After
    fun tearDown() = runBlocking {
        try {
            for (client in clients) {
                try {
                    withTimeoutOrNull(CLEANUP_TIMEOUT) {
                        client.firestore.terminate().await()
                    }
                } finally {
                    client.app.delete()
                }
            }
        } finally {
            clients.clear()
        }
    }

    private suspend fun createClient(
        signIn: Boolean = true,
    ): Client {
        val context = InstrumentationRegistry
            .getInstrumentation()
            .targetContext

        val options = FirebaseOptions.Builder()
            .setProjectId(DEMO_PROJECT_ID)
            .setApplicationId(TEST_APPLICATION_ID)
            .setApiKey(TEST_API_KEY)
            .build()

        val app = FirebaseApp.initializeApp(
            context,
            options,
            "journal-test-${UUID.randomUUID()}",
        )

        val auth = FirebaseAuth.getInstance(app)
        auth.useEmulator(
            EMULATOR_HOST,
            AUTH_EMULATOR_PORT,
        )

        val firestore = FirebaseFirestore.getInstance(app)

        firestore.firestoreSettings =
            FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(
                    MemoryCacheSettings.newBuilder().build()
                )
                .build()

        firestore.useEmulator(
            EMULATOR_HOST,
            FIRESTORE_EMULATOR_PORT,
        )

        val client = Client(app, auth, firestore)
        clients.add(client)

        if (signIn) {
            // This account exists only in the local Auth emulator.
            // It does not enable anonymous login in the production app.
            auth.signInAnonymously().await()
        }

        return client
    }

    private fun draft() = JournalDraft(
        id = UUID.randomUUID().toString(),
        ownerId = requireNotNull(owner.auth.currentUser).uid,
        title = "Cloud test",
        notes = "Private observation",
        photoPaths = listOf("/local/private-photo.jpg"),
        createdAtMillis = INITIAL_TIMESTAMP_MILLIS,
        updatedAtMillis = INITIAL_TIMESTAMP_MILLIS,
    )

    private fun document(
        client: Client,
        draft: JournalDraft,
    ): DocumentReference =
        client.firestore
            .collection("users")
            .document(draft.ownerId)
            .collection("journalEntries")
            .document(draft.id)

    private suspend fun upload(
        entry: JournalDraftEntity,
    ): JournalUploadResult =
        owner.service.execute(
            UploadJournalDraftTransaction(
                draft = entry,
                identity = owner.identity,
            )
        )

    private suspend fun expectPermissionDenied(
        block: suspend () -> Unit,
    ) {
        try {
            block()
            fail("Expected Firestore PERMISSION_DENIED")
        } catch (firestoreFailure: FirebaseFirestoreException) {
            assertEquals(
                FirebaseFirestoreException.Code.PERMISSION_DENIED,
                firestoreFailure.code,
            )
        }
    }

    private fun cloudTest(
        block: suspend () -> Unit,
    ): Unit = runBlocking {
        withTimeout(TEST_TIMEOUT) {
            block()
        }
    }

    @Test
    fun coordinatorSyncsTextEditsAndDeletion() = cloudTest {
        val context = InstrumentationRegistry
            .getInstrumentation()
            .targetContext

        val databaseName = "cloud-test-${UUID.randomUUID()}.db"

        val database = Room.databaseBuilder(
            context,
            JournalDatabase::class.java,
            databaseName,
        ).build()

        try {
            val dao = database.journalDraftDao()
            val repository = LocalJournalRepository(dao)

            val coordinator = JournalSyncCoordinator(
                dao = dao,
                service = owner.service,
                identity = owner.identity,
            )

            val original = draft()
            repository.saveDraft(original)

            assertEquals(
                JournalSyncOutcome.COMPLETE,
                coordinator.sync(original.ownerId),
            )

            assertTrue(
                dao.getPendingChanges(
                    original.ownerId,
                    PENDING_BATCH_SIZE,
                ).isEmpty()
            )

            val ref = document(owner, original)
            val uploaded = ref.get(Source.SERVER).await()

            assertEquals(
                original.notes,
                uploaded.getString("notes"),
            )
            assertEquals(1L, uploaded.getLong("revision"))
            assertNotNull(uploaded.getTimestamp("syncedAt"))

            // Local photo paths and local sync bookkeeping must stay local.
            assertFalse(uploaded.contains("photoPaths"))
            assertFalse(uploaded.contains("photoPathsJson"))
            assertFalse(uploaded.contains("syncedRevision"))

            repository.saveDraft(
                original.copy(
                    notes = "Edited observation",
                    updatedAtMillis = EDITED_TIMESTAMP_MILLIS,
                )
            )

            assertEquals(
                JournalSyncOutcome.COMPLETE,
                coordinator.sync(original.ownerId),
            )

            val edited = ref.get(Source.SERVER).await()

            assertEquals(
                "Edited observation",
                edited.getString("notes"),
            )
            assertEquals(2L, edited.getLong("revision"))

            repository.deleteDraft(
                original.ownerId,
                original.id,
            )

            assertEquals(
                JournalSyncOutcome.COMPLETE,
                coordinator.sync(original.ownerId),
            )

            val deleted = ref.get(Source.SERVER).await()

            assertEquals(3L, deleted.getLong("revision"))
            assertNotNull(deleted.getLong("deletedAtMillis"))
            assertEquals("", deleted.getString("title"))
            assertEquals("", deleted.getString("notes"))

            assertNull(
                repository.getDraft(
                    original.ownerId,
                    original.id,
                )
            )

            assertTrue(
                dao.getPendingChanges(
                    original.ownerId,
                    PENDING_BATCH_SIZE,
                ).isEmpty()
            )
        } finally {
            database.close()
            context.deleteDatabase(databaseName)
        }
    }

    @Test
    fun retriesAreIdempotentAndOlderRevisionsCannotOverwrite() =
        cloudTest {
            val original = draft()

            val newer = original.copy(
                notes = "Newer content",
                updatedAtMillis = EDITED_TIMESTAMP_MILLIS,
            ).toEntity().copy(revision = 2L)

            assertEquals(
                JournalUploadResult.WRITTEN,
                upload(newer),
            )

            assertEquals(
                JournalUploadResult.ALREADY_UPLOADED,
                upload(newer),
            )

            assertEquals(
                JournalUploadResult.CONFLICT,
                upload(original.toEntity()),
            )

            assertEquals(
                JournalUploadResult.CONFLICT,
                upload(
                    newer.copy(
                        notes = "Different same-revision text",
                    )
                ),
            )

            val ref = document(owner, original)
            val saved = ref.get(Source.SERVER).await()

            assertEquals(2L, saved.getLong("revision"))
            assertEquals(
                "Newer content",
                saved.getString("notes"),
            )

            // Bypass the transaction to test server-side rules directly.
            expectPermissionDenied {
                ref.update(
                    mapOf(
                        "revision" to 1L,
                        "notes" to "Stale overwrite",
                        "syncedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
            }

            expectPermissionDenied {
                ref.update(
                    mapOf(
                        "revision" to 3L,
                        "photoPathsJson" to
                            """["/local/private.jpg"]""",
                        "syncedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
            }

            expectPermissionDenied {
                ref.delete().await()
            }
        }

    @Test
    fun otherAccountsAndSignedOutClientsCannotAccessJournal() =
        cloudTest {
            val original = draft()

            assertEquals(
                JournalUploadResult.WRITTEN,
                upload(original.toEntity()),
            )

            val saved = document(owner, original)
                .get(Source.SERVER)
                .await()

            val changedFields =
                requireNotNull(saved.data).toMutableMap().apply {
                    this["notes"] = "Unauthorized change"
                    this["revision"] = 2L
                    this["syncedAt"] = FieldValue.serverTimestamp()
                }

            val otherAccount = createClient()

            assertNotEquals(
                original.ownerId,
                otherAccount.auth.currentUser?.uid,
            )

            val otherRef = document(otherAccount, original)

            expectPermissionDenied {
                otherRef.get(Source.SERVER).await()
            }

            expectPermissionDenied {
                otherRef.set(changedFields).await()
            }

            val signedOut = createClient(signIn = false)
            val signedOutRef = document(signedOut, original)

            expectPermissionDenied {
                signedOutRef.get(Source.SERVER).await()
            }

            expectPermissionDenied {
                signedOutRef.set(changedFields).await()
            }
        }

    private companion object {
        private val SETUP_TIMEOUT = 30.seconds
        private val CLEANUP_TIMEOUT = 5.seconds
        private val TEST_TIMEOUT = 60.seconds

        private const val PENDING_BATCH_SIZE = 50

        private const val INITIAL_TIMESTAMP_MILLIS = 100L
        private const val EDITED_TIMESTAMP_MILLIS = 200L

        private const val EMULATOR_HOST = "127.0.0.1"
        private const val AUTH_EMULATOR_PORT = 9099
        private const val FIRESTORE_EMULATOR_PORT = 8080

        private const val CLOUD_TEST_ARGUMENT = "journalCloudTests"
        private const val DEMO_PROJECT_ID = "demo-journal-sync"

        // Deliberately fake configuration for emulator-only clients.
        private const val TEST_APPLICATION_ID =
            "1:1234567890:android:1234567890abcdef"

        private const val TEST_API_KEY = "fake-api-key"
    }
}
