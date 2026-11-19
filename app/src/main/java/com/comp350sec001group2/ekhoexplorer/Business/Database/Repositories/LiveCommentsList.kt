package com.comp350sec001group2.ekhoexplorer.Business.Database.Repositories

import android.util.Log
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreException
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreErrorKind
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.Comment
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.Vote
import com.comp350sec001group2.ekhoexplorer.Business.Database.Transactions.VoteMath
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

class LiveCommentList(
    private val observe: (limit: Int) -> Flow<List<Comment>>,
    private val loadVotes: suspend (ids: List<String>) -> Map<String, Vote>,
    private val pageSize: Int = 20,
    private val maxWindow: Int = 500,
) {
    private val window = MutableStateFlow(pageSize)
    private val loading = MutableStateFlow(true)
    private val more = MutableStateFlow(false)
    private val failure = MutableStateFlow<FirestoreErrorKind?>(null)

    private val snapshot = MutableStateFlow<Snapshot?>(null)
    private val added = MutableStateFlow<List<Comment>>(emptyList())
    private val votes = MutableStateFlow<Map<String, Vote>>(emptyMap())
    private val overlays = MutableStateFlow<Map<String, Overlay>>(emptyMap())
    private val voteIdsAsked = HashSet<String>()

    @Volatile private var latest: List<Comment> = emptyList()

    private class Snapshot(val window: Int, val items: List<Comment>)

    private class Overlay(val myVote: Vote, val upDelta: Int, val downDelta: Int, val baseUp: Int, val baseDown: Int)

    val isLoading: StateFlow<Boolean> = loading.asStateFlow()

    val hasMore: StateFlow<Boolean> = more.asStateFlow()

    val error: StateFlow<FirestoreErrorKind?> = failure.asStateFlow()

    val comments: Flow<List<Comment>> = channelFlow {
        launch { listen() }
        combine(snapshot, added, votes, overlays) { snap, mine, voted, pending ->
            render(snap, mine, voted, pending)
        }.collect { rendered ->
            latest = rendered
            send(rendered)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun CoroutineScope.listen() {
        failure.value = null
        try {
            window.flatMapLatest { size -> observe(size + 1).map { size to it } }.collect { (size, items) ->
                snapshot.value = Snapshot(size, items.take(size))
                more.value = items.size > size
                if (size == window.value) loading.value = false
                fetchVotes(items.take(size))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: FirestoreException) {
            Log.w(TAG, "Comment listener stopped: ${e.kind}", e)
            failure.value = e.kind
            loading.value = false
        }
    }

    private fun CoroutineScope.fetchVotes(items: List<Comment>) {
        val wanted = synchronized(voteIdsAsked) { items.map { it.id }.filter { voteIdsAsked.add(it) } }
        if (wanted.isEmpty()) return
        launch {
            wanted.chunked(VOTE_CHUNK).forEach { chunk ->
                try {
                    val found = loadVotes(chunk)
                    votes.update { current -> current + chunk.associateWith { found[it] ?: Vote.NONE } }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: FirestoreException) {
                    Log.w(TAG, "Could not load comment votes: ${e.kind}", e)
                    synchronized(voteIdsAsked) { voteIdsAsked.removeAll(chunk.toSet()) }   // ask again next time
                }
            }
        }
    }

    private fun render(
        snap: Snapshot?,
        mine: List<Comment>,
        voted: Map<String, Vote>,
        pending: Map<String, Overlay>,
    ): List<Comment> {
        val live = snap?.items.orEmpty()
        val liveIds = live.mapTo(HashSet()) { it.id }
        val merged = live + mine.filter { it.id !in liveIds }
        return merged.map { c ->
            val overlay = pending[c.id]
            when {
                overlay == null -> c.copy(myVote = voted[c.id] ?: Vote.NONE)
                // The database count has not moved yet: show my vote on top of it.
                c.upCount == overlay.baseUp && c.downCount == overlay.baseDown -> c.copy(
                    upCount = maxOf(c.upCount + overlay.upDelta, 0),
                    downCount = maxOf(c.downCount + overlay.downDelta, 0),
                    myVote = overlay.myVote,
                )
                // The database count moved (it includes my vote now): trust it.
                else -> c.copy(myVote = overlay.myVote)
            }
        }
    }

    fun showMore() {
        if (!more.value || window.value >= maxWindow) return
        loading.value = true
        window.value = minOf(window.value + pageSize, maxWindow)
    }

    fun addLocal(comment: Comment) = added.update { it + comment }

    fun current(commentId: String): Comment? = latest.firstOrNull { it.id == commentId }

    fun beginVote(shown: Comment, tapped: Vote) {
        val next = VoteMath.next(shown.myVote, tapped)
        val (upDelta, downDelta) = VoteMath.deltas(shown.myVote, next)
        val base = snapshot.value?.items?.firstOrNull { it.id == shown.id }
            ?: added.value.firstOrNull { it.id == shown.id }
            ?: shown
        overlays.update { it + (shown.id to Overlay(next, upDelta, downDelta, base.upCount, base.downCount)) }
    }

    fun confirmVote(commentId: String, result: Vote) {
        votes.update { it + (commentId to result) }
        overlays.update { current ->
            val overlay = current[commentId] ?: return@update current
            current + (commentId to Overlay(result, overlay.upDelta, overlay.downDelta, overlay.baseUp, overlay.baseDown))
        }
    }

    fun cancelVote(commentId: String) = overlays.update { it - commentId }

    companion object {
        const val VOTE_CHUNK = 30
        private const val TAG = "LiveCommentList"
    }
}
