package com.comp350sec001group2.ekhoexplorer.Business.Database

import android.util.Log

interface FirestoreDiagnostics {
    fun onSuccess(operation: String, kind: OperationKind, elapsedMillis: Long) {}
    fun onFailure(operation: String, kind: OperationKind, elapsedMillis: Long, error: FirestoreError) {}

    object None : FirestoreDiagnostics
}

class LogcatDiagnostics(
    private val enabled: Boolean,
    private val tag: String = "Firestore",
    private val logSuccess: Boolean = true,
) : FirestoreDiagnostics {
    override fun onSuccess(operation: String, kind: OperationKind, ellapsedMillis: Long) {
        if(enabled && logSuccess) Log.d(tag, "$kind $operation okk in ${ellapsedMillis}ms")
    }

    override fun onFailure(operation: String, kind: OperationKind, elapsedMillis: Long, error: FirestoreError) {
        if (enabled) Log.w(tag, "$kind $operation failed (${error.kind}) after ${elapsedMillis}ms: ${error.detail}", error.cause)
    }
}
