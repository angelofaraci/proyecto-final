package com.example.proyectofinal

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.proyectofinal.domain.AuthRepository
import com.example.proyectofinal.domain.LearnerProfileRepository
import com.example.proyectofinal.ui.primitives.MProgressIndicator
import com.example.proyectofinal.ui.localization.AppLocaleController
import com.example.proyectofinal.ui.localization.AppLocaleHost
import com.example.proyectofinal.ui.theme.AppTheme
import com.example.proyectofinal.ui.AuthGateViewModel
import com.example.proyectofinal.ui.AuthView
import com.example.proyectofinal.ui.AuthenticatedHomeScaffold
import com.example.proyectofinal.ui.LoginScreen
import com.example.proyectofinal.ui.LoginViewModel
import com.example.proyectofinal.ui.OnboardingScreen
import com.example.proyectofinal.ui.OnboardingViewModel
import com.example.proyectofinal.ui.PasswordRecoveryScreen
import com.example.proyectofinal.ui.RegisterScreen
import com.example.proyectofinal.ui.RegisterViewModel
import com.example.proyectofinal.ui.resolveAuthView
import com.example.proyectofinal.ui.teacher.TeacherDashboardScreen
import com.example.proyectofinal.ui.teacher.TeacherDashboardViewModel
import com.example.proyectofinal.ui.teacher.teacherDashboardViewModelKey
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun App() {
    val localeController = remember { AppLocaleController() }
    AppLocaleHost(localeController) {
        AppTheme {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                AuthGate()
            }
        }
    }
}

@Composable
private fun AuthGate() {
    val authRepository = koinInject<AuthRepository>()
    val learnerProfileRepository = koinInject<LearnerProfileRepository>()
    val session by authRepository.session.collectAsState()
    val router = koinViewModel<AuthGateViewModel>()
    val target by router.target.collectAsState()
    var onboardingRefreshKey by remember(session.token) { mutableStateOf(0) }
    val onboardingComplete by produceState<Boolean?>(
        initialValue = if (session.user?.role == com.example.proyectofinal.models.UserRole.TEACHER) true
            else if (session.isAuthenticated) null else false,
        keys = arrayOf<Any?>(session.isAuthenticated, session.token, session.user?.id, onboardingRefreshKey)
    ) {
        val userId = session.user?.id
        value =
            if (session.user?.role == com.example.proyectofinal.models.UserRole.TEACHER) {
                true
            } else if (session.isAuthenticated && !userId.isNullOrBlank()) {
                learnerProfileRepository.isOnboardingComplete(userId)
            } else {
                false
            }
    }

    if (session.isAuthenticated && onboardingComplete == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            MProgressIndicator()
        }
        return
    }

    when (resolveAuthView(session, target, onboardingComplete = onboardingComplete ?: false)) {
        AuthView.AUTHENTICATED_HOME -> AuthenticatedHomeScaffold(onLogout = authRepository::logout)

        AuthView.TEACHER -> TeacherDashboardScreen(
            viewModel = koinViewModel<TeacherDashboardViewModel>(
                key = teacherDashboardViewModelKey(requireNotNull(session.user).id)
            ) {
                parametersOf(requireNotNull(session.user).id)
            },
            onLogout = authRepository::logout
        )

        AuthView.LOGIN -> LoginScreen(
            viewModel = koinViewModel<LoginViewModel>(),
            onSwitchToRegister = router::switchToRegister,
            onForgotPassword = router::switchToRecovery
        )

        AuthView.RECOVERY -> PasswordRecoveryScreen(onBackToLogin = router::switchToLogin)

        AuthView.REGISTER -> RegisterScreen(
            viewModel = koinViewModel<RegisterViewModel>(),
            onSwitchToLogin = router::switchToLogin
        )

        AuthView.ONBOARDING -> OnboardingScreen(
            viewModel = koinViewModel<OnboardingViewModel>(),
            onCompleted = { onboardingRefreshKey += 1 }
        )
    }
}
