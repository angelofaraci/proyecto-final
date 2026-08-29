package com.example.proyectofinal.data

import com.example.proyectofinal.di.ApiConfig
import com.example.proyectofinal.domain.ProfileRequestException
import com.example.proyectofinal.models.AvatarId
import com.example.proyectofinal.models.ChangePasswordRequest
import com.example.proyectofinal.models.ProfileError
import com.example.proyectofinal.models.ProfileErrorCode
import com.example.proyectofinal.models.ProfilePreferences
import com.example.proyectofinal.models.SupportedLanguage
import com.example.proyectofinal.models.UpdateAvatarRequest
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.http.content.TextContent
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UserApiProfileTest {
    private val json = Json { ignoreUnknownKeys = true }
    private val config = ApiConfig("https://example.test")

    @Test
    fun `profile preference avatar and password endpoints use shared contracts`() = runTest {
        val preferences = ProfilePreferences(
            notificationsEnabled = false,
            soundsEnabled = true,
            language = SupportedLanguage.ENGLISH,
            avatarId = AvatarId.AVATAR_2
        )
        val paths = mutableListOf<String>()
        val bodies = mutableMapOf<String, Set<String>>()
        val client = client(MockEngine { request ->
            paths += request.url.encodedPath
            (request.body as? TextContent)?.let { content ->
                bodies[request.url.encodedPath] = json.parseToJsonElement(content.text).jsonObject.keys
            }
            if (request.url.encodedPath == "/me/password") {
                respond(content = "", status = HttpStatusCode.NoContent)
            } else {
                respond(
                    content = json.encodeToString(preferences),
                    status = HttpStatusCode.OK,
                    headers = jsonHeaders
                )
            }
        })
        val api = UserApi(client, config)

        assertEquals(preferences, api.getProfilePreferences())
        assertEquals(preferences, api.updateProfilePreferences(preferences))
        assertEquals(preferences, api.updateAvatar(UpdateAvatarRequest(AvatarId.AVATAR_2)))
        api.changePassword(ChangePasswordRequest("current", "replacement"))

        assertEquals(
            listOf("/me/preferences", "/me/preferences", "/me/avatar", "/me/password"),
            paths
        )
        assertEquals(
            setOf("notificationsEnabled", "soundsEnabled", "language", "avatarId"),
            bodies.getValue("/me/preferences")
        )
        assertEquals(setOf("avatarId"), bodies.getValue("/me/avatar"))
        assertEquals(setOf("currentPassword", "newPassword"), bodies.getValue("/me/password"))
    }

    @Test
    fun `profile endpoint exposes typed server failure`() = runTest {
        val failure = ProfileError(ProfileErrorCode.EMAIL_CONFLICT, "EMAIL_CONFLICT")
        val api = UserApi(
            client(MockEngine {
                respond(
                    content = json.encodeToString(failure),
                    status = HttpStatusCode.Conflict,
                    headers = jsonHeaders
                )
            }),
            config
        )

        val exception = assertFailsWith<ProfileRequestException> {
            api.updateProfilePreferences(ProfilePreferences())
        }

        assertEquals(failure, exception.error)
        assertEquals(HttpStatusCode.Conflict.value, exception.statusCode)
    }

    private fun client(engine: MockEngine) = HttpClient(engine) {
        install(ContentNegotiation) { json(json) }
    }

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")
}
