package com.comp350sec001group2.ekhoexplorer.Business

import android.content.Context
import android.util.Log
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
import com.comp350sec001group2.ekhoexplorer.Business.Notifications.NotificationService
import com.comp350sec001group2.ekhoexplorer.Business.Settings.SettingsRepository
import com.comp350sec001group2.ekhoexplorer.Business.Settings.SharedPrefsSettingsRepository
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(context: Context) {
    private val service = FirestoreService(Firebase.firestore)
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val settingsRepository: SettingsRepository = SharedPrefsSettingsRepository(context)

    /** See NotificationService. */
    val notificationService = NotificationService(service)

    val notificationRepository: NotificationRepository = FirestoreNotificationRepository(service, appScope)

    val feedRepository: FeedRepository = FirestoreFeedRepository(service, notificationService)

    val userRelationsRepository: UserRelationsRepository = FirestoreUserRelationsRepository(service)

    /** Comments are per post, so each post screen asks for its own repository. */
    fun commentRepositoryFor(postId: String): CommentRepository =
        FirestoreCommentRepository(
            service,
            postId,
            notificationService,
            onCommentAdded = { feedRepository.onCommentAdded(postId) },
        )

    init {
        // TEMPORARY until sign-in exists. Sign-in should call EhkoCache.setActiveUser(...), then
        // settingsRepository.load() and userRelationsRepository.load(), instead.
        EhkoCache.setActiveUser(ActiveUser(userId = TEST_USER_ID, userName = "Test User"))
        settingsRepository.load()
        appScope.launch {
            try {
                userRelationsRepository.load()
            } catch (e: FirestoreException) {
                Log.w("AppContainer", "Could not load blocked users: ${e.kind}", e)
            }
        }
    }

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

}
