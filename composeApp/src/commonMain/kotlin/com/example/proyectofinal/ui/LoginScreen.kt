package com.example.proyectofinal.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import com.example.proyectofinal.ui.primitives.AuthFieldIcon
import com.example.proyectofinal.ui.primitives.AuthFieldIconType
import com.example.proyectofinal.ui.primitives.MButton
import com.example.proyectofinal.ui.primitives.MButtonStyle
import com.example.proyectofinal.ui.primitives.MTextField
import proyectofinal.composeapp.generated.resources.Res
import proyectofinal.composeapp.generated.resources.apple_logo
import proyectofinal.composeapp.generated.resources.google_logo
import proyectofinal.composeapp.generated.resources.login_action_login
import proyectofinal.composeapp.generated.resources.login_action_logging_in
import proyectofinal.composeapp.generated.resources.login_action_register
import proyectofinal.composeapp.generated.resources.login_divider_or
import proyectofinal.composeapp.generated.resources.login_email_label
import proyectofinal.composeapp.generated.resources.login_email_placeholder
import proyectofinal.composeapp.generated.resources.login_forgot_password
import proyectofinal.composeapp.generated.resources.login_no_account_prompt
import proyectofinal.composeapp.generated.resources.login_password_hide_description
import proyectofinal.composeapp.generated.resources.login_password_label
import proyectofinal.composeapp.generated.resources.login_password_show_description
import proyectofinal.composeapp.generated.resources.login_recovery_back
import proyectofinal.composeapp.generated.resources.login_recovery_description
import proyectofinal.composeapp.generated.resources.login_recovery_title
import proyectofinal.composeapp.generated.resources.login_social_apple
import proyectofinal.composeapp.generated.resources.login_social_google
import proyectofinal.composeapp.generated.resources.login_subtitle
import proyectofinal.composeapp.generated.resources.login_title

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onSwitchToRegister: () -> Unit,
    onForgotPassword: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    LoginContent(
        state = state,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onTogglePasswordVisibility = viewModel::togglePasswordVisibility,
        onLogin = viewModel::login,
        onSwitchToRegister = onSwitchToRegister,
        onForgotPassword = onForgotPassword
    )
}

@Composable
private fun LoginContent(
    state: LoginUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onLogin: () -> Unit,
    onSwitchToRegister: () -> Unit,
    onForgotPassword: () -> Unit
) {
    AuthScreenScaffold(
        formTitle = stringResource(Res.string.login_title),
        formSubtitle = stringResource(Res.string.login_subtitle)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            MTextField(
                value = state.email,
                onValueChange = onEmailChange,
                label = { Text(stringResource(Res.string.login_email_label)) },
                placeholder = { Text(stringResource(Res.string.login_email_placeholder)) },
                singleLine = true,
                isError = state.emailError != null,
                supportingText = state.emailError?.let { error -> { Text(error) } },
                leadingIcon = {
                    AuthFieldIcon(
                        type = AuthFieldIconType.Email,
                        tint = if (state.emailError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        description = stringResource(Res.string.login_email_label)
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                authStyle = true,
                modifier = Modifier.fillMaxWidth()
            )

            MTextField(
                value = state.password,
                onValueChange = onPasswordChange,
                label = { Text(stringResource(Res.string.login_password_label)) },
                singleLine = true,
                leadingIcon = {
                    AuthFieldIcon(
                        type = AuthFieldIconType.Lock,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        description = stringResource(Res.string.login_password_label)
                    )
                },
                trailingIcon = {
                    IconButton(onClick = onTogglePasswordVisibility) {
                        AuthFieldIcon(
                            type = if (state.isPasswordVisible) AuthFieldIconType.VisibilityOff else AuthFieldIconType.Visibility,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            description = if (state.isPasswordVisible) {
                                stringResource(Res.string.login_password_hide_description)
                            } else {
                                stringResource(Res.string.login_password_show_description)
                            }
                        )
                    }
                },
                visualTransformation = if (state.isPasswordVisible) {
                    androidx.compose.ui.text.input.VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                authStyle = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = stringResource(Res.string.login_forgot_password),
                modifier = Modifier
                    .align(Alignment.End)
                    .clickable(onClick = onForgotPassword),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium
            )

            if (state.errorMessage != null) {
                Text(
                    text = state.errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            MButton(
                onClick = onLogin,
                enabled = !state.isLoading && state.emailError == null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (state.isLoading) {
                        stringResource(Res.string.login_action_logging_in)
                    } else {
                        stringResource(Res.string.login_action_login)
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(Res.string.login_divider_or),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider(modifier = Modifier.weight(1f))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SocialButton(
                    text = stringResource(Res.string.login_social_google),
                    logo = { androidx.compose.foundation.Image(painterResource(Res.drawable.google_logo), null, Modifier.size(18.dp)) },
                    modifier = Modifier.weight(1f)
                )
                SocialButton(
                    text = stringResource(Res.string.login_social_apple),
                    logo = { androidx.compose.foundation.Image(painterResource(Res.drawable.apple_logo), null, Modifier.size(18.dp)) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(Res.string.login_no_account_prompt),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(Res.string.login_action_register),
                    modifier = Modifier.clickable(enabled = !state.isLoading, onClick = onSwitchToRegister),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
fun PasswordRecoveryScreen(onBackToLogin: () -> Unit) {
    AuthScreenScaffold(
        formTitle = stringResource(Res.string.login_recovery_title),
        formSubtitle = stringResource(Res.string.login_recovery_description)
    ) {
        MButton(
            onClick = onBackToLogin,
            modifier = Modifier.fillMaxWidth(),
            style = MButtonStyle.Outline
        ) {
            Text(stringResource(Res.string.login_recovery_back))
        }
    }
}

@Composable
private fun SocialButton(
    text: String,
    logo: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    MButton(
        onClick = {},
        modifier = modifier,
        style = MButtonStyle.Social
        ,contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
    ) {
        logo()
        Spacer(modifier = Modifier.size(4.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
    }
}
