package com.comp350sec001group2.ekhoexplorer.ui.map

import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.comp350sec001group2.ekhoexplorer.Business.Photos.PhotoRepository
import androidx.compose.runtime.produceState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ObservationDetails(
    observation: ObservationMarker,
    photoRepository: PhotoRepository,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    // Load the observation photo
    val photo = produceState<Bitmap?>(
        initialValue = null,
        key1 = observation.id,
        key2 = observation.photoUri
    ) {
        value = withContext(Dispatchers.IO) {

            // Try loading the local journal photo first
            val localPhoto = if (observation.photoUri.isNotBlank()) {
                try {
                    context.contentResolver.openInputStream(
                        Uri.parse(observation.photoUri)
                    )?.use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
                } catch (e: Exception) {
                    null
                }
            } else {
                null
            }

            // If there is no local photo, load it from Firestore
            localPhoto ?: try {
                photoRepository.load(observation.id, 0)?.let { bytes ->
                    BitmapFactory.decodeByteArray(
                        bytes,
                        0,
                        bytes.size
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
        }
    }.value

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(observation.displayName)
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Observation photo
                if (photo != null) {
                    Image(
                        bitmap = photo.asImageBitmap(),
                        contentDescription = "Observation photo",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentScale = ContentScale.Crop
                    )
                }

                // Category
                Text("Category: ${observation.category.name}")

                // Scientific name
                if (observation.scientificName.isNotBlank()) {
                    Text("Scientific name: ${observation.scientificName}")
                }

                // Description
                if (observation.description.isNotBlank()) {
                    Text("Description: ${observation.description}")
                }

                // GPS coordinates
                Text(
                    "Location: ${observation.location.latitude}, " +
                        "${observation.location.longitude}"
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
