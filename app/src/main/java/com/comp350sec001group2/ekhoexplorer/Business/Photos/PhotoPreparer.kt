package com.comp350sec001group2.ekhoexplorer.Business.Photos

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

class PhotoPreparer(
    private val encoder: PhotoEncoder = AndroidPhotoEncoder(),
    private val limits: PhotoLimits = PhotoLimits(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    suspend fun prepare(sources: List<() -> InputStream>): List<EncodedPhoto> {
        if (sources.size > limits.maxPhotos) {
            throw PhotoException(PhotoErrorKind.TOO_MANY_PHOTOS, "A post holds at most ${limits.maxPhotos} photos")
        }
        return withContext(dispatcher) { sources.map { open -> encoder.encode(open()) } }
    }
}

fun PhotoLimits.requireValidForPost(photos: List<EncodedPhoto>) {
    if (photos.size > maxPhotos) {
        throw PhotoException(PhotoErrorKind.TOO_MANY_PHOTOS, "A post holds at most $maxPhotos photos")
    }
    photos.forEach {
        if (it.sizeBytes > hardCapBytes) {
            throw PhotoException(PhotoErrorKind.TOO_DETAILED, "A photo is ${it.sizeBytes} bytes, over the $hardCapBytes byte cap")
        }
    }
}
