package com.example.proyectofinal.domain

import com.example.proyectofinal.models.User
import kotlinx.coroutines.flow.StateFlow

data class AuthSession(
    val token: String? = null,
    val user: User? = null
) {
    val isAuthenticated: Boolean
        get() = !token.isNullOrBlank()
}

interface AuthRepository {
    val session: StateFlow<AuthSession>

    suspend fun login(email: String, password: String): Result<User>

    suspend fun register(name: String, email: String, password: String): Result<User>

    fun replaceSessionUser(user: User, expectedToken: String?)

    fun logout()
}
