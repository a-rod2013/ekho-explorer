package com.comp350sec001group2.ekhoexplorer.Business.Auth

import com.comp350sec001group2.ekhoexplorer.Business.Cache.ActiveUser
interface SessionHooks {
    /** Runs before the app is told it is signed in, so screens never see a half-ready session. */
    suspend fun onSignedIn(user: ActiveUser)

    /** Runs before the account is actually signed out, so nothing keeps reading as the old user. */
    suspend fun onSignedOut()
}
