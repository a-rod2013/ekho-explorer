package com.fsql.data

import android.content.Context
import android.util.Log
import com.fsql.data.auth.AuthService
import com.fsql.data.auth.FirebaseAuthService
import com.fsql.data.internal.FirebaseServiceImpl
import com.fsql.data.internal.FirestoreExecutor
import com.fsql.data.internal.PlanLoader
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async

/**
 * @property defaultTimeoutMs how long a query may take before it fails with [DbError.Timeout]
 * @property emulatorHost set (for example "10.0.2.2" from an Android emulator) to use the local
 *   Firebase Emulator Suite instead of the real project. Leave null in production.
 */
data class FirebaseDataConfig(
    val defaultTimeoutMs: Long = 10_000,
    val emulatorHost: String? = null,
    val firestorePort: Int = 8080,
    val authPort: Int = 9099,
)

/**
 * Entry point. Call [init] once from `Application.onCreate`, then use [service] anywhere.
 *
 * Stored procedures are compiled at build time by the `com.fsql.compile` Gradle plugin into one
 * `assets/fsql/plans.json` file, which this loads once, in the background. There is no SQL parsing
 * at runtime: a broken procedure fails the Android build, not a call at runtime.
 */
object FirebaseData {
    private const val TAG = "FirebaseData"

    private val lock = Any()

    @Volatile
    private var serviceInstance: FirebaseService? = null

    @Volatile
    private var authInstance: AuthService? = null

    /** Safe to call more than once; later calls do nothing. */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun init(context: Context, config: FirebaseDataConfig = FirebaseDataConfig()) {
        synchronized(lock) {
            if (serviceInstance != null) return
            val app = context.applicationContext
            FirebaseApp.initializeApp(app)

            val db = FirebaseFirestore.getInstance()
            val auth = FirebaseAuth.getInstance()
            config.emulatorHost?.let { host ->
                db.useEmulator(host, config.firestorePort)
                db.firestoreSettings = FirebaseFirestoreSettings.Builder(db.firestoreSettings)
                    .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build())
                    .build()
                auth.useEmulator(host, config.authPort)
            }

            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            val plans = scope.async { PlanLoader.load(app.assets) }
            plans.invokeOnCompletion { error ->
                if (error != null) {
                    Log.e(TAG, "Could not load the compiled stored procedures", error)
                } else {
                    Log.i(TAG, "Loaded ${plans.getCompleted().size} stored procedure(s)")
                }
            }

            serviceInstance = FirebaseServiceImpl(plans, FirestoreExecutor(db), { db }, config.defaultTimeoutMs)
            authInstance = FirebaseAuthService(auth, config.defaultTimeoutMs)
        }
    }

    /** @throws IllegalStateException if [init] has not been called */
    val service: FirebaseService
        get() = serviceInstance ?: error("FirebaseData.init(context) must be called before using FirebaseData.service.")

    /** @throws IllegalStateException if [init] has not been called */
    val auth: AuthService
        get() = authInstance ?: error("FirebaseData.init(context) must be called before using FirebaseData.auth.")
}
