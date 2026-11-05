package com.comp350sec001group2.ekhoexplorer.ui.pages.auth.views


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.comp350sec001group2.ekhoexplorer.ui.pages.auth.AuthMessages
import com.comp350sec001group2.ekhoexplorer.ui.pages.auth.viewmodels.SignUpUiState
import com.comp350sec001group2.ekhoexplorer.ui.pages.auth.viewmodels.SignUpViewModel
import com.comp350sec001group2.ekhoexplorer.ui.pages.auth.viewmodels.signUpViewModelFactory
import com.comp350sec001group2.ekhoexplorer.ui.pages.auth.views.components.AuthButton
import com.comp350sec001group2.ekhoexplorer.ui.pages.auth.views.components.AuthErrorText
import com.comp350sec001group2.ekhoexplorer.ui.pages.auth.views.components.AuthLayout
import com.comp350sec001group2.ekhoexplorer.ui.pages.auth.views.components.AuthTextField
import com.comp350sec001group2.ekhoexplorer.ui.pages.auth.views.components.PasswordField
import com.comp350sec001group2.ekhoexplorer.ui.theme.AppStyle
import com.comp350sec001group2.ekhoexplorer.ui.theme.EkhoExplorerTheme

@Composable
fun SignUpScreen(
    onBackToLogin: () -> Unit,
    viewModel: SignUpViewModel = viewModel(factory = signUpViewModelFactory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SignUpScreenContent(
        state = state,
        onUsernameChange = viewModel::onUsernameChange,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onConfirmPasswordChange = viewModel::onConfirmPasswordChange,
        onSubmit = viewModel::onSubmit,
        onBackToLogin = onBackToLogin,
    )
}

@Composable
fun SignUpScreenContent(
    state: SignUpUiState,
    onUsernameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onBackToLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focus = LocalFocusManager.current
    val next = { focus.moveFocus(FocusDirection.Down); Unit }
    AuthLayout(
        title = "Create your account",
        subtitle = "Pick a username other explorers will see on your posts.",
        modifier = modifier.testTag("signup-screen"),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(AppStyle.dimens.sm),
        ) {
            AuthTextField(
                value = state.username,
                onValueChange = onUsernameChange,
                label = "Username",
                testTag = "signup-username",
                problem = state.usernameProblem?.let(AuthMessages::username),
                enabled = !state.isLoading,
                onImeAction = next,
                contentType = ContentType.NewUsername,
            )
            AuthTextField(
                value = state.email,
                onValueChange = onEmailChange,
                label = "Email",
                testTag = "signup-email",
                problem = state.emailProblem?.let(AuthMessages::email),
                enabled = !state.isLoading,
                keyboardType = KeyboardType.Email,
                onImeAction = next,
                contentType = ContentType.EmailAddress,
            )
            PasswordField(
                value = state.password,
                onValueChange = onPasswordChange,
                label = "Password",
                testTag = "signup-password",
                problem = state.passwordProblem?.let(AuthMessages::newPassword),
                enabled = !state.isLoading,
                onImeAction = next,
                isNewPassword = true,
            )
            PasswordField(
                value = state.confirmPassword,
                onValueChange = onConfirmPasswordChange,
                label = "Confirm password",
                testTag = "signup-confirm",
                problem = state.confirmProblem?.let(AuthMessages::confirmPassword),
                enabled = !state.isLoading,
                imeAction = ImeAction.Done,
                onImeAction = {
                    focus.clearFocus()
                    onSubmit()
                },
                isNewPassword = true,
            )
        }
        AuthErrorText(state.error?.let(AuthMessages::error))
        AuthButton(
            text = "Create account",
            onClick = {
                focus.clearFocus()
                onSubmit()
            },
            loading = state.isLoading,
            testTag = "signup-submit",
        )
        TextButton(
            onClick = onBackToLogin,
            enabled = !state.isLoading,
            modifier = Modifier.testTag("signup-back-to-login"),
        ) {
            Text("Already have an account? Sign in", color = AppStyle.colors.accent, style = AppStyle.type.captionAction)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SignUpPreview() {
    EkhoExplorerTheme {
        SignUpScreenContent(SignUpUiState(username = "jane", email = "jane@example.com"), {}, {}, {}, {}, {}, {})
    }
}
