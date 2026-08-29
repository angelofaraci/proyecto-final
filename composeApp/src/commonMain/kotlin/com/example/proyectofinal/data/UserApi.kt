package com.example.proyectofinal.data

import com.example.proyectofinal.di.ApiConfig
import com.example.proyectofinal.domain.ProfileRequestException
import com.example.proyectofinal.models.ChangePasswordRequest
import com.example.proyectofinal.models.ExerciseAttemptRequest
import com.example.proyectofinal.models.ExerciseAttemptResponse
import com.example.proyectofinal.models.ExerciseSubmission
import com.example.proyectofinal.models.User
import com.example.proyectofinal.models.UserProgress
import com.example.proyectofinal.models.ProfileError
import com.example.proyectofinal.models.ProfileErrorCode
import com.example.proyectofinal.models.ProfilePreferences
import com.example.proyectofinal.models.UpdateAvatarRequest
import com.example.proyectofinal.models.UpdateIdentityRequest
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.HttpResponse
import io.ktor.http.*
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement

class UserApi(
    private val client: HttpClient,
    private val apiConfig: ApiConfig
) {

    private val baseUrl: String = apiConfig.baseUrl
    private val profileJson = Json { encodeDefaults = true }

    suspend fun fetchUser(userId: String): User {
        return client.get("$baseUrl/users/$userId").body()
    }

    suspend fun updateUser(user: User) {
        client.put("$baseUrl/users/${user.id}") {
            contentType(ContentType.Application.Json)
            setBody(user)
        }
    }

    suspend fun updateIdentity(request: UpdateIdentityRequest): User =
        client.put("$baseUrl/me") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.profileBody()

    suspend fun changePassword(request: ChangePasswordRequest) {
        client.put("$baseUrl/me/password") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.ensureProfileSuccess()
    }

    suspend fun getProfilePreferences(): ProfilePreferences =
        client.get("$baseUrl/me/preferences").profileBody()

    suspend fun updateProfilePreferences(preferences: ProfilePreferences): ProfilePreferences =
        client.put("$baseUrl/me/preferences") {
            contentType(ContentType.Application.Json)
            setBody(profileJson.encodeToJsonElement(preferences))
        }.profileBody()

    suspend fun updateAvatar(request: UpdateAvatarRequest): ProfilePreferences =
        client.put("$baseUrl/me/avatar") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.profileBody()

    suspend fun fetchUserProgress(userId: String): UserProgress {
        return client.get("$baseUrl/progress/$userId").body()
    }

    suspend fun attemptExercise(
        exerciseId: String,
        submission: ExerciseSubmission,
        score: Int = 100
    ): ExerciseAttemptResponse {
        return client.post("$baseUrl/exercises/$exerciseId/attempt") {
            contentType(ContentType.Application.Json)
            setBody(
                ExerciseAttemptRequest(
                    exerciseId = exerciseId,
                    submission = submission,
                    score = score
                )
            )
        }.body()
    }

    private suspend inline fun <reified T> HttpResponse.profileBody(): T {
        ensureProfileSuccess()
        return body()
    }

    private suspend fun HttpResponse.ensureProfileSuccess() {
        if (status.isSuccess()) return

        val error = try {
            body<ProfileError>()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            ProfileError(ProfileErrorCode.INVALID_VALUE, "Profile request failed with HTTP ${status.value}")
        }
        throw ProfileRequestException(error = error, statusCode = status.value)
    }
}
