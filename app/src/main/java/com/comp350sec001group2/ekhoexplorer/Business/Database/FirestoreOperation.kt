package com.comp350sec001group2.ekhoexplorer.Business.Database

import kotlinx.coroutines.flow.Flow

enum class OperationKind {
    QUERY,
    COMMAND,
    STREAM,
    TRANSACTION,
    BATCH
}

abstract class FirestoreOperation {
    open val name: String get() = this::class.java.simpleName.ifEmpty {"AnonymousOperation"}

    abstract val kind: OperationKind
}

abstract class FirestoreAccessor<out T> : FirestoreOperation() {
    override val kind: OperationKind get() = OperationKind.QUERY

    abstract suspend fun run(db: FirestoreContext): T
}

abstract class FirestoreQuery<out T> : FirestoreAccessor<T>()

abstract class FirestoreCommand<out T> : FirestoreAccessor<T>() {
    override val kind: OperationKind get() = OperationKind.COMMAND
}

abstract class FirestoreStream<out T> : FirestoreOperation() {
    override val kind: OperationKind get() = OperationKind.STREAM

    abstract fun observe(db: FirestoreContext): Flow<T>
}
