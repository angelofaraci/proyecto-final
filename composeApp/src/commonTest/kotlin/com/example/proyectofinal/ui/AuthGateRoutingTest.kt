package com.example.proyectofinal.ui

import com.example.proyectofinal.domain.AuthSession
import com.example.proyectofinal.models.User
import com.example.proyectofinal.models.UserRole
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Runtime coverage for the `frontend-auth` "Auth Entry Flow" scenarios that the
 * verify gate flagged as untested. The routing decision lives in the pure
 * [AuthGateViewModel] / [resolveAuthView] construct, including the authenticated
 * dashboard landing path, so these are plain kotlin.test + StateFlow
 * assertions and need no Compose UI test harness.
 */
class AuthGateViewModelTest {

    // Scenario: "Default state is login" — default target is LOGIN, Register not selected.
    @Test
    fun `default target is login and register is not selected`() {
        val router = AuthGateViewModel()

        assertEquals(AuthScreenTarget.LOGIN, router.target.value)
    }

    // Scenario: "Default state is login" — GIVEN app starts with no token,
    // THEN Login screen visible, Register NOT shown.
    @Test
    fun `default view is login when session is anonymous`() {
        val anonymous = AuthSession()
        val router = AuthGateViewModel()

        val view = resolveAuthView(anonymous, router.target.value, onboardingComplete = false)

        assertEquals(AuthView.LOGIN, view)
    }

    // Scenario: "Text links switch forms" — GIVEN Login visible,
    // WHEN user selects the register link, THEN Register replaces it.
    @Test
    fun `selecting register link switches from login to register`() {
        val router = AuthGateViewModel()
        assertEquals(
            AuthView.LOGIN,
            resolveAuthView(AuthSession(), router.target.value, onboardingComplete = false)
        )

        router.switchToRegister()

        assertEquals(AuthScreenTarget.REGISTER, router.target.value)
        assertEquals(
            AuthView.REGISTER,
            resolveAuthView(AuthSession(), router.target.value, onboardingComplete = false)
        )
    }

    // Scenario: "Text links switch forms" — the reverse direction (Register link back to Login).
    @Test
    fun `selecting login link switches back from register to login`() {
        val router = AuthGateViewModel().apply { switchToRegister() }
        assertEquals(
            AuthView.REGISTER,
            resolveAuthView(AuthSession(), router.target.value, onboardingComplete = false)
        )

        router.switchToLogin()

        assertEquals(AuthScreenTarget.LOGIN, router.target.value)
        assertEquals(
            AuthView.LOGIN,
            resolveAuthView(AuthSession(), router.target.value, onboardingComplete = false)
        )
    }

    @Test
    fun `toggle flips between login and register in both directions`() {
        val router = AuthGateViewModel()

        router.toggle()
        assertEquals(AuthScreenTarget.REGISTER, router.target.value)

        router.toggle()
        assertEquals(AuthScreenTarget.LOGIN, router.target.value)
    }

    @Test
    fun `authenticated session with completed onboarding routes to dashboard landing`() {
        val authenticated = AuthSession(
            token = "token-123",
            user = User("1", "Alice", "alice@example.com", UserRole.STUDENT)
        )
        val router = AuthGateViewModel().apply { switchToRegister() }

        assertEquals(
            AuthView.AUTHENTICATED_HOME,
            resolveAuthView(authenticated, router.target.value, onboardingComplete = true)
        )
    }

    @Test
    fun `authenticated session with incomplete onboarding routes to onboarding`() {
        val authenticated = AuthSession(
            token = "token-123",
            user = User("1", "Alice", "alice@example.com", UserRole.STUDENT)
        )
        val router = AuthGateViewModel().apply { switchToRegister() }

        assertEquals(
            AuthView.ONBOARDING,
            resolveAuthView(authenticated, router.target.value, onboardingComplete = false)
        )
    }

    @Test
    fun `teacher bypasses learner onboarding and routes to teacher dashboard`() {
        val teacher = AuthSession(
            token = "teacher-token",
            user = User("teacher-1", "Teacher", "teacher@example.com", UserRole.TEACHER)
        )

        assertEquals(
            AuthView.TEACHER,
            resolveAuthView(teacher, AuthScreenTarget.LOGIN, onboardingComplete = false)
        )
    }

    @Test
    fun `admin keeps existing onboarding routing`() {
        val admin = AuthSession(
            token = "admin-token",
            user = User("admin-1", "Admin", "admin@example.com", UserRole.ADMIN)
        )

        assertEquals(
            AuthView.ONBOARDING,
            resolveAuthView(admin, AuthScreenTarget.LOGIN, onboardingComplete = false)
        )
    }

    @Test
    fun `register target remains selected when the view model is reused`() {
        val viewModel = AuthGateViewModel()

        viewModel.switchToRegister()
        val reusedViewModel = viewModel

        assertEquals(AuthScreenTarget.REGISTER, reusedViewModel.target.value)
    }

    @Test
    fun `forgot password routes to recovery and back to login`() {
        val router = AuthGateViewModel()

        router.switchToRecovery()
        assertEquals(
            AuthView.RECOVERY,
            resolveAuthView(AuthSession(), router.target.value, onboardingComplete = false)
        )

        router.switchToLogin()
        assertEquals(AuthScreenTarget.LOGIN, router.target.value)
    }
}
