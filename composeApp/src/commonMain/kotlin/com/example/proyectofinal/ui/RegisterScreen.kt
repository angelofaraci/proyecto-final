package com.example.proyectofinal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.example.proyectofinal.ui.primitives.AuthFieldIcon
import com.example.proyectofinal.ui.primitives.AuthFieldIconType
import com.example.proyectofinal.ui.primitives.MButton
import com.example.proyectofinal.ui.primitives.MTextField
import com.example.proyectofinal.ui.theme.AppThemeDefaults
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.Font
import proyectofinal.composeapp.generated.resources.Res
import proyectofinal.composeapp.generated.resources.register_action_back
import proyectofinal.composeapp.generated.resources.register_action_continue
import proyectofinal.composeapp.generated.resources.register_action_create
import proyectofinal.composeapp.generated.resources.register_action_creating
import proyectofinal.composeapp.generated.resources.register_email_label
import proyectofinal.composeapp.generated.resources.register_email_placeholder
import proyectofinal.composeapp.generated.resources.register_name_label
import proyectofinal.composeapp.generated.resources.register_name_placeholder
import proyectofinal.composeapp.generated.resources.register_password_hide_toggle
import proyectofinal.composeapp.generated.resources.register_password_label
import proyectofinal.composeapp.generated.resources.register_password_show_toggle
import proyectofinal.composeapp.generated.resources.register_password_strength_empty
import proyectofinal.composeapp.generated.resources.register_password_strength_medium
import proyectofinal.composeapp.generated.resources.register_password_strength_strong
import proyectofinal.composeapp.generated.resources.register_password_strength_weak
import proyectofinal.composeapp.generated.resources.register_step_indicator
import proyectofinal.composeapp.generated.resources.register_subtitle
import proyectofinal.composeapp.generated.resources.register_terms_text
import proyectofinal.composeapp.generated.resources.register_title
import proyectofinal.composeapp.generated.resources.jetbrains_mono_semibold

@Composable
fun RegisterScreen(
    viewModel: RegisterViewModel,
    onSwitchToLogin: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    RegisterContent(
        state = state,
        onNameChange = viewModel::onNameChange,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onTogglePasswordVisibility = viewModel::togglePasswordVisibility,
        onAcceptedTermsChange = viewModel::setAcceptedTerms,
        onContinue = viewModel::continueStep,
        onBack = {
            if (state.step == 1) {
                viewModel.reset()
                onSwitchToLogin()
            } else viewModel.goBack()
        }
    )
}

@Composable
private fun RegisterContent(
    state: RegisterUiState,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onAcceptedTermsChange: (Boolean) -> Unit,
    onContinue: () -> Unit,
    onBack: () -> Unit
) {
    AuthScreenScaffold(
        formTitle = stringResource(Res.string.register_title),
        formSubtitle = stringResource(Res.string.register_subtitle)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            WizardStepIndicator(currentStep = state.step)

            when (state.step) {
                1 -> NameStep(state, onNameChange)
                2 -> CredentialsStep(state, onEmailChange, onPasswordChange, onTogglePasswordVisibility)
                3 -> TermsStep(state, onAcceptedTermsChange)
            }

            if (state.errorMessage != null) {
                Text(
                    text = state.errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            MButton(
                onClick = onContinue,
                enabled = !state.isLoading && (state.step != 3 || state.acceptedTerms),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    when {
                        state.isLoading -> stringResource(Res.string.register_action_creating)
                        state.step == 3 -> stringResource(Res.string.register_action_create)
                        else -> stringResource(Res.string.register_action_continue)
                    }
                )
            }

            MButton(
                onClick = onBack,
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth(),
                style = com.example.proyectofinal.ui.primitives.MButtonStyle.Outline
            ) {
                Text(stringResource(Res.string.register_action_back))
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun WizardStepIndicator(currentStep: Int) {
    val jetBrainsMono = FontFamily(Font(Res.font.jetbrains_mono_semibold, FontWeight.SemiBold))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val step = index + 1
            val color = if (step <= currentStep) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(5.dp)
                    .background(color, RoundedCornerShape(AppThemeDefaults.shapes.stepSegment))
            )
        }
    }
    Text(
        text = stringResource(Res.string.register_step_indicator, currentStep),
        style = MaterialTheme.typography.labelMedium.copy(fontFamily = jetBrainsMono),
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun NameStep(state: RegisterUiState, onNameChange: (String) -> Unit) {
    MTextField(
        value = state.name,
        onValueChange = onNameChange,
        label = { Text(stringResource(Res.string.register_name_label)) },
        placeholder = { Text(stringResource(Res.string.register_name_placeholder)) },
        singleLine = true,
        isError = state.fieldErrors[RegisterField.Name] != null,
        supportingText = state.fieldErrors[RegisterField.Name]?.let { error -> { Text(error) } },
        authStyle = true,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun CredentialsStep(
    state: RegisterUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit
) {
    MTextField(
        value = state.email,
        onValueChange = onEmailChange,
        label = { Text(stringResource(Res.string.register_email_label)) },
        placeholder = { Text(stringResource(Res.string.register_email_placeholder)) },
        singleLine = true,
        isError = state.fieldErrors[RegisterField.Email] != null,
        supportingText = state.fieldErrors[RegisterField.Email]?.let { error -> { Text(error) } },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        authStyle = true,
        modifier = Modifier.fillMaxWidth()
    )

    MTextField(
        value = state.password,
        onValueChange = onPasswordChange,
        label = { Text(stringResource(Res.string.register_password_label)) },
        singleLine = true,
        isError = state.fieldErrors[RegisterField.Password] != null,
        supportingText = state.fieldErrors[RegisterField.Password]?.let { error -> { Text(error) } },
        visualTransformation = if (state.isPasswordVisible) {
            androidx.compose.ui.text.input.VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        trailingIcon = {
            IconButton(onClick = onTogglePasswordVisibility) {
                AuthFieldIcon(
                    type = if (state.isPasswordVisible) AuthFieldIconType.VisibilityOff else AuthFieldIconType.Visibility,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    description = if (state.isPasswordVisible) {
                        stringResource(Res.string.register_password_hide_toggle)
                    } else {
                        stringResource(Res.string.register_password_show_toggle)
                    }
                )
            }
        },
        authStyle = true,
        modifier = Modifier.fillMaxWidth()
    )

    PasswordStrengthMeter(state.passwordStrength)
}

@Composable
private fun PasswordStrengthMeter(strength: PasswordStrength) {
    val filledSegments = when (strength) {
        PasswordStrength.Empty -> 0
        PasswordStrength.Weak -> 1
        PasswordStrength.Medium -> 2
        PasswordStrength.Strong -> 3
    }
    val label = when (strength) {
        PasswordStrength.Empty -> stringResource(Res.string.register_password_strength_empty)
        PasswordStrength.Weak -> stringResource(Res.string.register_password_strength_weak)
        PasswordStrength.Medium -> stringResource(Res.string.register_password_strength_medium)
        PasswordStrength.Strong -> stringResource(Res.string.register_password_strength_strong)
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            repeat(3) { index ->
                val color = if (index < filledSegments) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant
                androidx.compose.foundation.Canvas(
                    modifier = Modifier
                        .weight(1f)
                        .height(5.dp)
                ) {
                    drawRoundRect(color = color, cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2))
                }
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TermsStep(state: RegisterUiState, onAcceptedTermsChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = state.acceptedTerms,
                role = Role.Checkbox,
                onValueChange = onAcceptedTermsChange
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TermsCheckboxBox(checked = state.acceptedTerms)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(Res.string.register_terms_text),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
    state.fieldErrors[RegisterField.Terms]?.let { error ->
        Text(
            text = error,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

/**
 * Redesign handoff terms control: 22x22dp box, 7dp checkbox radius, coral fill with
 * an on-coral checkmark when checked. Toggle semantics live on the parent row.
 */
@Composable
private fun TermsCheckboxBox(checked: Boolean) {
    val shape = RoundedCornerShape(AppThemeDefaults.shapes.checkbox)
    val boxModifier = if (checked) {
        Modifier.background(MaterialTheme.colorScheme.primary, shape)
    } else {
        Modifier
            .background(MaterialTheme.colorScheme.surface, shape)
            .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, shape)
    }
    val checkColor = MaterialTheme.colorScheme.onPrimary

    Box(
        modifier = Modifier
            .size(22.dp)
            .testTag("termsCheckboxBox")
            .then(boxModifier),
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            androidx.compose.foundation.Canvas(modifier = Modifier.size(12.dp)) {
                val strokeWidth = 2.dp.toPx()
                drawLine(
                    color = checkColor,
                    start = androidx.compose.ui.geometry.Offset(2.5.dp.toPx(), 6.dp.toPx()),
                    end = androidx.compose.ui.geometry.Offset(5.dp.toPx(), 9.dp.toPx()),
                    strokeWidth = strokeWidth,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
                drawLine(
                    color = checkColor,
                    start = androidx.compose.ui.geometry.Offset(5.dp.toPx(), 9.dp.toPx()),
                    end = androidx.compose.ui.geometry.Offset(9.5.dp.toPx(), 3.dp.toPx()),
                    strokeWidth = strokeWidth,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            }
        }
    }
}
