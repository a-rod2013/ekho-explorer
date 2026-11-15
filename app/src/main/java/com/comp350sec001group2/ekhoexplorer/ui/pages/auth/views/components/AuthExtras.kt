package com.comp350sec001group2.ekhoexplorer.ui.pages.auth.views.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.comp350sec001group2.ekhoexplorer.Business.Auth.AuthValidator
import com.comp350sec001group2.ekhoexplorer.ui.theme.AppStyle

@Composable
fun PasswordChecklist(password: String, modifier: Modifier = Modifier) {
    val checks = AuthValidator.passwordChecks(password)
    Column(
        modifier = modifier.fillMaxWidth().testTag("password-checklist"),
        verticalArrangement = Arrangement.spacedBy(AppStyle.dimens.xs),
    ) {
        ChecklistRow("8 to ${AuthValidator.MAX_PASSWORD_LENGTH} characters", checks.longEnough, "rule-length")
        ChecklistRow("At least one letter", checks.hasLetter, "rule-letter")
        ChecklistRow("At least one number", checks.hasDigit, "rule-digit")
    }
}

@Composable
private fun ChecklistRow(text: String, met: Boolean, tag: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppStyle.dimens.sm),
        modifier = Modifier
            .testTag(tag)
            .semantics { contentDescription = if (met) "$text, done" else "$text, not yet" },
    ) {
        Icon(
            imageVector = if (met) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (met) AppStyle.colors.accent else AppStyle.colors.textSecondary,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = text,
            style = AppStyle.type.caption,
            color = if (met) AppStyle.colors.textPrimary else AppStyle.colors.textSecondary,
        )
    }
}

@Composable
fun AuthSecondaryButton(
    text: String,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier
            .fillMaxWidth()
            .height(AppStyle.dimens.touchTarget)
            .testTag(testTag),
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.dp,
                color = AppStyle.colors.accent,
            )
        } else {
            Text(text, style = AppStyle.type.captionAction, color = AppStyle.colors.accent)
        }
    }
}

@Composable
fun AuthNoticeText(message: String?, testTag: String = "auth-notice", modifier: Modifier = Modifier) {
    if (message == null) return
    Text(
        text = message,
        color = AppStyle.colors.accent,
        style = AppStyle.type.caption,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth().testTag(testTag),
    )
}

@Composable
fun AuthBodyText(message: String, modifier: Modifier = Modifier, testTag: String? = null) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = AppStyle.colors.textPrimary,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth().then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
    )
}
