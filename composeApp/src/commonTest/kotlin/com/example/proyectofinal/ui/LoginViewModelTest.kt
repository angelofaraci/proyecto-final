package com.example.proyectofinal.ui

import com.example.proyectofinal.domain.AuthRepository
import com.example.proyectofinal.domain.AuthSession
import com.example.proyectofinal.models.User
import com.example.proyectofinal.models.UserRole
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {
    private lateinit var dispatcher: TestDispatcher

    @BeforeTest
    fun setUp() {
        dispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `login with empty fields shows required error without calling repository`() =
        runTest(dispatcher) {
            val repo = FakeAuthRepository()
            val vm = LoginViewModel(repo)

            vm.login()

            assertEquals("Email and password are required", vm.uiState.value.errorMessage)
            assertFalse(vm.uiState.value.isLoading)
            assertTrue(repo.loginCalls.isEmpty())
            assertFalse(repo.session.value.isAuthenticated)
        }

    @Test
    fun `login with malformed nonblank email sets validation error without calling repository`() =
        runTest(dispatcher) {
            val repo = FakeAuthRepository()
            val vm = LoginViewModel(repo)

            vm.onEmailChange("not-an-email")
            vm.onPasswordChange("secret")
            vm.login()

            assertEquals("Ingresá un correo electrónico válido.", vm.uiState.value.emailError)
            assertNull(vm.uiState.value.errorMessage)
            assertTrue(repo.loginCalls.isEmpty())
        }

    @Test
    fun `password visibility toggles without changing the password`() = runTest(dispatcher) {
        val vm = LoginViewModel(FakeAuthRepository())

        vm.onPasswordChange("secret")
        vm.togglePasswordVisibility()

        assertTrue(vm.uiState.value.isPasswordVisible)
        assertEquals("secret", vm.uiState.value.password)

        vm.togglePasswordVisibility()

        assertFalse(vm.uiState.value.isPasswordVisible)
    }

    @Test
    fun `login sets loading then clears it on success and authenticates session`() =
        runTest(dispatcher) {
            val user = User("1", "Alice", "alice@example.com", UserRole.STUDENT)
            val repo = FakeAuthRepository().apply { loginResult = Result.success(user) }
            val vm = LoginViewModel(repo)

            vm.onEmailChange("alice@example.com")
            vm.onPasswordChange("secret")
            vm.login()

            // The launched coroutine ran synchronously up to the gated repo call,
            // so the loading flag should already be visible at the suspension point.
            assertTrue(vm.uiState.value.isLoading)
            assertNull(vm.uiState.value.errorMessage)
            assertEquals(listOf("alice@example.com" to "secret"), repo.loginCalls)

            repo.completeLogin()
            advanceUntilIdle()

            assertFalse(vm.uiState.value.isLoading)
            assertNull(vm.uiState.value.errorMessage)
            assertEquals(user, repo.session.value.user)
            assertTrue(repo.session.value.isAuthenticated)
        }

    @Test
    fun `login surfaces raw repository error message and keeps session anonymous`() =
        runTest(dispatcher) {
            val repo = FakeAuthRepository().apply {
                loginResult = Result.failure(RuntimeException("Invalid credentials"))
            }
            val vm = LoginViewModel(repo)

            vm.onEmailChange("alice@example.com")
            vm.onPasswordChange("wrong")
            vm.login()

            repo.completeLogin()
            advanceUntilIdle()

            assertEquals("Invalid credentials", vm.uiState.value.errorMessage)
            assertFalse(vm.uiState.value.isLoading)
            assertNull(repo.session.value.user)
            assertFalse(repo.session.value.isAuthenticated)
        }
}

/**
 * Deterministic [AuthRepository] used by the ViewModel tests. The suspend actions
 * block on a [CompletableDeferred] so tests can observe the loading state before
 * the repository call resolves, then drive it to completion.
 */
class FakeAuthRepository : AuthRepository {
    override val session: MutableStateFlow<AuthSession> = MutableStateFlow(AuthSession())

    var loginResult: Result<User> = Result.success(User("id", "name", "email", UserRole.STUDENT))
    var registerResult: Result<User> = Result.success(User("id", "name", "email", UserRole.STUDENT))

    private val loginGate = CompletableDeferred<Unit>()
    private val registerGate = CompletableDeferred<Unit>()

    val loginCalls = mutableListOf<Pair<String, String>>()
    val registerCalls = mutableListOf<Triple<String, String, String>>()
    var logoutCount = 0

    override suspend fun login(email: String, password: String): Result<User> {
        loginCalls.add(email to password)
        loginGate.await()
        loginResult.onSuccess { session.value = AuthSession("token", it) }
        return loginResult
    }

    override suspend fun register(name: String, email: String, password: String): Result<User> {
        registerCalls.add(Triple(name, email, password))
        registerGate.await()
        registerResult.onSuccess { session.value = AuthSession("token", it) }
        return registerResult
    }

    override fun replaceSessionUser(user: User, expectedToken: String?) {
        session.value = session.value.copy(user = user)
    }

    override fun logout() {
        logoutCount++
        session.value = AuthSession()
    }

    fun completeLogin() { loginGate.complete(Unit) }
    fun completeRegister() { registerGate.complete(Unit) }
}
