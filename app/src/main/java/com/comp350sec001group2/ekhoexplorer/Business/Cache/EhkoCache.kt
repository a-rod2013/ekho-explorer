package com.comp350sec001group2.ekhoexplorer.Business.Cache

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** App wide, in memory cache for passing shared states. This is only kept in memory, not persisted. */
object EhkoCache {
    private val activeUserState = MutableStateFlow(ActiveUser.SignedOut)

    val activeUserFlow: StateFlow<ActiveUser> = activeUserState.asStateFlow()

    /** The signed-in user, or [ActiveUser.SignedOut] when nobody is. */
    val activeUser: ActiveUser get() = activeUserState.value

    fun setActiveUser(user: ActiveUser) {
        activeUserState.value = user
    }

    fun updateActiveUser(transform: (ActiveUser) -> ActiveUser) {
        activeUserState.update(transform)
    }

    /** Call on sign-out. */
    fun clear() {
        activeUserState.value = ActiveUser.SignedOut
    }
}
