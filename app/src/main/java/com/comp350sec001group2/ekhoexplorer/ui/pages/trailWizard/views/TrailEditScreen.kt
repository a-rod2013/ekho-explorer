package com.comp350sec001group2.ekhoexplorer.ui.pages.trailWizard.views

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreException
import com.comp350sec001group2.ekhoexplorer.Business.Location.AndroidLocationAccessReader
import com.comp350sec001group2.ekhoexplorer.Business.Trails.Trail
import com.comp350sec001group2.ekhoexplorer.EkhoApp
import com.comp350sec001group2.ekhoexplorer.ui.pages.trailWizard.viewmodels.TrailWizardViewModel
import com.comp350sec001group2.ekhoexplorer.ui.theme.AppStyle
import kotlin.coroutines.cancellation.CancellationException

@Composable
fun TrailEditScreen(trailId: String, onSaved: () -> Unit, modifier: Modifier = Modifier) {
    val app = LocalContext.current.applicationContext as EkhoApp
    val repository = app.container.trailRepository

    val trail: Trail? by produceState<Trail?>(null, trailId) {
        value = repository.trails.value.firstOrNull { it.id == trailId }
            ?: try {
                repository.get(trailId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: FirestoreException) {
                null
            }
    }

    val loaded = trail
    if (loaded == null) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val viewModel: TrailWizardViewModel = viewModel(
        key = "edit-$trailId",
        factory = viewModelFactory {
            initializer {
                TrailWizardViewModel(
                    repository,
                    app.container.photoPreparer,
                    app.contentResolver,
                    walk = null,
                    access = AndroidLocationAccessReader(app),
                    editing = loaded,
                )
            }
        },
    )
    TrailWizardScreen(onSaved = { onSaved() }, modifier = modifier, viewModel = viewModel, map = {}, walkDriver = { _, _, _ -> })
}
