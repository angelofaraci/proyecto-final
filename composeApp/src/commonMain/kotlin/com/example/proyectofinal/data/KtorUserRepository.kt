package com.example.proyectofinal.data

import com.example.proyectofinal.db.AppDatabase
import com.example.proyectofinal.di.TokenStore
import com.example.proyectofinal.domain.AuthRepository
import com.example.proyectofinal.domain.UserRepository
import com.example.proyectofinal.models.ChangePasswordRequest
import com.example.proyectofinal.models.ExerciseAttemptResponse
import com.example.proyectofinal.models.ExerciseSubmission
import com.example.proyectofinal.models.User
import com.example.proyectofinal.models.UserProgress
import com.example.proyectofinal.models.UserRole
import com.example.proyectofinal.models.ProfilePreferences
import com.example.proyectofinal.models.UpdateAvatarRequest
import com.example.proyectofinal.models.UpdateIdentityRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class KtorUserRepository(
    private val api: UserApi,
    private val database: AppDatabase,
    private val tokenStore: TokenStore,
    private val authRepository: AuthRepository
) : UserRepository {

    override suspend fun getCurrentUser(): User? = withContext(Dispatchers.IO) {
        val userId = tokenStore.accessToken?.let(::extractUserIdFromToken) ?: return@withContext null

        try {
            val remote = api.fetchUser(userId)
            insertUserToLocal(remote)
            remote
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getUserRole(userId: String): UserRole = withContext(Dispatchers.IO) {
        try {
            val user = api.fetchUser(userId)
            insertUserToLocal(user)
            user.role
        } catch (e: Exception) {
            UserRole.STUDENT
        }
    }

    override suspend fun updateUser(user: User) = withContext(Dispatchers.IO) {
        api.updateUser(user)
        insertUserToLocal(user)
    }

    override suspend fun updateIdentity(request: UpdateIdentityRequest): User = withContext(Dispatchers.IO) {
        val expectedToken = requireNotNull(authRepository.session.value.token)
        val authoritative = api.updateIdentity(request, expectedToken)
        authRepository.replaceSessionUser(authoritative, expectedToken)
        try {
            insertUserToLocal(authoritative)
        } catch (_: Exception) {
            // Remote state and authenticated session are authoritative; the cache is best effort.
        }
        authoritative
    }

    override suspend fun changePassword(request: ChangePasswordRequest) = withContext(Dispatchers.IO) {
        api.changePassword(request)
    }

    override suspend fun getProfilePreferences(): ProfilePreferences = withContext(Dispatchers.IO) {
        api.getProfilePreferences()
    }

    override suspend fun updateProfilePreferences(
        preferences: ProfilePreferences
    ): ProfilePreferences = withContext(Dispatchers.IO) {
        api.updateProfilePreferences(preferences)
    }

    override suspend fun updateAvatar(request: UpdateAvatarRequest): ProfilePreferences =
        withContext(Dispatchers.IO) {
            api.updateAvatar(request)
        }

    override suspend fun getUserProgress(userId: String): UserProgress = withContext(Dispatchers.IO) {
        try {
            val remote = api.fetchUserProgress(userId)
            syncUserProgressToLocal(remote)
            remote
        } catch (e: Exception) {
            readUserProgressFromLocal(userId)
        }
    }

    override suspend fun attemptExercise(
        exerciseId: String,
        submission: ExerciseSubmission,
        score: Int
    ): ExerciseAttemptResponse = withContext(Dispatchers.IO) {
        val response = api.attemptExercise(exerciseId = exerciseId, submission = submission, score = score)
        syncUserProgressToLocal(response.progress)
        response
    }

    private fun insertUserToLocal(user: User) {
        database.appDatabaseQueries.insertUser(
            id = user.id,
            name = user.name,
            email = user.email,
            role = user.role
        )
    }

    private fun syncUserProgressToLocal(progress: UserProgress) {
        database.appDatabaseQueries.insertProgress(
            userId = progress.userId,
            totalScore = progress.totalScore
        )
        progress.completedLessonIds.forEach { lessonId ->
            database.appDatabaseQueries.insertCompletedLesson(
                userId = progress.userId,
                lessonId = lessonId
            )
        }
        progress.completedExerciseIds.forEach { exerciseId ->
            database.appDatabaseQueries.insertCompletedExercise(
                userId = progress.userId,
                exerciseId = exerciseId
            )
        }
        progress.enrolledCourseIds.forEach { courseId ->
            database.appDatabaseQueries.insertEnrolledCourse(
                userId = progress.userId,
                courseId = courseId
            )
        }
    }

    private fun readUserProgressFromLocal(userId: String): UserProgress {
        val progress = database.appDatabaseQueries.selectProgressByUserId(userId).executeAsOneOrNull()
        val completedLessonIds = database.appDatabaseQueries
            .selectCompletedLessonsByUserId(userId)
            .executeAsList()
            .toSet()
        val completedExerciseIds = database.appDatabaseQueries
            .selectCompletedExercisesByUserId(userId)
            .executeAsList()
            .toSet()
        val enrolledCourseIds = database.appDatabaseQueries
            .selectEnrolledCoursesByUserId(userId)
            .executeAsList()
            .toSet()

        return UserProgress(
            userId = userId,
            completedLessonIds = completedLessonIds,
            completedExerciseIds = completedExerciseIds,
            totalScore = progress?.totalScore ?: 0,
            enrolledCourseIds = enrolledCourseIds
        )
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun extractUserIdFromToken(token: String): String? {
        val payloadSegment = token.split('.').getOrNull(1) ?: return null
        val normalizedPayload = payloadSegment.padEnd(
            length = payloadSegment.length + (4 - payloadSegment.length % 4) % 4,
            padChar = '='
        )

        return runCatching {
            val payload = Base64.UrlSafe.decode(normalizedPayload).decodeToString()
            Json.parseToJsonElement(payload)
                .jsonObject["userId"]
                ?.jsonPrimitive
                ?.contentOrNull
                ?.takeIf { it.isNotBlank() }
        }.getOrNull()
    }
}
