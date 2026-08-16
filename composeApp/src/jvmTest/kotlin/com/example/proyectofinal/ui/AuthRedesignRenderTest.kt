package com.example.proyectofinal.ui

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.example.proyectofinal.ui.theme.AppTheme
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Render coverage for the `ui-redesign-sync` auth slice (Jul 16 handoff). Asserts the
 * semantics-observable token changes (step-label copy, 22x22 terms checkbox) and guards
 * the behavior that must survive the visual sync (footer navigation, terms toggling).
 */
class AuthRedesignRenderTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `login renders redesign copy and footer link navigates to register`() {
        var registerTaps = 0

        composeTestRule.setContent {
            AppTheme {
                LoginScreen(
                    viewModel = LoginViewModel(FakeAuthRepository()),
                    onSwitchToRegister = { registerTaps++ }
                )
            }
        }

        composeTestRule.onNodeWithText("Welcome back").assertExists()
        composeTestRule.onNodeWithText("Forgot your password?").assertExists()
        composeTestRule.onNodeWithText("Log in").assertExists()
        composeTestRule.onNodeWithText("or continue with").assertExists()
        composeTestRule.onNodeWithText("Google").assertExists()
        composeTestRule.onNodeWithText("Apple").assertExists()
        composeTestRule.onNodeWithText("Don't have an account?", substring = true).assertExists()

        composeTestRule.onNodeWithText("Sign up").assertExists().performClick()

        assertEquals(1, registerTaps)
    }

    @Test
    fun `social providers are no-ops and forgot password invokes recovery callback`() {
        var registerTaps = 0
        var recoveryTaps = 0
        composeTestRule.setContent {
            AppTheme {
                LoginScreen(
                    LoginViewModel(FakeAuthRepository()),
                    onSwitchToRegister = { registerTaps++ },
                    onForgotPassword = { recoveryTaps++ }
                )
            }
        }

        composeTestRule.onNodeWithText("Google").performClick()
        composeTestRule.onNodeWithText("Apple").performClick()
        assertEquals(0, registerTaps)
        assertEquals(0, recoveryTaps)

        composeTestRule.onNodeWithText("Forgot your password?").performClick()
        assertEquals(1, recoveryTaps)
    }

    @Test
    fun `password recovery destination returns to login`() {
        var backTaps = 0
        composeTestRule.setContent {
            AppTheme { PasswordRecoveryScreen(onBackToLogin = { backTaps++ }) }
        }

        composeTestRule.onNodeWithText("Password recovery").assertExists()
        composeTestRule.onNodeWithText("Back to login").performClick()

        assertEquals(1, backTaps)
    }

    @Test
    fun `back from register step one clears state and returns to login`() {
        val viewModel = RegisterViewModel(FakeAuthRepository()).apply { onNameChange("Ana") }
        var loginTaps = 0
        composeTestRule.setContent {
            AppTheme { RegisterScreen(viewModel, onSwitchToLogin = { loginTaps++ }) }
        }

        composeTestRule.onNodeWithText("Back").performClick()

        assertEquals(1, loginTaps)
        assertEquals(RegisterUiState(), viewModel.uiState.value)
    }

    @Test
    fun `register step label uses handoff copy and follows the wizard step`() {
        val viewModel = RegisterViewModel(FakeAuthRepository())

        composeTestRule.setContent {
            AppTheme {
                RegisterScreen(viewModel = viewModel)
            }
        }

        composeTestRule.onNodeWithText("Step 1 / 3").assertExists()

        composeTestRule.runOnIdle {
            viewModel.onNameChange("Ana")
            viewModel.continueStep()
        }

        composeTestRule.onNodeWithText("Step 1 / 3").assertDoesNotExist()
        composeTestRule.onNodeWithText("Step 2 / 3").assertExists()
    }

    @Test
    fun `terms checkbox is 22 by 22 dp and toggles acceptance from the row`() {
        val viewModel = RegisterViewModel(FakeAuthRepository()).apply {
            onNameChange("Ana")
            continueStep()
            onEmailChange("ana@correo.com")
            onPasswordChange("Password1!")
            continueStep()
        }

        composeTestRule.setContent {
            AppTheme {
                RegisterScreen(viewModel = viewModel)
            }
        }

        composeTestRule.onNodeWithText("Step 3 / 3").assertExists()
        // The toggleable row merges its descendants; the tagged box only exists in the unmerged tree.
        composeTestRule.onNodeWithTag("termsCheckboxBox", useUnmergedTree = true)
            .assertWidthIsEqualTo(22.dp)
            .assertHeightIsEqualTo(22.dp)

        val checkbox = composeTestRule.onNode(
            SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox)
        )
        checkbox.assertExists().assertIsOff()

        checkbox.performClick()

        checkbox.assertIsOn()
        assertTrue(viewModel.uiState.value.acceptedTerms)
    }
}
