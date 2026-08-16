package com.example.proyectofinal.ui

import androidx.lifecycle.ViewModel
import com.example.proyectofinal.domain.AuthSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Which public auth form the gate should render when the user is not authenticated.
 */
enum class AuthScreenTarget { LOGIN, REGISTER, RECOVERY }

/**
 * Fully resolved view for the top-level auth gate. Authenticated teachers route to
 * [TEACHER], while other authenticated users route to [ONBOARDING] or
 * [AUTHENTICATED_HOME], so public auth forms are not shown in those states.
 */
enum class AuthView { AUTHENTICATED_HOME, TEACHER, LOGIN, REGISTER, RECOVERY, ONBOARDING }

/**
 * Retained state holder for the auth-gate routing decision.
 *
 * It does not touch Compose, so the auth entry flow behavior remains unit-testable
 * in commonTest without a Compose UI test harness. As a [ViewModel], it also retains
 * the selected target across platform configuration changes. Defaults to
 * [AuthScreenTarget.LOGIN] (spec: "Default state is login") and exposes the
 * text-link switching actions (spec: "Text links switch forms").
 */
class AuthGateViewModel : ViewModel() {
    private val _target = MutableStateFlow(AuthScreenTarget.LOGIN)

    /** Currently selected auth form target. Defaults to [AuthScreenTarget.LOGIN]. */
    val target: StateFlow<AuthScreenTarget> = _target.asStateFlow()

    /** Selects the Login form (the "Already have an account? Login" link action). */
    fun switchToLogin() {
        _target.value = AuthScreenTarget.LOGIN
    }

    /** Selects the Register form (the "Don't have an account? Register" link action). */
    fun switchToRegister() {
        _target.value = AuthScreenTarget.REGISTER
    }

    fun switchToRecovery() {
        _target.value = AuthScreenTarget.RECOVERY
    }

    /** Flips the current target LOGIN <-> REGISTER. */
    fun toggle() {
        _target.value =
            if (_target.value == AuthScreenTarget.LOGIN) AuthScreenTarget.REGISTER
            else AuthScreenTarget.LOGIN
    }
}

/**
 * Resolves the top-level view from the current session, onboarding completion,
 * and form target.
 *
 * When the session is authenticated the auth area is not shown. Teachers go to
 * [AuthView.TEACHER]. Other users with a completed learner profile go to
 * [AuthView.AUTHENTICATED_HOME], which renders the authenticated scaffold hosting
 * the dashboard landing; otherwise they are gated into [AuthView.ONBOARDING].
 * Anonymous sessions render the selected public form target.
 */
fun resolveAuthView(
    session: AuthSession,
    target: AuthScreenTarget,
    onboardingComplete: Boolean
): AuthView =
    if (session.user?.role == com.example.proyectofinal.models.UserRole.TEACHER) {
        AuthView.TEACHER
    } else if (session.isAuthenticated) {
        if (onboardingComplete) AuthView.AUTHENTICATED_HOME else AuthView.ONBOARDING
    }
    else when (target) {
        AuthScreenTarget.LOGIN -> AuthView.LOGIN
        AuthScreenTarget.REGISTER -> AuthView.REGISTER
        AuthScreenTarget.RECOVERY -> AuthView.RECOVERY
    }
