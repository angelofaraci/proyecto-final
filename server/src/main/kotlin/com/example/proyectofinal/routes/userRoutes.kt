package com.example.proyectofinal.routes

import com.example.proyectofinal.models.CompleteLessonRequest
import com.example.proyectofinal.models.ChangePasswordRequest
import com.example.proyectofinal.models.ProfilePreferences
import com.example.proyectofinal.models.ExerciseAttemptRequest
import com.example.proyectofinal.models.ProfileError
import com.example.proyectofinal.models.ProfileErrorCode
import com.example.proyectofinal.models.UpdateIdentityRequest
import com.example.proyectofinal.models.UpdateAvatarRequest
import com.example.proyectofinal.models.UpdateUserRequest
import com.example.proyectofinal.models.UserRole
import com.example.proyectofinal.plugins.currentRole
import com.example.proyectofinal.plugins.currentUserId
import com.example.proyectofinal.plugins.requireSelfOrAdmin
import com.example.proyectofinal.service.ExerciseAttemptResult
import com.example.proyectofinal.service.IdentityUpdateResult
import com.example.proyectofinal.service.PasswordChangeResult
import com.example.proyectofinal.service.UserService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
fun Application.userRoutes(service: UserService) {
    routing {
        authenticate("auth-jwt") {
            put("/me") {
                val userId = call.currentUserId() ?: return@put call.respond(HttpStatusCode.Unauthorized)
                val request = try {
                    call.receive<UpdateIdentityRequest>()
                } catch (_: Exception) {
                    return@put call.respond(HttpStatusCode.BadRequest, profileError(ProfileErrorCode.INVALID_VALUE))
                }
                when (val result = service.updateIdentity(userId, request)) {
                    is IdentityUpdateResult.Success -> call.respond(result.user)
                    IdentityUpdateResult.InvalidValue -> call.respond(HttpStatusCode.BadRequest, profileError(ProfileErrorCode.INVALID_VALUE))
                    IdentityUpdateResult.EmailConflict -> call.respond(HttpStatusCode.Conflict, profileError(ProfileErrorCode.EMAIL_CONFLICT))
                    IdentityUpdateResult.NotFound -> call.respond(HttpStatusCode.NotFound)
                }
            }

            put("/me/password") {
                val userId = call.currentUserId() ?: return@put call.respond(HttpStatusCode.Unauthorized)
                val request = try {
                    call.receive<ChangePasswordRequest>()
                } catch (_: Exception) {
                    return@put call.respond(HttpStatusCode.BadRequest, profileError(ProfileErrorCode.INVALID_VALUE))
                }
                when (service.changePassword(userId, request)) {
                    PasswordChangeResult.Success -> call.respond(HttpStatusCode.NoContent)
                    PasswordChangeResult.InvalidValue -> call.respond(HttpStatusCode.BadRequest, profileError(ProfileErrorCode.INVALID_VALUE))
                    PasswordChangeResult.InvalidPassword -> call.respond(HttpStatusCode.BadRequest, profileError(ProfileErrorCode.INVALID_PASSWORD))
                    PasswordChangeResult.NotFound -> call.respond(HttpStatusCode.NotFound)
                }
            }

            get("/me/preferences") {
                val userId = call.currentUserId() ?: return@get call.respond(HttpStatusCode.Unauthorized)
                val preferences = service.getProfilePreferences(userId)
                    ?: return@get call.respond(HttpStatusCode.NotFound)
                call.respond(preferences)
            }

            put("/me/preferences") {
                val userId = call.currentUserId() ?: return@put call.respond(HttpStatusCode.Unauthorized)
                val request = try {
                    val payload = call.receive<JsonObject>()
                    require(payload.keys.containsAll(PROFILE_PREFERENCE_FIELDS))
                    PROFILE_JSON.decodeFromJsonElement<ProfilePreferences>(payload)
                } catch (_: Exception) {
                    return@put call.respond(HttpStatusCode.BadRequest, profileError(ProfileErrorCode.INVALID_VALUE))
                }
                val preferences = service.updateProfilePreferences(userId, request)
                    ?: return@put call.respond(HttpStatusCode.NotFound)
                call.respond(preferences)
            }

            put("/me/avatar") {
                val userId = call.currentUserId() ?: return@put call.respond(HttpStatusCode.Unauthorized)
                val request = try {
                    call.receive<UpdateAvatarRequest>()
                } catch (_: Exception) {
                    return@put call.respond(HttpStatusCode.BadRequest, profileError(ProfileErrorCode.INVALID_VALUE))
                }
                val preferences = service.updateAvatar(userId, request)
                    ?: return@put call.respond(HttpStatusCode.NotFound)
                call.respond(preferences)
            }

            get("/users/{id}") {
                val userId = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                if (!call.requireSelfOrAdmin(userId)) return@get

                val user = service.getUserById(userId) ?: return@get call.respond(HttpStatusCode.NotFound)

                call.respond(user)
            }

            put("/users/{id}") {
                val userId = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
                if (!call.requireSelfOrAdmin(userId)) return@put

                val request = call.receive<UpdateUserRequest>()
                val currentRole = call.currentRole() ?: return@put call.respond(HttpStatusCode.Unauthorized)

                if (request.role != null && currentRole != UserRole.ADMIN) {
                    call.respond(HttpStatusCode.Forbidden, "Only admins can change roles")
                    return@put
                }

                val user = service.updateUser(userId, request) ?: return@put call.respond(HttpStatusCode.NotFound)
                call.respond(user)
            }

            get("/progress/{userId}") {
                val userId = call.parameters["userId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                if (!call.requireSelfOrAdmin(userId)) return@get

                call.respond(service.getUserProgress(userId))
            }

            post("/exercises/{id}/complete") {
                call.respond(
                    HttpStatusCode.Gone,
                    "Direct exercise completion is deprecated for students; submit /attempt instead"
                )
            }

            post("/exercises/{id}/attempt") {
                val exerciseId = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                val request = try {
                    call.receive<ExerciseAttemptRequest>()
                } catch (_: Exception) {
                    return@post call.respond(HttpStatusCode.BadRequest, "Invalid request body")
                }

                if (exerciseId != request.exerciseId) {
                    return@post call.respond(HttpStatusCode.BadRequest, "Path id must match body exerciseId")
                }

                val userId = call.currentUserId()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, "Invalid or expired token")
                val role = call.currentRole()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, "Invalid or expired token")

                when (val result = service.attemptExercise(userId, role, request)) {
                    is ExerciseAttemptResult.Success -> call.respond(result.response)
                    is ExerciseAttemptResult.InvalidRequest -> call.respond(HttpStatusCode.BadRequest, result.message)
                    ExerciseAttemptResult.Forbidden -> call.respond(HttpStatusCode.Forbidden, "Forbidden")
                    ExerciseAttemptResult.NotFound -> call.respond(HttpStatusCode.NotFound)
                }
            }

            post("/progress") {
                val currentRole = call.currentRole() ?: return@post call.respond(HttpStatusCode.Unauthorized)
                if (currentRole == UserRole.STUDENT) {
                    return@post call.respond(
                        HttpStatusCode.Gone,
                        "Direct lesson completion is deprecated for students; complete exercises instead"
                    )
                }
                if (currentRole != UserRole.ADMIN) {
                    return@post call.respond(HttpStatusCode.Forbidden, "Forbidden")
                }
                val request = call.receive<CompleteLessonRequest>()
                service.updateProgress(request)
                call.respond(HttpStatusCode.OK)
            }
        }
    }
}

private fun profileError(code: ProfileErrorCode) = ProfileError(code, code.name)

private val PROFILE_PREFERENCE_FIELDS =
    setOf("notificationsEnabled", "soundsEnabled", "language", "avatarId")
private val PROFILE_JSON = Json { ignoreUnknownKeys = true }
