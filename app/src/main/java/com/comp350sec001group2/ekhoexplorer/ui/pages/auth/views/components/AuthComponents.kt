package com.comp350sec001group2.ekhoexplorer.ui.pages.auth.views.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import com.comp350sec001group2.ekhoexplorer.ui.components.LogoPlaceholder
import com.comp350sec001group2.ekhoexplorer.ui.theme.AppStyle
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.unit.dp

@Composable
fun AuthLayout(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val d = AppStyle.dimens
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppStyle.colors.background)
            .imePadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = d.xl, vertical = d.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(d.md),
        ) {
            Spacer(Modifier.height(d.xl))
            LogoPlaceholder(Modifier.size(72.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = AppStyle.colors.textPrimary,
                textAlign = TextAlign.Center,
            )
            Text(
                text = subtitle,
                style = AppStyle.type.caption,
                color = AppStyle.colors.textSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(d.sm))
            content()
        }
    }
}

@Composable
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    testTag: String,
    modifier: Modifier = Modifier,
    problem: String? = null,
    enabled: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {},
    contentType: ContentType? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        enabled = enabled,
        isError = problem != null,
        supportingText = problem?.let { message -> { Text(message) } },
        trailingIcon = trailingIcon,
        visualTransformation = visualTransformation,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions = KeyboardActions(onAny = { onImeAction() }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AppStyle.colors.accent,
            focusedLabelColor = AppStyle.colors.accent,
            cursorColor = AppStyle.colors.accent,
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag)
            .then(if (contentType != null) Modifier.semantics { this.contentType = contentType } else Modifier),
    )
}

/** A password field with a show / hide toggle. */
@Composable
fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    testTag: String,
    modifier: Modifier = Modifier,
    problem: String? = null,
    enabled: Boolean = true,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {},
    isNewPassword: Boolean = false,
) {
    var visible by remember { mutableStateOf(false) }
    AuthTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        testTag = testTag,
        modifier = modifier,
        problem = problem,
        enabled = enabled,
        keyboardType = KeyboardType.Password,
        imeAction = imeAction,
        onImeAction = onImeAction,
        contentType = if (isNewPassword) ContentType.NewPassword else ContentType.Password,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }, modifier = Modifier.testTag("$testTag-toggle")) {
                Icon(
                    imageVector = if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = if (visible) "Hide password" else "Show password",
                    tint = AppStyle.colors.textSecondary,
                )
            }
        },
    )
}

/** Full-width button that swaps its text for a spinner while [loading]. */
@Composable
fun AuthButton(
    text: String,
    onClick: () -> Unit,
    loading: Boolean,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        enabled = !loading,
        colors = ButtonDefaults.buttonColors(
            containerColor = AppStyle.colors.accent,
            contentColor = AppStyle.colors.onAccent,
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(AppStyle.dimens.touchTarget)
            .testTag(testTag),
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp).testTag("$testTag-spinner"),
                strokeWidth = 2.dp,
                color = AppStyle.colors.onAccent,
            )
        } else {
            Text(text, style = AppStyle.type.captionAction)
        }
    }
}

/** A failure message shown above the button. */
@Composable
fun AuthErrorText(message: String?, modifier: Modifier = Modifier) {
    if (message == null) return
    Text(
        text = message,
        color = MaterialTheme.colorScheme.error,
        style = AppStyle.type.caption,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth().testTag("auth-error"),
    )
}
