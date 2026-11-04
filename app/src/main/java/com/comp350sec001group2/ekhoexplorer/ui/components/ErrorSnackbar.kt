package com.comp350sec001group2.ekhoexplorer.ui.components

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember

private const val DEFAULT_ERROR_MESSAGE = "Something went wrong. Please try again."

/**
 * A snackbar host that shows [message] whenever [error] becomes non-null or changes.
 * Pass the result to `Scaffold(snackbarHost = { SnackbarHost(state) })`.
 */
@Composable
fun rememberErrorSnackbarState(error: Any?, message: String = DEFAULT_ERROR_MESSAGE): SnackbarHostState {
    val state = remember { SnackbarHostState() }
    LaunchedEffect(error) {
        if (error != null) state.showSnackbar(message)
    }
    return state
}
