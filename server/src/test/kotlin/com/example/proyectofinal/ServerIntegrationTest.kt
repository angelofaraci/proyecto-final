package com.example.proyectofinal

import at.favre.lib.crypto.bcrypt.BCrypt
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.example.proyectofinal.database.CompletedExercises
import com.example.proyectofinal.database.CompletedLessons
import com.example.proyectofinal.database.Courses
import com.example.proyectofinal.database.DatabaseFactory
import com.example.proyectofinal.database.EnrolledCourses
import com.example.proyectofinal.database.Exercises
import com.example.proyectofinal.database.Lessons
import com.example.proyectofinal.database.Users
import com.example.proyectofinal.database.UserProgress as UserProgressTable
import com.example.proyectofinal.models.AuthResponse
import com.example.proyectofinal.models.ChoiceOption
import com.example.proyectofinal.models.CompleteLessonRequest
import com.example.proyectofinal.models.Course
import com.example.proyectofinal.models.CourseStudentsProgressResponse
import com.example.proyectofinal.models.ExerciseAttemptRequest
import com.example.proyectofinal.models.ExerciseAttemptResponse
import com.example.proyectofinal.models.CreateExerciseRequest
import com.example.proyectofinal.models.CreateLessonRequest
import com.example.proyectofinal.models.Exercise
import com.example.proyectofinal.models.ExerciseType
import com.example.proyectofinal.models.InputValueSubmission
import com.example.proyectofinal.models.Lesson
import com.example.proyectofinal.models.MultiSelectSubmission
import com.example.proyectofinal.models.MultipleChoicePayload
import com.example.proyectofinal.models.MultipleChoiceSubmission
import com.example.proyectofinal.models.RegisterRequest
import com.example.proyectofinal.models.TheoryUpdateRequest
import com.example.proyectofinal.models.UpdateExerciseRequest
import com.example.proyectofinal.models.UpdateLessonRequest
import com.example.proyectofinal.models.User
import com.example.proyectofinal.models.UserRole
import com.example.proyectofinal.plugins.Security
import com.example.proyectofinal.service.ExercisePayloadSupport
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ServerIntegrationTest {
    private fun testDbUrl(): String =
        "jdbc:h2:mem:${UUID.randomUUID()};MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE"

    private fun setupTestDatabase(jwtSecret: String = "test-jwt-secret") {
        System.setProperty("jwt.secret", jwtSecret)
        DatabaseFactory.init(
            url = testDbUrl(),
            driver = "org.h2.Driver",
            user = "sa",
            password = ""
        )
    }

    @Test
    fun `seed uses configured admin credentials hashes password and avoids secret output`() = testApplication {
        val jwtSecret = "seed-jwt-secret"
        val adminId = "seed-admin"
        val adminName = "Seeded Admin"
        val adminEmail = "seed-admin@example.com"
        val adminPassword = "SeedPassword123!"

        val output = withSystemProperties(
            mapOf(
                "seed.admin.id" to adminId,
                "seed.admin.name" to adminName,
                "seed.admin.email" to adminEmail,
                "seed.admin.password" to adminPassword
            )
        ) {
            setupTestDatabase(jwtSecret = jwtSecret)

            captureStandardOut {
                application {
                    module(initDatabase = false, seedData = true)
                }

                assertEquals(HttpStatusCode.Unauthorized, client.get("/courses/official").status)
            }
        }

        transaction {
            val seededAdmin = Users.selectAll().where { Users.email eq adminEmail }.single()
            val passwordHash = seededAdmin[Users.passwordHash]

            assertEquals(adminId, seededAdmin[Users.id])
            assertEquals(adminName, seededAdmin[Users.name])
            assertEquals("ADMIN", seededAdmin[Users.role])
            assertTrue(passwordHash != adminPassword)
            assertTrue(passwordHash.startsWith("\$2"))
            assertTrue(BCrypt.verifyer().verify(adminPassword.toCharArray(), passwordHash).verified)
        }

        assertTrue(output.contains("Seeding official courses..."))
        assertTrue(output.contains("Seed data created successfully!"))
        assertTrue(!output.contains(adminPassword))
        assertTrue(!output.contains(jwtSecret))
    }

    @Test
    fun `register returns token and persisted user`() = testApplication {
        setupTestDatabase()

        application {
            module(initDatabase = false, seedData = false)
        }

        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val response = client.post("/auth/register") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(
                RegisterRequest(
                    name = "Test User",
                    email = "test@example.com",
                    password = "secret123"
                )
            )
        }

        assertEquals(HttpStatusCode.OK, response.status)

        val auth = response.body<AuthResponse>()
        assertEquals("Test User", auth.user.name)
        assertEquals("test@example.com", auth.user.email)
        assertEquals(UserRole.STUDENT, auth.user.role)
        assertTrue(auth.token.isNotBlank())

        transaction {
            val savedUser = Users.selectAll().where { Users.email eq "test@example.com" }.single()
            assertEquals("Test User", savedUser[Users.name])
            assertEquals("STUDENT", savedUser[Users.role])
        }
    }

    @Test
    fun `public registration ignores legacy role payload and persists student`() = testApplication {
        setupTestDatabase()

        application {
            module(initDatabase = false, seedData = false)
        }

        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val response = client.post("/auth/register") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(
                """
                {
                  "name": "Legacy Admin Attempt",
                  "email": "legacy-admin@example.com",
                  "password": "secret123",
                  "role": "ADMIN"
                }
                """.trimIndent()
            )
        }

        assertEquals(HttpStatusCode.OK, response.status)

        val auth = response.body<AuthResponse>()
        assertEquals(UserRole.STUDENT, auth.user.role)

        transaction {
            val savedUser = Users.selectAll().where { Users.email eq "legacy-admin@example.com" }.single()
            assertEquals("STUDENT", savedUser[Users.role])
        }
    }

    @Test
    fun `protected courses route rejects missing token`() = testApplication {
        setupTestDatabase()

        application {
            module(initDatabase = false, seedData = false)
        }

        val response = client.get("/courses/official")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `protected read routes return 404 for missing resources with valid token`() = testApplication {
        setupTestDatabase()

        application {
            module(initDatabase = false, seedData = false)
        }

        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val token = registerUserAndGetToken(client, email = "missing-resource@example.com")

        listOf(
            "/courses/missing-course",
            "/courses/missing-course/lessons",
            "/lessons/missing-lesson",
            "/lessons/missing-lesson/exercises"
        ).forEach { path ->
            assertEquals(HttpStatusCode.NotFound, client.get(path) { bearerAuth(token) }.status, path)
        }
    }

    @Test
    fun `signed token without userId is rejected`() = testApplication {
        setupTestDatabase()

        application {
            module(initDatabase = false, seedData = false)
        }

        val tokenWithoutUserId = JWT.create()
            .withIssuer(Security.ISSUER)
            .withClaim("role", UserRole.STUDENT.name)
            .sign(Algorithm.HMAC256("test-jwt-secret"))

        val response = client.get("/courses/official") {
            bearerAuth(tokenWithoutUserId)
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `legacy learner role values still authenticate and hydrate as student`() = testApplication {
        setupTestDatabase()

        application {
            module(initDatabase = false, seedData = false)
        }

        transaction {
            Users.insert {
                it[id] = "legacy-student"
                it[name] = "Legacy Student"
                it[email] = "legacy-student@example.com"
                it[passwordHash] = "hash"
                it[role] = "LEARNER"
            }
        }

        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val response = client.get("/users/legacy-student") {
            bearerAuth(Security.generateToken("legacy-student", "LEARNER"))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(UserRole.STUDENT, response.body<User>().role)
    }

    @Test
    fun `authorized user can fetch official courses`() = testApplication {
        setupTestDatabase()

        application {
            module(initDatabase = false, seedData = false)
        }

        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val token = registerUserAndGetToken(client, email = "courses@example.com")
        seedOfficialCourse(schoolYear = 3)

        val response = client.get("/courses/official") {
            bearerAuth(token)
        }

        assertEquals(HttpStatusCode.OK, response.status)

        val courses = response.body<List<Course>>()
        assertEquals(1, courses.size)
        assertEquals("Official Test Course", courses.single().title)
        assertTrue(courses.single().isOfficial)
        assertEquals(3, courses.single().schoolYear)
    }

    @Test
    fun `student cannot create a course`() = testApplication {
        setupTestDatabase()
        application { module(initDatabase = false, seedData = false) }

        transaction {
            Users.insert {
                it[id] = "student-course-creator"
                it[name] = "Student Creator"
                it[email] = "student-course-creator@example.com"
                it[passwordHash] = "hash"
                it[role] = UserRole.STUDENT.name
            }
        }

        val response = client.post("/courses") {
            bearerAuth(Security.generateToken("student-course-creator", UserRole.STUDENT.name))
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(
                """{"id":"student-course","title":"Forbidden","description":"Forbidden","creatorId":"student-course-creator"}"""
            )
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
        transaction {
            assertTrue(Courses.selectAll().where { Courses.id eq "student-course" }.none())
        }
    }

    @Test
    fun `teacher course creation derives ownership and official status from server policy`() = testApplication {
        setupTestDatabase()
        application { module(initDatabase = false, seedData = false) }

        val jsonClient = createClient {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }

        transaction {
            Users.insert {
                it[id] = "teacher-course-creator"
                it[name] = "Teacher Creator"
                it[email] = "teacher-course-creator@example.com"
                it[passwordHash] = "hash"
                it[role] = UserRole.TEACHER.name
            }
        }

        val response = jsonClient.post("/courses") {
            bearerAuth(Security.generateToken("teacher-course-creator", UserRole.TEACHER.name))
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(
                """{"id":"teacher-created-course","title":"Teacher Course","description":"Teacher owned","creatorId":"attacker-selected-owner","isOfficial":true}"""
            )
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val created = response.body<Course>()
        assertEquals("teacher-course-creator", created.creatorId)
        assertEquals(false, created.isOfficial)
        transaction {
            val persisted = Courses.selectAll().where { Courses.id eq "teacher-created-course" }.single()
            assertEquals("teacher-course-creator", persisted[Courses.creatorId])
            assertEquals(false, persisted[Courses.isOfficial])
        }
    }

    @Test
    fun `admin can create a regular course owned by the authenticated principal`() = testApplication {
        setupTestDatabase()
        application { module(initDatabase = false, seedData = false) }

        val jsonClient = createClient {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }

        transaction {
            Users.insert {
                it[id] = "regular-course-admin"
                it[name] = "Course Admin"
                it[email] = "regular-course-admin@example.com"
                it[passwordHash] = "hash"
                it[role] = UserRole.ADMIN.name
            }
        }

        val response = jsonClient.post("/courses") {
            bearerAuth(Security.generateToken("regular-course-admin", UserRole.ADMIN.name))
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(
                """{"id":"admin-regular-course","title":"Admin Regular Course","description":"Non-official","creatorId":"another-user","isOfficial":true}"""
            )
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val created = response.body<Course>()
        assertEquals("regular-course-admin", created.creatorId)
        assertEquals(false, created.isOfficial)
    }

    @Test
    fun `course progress roster is visible only to its teacher owner and admins`() = testApplication {
        setupTestDatabase()
        application { module(initDatabase = false, seedData = false) }
        val jsonClient = createClient {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }

        transaction {
            listOf(
                Triple("roster-owner", "TEACHER", "owner@example.com"),
                Triple("roster-other", "TEACHER", "other@example.com"),
                Triple("roster-student", "STUDENT", "student@example.com"),
                Triple("roster-admin", "ADMIN", "admin@example.com")
            ).forEach { (userId, userRole, userEmail) ->
                Users.insert {
                    it[id] = userId; it[name] = userId; it[email] = userEmail
                    it[passwordHash] = "hash"; it[role] = userRole
                }
            }
            Courses.insert {
                it[id] = "secured-roster-course"; it[title] = "Secured roster"
                it[description] = "Roster"; it[creatorId] = "roster-owner"; it[isOfficial] = false
            }
            EnrolledCourses.insert {
                it[userId] = "roster-student"; it[courseId] = "secured-roster-course"
            }
        }

        suspend fun requestAs(userId: String, role: UserRole) =
            jsonClient.get("/courses/secured-roster-course/students/progress") {
                bearerAuth(Security.generateToken(userId, role.name))
            }

        val ownerResponse = requestAs("roster-owner", UserRole.TEACHER)
        val otherTeacherResponse = requestAs("roster-other", UserRole.TEACHER)
        val studentResponse = requestAs("roster-student", UserRole.STUDENT)
        val adminResponse = requestAs("roster-admin", UserRole.ADMIN)
        val missingResponse = jsonClient.get("/courses/missing-course/students/progress") {
            bearerAuth(Security.generateToken("roster-owner", UserRole.TEACHER.name))
        }

        assertEquals(HttpStatusCode.OK, ownerResponse.status)
        assertEquals(listOf("roster-student"), ownerResponse.body<CourseStudentsProgressResponse>().students.map { it.studentId })
        assertEquals(HttpStatusCode.Forbidden, otherTeacherResponse.status)
        assertEquals(HttpStatusCode.Forbidden, studentResponse.status)
        assertEquals(HttpStatusCode.OK, adminResponse.status)
        assertEquals(HttpStatusCode.NotFound, missingResponse.status)
    }

    @Test
    fun `student matching legacy course creator id cannot read progress roster`() = testApplication {
        setupTestDatabase()
        application { module(initDatabase = false, seedData = false) }

        transaction {
            Users.insert {
                it[id] = "legacy-student-owner"; it[name] = "Legacy Student"
                it[email] = "legacy-student-owner@example.com"; it[passwordHash] = "hash"
                it[role] = UserRole.STUDENT.name
            }
            Courses.insert {
                it[id] = "legacy-student-owned-course"; it[title] = "Legacy Course"
                it[description] = "Imported data"; it[creatorId] = "legacy-student-owner"
                it[isOfficial] = false
            }
            EnrolledCourses.insert {
                it[userId] = "legacy-student-owner"; it[courseId] = "legacy-student-owned-course"
            }
        }

        val response = client.get("/courses/legacy-student-owned-course/students/progress") {
            bearerAuth(Security.generateToken("legacy-student-owner", UserRole.STUDENT.name))
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
        assertEquals("Forbidden", response.body<String>())
    }

    @Test
    fun `official courses support school year filtering and reject invalid filters`() = testApplication {
        setupTestDatabase()

        application {
            module(initDatabase = false, seedData = false)
        }

        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val token = registerUserAndGetToken(client, email = "school-year@example.com")
        seedOfficialCourse(courseId = "official-year-3", title = "Year 3", schoolYear = 3)
        seedOfficialCourse(courseId = "official-year-4", title = "Year 4", schoolYear = 4)

        val filteredResponse = client.get("/courses/official?schoolYear=3") {
            bearerAuth(token)
        }
        val emptyResponse = client.get("/courses/official?schoolYear=6") {
            bearerAuth(token)
        }
        val invalidResponse = client.get("/courses/official?schoolYear=third") {
            bearerAuth(token)
        }

        assertEquals(HttpStatusCode.OK, filteredResponse.status)
        assertEquals(listOf("official-year-3"), filteredResponse.body<List<Course>>().map { it.id })
        assertEquals(HttpStatusCode.OK, emptyResponse.status)
        assertTrue(emptyResponse.body<List<Course>>().isEmpty())
        assertEquals(HttpStatusCode.BadRequest, invalidResponse.status)
    }

    @Test
    fun `official courses include discovery metadata in responses`() = testApplication {
        setupTestDatabase()

        application {
            module(initDatabase = false, seedData = false)
        }

        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val token = registerUserAndGetToken(client, email = "catalog@example.com")
        seedOfficialCourse(
            courseId = "official-with-discovery",
            title = "Discovery Course",
            schoolYear = 3,
            topic = "Fracciones",
            difficulty = "Fácil",
            durationMinutes = 15,
            xpReward = 50
        )

        val response = client.get("/courses/official?schoolYear=3") {
            bearerAuth(token)
        }

        assertEquals(HttpStatusCode.OK, response.status)

        val course = response.body<List<Course>>().single()
        assertEquals("official-with-discovery", course.id)
        assertEquals("Fracciones", course.topic)
        assertEquals("Fácil", course.difficulty)
        assertEquals(15, course.durationMinutes)
        assertEquals(50, course.xpReward)
    }

    @Test
    fun `exercise completion uses authenticated learner identity and updates progress`() = testApplication {
        setupTestDatabase()

        application {
            module(initDatabase = false, seedData = false)
        }

        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
        val token = registerUserAndGetToken(client, email = "progress@example.com")
        registerUserAndGetToken(client, email = "other-progress@example.com")
        val userId = transaction { Users.selectAll().where { Users.email eq "progress@example.com" }.single()[Users.id] }
        val otherUserId = transaction { Users.selectAll().where { Users.email eq "other-progress@example.com" }.single()[Users.id] }
        seedOfficialCourseWithLesson(courseId = "course-progress", lessonId = "lesson-1", schoolYear = 3)
        transaction {
            Exercises.insert {
                it[id] = "exercise-1"
                it[lessonId] = "lesson-1"
                it[question] = "2 + 2 = ?"
                it[options] = "3,4"
                it[correctAnswer] = "4"
                it[type] = "MULTIPLE_CHOICE"
                it[payload] = ExercisePayloadSupport.legacyPayloadJson(
                    type = ExerciseType.MULTIPLE_CHOICE,
                    optionsCsv = "3,4",
                    correctAnswer = "4"
                )
            }
        }
        val updateResponse = client.post("/exercises/exercise-1/attempt") {
            bearerAuth(token)
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(
                ExerciseAttemptRequest(
                    exerciseId = "exercise-1",
                    submission = MultipleChoiceSubmission(selectedOptionId = "4"),
                    score = 15
                )
            )
        }
        assertEquals(HttpStatusCode.OK, updateResponse.status)
        val completion = updateResponse.body<ExerciseAttemptResponse>()
        assertEquals(true, completion.isCorrect)
        assertEquals(true, completion.lessonCompleted)
        assertEquals(userId, completion.progress.userId)
        assertEquals(setOf("exercise-1"), completion.progress.completedExerciseIds)
        assertEquals(setOf("lesson-1"), completion.progress.completedLessonIds)
        assertEquals(15, completion.progress.totalScore)
        transaction {
            assertEquals(
                1L,
                CompletedExercises.selectAll()
                    .where {
                        (CompletedExercises.userId eq userId) and
                            (CompletedExercises.exerciseId eq "exercise-1")
                    }
                    .count()
            )
            assertEquals(
                0L,
                CompletedExercises.selectAll()
                    .where { CompletedExercises.userId eq otherUserId }
                    .count()
            )
            val completed = CompletedLessons.selectAll().where { CompletedLessons.userId eq userId }.single()
            assertEquals("lesson-1", completed[CompletedLessons.lessonId])
        }
    }

    @Test
    fun `exercise completion endpoint is gone and learner progress endpoint stays deprecated`() = testApplication {
        setupTestDatabase()

        application {
            module(initDatabase = false, seedData = false)
        }

        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
        val token = registerUserAndGetToken(client, email = "mismatch@example.com")
        val userId = transaction {
            Users.selectAll().where { Users.email eq "mismatch@example.com" }.single()[Users.id]
        }
        seedOfficialCourseWithLesson(courseId = "course-mismatch", lessonId = "lesson-mismatch", schoolYear = 3)
        transaction {
            Exercises.insert {
                it[id] = "exercise-real"
                it[lessonId] = "lesson-mismatch"
                it[question] = "Question"
                it[options] = "a,b"
                it[correctAnswer] = "a"
                it[type] = "MULTIPLE_CHOICE"
                it[payload] = ExercisePayloadSupport.legacyPayloadJson(
                    type = ExerciseType.MULTIPLE_CHOICE,
                    optionsCsv = "a,b",
                    correctAnswer = "a"
                )
            }
        }
        val response = client.post("/exercises/exercise-real/complete") {
            bearerAuth(token)
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("{}")
        }
        val deprecatedResponse = client.post("/progress") {
            bearerAuth(token)
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(CompleteLessonRequest(userId = userId, lessonId = "lesson-legacy", score = 12))
        }
        assertEquals(HttpStatusCode.Gone, response.status)
        assertTrue(response.body<String>().contains("submit /attempt"))
        assertEquals(HttpStatusCode.Gone, deprecatedResponse.status)
        transaction {
            assertEquals(0L, CompletedExercises.selectAll().where { CompletedExercises.userId eq userId }.count())
            assertEquals(0L, CompletedLessons.selectAll().where { CompletedLessons.userId eq userId }.count())
            assertEquals(0L, UserProgressTable.selectAll().where { UserProgressTable.userId eq userId }.count())
        }
    }

    @Test
    fun `attempt endpoint validates wrong and correct typed answers`() = testApplication {
        setupTestDatabase()

        application {
            module(initDatabase = false, seedData = false)
        }

        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val token = registerUserAndGetToken(client, email = "attempts@example.com")
        seedOfficialCourseWithLesson(courseId = "course-attempt", lessonId = "lesson-attempt", schoolYear = 3)

        transaction {
            Exercises.insert {
                it[id] = "exercise-attempt"
                it[lessonId] = "lesson-attempt"
                it[question] = "2 + 2 = ?"
                it[options] = "3,4"
                it[correctAnswer] = "4"
                it[type] = ExerciseType.MULTIPLE_CHOICE.name
                it[payload] = ExercisePayloadSupport.legacyPayloadJson(
                    type = ExerciseType.MULTIPLE_CHOICE,
                    optionsCsv = "3,4",
                    correctAnswer = "4"
                )
            }
        }

        val wrongResponse = client.post("/exercises/exercise-attempt/attempt") {
            bearerAuth(token)
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(
                ExerciseAttemptRequest(
                    exerciseId = "exercise-attempt",
                    submission = MultipleChoiceSubmission(selectedOptionId = "3"),
                    score = 20
                )
            )
        }

        assertEquals(HttpStatusCode.OK, wrongResponse.status)
        val wrongAttempt = wrongResponse.body<ExerciseAttemptResponse>()
        assertEquals(false, wrongAttempt.isCorrect)
        assertEquals(false, wrongAttempt.lessonCompleted)
        assertTrue(wrongAttempt.message?.contains("Incorrect") == true)

        val badSubmission = client.post("/exercises/exercise-attempt/attempt") {
            bearerAuth(token)
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(
                ExerciseAttemptRequest(
                    exerciseId = "exercise-attempt",
                    submission = InputValueSubmission(value = "4")
                )
            )
        }
        assertEquals(HttpStatusCode.BadRequest, badSubmission.status)

        val correctResponse = client.post("/exercises/exercise-attempt/attempt") {
            bearerAuth(token)
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(
                ExerciseAttemptRequest(
                    exerciseId = "exercise-attempt",
                    submission = MultipleChoiceSubmission(selectedOptionId = "4"),
                    score = 20
                )
            )
        }

        assertEquals(HttpStatusCode.OK, correctResponse.status)
        val correctAttempt = correctResponse.body<ExerciseAttemptResponse>()
        assertEquals(true, correctAttempt.isCorrect)
        assertEquals(true, correctAttempt.lessonCompleted)
        assertEquals(20, correctAttempt.progress.totalScore)
    }

    @Test
    fun `learner content hides correct answers`() = testApplication {
        setupTestDatabase()

        application {
            module(initDatabase = false, seedData = false)
        }

        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val learnerToken = registerUserAndGetToken(client, email = "learner-content@example.com")

        transaction {
            val learnerId = Users.selectAll().where { Users.email eq "learner-content@example.com" }.single()[Users.id]

            Courses.insert {
                it[id] = "course-content"
                it[title] = "Content Course"
                it[description] = "Course for content masking"
                it[creatorId] = "teacher-owner"
                it[isOfficial] = false
                it[joinCode] = "CONTENT1"
            }

            Lessons.insert {
                it[id] = "lesson-content"
                it[courseId] = "course-content"
                it[title] = "Lesson Content"
                it[theoryContent] = "Theory"
                it[orderIndex] = 0
            }

            Exercises.insert {
                it[id] = "exercise-content"
                it[lessonId] = "lesson-content"
                it[question] = "2 + 2 = ?"
                it[options] = "3,4,5"
                it[correctAnswer] = "4"
                it[type] = "MULTIPLE_CHOICE"
                it[payload] = ExercisePayloadSupport.legacyPayloadJson(
                    type = ExerciseType.MULTIPLE_CHOICE,
                    optionsCsv = "3,4,5",
                    correctAnswer = "4"
                )
            }

            EnrolledCourses.insert {
                it[userId] = learnerId
                it[courseId] = "course-content"
            }
        }

        val lesson = client.get("/lessons/lesson-content") { bearerAuth(learnerToken) }.body<Lesson>()
        val exercises = client.get("/lessons/lesson-content/exercises") { bearerAuth(learnerToken) }.body<List<Exercise>>()

        assertEquals("", lesson.exercises.single().correctAnswer)
        assertEquals("", exercises.single().correctAnswer)
    }

    @Test
    fun `lesson read route enforces visibility scopes`() = testApplication {
        setupTestDatabase()

        application {
            module(initDatabase = false, seedData = false)
        }

        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val enrolledToken = registerUserAndGetToken(client, email = "enrolled-learner@example.com")
        val enrolledLearnerId = transaction {
            Users.selectAll().where { Users.email eq "enrolled-learner@example.com" }.single()[Users.id]
        }

        seedOfficialCourseWithLesson(
            courseId = "official-visible-course",
            lessonId = "official-visible-lesson",
            schoolYear = 3
        )
        seedOwnedCourseWithLesson(
            courseId = "private-visible-course",
            creatorId = "teacher-owner",
            lessonId = "private-visible-lesson"
        )

        transaction {
            EnrolledCourses.insert {
                it[userId] = enrolledLearnerId
                it[courseId] = "private-visible-course"
            }

            Exercises.insert {
                it[id] = "private-visible-exercise"
                it[lessonId] = "private-visible-lesson"
                it[question] = "Private question"
                it[options] = "a,b"
                it[correctAnswer] = "a"
                it[type] = "MULTIPLE_CHOICE"
                it[payload] = ExercisePayloadSupport.legacyPayloadJson(
                    type = ExerciseType.MULTIPLE_CHOICE,
                    optionsCsv = "a,b",
                    correctAnswer = "a"
                )
            }
        }

        val outsiderLearnerToken = Security.generateToken("learner-outsider", UserRole.STUDENT.name)
        val ownerToken = Security.generateToken("teacher-owner", UserRole.TEACHER.name)
        val otherTeacherToken = Security.generateToken("teacher-other", UserRole.TEACHER.name)
        val adminToken = Security.generateToken("admin-user", UserRole.ADMIN.name)

        val officialVisible = client.get("/lessons/official-visible-lesson") {
            bearerAuth(outsiderLearnerToken)
        }
        val enrolledVisible = client.get("/lessons/private-visible-lesson") {
            bearerAuth(enrolledToken)
        }
        val ownerVisible = client.get("/lessons/private-visible-lesson") {
            bearerAuth(ownerToken)
        }
        val adminVisible = client.get("/lessons/private-visible-lesson") {
            bearerAuth(adminToken)
        }
        val learnerForbidden = client.get("/lessons/private-visible-lesson") {
            bearerAuth(outsiderLearnerToken)
        }
        val teacherForbidden = client.get("/lessons/private-visible-lesson") {
            bearerAuth(otherTeacherToken)
        }
        val courseLessonsForbidden = client.get("/courses/private-visible-course/lessons") {
            bearerAuth(outsiderLearnerToken)
        }
        val courseDetailForbidden = client.get("/courses/private-visible-course") {
            bearerAuth(outsiderLearnerToken)
        }
        val exercisesForbidden = client.get("/lessons/private-visible-lesson/exercises") {
            bearerAuth(outsiderLearnerToken)
        }
        val courseLessonsVisible = client.get("/courses/private-visible-course/lessons") {
            bearerAuth(enrolledToken)
        }
        val courseDetailVisible = client.get("/courses/private-visible-course") {
            bearerAuth(ownerToken)
        }
        val exercisesVisible = client.get("/lessons/private-visible-lesson/exercises") {
            bearerAuth(enrolledToken)
        }

        assertEquals(HttpStatusCode.OK, officialVisible.status)
        assertEquals(HttpStatusCode.OK, enrolledVisible.status)
        assertEquals(HttpStatusCode.OK, ownerVisible.status)
        assertEquals(HttpStatusCode.OK, adminVisible.status)
        assertEquals(HttpStatusCode.Forbidden, learnerForbidden.status)
        assertEquals(HttpStatusCode.Forbidden, teacherForbidden.status)
        assertEquals(HttpStatusCode.Forbidden, courseLessonsForbidden.status)
        assertEquals(HttpStatusCode.Forbidden, courseDetailForbidden.status)
        assertEquals(HttpStatusCode.Forbidden, exercisesForbidden.status)
        assertEquals(HttpStatusCode.OK, courseLessonsVisible.status)
        assertEquals(listOf("private-visible-lesson"), courseLessonsVisible.body<List<Lesson>>().map { it.id })
        assertEquals(HttpStatusCode.OK, courseDetailVisible.status)
        assertEquals(listOf("private-visible-lesson"), courseDetailVisible.body<Course>().lessons.map { it.id })
        assertEquals(HttpStatusCode.OK, exercisesVisible.status)
        assertEquals("", exercisesVisible.body<List<Exercise>>().single().correctAnswer)
    }

    @Test
    fun `lesson mutations require course owner or admin`() = testApplication {
        setupTestDatabase()

        application {
            module(initDatabase = false, seedData = false)
        }

        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        seedOwnedCourseWithLesson(
            courseId = "course-owned-lesson",
            creatorId = "teacher-owner",
            lessonId = "lesson-owned"
        )

        val learnerToken = Security.generateToken("learner-user", UserRole.STUDENT.name)
        val ownerToken = Security.generateToken("teacher-owner", UserRole.TEACHER.name)
        val adminToken = Security.generateToken("admin-user", UserRole.ADMIN.name)

        val forbiddenCreate = client.post("/lessons") {
            bearerAuth(learnerToken)
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(CreateLessonRequest("lesson-forbidden", "course-owned-lesson", "Forbidden", "Forbidden"))
        }
        assertEquals(HttpStatusCode.Forbidden, forbiddenCreate.status)

        val created = client.post("/lessons") {
            bearerAuth(ownerToken)
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(CreateLessonRequest("lesson-created", "course-owned-lesson", "Created", "Created theory"))
        }
        assertEquals(HttpStatusCode.OK, created.status)

        val forbiddenUpdate = client.put("/lessons/lesson-owned") {
            bearerAuth(learnerToken)
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(UpdateLessonRequest(title = "Nope"))
        }
        assertEquals(HttpStatusCode.Forbidden, forbiddenUpdate.status)

        val updated = client.put("/lessons/lesson-owned") {
            bearerAuth(ownerToken)
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(UpdateLessonRequest(title = "Updated Title"))
        }
        assertEquals(HttpStatusCode.OK, updated.status)

        val forbiddenDelete = client.delete("/lessons/lesson-created") {
            bearerAuth(learnerToken)
        }
        assertEquals(HttpStatusCode.Forbidden, forbiddenDelete.status)

        val deleted = client.delete("/lessons/lesson-created") {
            bearerAuth(adminToken)
        }
        assertEquals(HttpStatusCode.NoContent, deleted.status)
    }

    @Test
    fun `theory route enforces auth scope and path body validation`() = testApplication {
        setupTestDatabase()

        application {
            module(initDatabase = false, seedData = false)
        }

        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        seedOfficialCourseWithLesson(
            courseId = "official-theory-course",
            lessonId = "official-theory-lesson",
            schoolYear = 3
        )
        seedOwnedCourseWithLesson(
            courseId = "teacher-owned-course",
            creatorId = "teacher-owner",
            lessonId = "teacher-owned-lesson"
        )

        val adminToken = Security.generateToken("admin-test", UserRole.ADMIN.name)
        val ownerToken = Security.generateToken("teacher-owner", UserRole.TEACHER.name)
        val otherTeacherToken = Security.generateToken("teacher-other", UserRole.TEACHER.name)

        val unauthorized = client.put("/lessons/official-theory-lesson/theory") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(TheoryUpdateRequest(lessonId = "official-theory-lesson", theoryContent = "Unauthorized"))
        }
        val adminSuccess = client.put("/lessons/official-theory-lesson/theory") {
            bearerAuth(adminToken)
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(TheoryUpdateRequest(lessonId = "official-theory-lesson", theoryContent = "Admin theory"))
        }
        val teacherSuccess = client.put("/lessons/teacher-owned-lesson/theory") {
            bearerAuth(ownerToken)
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(TheoryUpdateRequest(lessonId = "teacher-owned-lesson", theoryContent = "Teacher theory"))
        }
        val forbidden = client.put("/lessons/teacher-owned-lesson/theory") {
            bearerAuth(otherTeacherToken)
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(TheoryUpdateRequest(lessonId = "teacher-owned-lesson", theoryContent = "Forbidden"))
        }
        val mismatchedIds = client.put("/lessons/teacher-owned-lesson/theory") {
            bearerAuth(ownerToken)
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(TheoryUpdateRequest(lessonId = "different-lesson", theoryContent = "Mismatch"))
        }

        assertEquals(HttpStatusCode.Unauthorized, unauthorized.status)
        assertEquals(HttpStatusCode.OK, adminSuccess.status)
        assertEquals("Admin theory", adminSuccess.body<Lesson>().theoryContent)
        assertEquals(HttpStatusCode.OK, teacherSuccess.status)
        assertEquals("Teacher theory", teacherSuccess.body<Lesson>().theoryContent)
        assertEquals(HttpStatusCode.Forbidden, forbidden.status)
        assertEquals(HttpStatusCode.BadRequest, mismatchedIds.status)

        transaction {
            assertEquals(
                "Admin theory",
                Lessons.selectAll().where { Lessons.id eq "official-theory-lesson" }.single()[Lessons.theoryContent]
            )
            assertEquals(
                "Teacher theory",
                Lessons.selectAll().where { Lessons.id eq "teacher-owned-lesson" }.single()[Lessons.theoryContent]
            )
        }
    }

    @Test
    fun `exercise mutations require parent course owner or admin`() = testApplication {
        setupTestDatabase()

        application {
            module(initDatabase = false, seedData = false)
        }

        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        seedOwnedCourseWithLessonAndExercise(
            courseId = "course-owned-exercise",
            creatorId = "teacher-owner",
            lessonId = "lesson-owned-exercise",
            exerciseId = "exercise-owned"
        )

        val learnerToken = Security.generateToken("learner-user", UserRole.STUDENT.name)
        val ownerToken = Security.generateToken("teacher-owner", UserRole.TEACHER.name)
        val adminToken = Security.generateToken("admin-user", UserRole.ADMIN.name)

        val forbiddenCreate = client.post("/exercises") {
            bearerAuth(learnerToken)
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(
                CreateExerciseRequest(
                    id = "exercise-forbidden",
                    lessonId = "lesson-owned-exercise",
                    title = "Forbidden?",
                    type = ExerciseType.MULTIPLE_CHOICE,
                    payload = MultipleChoicePayload(
                        options = listOf(
                            ChoiceOption(id = "a", text = "a"),
                            ChoiceOption(id = "b", text = "b")
                        ),
                        correctOptionId = "a"
                    )
                )
            )
        }
        assertEquals(HttpStatusCode.Forbidden, forbiddenCreate.status)

        val created = client.post("/exercises") {
            bearerAuth(ownerToken)
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(
                CreateExerciseRequest(
                    id = "exercise-created",
                    lessonId = "lesson-owned-exercise",
                    title = "Created?",
                    type = ExerciseType.MULTIPLE_CHOICE,
                    payload = MultipleChoicePayload(
                        options = listOf(
                            ChoiceOption(id = "a", text = "a"),
                            ChoiceOption(id = "b", text = "b")
                        ),
                        correctOptionId = "a"
                    )
                )
            )
        }
        assertEquals(HttpStatusCode.OK, created.status)

        val forbiddenUpdate = client.put("/exercises/exercise-owned") {
            bearerAuth(learnerToken)
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(UpdateExerciseRequest(title = "Nope"))
        }
        assertEquals(HttpStatusCode.Forbidden, forbiddenUpdate.status)

        val updated = client.put("/exercises/exercise-owned") {
            bearerAuth(ownerToken)
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(UpdateExerciseRequest(title = "Updated question"))
        }
        assertEquals(HttpStatusCode.OK, updated.status)

        val forbiddenDelete = client.delete("/exercises/exercise-created") {
            bearerAuth(learnerToken)
        }
        assertEquals(HttpStatusCode.Forbidden, forbiddenDelete.status)

        val deleted = client.delete("/exercises/exercise-created") {
            bearerAuth(adminToken)
        }
        assertEquals(HttpStatusCode.NoContent, deleted.status)
    }

    @Test
    fun `foreign keys cascade for course lesson and student deletions while teacher owned courses survive`() = testApplication {
        setupTestDatabase()

        application {
            module(initDatabase = false, seedData = false)
        }

        transaction {
            Users.insert { it[id] = "teacher-1"; it[name] = "teacher-1"; it[email] = "teacher1@example.com"; it[passwordHash] = "hash"; it[role] = "TEACHER" }
            Users.insert { it[id] = "teacher-2"; it[name] = "teacher-2"; it[email] = "teacher2@example.com"; it[passwordHash] = "hash"; it[role] = "TEACHER" }
            Users.insert { it[id] = "teacher-3"; it[name] = "teacher-3"; it[email] = "teacher3@example.com"; it[passwordHash] = "hash"; it[role] = "TEACHER" }
            Users.insert { it[id] = "student-1"; it[name] = "student-1"; it[email] = "student1@example.com"; it[passwordHash] = "hash"; it[role] = "STUDENT" }
            Users.insert { it[id] = "student-2"; it[name] = "student-2"; it[email] = "student2@example.com"; it[passwordHash] = "hash"; it[role] = "STUDENT" }

            Courses.insert { it[id] = "course-1"; it[title] = "course-1"; it[description] = "course-1"; it[creatorId] = "teacher-1"; it[isOfficial] = false; it[joinCode] = "JOIN1" }
            Courses.insert { it[id] = "course-2"; it[title] = "course-2"; it[description] = "course-2"; it[creatorId] = "teacher-2"; it[isOfficial] = false; it[joinCode] = "JOIN2" }
            Courses.insert { it[id] = "course-3"; it[title] = "course-3"; it[description] = "course-3"; it[creatorId] = "teacher-3"; it[isOfficial] = false; it[joinCode] = "JOIN3" }

            Lessons.insert { it[id] = "lesson-1"; it[courseId] = "course-1"; it[title] = "lesson-1"; it[theoryContent] = "lesson-1"; it[orderIndex] = 0 }
            Lessons.insert { it[id] = "lesson-2"; it[courseId] = "course-2"; it[title] = "lesson-2"; it[theoryContent] = "lesson-2"; it[orderIndex] = 0 }
            Lessons.insert { it[id] = "lesson-3"; it[courseId] = "course-3"; it[title] = "lesson-3"; it[theoryContent] = "lesson-3"; it[orderIndex] = 0 }

            Exercises.insert { it[id] = "exercise-1"; it[lessonId] = "lesson-1"; it[question] = "exercise-1"; it[options] = "a,b,c"; it[correctAnswer] = "a"; it[type] = "MULTIPLE_CHOICE"; it[payload] = ExercisePayloadSupport.legacyPayloadJson(ExerciseType.MULTIPLE_CHOICE, "a,b,c", "a") }
            Exercises.insert { it[id] = "exercise-2"; it[lessonId] = "lesson-2"; it[question] = "exercise-2"; it[options] = "a,b,c"; it[correctAnswer] = "a"; it[type] = "MULTIPLE_CHOICE"; it[payload] = ExercisePayloadSupport.legacyPayloadJson(ExerciseType.MULTIPLE_CHOICE, "a,b,c", "a") }
            Exercises.insert { it[id] = "exercise-3"; it[lessonId] = "lesson-3"; it[question] = "exercise-3"; it[options] = "a,b,c"; it[correctAnswer] = "a"; it[type] = "MULTIPLE_CHOICE"; it[payload] = ExercisePayloadSupport.legacyPayloadJson(ExerciseType.MULTIPLE_CHOICE, "a,b,c", "a") }

            UserProgressTable.insert { it[userId] = "student-1"; it[totalScore] = 10 }
            UserProgressTable.insert { it[userId] = "student-2"; it[totalScore] = 20 }
            CompletedLessons.insert { it[userId] = "student-1"; it[lessonId] = "lesson-1" }
            CompletedLessons.insert { it[userId] = "student-2"; it[lessonId] = "lesson-2" }
            EnrolledCourses.insert { it[userId] = "student-1"; it[courseId] = "course-1" }
            EnrolledCourses.insert { it[userId] = "student-2"; it[courseId] = "course-2" }
        }

        transaction {
            Courses.deleteWhere { Courses.id eq "course-1" }
            Lessons.deleteWhere { Lessons.id eq "lesson-2" }
            Users.deleteWhere { Users.id eq "student-2" }
            Users.deleteWhere { Users.id eq "teacher-3" }

            assertEquals(0L, Lessons.selectAll().where { Lessons.id eq "lesson-1" }.count())
            assertEquals(0L, Exercises.selectAll().where { Exercises.id eq "exercise-1" }.count())
            assertEquals(0L, EnrolledCourses.selectAll().where { EnrolledCourses.courseId eq "course-1" }.count())
            assertEquals(0L, CompletedLessons.selectAll().where { CompletedLessons.lessonId eq "lesson-1" }.count())
            assertEquals(0L, Exercises.selectAll().where { Exercises.id eq "exercise-2" }.count())
            assertEquals(0L, CompletedLessons.selectAll().where { CompletedLessons.lessonId eq "lesson-2" }.count())
            assertEquals(0L, UserProgressTable.selectAll().where { UserProgressTable.userId eq "student-2" }.count())
            assertEquals(0L, EnrolledCourses.selectAll().where { EnrolledCourses.userId eq "student-2" }.count())
            assertEquals(1L, Courses.selectAll().where { Courses.id eq "course-3" }.count())
        }
    }

    private suspend fun registerUserAndGetToken(
        client: io.ktor.client.HttpClient,
        email: String
    ): String {
        val response = client.post("/auth/register") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(
                RegisterRequest(
                    name = "Integration User",
                    email = email,
                    password = "secret123"
                )
            )
        }

        return response.body<AuthResponse>().token
    }

    private fun seedOfficialCourse(
        courseId: String = "course-1",
        title: String = "Official Test Course",
        schoolYear: Int = 3,
        topic: String? = null,
        difficulty: String? = null,
        durationMinutes: Int? = null,
        xpReward: Int? = null
    ) {
        transaction {
            if (Users.selectAll().where { Users.id eq "admin-test" }.firstOrNull() == null) {
                Users.insert {
                    it[id] = "admin-test"
                    it[name] = "Admin"
                    it[email] = "admin-test@example.com"
                    it[passwordHash] = "hash"
                    it[role] = "ADMIN"
                }
            }

            Courses.insert {
                it[id] = courseId
                it[Courses.title] = title
                it[description] = "Basic test course"
                it[creatorId] = "admin-test"
                it[isOfficial] = true
                it[Courses.schoolYear] = schoolYear
                it[joinCode] = "JOIN123"
                it[Courses.topic] = topic
                it[Courses.difficulty] = difficulty
                it[Courses.durationMinutes] = durationMinutes
                it[Courses.xpReward] = xpReward
            }
        }
    }

    private fun seedOfficialCourseWithLesson(courseId: String, lessonId: String, schoolYear: Int) {
        seedOfficialCourse(courseId = courseId, title = courseId, schoolYear = schoolYear)

        transaction {
            Lessons.insert {
                it[id] = lessonId
                it[Lessons.courseId] = courseId
                it[title] = lessonId
                it[theoryContent] = lessonId
                it[orderIndex] = 0
            }
        }
    }

    private fun seedCourseLesson(courseId: String, lessonId: String) {
        transaction {
            Courses.insert {
                it[id] = courseId
                it[title] = courseId
                it[description] = courseId
                it[creatorId] = "teacher-owner"
                it[isOfficial] = false
                it[Courses.schoolYear] = 0
                it[joinCode] = null
            }

            Lessons.insert {
                it[id] = lessonId
                it[Lessons.courseId] = courseId
                it[title] = lessonId
                it[theoryContent] = lessonId
                it[orderIndex] = 0
            }
        }
    }

    private fun seedOwnedCourseWithLesson(courseId: String, creatorId: String, lessonId: String) {
        transaction {
            Courses.insert {
                it[id] = courseId
                it[title] = courseId
                it[description] = courseId
                it[Courses.creatorId] = creatorId
                it[isOfficial] = false
                it[Courses.schoolYear] = 0
                it[joinCode] = null
            }

            Lessons.insert {
                it[id] = lessonId
                it[Lessons.courseId] = courseId
                it[title] = lessonId
                it[theoryContent] = lessonId
                it[orderIndex] = 0
            }
        }
    }

    private fun seedOwnedCourseWithLessonAndExercise(
        courseId: String,
        creatorId: String,
        lessonId: String,
        exerciseId: String
    ) {
        seedOwnedCourseWithLesson(courseId = courseId, creatorId = creatorId, lessonId = lessonId)

        transaction {
            Exercises.insert {
                it[id] = exerciseId
                it[Exercises.lessonId] = lessonId
                it[question] = exerciseId
                it[options] = "a,b"
                it[correctAnswer] = "a"
                it[type] = "MULTIPLE_CHOICE"
                it[payload] = ExercisePayloadSupport.legacyPayloadJson(
                    type = ExerciseType.MULTIPLE_CHOICE,
                    optionsCsv = "a,b",
                    correctAnswer = "a"
                )
            }
        }
    }

    private suspend fun <T> withSystemProperties(properties: Map<String, String>, block: suspend () -> T): T {
        val previousValues = properties.mapValues { (name, _) -> System.getProperty(name) }

        properties.forEach { (name, value) ->
            System.setProperty(name, value)
        }

        return try {
            block()
        } finally {
            previousValues.forEach { (name, previousValue) ->
                if (previousValue == null) {
                    System.clearProperty(name)
                } else {
                    System.setProperty(name, previousValue)
                }
            }
        }
    }

    private suspend fun captureStandardOut(block: suspend () -> Unit): String {
        val originalOut = System.out
        val outputBuffer = ByteArrayOutputStream()
        val captureStream = PrintStream(outputBuffer, true, Charsets.UTF_8.name())

        try {
            System.setOut(captureStream)
            block()
        } finally {
            captureStream.flush()
            System.setOut(originalOut)
            captureStream.close()
        }

        return outputBuffer.toString(Charsets.UTF_8.name())
    }
}
