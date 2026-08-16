package com.example.proyectofinal.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.proyectofinal.domain.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class RegisterField {
    Name,
    Email,
    Password,
    Terms
}

/** The empty state plus the three levels rendered by the password-strength meter. */
enum class PasswordStrength {
    Empty,
    Weak,
    Medium,
    Strong
}

/**
 * Screen-local state for the public registration wizard. There is intentionally no
 * role field: public registration always creates a STUDENT account.
 */
data class RegisterUiState(
    val step: Int = 1,
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val acceptedTerms: Boolean = false,
    val passwordStrength: PasswordStrength = PasswordStrength.Empty,
    val fieldErrors: Map<RegisterField, String> = emptyMap(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class RegisterViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun onNameChange(value: String) {
        _uiState.value = _uiState.value.copy(
            name = value,
            fieldErrors = _uiState.value.fieldErrors - RegisterField.Name
        )
    }

    fun onEmailChange(value: String) {
        _uiState.value = _uiState.value.copy(
            email = value,
            fieldErrors = _uiState.value.fieldErrors - RegisterField.Email
        )
    }

    fun onPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(
            password = value,
            passwordStrength = passwordStrengthFor(value),
            fieldErrors = _uiState.value.fieldErrors - RegisterField.Password
        )
    }

    fun togglePasswordVisibility() {
        _uiState.value = _uiState.value.copy(
            isPasswordVisible = !_uiState.value.isPasswordVisible
        )
    }

    fun setAcceptedTerms(accepted: Boolean) {
        _uiState.value = _uiState.value.copy(
            acceptedTerms = accepted,
            fieldErrors = if (accepted) {
                _uiState.value.fieldErrors - RegisterField.Terms
            } else {
                _uiState.value.fieldErrors
            }
        )
    }

    /**
     * Advances only when the current step is valid. The final step submits the
     * unchanged name/email/password registration payload.
     */
    fun continueStep() {
        val current = _uiState.value
        val errors = validationErrorsFor(current)
        if (errors.isNotEmpty()) {
            _uiState.value = current.copy(fieldErrors = errors)
            return
        }

        if (current.step < FINAL_STEP) {
            _uiState.value = current.copy(step = current.step + 1, fieldErrors = emptyMap())
        } else {
            register()
        }
    }

    fun goBack() {
        val current = _uiState.value
        if (current.step > 1 && !current.isLoading) {
            _uiState.value = current.copy(
                step = current.step - 1,
                fieldErrors = emptyMap(),
                errorMessage = null
            )
        }
    }

    fun reset() {
        if (!_uiState.value.isLoading) _uiState.value = RegisterUiState()
    }

    fun register() {
        val current = _uiState.value
        // Retain the pre-wizard public contract for callers that submit without
        // completing the form: blank credentials surface the same generic error.
        if (current.name.isBlank() || current.email.isBlank() || current.password.isBlank()) {
            _uiState.value = current.copy(
                errorMessage = "Name, email, and password are required"
            )
            return
        }

        val credentialErrors = credentialErrors(current)
        if (credentialErrors.isNotEmpty()) {
            _uiState.value = current.copy(fieldErrors = credentialErrors)
            return
        }

        if (!current.acceptedTerms) {
            _uiState.value = current.copy(
                fieldErrors = mapOf(RegisterField.Terms to TERMS_REQUIRED_ERROR)
            )
            return
        }

        _uiState.value = current.copy(isLoading = true, errorMessage = null, fieldErrors = emptyMap())

        viewModelScope.launch {
            val result = authRepository.register(
                name = current.name.trim(),
                email = current.email.trim(),
                password = current.password
            )

            result.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = error.message ?: "Unknown error"
                )
            }

            // On success AuthRepository flips its session StateFlow, which App.kt
            // observes to replace this screen with onboarding or courses.
            result.onSuccess {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = null)
            }
        }
    }

    private fun validationErrorsFor(state: RegisterUiState): Map<RegisterField, String> =
        when (state.step) {
            1 -> if (state.name.isBlank()) mapOf(RegisterField.Name to NAME_REQUIRED_ERROR) else emptyMap()
            2 -> credentialErrors(state)
            3 -> if (!state.acceptedTerms) mapOf(RegisterField.Terms to TERMS_REQUIRED_ERROR) else emptyMap()
            else -> emptyMap()
        }

    private fun credentialErrors(state: RegisterUiState): Map<RegisterField, String> = buildMap {
        if (state.email.isBlank()) {
            put(RegisterField.Email, EMAIL_REQUIRED_ERROR)
        } else if (!EMAIL_PATTERN.matches(state.email.trim())) {
            put(RegisterField.Email, EMAIL_FORMAT_ERROR)
        }
        if (state.password.isBlank()) {
            put(RegisterField.Password, PASSWORD_REQUIRED_ERROR)
        }
    }

    private fun passwordStrengthFor(password: String): PasswordStrength {
        if (password.isEmpty()) return PasswordStrength.Empty

        val characterClasses = listOf(
            password.any(Char::isLowerCase),
            password.any(Char::isUpperCase),
            password.any(Char::isDigit),
            password.any { !it.isLetterOrDigit() }
        ).count { it }

        return when {
            password.length >= 8 && characterClasses >= 3 -> PasswordStrength.Strong
            password.length >= 6 && characterClasses >= 2 -> PasswordStrength.Medium
            else -> PasswordStrength.Weak
        }
    }

    private companion object {
        const val FINAL_STEP = 3
        const val NAME_REQUIRED_ERROR = "Ingresá tu nombre."
        const val EMAIL_REQUIRED_ERROR = "Ingresá tu correo electrónico."
        const val EMAIL_FORMAT_ERROR = "Ingresá un correo electrónico válido."
        const val PASSWORD_REQUIRED_ERROR = "Ingresá una contraseña."
        const val TERMS_REQUIRED_ERROR = "Aceptá los términos y condiciones para continuar."
        val EMAIL_PATTERN = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    }
}
