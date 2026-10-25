package com.comp350sec001group2.ekhoexplorer.Business

import android.util.Log
import com.comp350sec001group2.ekhoexplorer.Business.Database.Batches.SeedFeedPostsBatch
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreException
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreService
import com.comp350sec001group2.ekhoexplorer.Business.Database.Repositories.FeedRepository
import com.comp350sec001group2.ekhoexplorer.Business.Database.Repositories.FirestoreFeedRepository
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer {
    private val service = FirestoreService(Firebase.firestore)

    val feedRepository: FeedRepository = FirestoreFeedRepository(
        service = service,
        currentUserId = { "test-user" },       // TEMPORARY until sign-in exists
        currentUsername = { "Test User" },
        currentAvatarUrl = { null },
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

}
