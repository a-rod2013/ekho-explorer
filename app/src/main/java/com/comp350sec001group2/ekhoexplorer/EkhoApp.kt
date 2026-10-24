package com.comp350sec001group2.ekhoexplorer

import android.app.Application
import com.comp350sec001group2.ekhoexplorer.Business.AppContainer

/** DEV ONLY - one time use to seed sample posts, do not enable */
private const val SEED_SAMPLE_POSTS = false

class EkhoApp : Application() {
    val container by lazy { AppContainer() }

    override fun onCreate() {
        super.onCreate()

        if(SEED_SAMPLE_POSTS) container.seedSamplePosts()
    }
}
