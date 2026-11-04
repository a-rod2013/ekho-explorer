package com.comp350sec001group2.ekhoexplorer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.comp350sec001group2.ekhoexplorer.Business.Photos.NoPhotoLoader
import com.comp350sec001group2.ekhoexplorer.Business.Photos.PhotoLoader
import com.comp350sec001group2.ekhoexplorer.ui.theme.AppStyle
import java.nio.ByteBuffer
import kotlin.coroutines.cancellation.CancellationException

val LocalPhotoLoader = staticCompositionLocalOf<PhotoLoader> { NoPhotoLoader }

/** What one page of the carousel is showing. */
internal sealed interface PhotoState {
    data object Loading : PhotoState
    class Loaded(val bytes: ByteArray) : PhotoState
    data object Unavailable : PhotoState
}

@Composable
fun PhotoCarousel(postId: String, count: Int, modifier: Modifier = Modifier) {
    val pagerState = rememberPagerState(pageCount = { count })
    Box(modifier = modifier.fillMaxWidth().aspectRatio(1f)) {
        HorizontalPager(
            state = pagerState,
            beyondViewportPageCount = 0,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            StoredPhoto(postId = postId, index = page)
        }
        if (count > 1) {
            PageDots(
                count = count,
                current = pagerState.currentPage,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = AppStyle.dimens.md),
            )
        }
    }
}

@Composable
private fun StoredPhoto(postId: String, index: Int) {
    val loader = LocalPhotoLoader.current
    var attempt by remember(postId, index) { mutableIntStateOf(0) }
    val state by produceState<PhotoState>(PhotoState.Loading, postId, index, loader, attempt) {
        value = PhotoState.Loading
        value = try {
            loader.load(postId, index)?.let { PhotoState.Loaded(it) } ?: PhotoState.Unavailable
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            PhotoState.Unavailable
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(AppStyle.colors.surface),
        contentAlignment = Alignment.Center,
    ) {
        when (val current = state) {
            PhotoState.Loading -> CircularProgressIndicator(
                modifier = Modifier.size(28.dp).testTag("photo-loading-$index"),
                strokeWidth = 2.dp,
                color = AppStyle.colors.accent,
            )

            is PhotoState.Loaded -> {
                val context = LocalContext.current
                val request = remember(current.bytes) {
                    ImageRequest.Builder(context)
                        .data(ByteBuffer.wrap(current.bytes))
                        // Bytes have no identity of their own, so name the entry for the memory cache.
                        .memoryCacheKey("photo:$postId:$index")
                        .build()
                }
                AsyncImage(
                    model = request,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().testTag("photo-$index"),
                )
            }

            PhotoState.Unavailable -> Text(
                text = "Photo unavailable. Tap to retry.",
                color = AppStyle.colors.textSecondary,
                style = AppStyle.type.caption,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { attempt++ }
                    .padding(AppStyle.dimens.xl)
                    .testTag("photo-unavailable-$index"),
            )
        }
    }
}
