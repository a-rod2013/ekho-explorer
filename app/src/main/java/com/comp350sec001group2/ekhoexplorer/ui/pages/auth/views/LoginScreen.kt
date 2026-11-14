package com.comp350sec001group2.ekhoexplorer.ui.pages.auth.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
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
import com.comp350sec001group2.ekhoexplorer.Business.Auth.AuthErrorKind
import com.comp350sec001group2.ekhoexplorer.Business.Auth.InputProblem
import com.comp350sec001group2.ekhoexplorer.ui.pages.auth.AuthMessages
import com.comp350sec001group2.ekhoexplorer.ui.pages.auth.viewmodels.LoginUiState
import com.comp350sec001group2.ekhoexplorer.ui.pages.auth.viewmodels.LoginViewModel
import com.comp350sec001group2.ekhoexplorer.ui.pages.auth.viewmodels.loginViewModelFactory
import com.comp350sec001group2.ekhoexplorer.ui.pages.auth.views.components.AuthButton
import com.comp350sec001group2.ekhoexplorer.ui.pages.auth.views.components.AuthErrorText
import com.comp350sec001group2.ekhoexplorer.ui.pages.auth.views.components.AuthLayout
import com.comp350sec001group2.ekhoexplorer.ui.pages.auth.views.components.AuthTextField
import com.comp350sec001group2.ekhoexplorer.ui.pages.auth.views.components.PasswordField
import com.comp350sec001group2.ekhoexplorer.ui.theme.AppStyle
import com.comp350sec001group2.ekhoexplorer.ui.theme.EkhoExplorerTheme

@Composable
fun LoginScreen(
    onCreateAccount: () -> Unit,
    onForgotPassword: () -> Unit,
    viewModel: LoginViewModel = viewModel(factory = loginViewModelFactory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LoginScreenContent(
        state = state,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onSubmit = viewModel::onSubmit,
        onCreateAccount = onCreateAccount,
        onForgotPassword = onForgotPassword,
    )
}

@Composable
fun LoginScreenContent(
    state: LoginUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onCreateAccount: () -> Unit,
    onForgotPassword: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focus = LocalFocusManager.current
    AuthLayout(
        title = "Welcome back",
        subtitle = "Sign in to keep exploring.",
        modifier = modifier.testTag("login-screen"),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(AppStyle.dimens.sm),
        ) {
            AuthTextField(
                value = state.email,
                onValueChange = onEmailChange,
                label = "Email",
                testTag = "login-email",
                problem = state.emailProblem?.let(AuthMessages::email),
                enabled = !state.isLoading,
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
                onImeAction = { focus.moveFocus(FocusDirection.Down) },
                contentType = ContentType.EmailAddress,
            )
            PasswordField(
                value = state.password,
                onValueChange = onPasswordChange,
                label = "Password",
                testTag = "login-password",
                problem = state.passwordProblem?.let(AuthMessages::password),
                enabled = !state.isLoading,
                imeAction = ImeAction.Done,
                onImeAction = {
                    focus.clearFocus()
                    onSubmit()
                },
            )
        }
        TextButton(
            onClick = onForgotPassword,
            enabled = !state.isLoading,
            modifier = Modifier.align(Alignment.End).testTag("login-forgot-password"),
        ) {
            Text("Forgot password?", color = AppStyle.colors.accent, style = AppStyle.type.captionAction)
        }
        AuthErrorText(state.error?.let(AuthMessages::error))
        AuthButton(
            text = "Sign in",
            onClick = {
                focus.clearFocus()
                onSubmit()
            },
            loading = state.isLoading,
            testTag = "login-submit",
        )
        TextButton(
            onClick = onCreateAccount,
            enabled = !state.isLoading,
            modifier = Modifier.testTag("login-create-account"),
        ) {
            Text("New here? Create an account", color = AppStyle.colors.accent, style = AppStyle.type.captionAction)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginPreview() {
    EkhoExplorerTheme {
        LoginScreenContent(LoginUiState(email = "jane@example.com"), {}, {}, {}, {}, {})
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginErrorPreview() {
    EkhoExplorerTheme {
        LoginScreenContent(
            LoginUiState(
                email = "jane@example",
                emailProblem = InputProblem.EMAIL_FORMAT,
                error = AuthErrorKind.WRONG_CREDENTIALS,
            ),
            {}, {}, {}, {}, {},
        )
    }
}
