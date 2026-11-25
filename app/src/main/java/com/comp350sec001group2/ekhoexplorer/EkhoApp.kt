package com.comp350sec001group2.ekhoexplorer

import android.app.Application
import com.comp350sec001group2.ekhoexplorer.Business.AppContainer
import com.comp350sec001group2.ekhoexplorer.Business.Cache.ByteCache
import com.comp350sec001group2.ekhoexplorer.Business.Cache.DiskLruByteCache
import com.comp350sec001group2.ekhoexplorer.Business.Photos.DiskPhotoCache
import java.io.File
import android.util.Log
import com.comp350sec001group2.ekhoexplorer.Business.Cache.PendingCacheClear
import com.comp350sec001group2.ekhoexplorer.Business.Cache.SharedPrefsFlagStore
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.firestore

/** DEV ONLY - one time use to seed sample posts, do not enable */
private const val SEED_SAMPLE_POSTS = false

private const val FIRESTORE_CACHE_BYTES = 150L * 1024 * 1024

class EkhoApp : Application() {
    val byteCache: ByteCache by lazy { DiskLruByteCache(File(cacheDir, "ekho_bytes"), 150L * 1024 * 1024) }

    val container by lazy { AppContainer(this, photoCache = DiskPhotoCache(byteCache), byteCache = byteCache) }

    override fun onCreate() {
        super.onCreate()

        val firestore = Firebase.firestore
        firestore.firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(PersistentCacheSettings.newBuilder().setSizeBytes(FIRESTORE_CACHE_BYTES).build())
            .build()
        if (PendingCacheClear(SharedPrefsFlagStore(this)).consume()) {
            firestore.clearPersistence().addOnFailureListener { Log.w("EkhoApp", "Could not clear the Firestore cache", it) }
        }


        container.startJournalSync()

        if(SEED_SAMPLE_POSTS) {
            container.seedSamplePosts()
        }
    }
}
