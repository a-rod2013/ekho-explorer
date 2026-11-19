package com.comp350sec001group2.ekhoexplorer.ui.pages.profilePage.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.comp350sec001group2.ekhoexplorer.Business.Auth.AuthValidator
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.SupportRequestStatus
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.UsernameChangeRequest
import com.comp350sec001group2.ekhoexplorer.ui.pages.auth.views.components.AuthButton
import com.comp350sec001group2.ekhoexplorer.ui.pages.profilePage.ProfileMessages
import com.comp350sec001group2.ekhoexplorer.ui.pages.profilePage.viewmodels.UsernameChangeUiState
import com.comp350sec001group2.ekhoexplorer.ui.pages.profilePage.viewmodels.UsernameChangeViewModel
import com.comp350sec001group2.ekhoexplorer.ui.pages.profilePage.viewmodels.usernameChangeViewModelFactory
import com.comp350sec001group2.ekhoexplorer.ui.theme.AppStyle
import com.comp350sec001group2.ekhoexplorer.ui.theme.EkhoExplorerTheme

@Composable
fun UsernameChangeScreen(
    onBack: () -> Unit,
    viewModel: UsernameChangeViewModel = viewModel(factory = usernameChangeViewModelFactory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    UsernameChangeScreenContent(
        state = state,
        onRequestedChange = viewModel::onRequestedChange,
        onReasonChange = viewModel::onReasonChange,
        onSubmit = viewModel::submit,
        onRetry = viewModel::load,
        onBack = onBack,
    )
}

@Composable
fun UsernameChangeScreenContent(
    state: UsernameChangeUiState,
    onRequestedChange: (String) -> Unit,
    onReasonChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = AppStyle.dimens
    val focus = LocalFocusManager.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppStyle.colors.background)
            .testTag("username-change-screen"),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = d.authMaxWidth)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = d.xl, vertical = d.lg),
            verticalArrangement = Arrangement.spacedBy(d.md),
        ) {
            Text(
                text = "Request a username change",
                style = MaterialTheme.typography.headlineSmall,
                color = AppStyle.colors.textPrimary,
            )
            Text(
                text = "A person on the team reviews each request. Your username stays the same until it is approved.",
                style = AppStyle.type.caption,
                color = AppStyle.colors.textSecondary,
            )

            when {
                state.isLoading -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AppStyle.colors.accent)
                }
                state.currentUsername.isEmpty() -> {
                    ErrorLine(state.error?.let(ProfileMessages::error) ?: "Could not load your profile.")
                    AuthButton(text = "Try again", onClick = onRetry, loading = false, testTag = "username-retry")
                }
                else -> {
                    Text(
                        text = "Current username: ${state.currentUsername}",
                        style = AppStyle.type.username,
                        color = AppStyle.colors.textPrimary,
                        modifier = Modifier.testTag("username-current"),
                    )
                    if (state.submitted) {
                        StatusCard(
                            "Request sent",
                            "We will review it soon. You can check back on this page for the decision.",
                            "username-submitted",
                        )
                    } else {
                        state.existing?.let { StatusCard(it) }
                    }
                    if (state.canShowForm) {
                        OutlinedTextField(
                            value = state.requested,
                            onValueChange = onRequestedChange,
                            label = { Text("New username") },
                            singleLine = true,
                            enabled = !state.isSubmitting,
                            isError = state.requestedProblem != null,
                            supportingText = state.requestedProblem?.let { problem ->
                                { Text(ProfileMessages.requestedUsername(problem)) }
                            },
                            colors = fieldColors(),
                            modifier = Modifier.fillMaxWidth().testTag("username-requested"),
                        )
                        OutlinedTextField(
                            value = state.reason,
                            onValueChange = onReasonChange,
                            label = { Text("Reason (optional)") },
                            minLines = 2,
                            maxLines = 4,
                            enabled = !state.isSubmitting,
                            isError = state.reasonProblem != null,
                            supportingText = {
                                Text(
                                    state.reasonProblem?.let(ProfileMessages::reason)
                                        ?: "${state.reason.trim().length}/${AuthValidator.MAX_REASON_LENGTH}",
                                )
                            },
                            colors = fieldColors(),
                            modifier = Modifier.fillMaxWidth().testTag("username-reason"),
                        )
                        state.error?.let { ErrorLine(ProfileMessages.error(it)) }
                        AuthButton(
                            text = "Send request",
                            onClick = {
                                focus.clearFocus()
                                onSubmit()
                            },
                            loading = state.isSubmitting,
                            testTag = "username-submit",
                        )
                    }
                }
            }
            TextButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterHorizontally).testTag("username-back")) {
                Text("Back to profile", color = AppStyle.colors.accent, style = AppStyle.type.captionAction)
            }
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AppStyle.colors.accent,
    focusedLabelColor = AppStyle.colors.accent,
    cursorColor = AppStyle.colors.accent,
)

@Composable
private fun ErrorLine(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.error,
        style = AppStyle.type.caption,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().testTag("username-error"),
    )
}

@Composable
private fun StatusCard(request: UsernameChangeRequest) {
    when (request.status) {
        SupportRequestStatus.PENDING -> StatusCard(
            "Waiting for review",
            "You asked for \"${request.requestedUsername}\". We will get to it soon.",
            "username-pending",
        )
        SupportRequestStatus.APPROVED -> StatusCard(
            "Approved",
            "\"${request.requestedUsername}\" was approved. It shows after you sign in again. You can send a new request below.",
            "username-approved",
        )
        SupportRequestStatus.REJECTED -> StatusCard(
            "Not approved",
            "\"${request.requestedUsername}\" was not approved." +
                (request.adminNote?.takeIf { it.isNotBlank() }?.let { " Note from the team: $it" } ?: "") +
                " You can send a new request below.",
            "username-rejected",
        )
    }
}

@Composable
private fun StatusCard(title: String, body: String, tag: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppStyle.colors.surface, RoundedCornerShape(AppStyle.dimens.radiusMd))
            .padding(AppStyle.dimens.md)
            .semantics(mergeDescendants = true) {}
            .testTag(tag),
        verticalArrangement = Arrangement.spacedBy(AppStyle.dimens.xs),
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = AppStyle.colors.textPrimary)
        Text(body, style = AppStyle.type.caption, color = AppStyle.colors.textSecondary)
    }
}

@Preview(showBackground = true)
@Composable
private fun UsernameChangePreview() {
    EkhoExplorerTheme {
        UsernameChangeScreenContent(
            UsernameChangeUiState(isLoading = false, currentUsername = "jane"),
            {}, {}, {}, {}, {},
        )
    }
}
