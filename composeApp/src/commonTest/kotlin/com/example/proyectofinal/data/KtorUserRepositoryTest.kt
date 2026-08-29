package com.example.proyectofinal.data

import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.EnumColumnAdapter
import app.cash.sqldelight.db.SqlDriver
import com.example.proyectofinal.db.*
import com.example.proyectofinal.di.ApiConfig
import com.example.proyectofinal.di.TokenStore
import com.example.proyectofinal.di.userRoleColumnAdapter
import com.example.proyectofinal.domain.AuthRepository
import com.example.proyectofinal.domain.AuthSession
import com.example.proyectofinal.models.ExerciseAttemptRequest
import com.example.proyectofinal.models.ExerciseAttemptResponse
import com.example.proyectofinal.models.MultipleChoiceSubmission
import com.example.proyectofinal.models.UpdateIdentityRequest
import com.example.proyectofinal.models.User
import com.example.proyectofinal.models.UserProgress
import com.example.proyectofinal.models.UserRole
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.bearerAuth
import io.ktor.http.*
import io.ktor.http.content.TextContent
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class KtorUserRepositoryTest {
    private lateinit var database: AppDatabase
    private lateinit var driver: SqlDriver
    private val apiConfig = ApiConfig("https://example.test")
    private val json = Json { ignoreUnknownKeys = true }

    @BeforeTest
    fun setup() {
        driver = createTestDriver()

        val intAdapter = object : ColumnAdapter<Int, Long> {
            override fun decode(databaseValue: Long): Int = databaseValue.toInt()
            override fun encode(value: Int): Long = value.toLong()
        }

        database = AppDatabase(
            driver = driver,
            CourseEntityAdapter = CourseEntity.Adapter(
                schoolYearAdapter = intAdapter,
                durationMinutesAdapter = intAdapter,
                xpRewardAdapter = intAdapter
            ),
            ExerciseEntityAdapter = ExerciseEntity.Adapter(
                typeAdapter = EnumColumnAdapter()
            ),
            UserProgressEntityAdapter = UserProgressEntity.Adapter(
                totalScoreAdapter = intAdapter
            ),
            UserEntityAdapter = UserEntity.Adapter(
                roleAdapter = userRoleColumnAdapter
            )
        )
    }

    @Test
    fun `user role adapter decodes legacy learner rows and preserves canonical student rows`() {
        driver.execute(
            null,
            "INSERT INTO UserEntity(id, name, email, role) VALUES (?, ?, ?, ?)",
            4
        ) {
            var parameterIndex = 0
            bindString(parameterIndex++, "legacy-user")
            bindString(parameterIndex++, "Legacy User")
            bindString(parameterIndex++, "legacy@example.com")
            bindString(parameterIndex++, "LEARNER")
        }

        val legacyUser = database.appDatabaseQueries.selectUserById("legacy-user").executeAsOne()
        assertEquals(UserRole.STUDENT, legacyUser.role)

        database.appDatabaseQueries.insertUser(
            id = "student-user",
            name = "Student User",
            email = "student@example.com",
            role = UserRole.STUDENT
        )

        val studentUser = database.appDatabaseQueries.selectUserById("student-user").executeAsOne()
        assertEquals(UserRole.STUDENT, studentUser.role)
    }

    @Test
    fun `getCurrentUser fetches from API and returns user`() = runTest {
        val mockUser = User(
            id = "user-1",
            name = "John Doe",
            email = "john@example.com",
            role = UserRole.STUDENT
        )

        val mockEngine = MockEngine { request ->
            when (request.url.encodedPath) {
                "/users/user-1" -> respond(
                    content = json.encodeToString(mockUser),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                else -> error("Unexpected request: ${request.url.encodedPath}")
            }
        }

        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(json)
            }
        }

        val api = UserApi(httpClient, apiConfig)
        val repository = KtorUserRepository(api, database, TestTokenStore(tokenForUser("user-1")), TestAuthRepository)

        val user = repository.getCurrentUser()

        assertEquals("John Doe", user?.name)
        assertEquals("john@example.com", user?.email)
        assertEquals(UserRole.STUDENT, user?.role)

        val dbUser = database.appDatabaseQueries.selectUserById("user-1").executeAsOneOrNull()
        assertEquals("John Doe", dbUser?.name)
        assertEquals("john@example.com", dbUser?.email)
    }

    @Test
    fun `getUserRole fetches user role from API`() = runTest {
        val userId = "user-teacher"
        val mockUser = User(
            id = userId,
            name = "Teacher User",
            email = "teacher@example.com",
            role = UserRole.TEACHER
        )

        val mockEngine = MockEngine { request ->
            when (request.url.encodedPath) {
                "/users/$userId" -> respond(
                    content = json.encodeToString(mockUser),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                else -> error("Unexpected request: ${request.url.encodedPath}")
            }
        }

        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(json)
            }
        }

        val api = UserApi(httpClient, apiConfig)
        val repository = KtorUserRepository(api, database, TestTokenStore(), TestAuthRepository)

        val role = repository.getUserRole(userId)

        assertEquals(UserRole.TEACHER, role)
    }

    @Test
    fun `getUserRole returns STUDENT as default on failure`() = runTest {
        val userId = "unknown-user"

        val mockEngine = MockEngine { _ ->
            respond(
                content = "",
                status = HttpStatusCode.NotFound,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(json)
            }
        }

        val api = UserApi(httpClient, apiConfig)
        val repository = KtorUserRepository(api, database, TestTokenStore(), TestAuthRepository)

        val role = repository.getUserRole(userId)

        assertEquals(UserRole.STUDENT, role)
    }

    @Test
    fun `updateUser puts to API and updates locally`() = runTest {
        val user = User(
            id = "user-update",
            name = "Original Name",
            email = "original@example.com",
            role = UserRole.STUDENT
        )

        database.appDatabaseQueries.insertUser(
            id = user.id,
            name = user.name,
            email = user.email,
            role = user.role
        )

        val updatedUser = user.copy(name = "Updated Name", email = "updated@example.com")
        var updateCalled = false

        val mockEngine = MockEngine { request ->
            when (request.url.encodedPath) {
                "/users/${updatedUser.id}" -> {
                    updateCalled = true
                    respond(
                        content = "",
                        status = HttpStatusCode.NoContent,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
                else -> error("Unexpected request: ${request.url.encodedPath}")
            }
        }

        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(json)
            }
        }

        val api = UserApi(httpClient, apiConfig)
        val repository = KtorUserRepository(api, database, TestTokenStore(), TestAuthRepository)

        repository.updateUser(updatedUser)

        assertEquals(true, updateCalled)

        val dbUser = database.appDatabaseQueries.selectUserById("user-update").executeAsOneOrNull()
        assertEquals("Updated Name", dbUser?.name)
        assertEquals("updated@example.com", dbUser?.email)
    }

    @Test
    fun `identity update uses authoritative response for local and authenticated state`() = runTest {
        val authoritative = User(
            id = "user-update",
            name = "Canonical Name",
            email = "canonical@example.com",
            role = UserRole.STUDENT
        )
        val tokenStore = TestTokenStore(tokenForUser(authoritative.id))
        val mockEngine = MockEngine { request ->
            val payload = json.parseToJsonElement((request.body as TextContent).text).jsonObject
            assertEquals("/me", request.url.encodedPath)
            assertEquals(HttpMethod.Put, request.method)
            assertEquals(setOf("name", "email"), payload.keys)
            assertFalse("role" in payload)
            respond(
                content = json.encodeToString(authoritative),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(json) }
        }
        val authRepository = KtorAuthRepository(AuthApi(httpClient, apiConfig), tokenStore)
        val repository = KtorUserRepository(
            UserApi(httpClient, apiConfig),
            database,
            tokenStore,
            authRepository
        )

        val result = repository.updateIdentity(
            UpdateIdentityRequest(name = "Requested Name", email = "requested@example.com")
        )

        assertEquals(authoritative, result)
        assertEquals(authoritative, authRepository.session.value.user)
        val local = database.appDatabaseQueries.selectUserById(authoritative.id).executeAsOne()
        assertEquals(authoritative.name, local.name)
        assertEquals(authoritative.email, local.email)
    }

    @Test
    fun `identity update binds request authorization to captured session`() = runTest {
        val originalToken = tokenForUser("user-original")
        val replacementToken = tokenForUser("user-replacement")
        val tokenStore = TestTokenStore(originalToken)
        var authorization: String? = null
        val httpClient = HttpClient(MockEngine { request ->
            authorization = request.headers[HttpHeaders.Authorization]
            respond(
                content = json.encodeToString(User("user-original", "Name", "name@example.com", UserRole.STUDENT)),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }) {
            defaultRequest {
                tokenStore.accessToken = replacementToken
                bearerAuth(requireNotNull(tokenStore.accessToken))
            }
            install(ContentNegotiation) { json(json) }
        }
        val authRepository = KtorAuthRepository(AuthApi(httpClient, apiConfig), tokenStore)
        val repository = KtorUserRepository(UserApi(httpClient, apiConfig), database, tokenStore, authRepository)

        repository.updateIdentity(UpdateIdentityRequest("Name", "name@example.com"))

        assertEquals("Bearer $originalToken", authorization)
    }

    @Test
    fun `identity update keeps authoritative session when local cache fails`() = runTest {
        val authoritative = User(
            id = "user-update",
            name = "Canonical Name",
            email = "canonical@example.com",
            role = UserRole.STUDENT
        )
        val tokenStore = TestTokenStore(tokenForUser(authoritative.id))
        val httpClient = HttpClient(MockEngine {
            respond(
                content = json.encodeToString(authoritative),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }) {
            install(ContentNegotiation) { json(json) }
        }
        val authRepository = KtorAuthRepository(AuthApi(httpClient, apiConfig), tokenStore)
        val repository = KtorUserRepository(UserApi(httpClient, apiConfig), database, tokenStore, authRepository)
        driver.close()

        val result = repository.updateIdentity(UpdateIdentityRequest("Requested Name", "requested@example.com"))

        assertEquals(authoritative, result)
        assertEquals(authoritative, authRepository.session.value.user)
    }

    @Test
    fun `logout prevents a stale identity response from restoring the session`() {
        val tokenStore = TestTokenStore(tokenForUser("user-update"))
        val authRepository = KtorAuthRepository(
            AuthApi(HttpClient(MockEngine { error("Not used") }), apiConfig),
            tokenStore
        )
        val expectedToken = authRepository.session.value.token

        authRepository.logout()
        authRepository.replaceSessionUser(
            User("user-update", "Stale Name", "stale@example.com", UserRole.STUDENT),
            expectedToken
        )

        assertEquals(AuthSession(), authRepository.session.value)
        assertEquals(null, tokenStore.accessToken)
    }

    @Test
    fun `attemptExercise posts typed attempt request to attempt endpoint`() = runTest {
        val exerciseId = "exercise-attempt"
        val requestBody = ExerciseAttemptRequest(
            exerciseId = exerciseId,
            submission = MultipleChoiceSubmission(selectedOptionId = "option-b"),
            score = 100
        )
        var capturedPath: String? = null
        var capturedMethod: HttpMethod? = null
        var capturedRequest: ExerciseAttemptRequest? = null

        val mockEngine = MockEngine { request ->
            capturedPath = request.url.encodedPath
            capturedMethod = request.method
            capturedRequest = json.decodeFromString((request.body as TextContent).text)

            respond(
                content = json.encodeToString(
                    ExerciseAttemptResponse(
                        exerciseId = exerciseId,
                        lessonId = "lesson-progress",
                        isCorrect = false,
                        message = "Incorrect answer. Try again.",
                        progress = UserProgress(userId = "user-progress")
                    )
                ),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(json)
            }
        }

        val api = UserApi(httpClient, apiConfig)

        api.attemptExercise(
            exerciseId = exerciseId,
            submission = requestBody.submission,
            score = requestBody.score
        )

        assertEquals("/exercises/$exerciseId/attempt", capturedPath)
        assertEquals(HttpMethod.Post, capturedMethod)
        assertEquals(requestBody, capturedRequest)
    }

    @Test
    fun `getUserProgress merges completed exercises locally`() = runTest {
        val userId = "user-progress"
        val progress = UserProgress(
            userId = userId,
            completedLessonIds = setOf("lesson-1"),
            completedExerciseIds = setOf("exercise-1"),
            totalScore = 15,
            enrolledCourseIds = setOf("course-1")
        )

        database.appDatabaseQueries.insertUser(
            id = userId,
            name = "Progress User",
            email = "progress@example.com",
            role = UserRole.STUDENT
        )
        database.appDatabaseQueries.insertCourse(
            id = "course-1",
            title = "Course",
            description = "Description",
            creatorId = "teacher-1",
            isOfficial = true,
            schoolYear = 1,
            joinCode = null,
            topic = null,
            difficulty = null,
            durationMinutes = null,
            xpReward = null
        )
        database.appDatabaseQueries.insertLesson(
            id = "lesson-1",
            courseId = "course-1",
            creatorId = null,
            title = "Lesson",
            theoryContent = "Theory"
        )
        database.appDatabaseQueries.insertExercise(
            id = "exercise-1",
            lessonId = "lesson-1",
            title = "Question",
            type = com.example.proyectofinal.models.ExerciseType.MULTIPLE_CHOICE,
            payload = ExercisePayloadJson.legacyPayloadJson(
                type = com.example.proyectofinal.models.ExerciseType.MULTIPLE_CHOICE,
                optionsCsv = "A,B",
                correctAnswer = "Answer"
            )
        )

        val mockEngine = MockEngine { request ->
            when (request.url.encodedPath) {
                "/progress/$userId" -> respond(
                    content = json.encodeToString(progress),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                else -> error("Unexpected request: ${request.url.encodedPath}")
            }
        }

        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(json)
            }
        }

        val repository = KtorUserRepository(UserApi(httpClient, apiConfig), database, TestTokenStore(), TestAuthRepository)

        val syncedProgress = repository.getUserProgress(userId)

        assertEquals(progress, syncedProgress)
        assertEquals(progress.totalScore, database.appDatabaseQueries.selectProgressByUserId(userId).executeAsOne().totalScore)
        assertEquals(progress.completedLessonIds, database.appDatabaseQueries.selectCompletedLessonsByUserId(userId).executeAsList().toSet())
        assertEquals(progress.completedExerciseIds, database.appDatabaseQueries.selectCompletedExercisesByUserId(userId).executeAsList().toSet())
        assertEquals(progress.enrolledCourseIds, database.appDatabaseQueries.selectEnrolledCoursesByUserId(userId).executeAsList().toSet())
    }

    @Test
    fun `attemptExercise duplicate sync preserves single local record`() = runTest {
        val userId = "user-progress"
        val exerciseId = "exercise-1"
        val response = ExerciseAttemptResponse(
            exerciseId = exerciseId,
            lessonId = "lesson-1",
            isCorrect = true,
            lessonCompleted = false,
            progress = UserProgress(
                userId = userId,
                completedLessonIds = setOf("lesson-1"),
                completedExerciseIds = setOf(exerciseId),
                totalScore = 10,
                enrolledCourseIds = setOf("course-1")
            )
        )

        database.appDatabaseQueries.insertUser(
            id = userId,
            name = "Progress User",
            email = "progress@example.com",
            role = UserRole.STUDENT
        )
        database.appDatabaseQueries.insertCourse(
            id = "course-1",
            title = "Course",
            description = "Description",
            creatorId = "teacher-1",
            isOfficial = true,
            schoolYear = 1,
            joinCode = null,
            topic = null,
            difficulty = null,
            durationMinutes = null,
            xpReward = null
        )
        database.appDatabaseQueries.insertLesson(
            id = "lesson-1",
            courseId = "course-1",
            creatorId = null,
            title = "Lesson",
            theoryContent = "Theory"
        )
        database.appDatabaseQueries.insertExercise(
            id = exerciseId,
            lessonId = "lesson-1",
            title = "Question",
            type = com.example.proyectofinal.models.ExerciseType.MULTIPLE_CHOICE,
            payload = ExercisePayloadJson.legacyPayloadJson(
                type = com.example.proyectofinal.models.ExerciseType.MULTIPLE_CHOICE,
                optionsCsv = "A,B",
                correctAnswer = "Answer"
            )
        )

        val mockEngine = MockEngine { request ->
            when (request.url.encodedPath) {
                "/exercises/$exerciseId/attempt" -> respond(
                    content = json.encodeToString(response),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                else -> error("Unexpected request: ${request.url.encodedPath}")
            }
        }

        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(json)
            }
        }

        val repository = KtorUserRepository(UserApi(httpClient, apiConfig), database, TestTokenStore(), TestAuthRepository)

        repository.attemptExercise(exerciseId = exerciseId, submission = MultipleChoiceSubmission(selectedOptionId = "Answer"), score = 10)
        repository.attemptExercise(exerciseId = exerciseId, submission = MultipleChoiceSubmission(selectedOptionId = "Answer"), score = 99)

        assertEquals(listOf(exerciseId), database.appDatabaseQueries.selectCompletedExercisesByUserId(userId).executeAsList())
        assertEquals(10, database.appDatabaseQueries.selectProgressByUserId(userId).executeAsOne().totalScore)
    }
}

private class TestTokenStore(
    override var accessToken: String? = null
) : TokenStore

private object TestAuthRepository : AuthRepository {
    override val session = MutableStateFlow(AuthSession())
    override suspend fun login(email: String, password: String) = error("Not used")
    override suspend fun register(name: String, email: String, password: String) = error("Not used")
    override fun replaceSessionUser(user: User, expectedToken: String?) = Unit
    override fun logout() = Unit
}

@OptIn(ExperimentalEncodingApi::class)
private fun tokenForUser(userId: String): String {
    val header = Base64.UrlSafe.encode("{}".encodeToByteArray()).trimEnd('=')
    val payload = Base64.UrlSafe.encode("{\"userId\":\"$userId\"}".encodeToByteArray()).trimEnd('=')
    return "$header.$payload.signature"
}
