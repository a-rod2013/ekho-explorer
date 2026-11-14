package com.comp350sec001group2.ekhoexplorer.ui.pages.auth.views

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.comp350sec001group2.ekhoexplorer.ui.pages.auth.AuthMessages
import com.comp350sec001group2.ekhoexplorer.ui.pages.auth.viewmodels.ForgotPasswordUiState
import com.comp350sec001group2.ekhoexplorer.ui.pages.auth.viewmodels.ForgotPasswordViewModel
import com.comp350sec001group2.ekhoexplorer.ui.pages.auth.viewmodels.forgotPasswordViewModelFactory
import com.comp350sec001group2.ekhoexplorer.ui.pages.auth.views.components.AuthButton
import com.comp350sec001group2.ekhoexplorer.ui.pages.auth.views.components.AuthErrorText
import com.comp350sec001group2.ekhoexplorer.ui.pages.auth.views.components.AuthLayout
import com.comp350sec001group2.ekhoexplorer.ui.pages.auth.views.components.AuthNoticeText
import com.comp350sec001group2.ekhoexplorer.ui.pages.auth.views.components.AuthTextField
import com.comp350sec001group2.ekhoexplorer.ui.theme.AppStyle
import com.comp350sec001group2.ekhoexplorer.ui.theme.EkhoExplorerTheme

@Composable
fun ForgotPasswordScreen(
    onBackToLogin: () -> Unit,
    viewModel: ForgotPasswordViewModel = viewModel(factory = forgotPasswordViewModelFactory),
    title: String = "Reset your password",
    subtitle: String = "Enter your email and we will send you a link to choose a new password.",
    backLabel: String = "Back to sign in",
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ForgotPasswordScreenContent(
        state = state,
        onEmailChange = viewModel::onEmailChange,
        onSubmit = viewModel::onSubmit,
        onBackToLogin = onBackToLogin,
        title = title,
        subtitle = subtitle,
        backLabel = backLabel,
    )
}

@Composable
fun ForgotPasswordScreenContent(
    state: ForgotPasswordUiState,
    onEmailChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onBackToLogin: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Reset your password",
    subtitle: String = "Enter your email and we will send you a link to choose a new password.",
    backLabel: String = "Back to sign in",
) {
    val focus = LocalFocusManager.current
    AuthLayout(
        title = title,
        subtitle = subtitle,
        modifier = modifier.testTag("forgot-screen"),
    ) {
        AuthTextField(
            value = state.email,
            onValueChange = onEmailChange,
            label = "Email",
            testTag = "forgot-email",
            problem = state.emailProblem?.let(AuthMessages::email),
            enabled = !state.isLoading,
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Done,
            onImeAction = {
                focus.clearFocus()
                onSubmit()
            },
            contentType = ContentType.EmailAddress,
        )
        AuthErrorText(state.error?.let(AuthMessages::error))
        AuthNoticeText(
            message = if (state.sent) AuthMessages.RESET_SENT else null,
            testTag = "forgot-sent",
        )
        AuthButton(
            text = if (state.sent) "Send again" else "Send reset link",
            onClick = {
                focus.clearFocus()
                onSubmit()
            },
            loading = state.isLoading,
            testTag = "forgot-submit",
        )
        TextButton(
            onClick = onBackToLogin,
            enabled = !state.isLoading,
            modifier = Modifier.testTag("forgot-back"),
        ) {
            Text(backLabel, color = AppStyle.colors.accent, style = AppStyle.type.captionAction)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ForgotPasswordPreview() {
    EkhoExplorerTheme {
        ForgotPasswordScreenContent(ForgotPasswordUiState(email = "jane@example.com", sent = true), {}, {}, {})
    }
}
