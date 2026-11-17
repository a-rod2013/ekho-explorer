package com.comp350sec001group2.ekhoexplorer.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.comp350sec001group2.ekhoexplorer.ui.theme.AppStyle
import java.io.File

@Composable
fun CameraActionButton(
    description: String,
    onPhotoCaptured: (Uri) -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current

    var pendingUri by rememberSaveable {
        mutableStateOf<String?>(null)
    }
    var pendingPath by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingUri

        if (success && uri != null) {
            onPhotoCaptured(Uri.parse(uri))
        } else {
            pendingPath?.let { File(it).delete() }
        }

        pendingUri = null
        pendingPath = null
    }

    FloatingActionButton(
        containerColor = AppStyle.colors.accent,
        contentColor = AppStyle.colors.onAccent,
        onClick = {
            try {
                val directory = File(context.filesDir, "observations")
                check(directory.isDirectory || directory.mkdirs())

                val file = File.createTempFile(
                    "action_photo_",
                    ".jpg",
                    directory
                )

                pendingPath = file.absolutePath

                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )

                pendingUri = uri.toString()
                launcher.launch(uri)
            } catch (e: Exception) {
                pendingPath?.let { File(it).delete() }
                pendingUri = null
                pendingPath = null
                onError("Could not open the camera.")
            }
        }
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = description
        )
    }
}
