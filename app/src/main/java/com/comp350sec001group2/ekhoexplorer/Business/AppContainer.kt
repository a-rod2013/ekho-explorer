package com.comp350sec001group2.ekhoexplorer.Business

import android.content.Context
import android.util.Log

import com.comp350sec001group2.ekhoexplorer.Business.Database.Commands.UpdateUserAvatarCommand
import com.comp350sec001group2.ekhoexplorer.Business.Database.Queries.GetUserAvatarQuery
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.UserAvatar
import com.comp350sec001group2.ekhoexplorer.Business.Photos.EncodedPhoto
import com.comp350sec001group2.ekhoexplorer.Business.Auth.AuthRepository
import com.comp350sec001group2.ekhoexplorer.Business.Auth.FirebaseAuthBackend
import com.comp350sec001group2.ekhoexplorer.Business.Auth.FirebaseAuthRepository
import com.comp350sec001group2.ekhoexplorer.Business.Auth.FirestoreProfileStore
import com.comp350sec001group2.ekhoexplorer.Business.Auth.SessionHooks
import com.comp350sec001group2.ekhoexplorer.Business.Cache.ActiveUser
import com.comp350sec001group2.ekhoexplorer.Business.Cache.EhkoCache
import com.comp350sec001group2.ekhoexplorer.Business.Database.Batches.SeedFeedPostsBatch
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreException
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreService
import com.comp350sec001group2.ekhoexplorer.Business.Database.Repositories.CommentRepository
import com.comp350sec001group2.ekhoexplorer.Business.Database.Repositories.FeedRepository
import com.comp350sec001group2.ekhoexplorer.Business.Database.Repositories.FirestoreCommentRepository
import com.comp350sec001group2.ekhoexplorer.Business.Database.Repositories.FirestoreFeedRepository
import com.comp350sec001group2.ekhoexplorer.Business.Database.Repositories.FirestoreNotificationRepository
import com.comp350sec001group2.ekhoexplorer.Business.Database.Repositories.FirestoreUserRelationsRepository
import com.comp350sec001group2.ekhoexplorer.Business.Database.Repositories.NotificationRepository
import com.comp350sec001group2.ekhoexplorer.Business.Database.Repositories.UserRelationsRepository
import com.comp350sec001group2.ekhoexplorer.Business.Journal.JournalCaptureStoring
import com.comp350sec001group2.ekhoexplorer.Business.Journal.JournalDatabase
import com.comp350sec001group2.ekhoexplorer.Business.Journal.JournalIdentity
import com.comp350sec001group2.ekhoexplorer.Business.Journal.JournalPhotoStorage
import com.comp350sec001group2.ekhoexplorer.Business.Journal.JournalRepository
import com.comp350sec001group2.ekhoexplorer.Business.Journal.LocalJournalRepository
import com.comp350sec001group2.ekhoexplorer.Business.Journal.JournalSyncCoordinator
import com.comp350sec001group2.ekhoexplorer.Business.Journal.JournalSyncScheduler
import com.comp350sec001group2.ekhoexplorer.Business.Notifications.NotificationService
import com.comp350sec001group2.ekhoexplorer.Business.Photos.CachedPhotoRepository
import com.comp350sec001group2.ekhoexplorer.Business.Photos.FirestorePhotoStore
import com.comp350sec001group2.ekhoexplorer.Business.Photos.NoPhotoCache
import com.comp350sec001group2.ekhoexplorer.Business.Photos.PhotoCache
import com.comp350sec001group2.ekhoexplorer.Business.Photos.PhotoPreparer
import com.comp350sec001group2.ekhoexplorer.Business.Photos.PhotoRepository
import com.comp350sec001group2.ekhoexplorer.Business.Settings.SettingsRepository
import com.comp350sec001group2.ekhoexplorer.Business.Settings.SharedPrefsSettingsRepository
import com.comp350sec001group2.ekhoexplorer.Business.Database.Queries.GetUserProfileQuery
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.UserProfile
import com.comp350sec001group2.ekhoexplorer.Business.Database.Commands.UpdateUserBioCommand
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(context: Context, photoCache: PhotoCache = NoPhotoCache) {
    private val service = FirestoreService(Firebase.firestore)
    suspend fun getUserProfile(userId: String): UserProfile? {
        return service.execute(GetUserProfileQuery(userId))
    }

    suspend fun updateUserBio(userId: String, bio: String) {
        service.execute(UpdateUserBioCommand(userId, bio))
    }

    suspend fun getUserAvatar(userId: String): UserAvatar? {
        return service.execute(GetUserAvatarQuery(userId))
    }

    suspend fun updateUserAvatar(userId: String, photo: EncodedPhoto) {
        service.execute(UpdateUserAvatarCommand(userId, photo))
    }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val journalDatabase = JournalDatabase.create(context)

    val journalPhotoStorage = JournalPhotoStorage(context)

    val journalIdentity = JournalIdentity()

    val journalSyncScheduler = JournalSyncScheduler(context)

    val journalSyncCoordinator = JournalSyncCoordinator(
        dao = journalDatabase.journalDraftDao(),
        service = service,
        identity = journalIdentity,
    )

    val journalRepository: JournalRepository =
        LocalJournalRepository(
            journalDatabase.journalDraftDao(),
            onDraftChanged = { ownerId ->
                if (journalIdentity.isCurrentOwner(ownerId)) {
                    journalSyncScheduler.requestSync(ownerId)
                }
            },
        )

    val journalCaptureStoring = JournalCaptureStoring(
        repository = journalRepository,
        photoStorage = journalPhotoStorage,
        scope = appScope,
        ownerProvider = {
            val fireBaseOwner = journalIdentity.currentOwnerId()
            val activeUser = EhkoCache.activeUser

            fireBaseOwner?. takeIf {
                activeUser.isSignedIn && activeUser.userId == it
            }
        }
    )

    val settingsRepository: SettingsRepository = SharedPrefsSettingsRepository(context)

    /** See NotificationService. */
    val notificationService = NotificationService(service)

    val notificationRepository: NotificationRepository = FirestoreNotificationRepository(service, appScope)

    val photoRepository: PhotoRepository = CachedPhotoRepository(FirestorePhotoStore(service), photoCache)

    val photoPreparer = PhotoPreparer()

    val feedRepository: FeedRepository = FirestoreFeedRepository(service, notificationService, photoRepository)

    val userRelationsRepository: UserRelationsRepository = FirestoreUserRelationsRepository(service)

    /** Comments are per post, so each post screen asks for its own repository. */
    fun commentRepositoryFor(postId: String): CommentRepository =
        FirestoreCommentRepository(
            service,
            postId,
            notificationService,
            onCommentAdded = { feedRepository.onCommentAdded(postId) },
        )

    val authRepository: AuthRepository = FirebaseAuthRepository(
        backend = FirebaseAuthBackend(),
        profiles = FirestoreProfileStore(service),
        hooks = object : SessionHooks {
            override suspend fun onSignedIn(user: ActiveUser) {
                EhkoCache.setActiveUser(user)
                settingsRepository.load()
                // The blocked list fills in a moment later; the feed re-filters when it arrives.
                appScope.launch {
                    try {
                        userRelationsRepository.load()
                    } catch (e: FirestoreException) {
                        Log.w("AppContainer", "Could not load blocked users: ${e.kind}", e)
                    }
                }
            }

            override suspend fun onSignedOut() {
                feedRepository.clear()
                EhkoCache.clear()
            }
        },
        scope = appScope,
    )

    /** DEV ONLY - writes sample posts to db */
    fun seedSamplePosts() {
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                service.execute(SeedFeedPostsBatch(userId = TEST_USER_ID))
                Log.i("Seed", "Sample posts written. Set SEED_SAMPLE_POSTS to false and relaunch.")
            } catch(e: FirestoreException) {
                Log.w("Seed", "Seeding failed: ${e.kind}", e)
            }
        }
    }

    private companion object{
        const val TEST_USER_ID = "test-user"
    }
    private var previousJournalOwner: String? = null

    private val journalAuthListener =
        FirebaseAuth.AuthStateListener { auth ->
            val ownerId = auth.currentUser?.uid
            val previous = previousJournalOwner

            if (previous != null && previous != ownerId) {
                journalSyncScheduler.cancel(previous)
            }

            previousJournalOwner = ownerId

            if (ownerId != null) {
                journalSyncScheduler.ensurePeriodicSync(ownerId)
                journalSyncScheduler.requestSync(ownerId)
            }
        }

    private var journalSyncStarted = false

    fun startJournalSync() {
        if (journalSyncStarted) return

        journalSyncStarted = true
        FirebaseAuth.getInstance()
            .addAuthStateListener(journalAuthListener)
    }
}
