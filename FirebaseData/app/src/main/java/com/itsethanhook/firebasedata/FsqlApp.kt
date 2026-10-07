package com.itsethanhook.firebasedata

import android.app.Application
import com.fsql.data.FirebaseData
import com.fsql.data.FirebaseDataConfig

class FsqlApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Uses the real Firebase project from google-services.json.
        // For the local emulator instead, use:
        //   FirebaseData.init(this, FirebaseDataConfig(emulatorHost = "10.0.2.2"))
        FirebaseData.init(this, FirebaseDataConfig())
    }
}