package com.comp350sec001group2.ekhoexplorer.Business.Database

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await

/** Position in a result set */
class PageCursor internal constructor(internal val snapshot: DocumentSnapshot)

/** Single page of results */
data class Page<out T>(
    val items: List<T>,
    val nextCursor: PageCursor?,
) {
    val hasMore: Boolean get() = nextCursor != null
}

abstract class FirestorePagedQuery<T>(
    private val pageSize: Int,
    private val after: PageCursor?,
) : FirestoreAccessor<Page<T>>() {
    protected abstract val reader: DocumentReader<T>

    protected abstract fun baseQuery(db: FirestoreContext): Query

    final override suspend fun run(db: FirestoreContext): Page<T> {
        if(pageSize !in 1..MAX_PAGE_SIZE) {
            throw FirestoreException(FirestoreErrorKind.INVALID_ARGUMENT, "pageSize must be in 1..$MAX_PAGE_SIZE but was $pageSize")
        }

        var query = baseQuery(db)
        if(after != null) query = query.startAfter(after.snapshot)

        val documents = query.limit(pageSize + 1L).get().await().documents
        val hasMore = documents.size > pageSize
        val pageDocuments = if(hasMore) documents.subList(0, pageSize) else documents

        return Page(
            items = pageDocuments.map { reader.readSafely(it) },
            nextCursor = if(hasMore) PageCursor(pageDocuments.last()) else null
        )
    }

    companion object {
        const val MAX_PAGE_SIZE = 200
    }
}

class Pager<T>(private val loadPage: suspend(PageCursor?) -> Page<T>) {
    private val mutex = Mutex()
    private var cursor: PageCursor? = null

    @Volatile
    private var exhausted = false

    @Volatile
    var items: List<T> = emptyList()
        private set

    val hasMore: Boolean get() = !exhausted

    suspend fun loadNext(): List<T> = mutex.withLock {
        if(exhausted) return emptyList()

        val page = loadPage(cursor)
        cursor = page.nextCursor
        exhausted = page.nextCursor == null
        items = items + page.items
        page.items
    }

    suspend fun reset() = mutex.withLock {
        cursor = null
        exhausted = false
        items = emptyList()
    }
}
