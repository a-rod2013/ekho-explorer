package com.comp350sec001group2.ekhoexplorer.ui.pages.auth.views

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.comp350sec001group2.ekhoexplorer.ui.pages.auth.AuthMessages
import com.comp350sec001group2.ekhoexplorer.ui.pages.auth.viewmodels.VerifyEmailUiState
import com.comp350sec001group2.ekhoexplorer.ui.pages.auth.viewmodels.VerifyEmailViewModel
import com.comp350sec001group2.ekhoexplorer.ui.pages.auth.viewmodels.VerifyNotice
import com.comp350sec001group2.ekhoexplorer.ui.pages.auth.viewmodels.verifyEmailViewModelFactory
import com.comp350sec001group2.ekhoexplorer.ui.pages.auth.views.components.AuthBodyText
import com.comp350sec001group2.ekhoexplorer.ui.pages.auth.views.components.AuthButton
import com.comp350sec001group2.ekhoexplorer.ui.pages.auth.views.components.AuthErrorText
import com.comp350sec001group2.ekhoexplorer.ui.pages.auth.views.components.AuthLayout
import com.comp350sec001group2.ekhoexplorer.ui.pages.auth.views.components.AuthNoticeText
import com.comp350sec001group2.ekhoexplorer.ui.pages.auth.views.components.AuthSecondaryButton
import com.comp350sec001group2.ekhoexplorer.ui.theme.AppStyle
import com.comp350sec001group2.ekhoexplorer.ui.theme.EkhoExplorerTheme

@Composable
fun VerifyEmailScreen(viewModel: VerifyEmailViewModel = viewModel(factory = verifyEmailViewModelFactory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    VerifyEmailScreenContent(
        state = state,
        onCheck = viewModel::onCheck,
        onResend = viewModel::onResend,
        onSignOut = viewModel::onSignOut,
    )
}

@Composable
fun VerifyEmailScreenContent(
    state: VerifyEmailUiState,
    onCheck: () -> Unit,
    onResend: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AuthLayout(
        title = "Check your inbox",
        subtitle = "One last step before you start exploring.",
        modifier = modifier.testTag("verify-screen"),
    ) {
        AuthBodyText(
            message = if (state.email.isNotBlank()) {
                "We sent a confirmation link to ${state.email}. Open it, then come back and tap the button below."
            } else {
                "We sent you a confirmation link. Open it, then come back and tap the button below."
            },
            testTag = "verify-body",
        )
        AuthErrorText(state.error?.let(AuthMessages::error))
        AuthNoticeText(
            message = when (state.notice) {
                VerifyNotice.RESENT -> "A new link is on its way. Check your spam folder too."
                VerifyNotice.NOT_YET -> "This address is not confirmed yet. Open the link in the email, then try again."
                null -> null
            },
            testTag = "verify-notice",
        )
        AuthButton(
            text = "I verified my email",
            onClick = onCheck,
            loading = state.isChecking,
            testTag = "verify-check",
        )
        AuthSecondaryButton(
            text = "Resend the email",
            onClick = onResend,
            loading = state.isResending,
            enabled = !state.isChecking,
            testTag = "verify-resend",
        )
        TextButton(onClick = onSignOut, modifier = Modifier.testTag("verify-sign-out")) {
            Text("Use a different account", color = AppStyle.colors.accent, style = AppStyle.type.captionAction)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun VerifyEmailPreview() {
    EkhoExplorerTheme {
        VerifyEmailScreenContent(VerifyEmailUiState(email = "jane@example.com"), {}, {}, {})
    }
}
