package com.example.proyectofinal.data

import com.example.proyectofinal.di.TokenStore
import com.example.proyectofinal.domain.AuthRepository
import com.example.proyectofinal.domain.AuthSession
import com.example.proyectofinal.models.AuthResponse
import com.example.proyectofinal.models.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class KtorAuthRepository(
    private val authApi: AuthApi,
    private val tokenStore: TokenStore
) : AuthRepository {

    private val _session = MutableStateFlow(
        tokenStore.accessToken
            ?.takeIf { it.isNotBlank() }
            ?.let { AuthSession(token = it) }
            ?: AuthSession()
    )

    override val session: StateFlow<AuthSession> = _session.asStateFlow()

    override suspend fun login(email: String, password: String): Result<User> =
        authenticate { authApi.login(email = email, password = password) }

    override suspend fun register(name: String, email: String, password: String): Result<User> =
        authenticate { authApi.register(name = name, email = email, password = password) }

    override fun replaceSessionUser(user: User, expectedToken: String?) {
        _session.update { current ->
            if (expectedToken != null && current.token == expectedToken && tokenStore.accessToken == expectedToken) {
                current.copy(user = user)
            } else {
                current
            }
        }
    }

    override fun logout() {
        tokenStore.accessToken = null
        _session.update { AuthSession() }
    }

    private suspend fun authenticate(request: suspend () -> AuthResponse): Result<User> =
        runCatching { request() }
            .map { response ->
                tokenStore.accessToken = response.token
                _session.value = AuthSession(token = response.token, user = response.user)
                response.user
            }
}
