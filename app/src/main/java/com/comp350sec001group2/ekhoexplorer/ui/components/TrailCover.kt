package com.comp350sec001group2.ekhoexplorer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.comp350sec001group2.ekhoexplorer.Business.Trails.NoTrailPhotoLoader
import com.comp350sec001group2.ekhoexplorer.Business.Trails.TrailPhotoLoader
import com.comp350sec001group2.ekhoexplorer.ui.theme.AppStyle
import java.nio.ByteBuffer
import kotlin.coroutines.cancellation.CancellationException

val LocalTrailPhotoLoader = staticCompositionLocalOf<TrailPhotoLoader> { NoTrailPhotoLoader }

internal sealed interface StoredImageState {
    data object Loading : StoredImageState
    class Loaded(val bytes: ByteArray) : StoredImageState
    data object Unavailable : StoredImageState
}

@Composable
fun StoredImage(
    cacheKey: String,
    tag: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    load: suspend () -> ByteArray?,
) {
    var attempt by remember(cacheKey) { mutableIntStateOf(0) }
    val state by produceState<StoredImageState>(StoredImageState.Loading, cacheKey, attempt) {
        value = StoredImageState.Loading
        value = try {
            load()?.let { StoredImageState.Loaded(it) } ?: StoredImageState.Unavailable
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            StoredImageState.Unavailable
        }
    }

    Box(modifier = modifier.background(AppStyle.colors.surface), contentAlignment = Alignment.Center) {
        when (val current = state) {
            StoredImageState.Loading -> CircularProgressIndicator(
                modifier = Modifier.size(28.dp).testTag("$tag-loading"),
                strokeWidth = 2.dp,
                color = AppStyle.colors.accent,
            )

            is StoredImageState.Loaded -> {
                val context = LocalContext.current
                val request = remember(current.bytes) {
                    ImageRequest.Builder(context)
                        .data(ByteBuffer.wrap(current.bytes))
                        .memoryCacheKey(cacheKey)
                        .build()
                }
                AsyncImage(
                    model = request,
                    contentDescription = null,
                    contentScale = contentScale,
                    modifier = Modifier.fillMaxSize().testTag(tag),
                )
            }

            StoredImageState.Unavailable -> Text(
                text = "Picture unavailable. Tap to retry.",
                color = AppStyle.colors.textSecondary,
                style = AppStyle.type.caption,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { attempt++ }
                    .padding(AppStyle.dimens.md)
                    .testTag("$tag-unavailable"),
            )
        }
    }
}

@Composable
fun TrailCover(trailId: String, photoCount: Int, modifier: Modifier = Modifier) {
    if (photoCount <= 0) {
        Box(modifier = modifier.background(AppStyle.colors.surface), contentAlignment = Alignment.Center) {
            Text("No pictures", color = AppStyle.colors.textSecondary, style = AppStyle.type.caption)
        }
        return
    }
    val loader = LocalTrailPhotoLoader.current
    StoredImage(
        cacheKey = "trail:$trailId:0",
        tag = "trail-cover-$trailId",
        modifier = modifier,
        load = { loader.loadTrailPhoto(trailId, 0) },
    )
}
