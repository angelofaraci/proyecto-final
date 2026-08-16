package com.example.proyectofinal

import at.favre.lib.crypto.bcrypt.BCrypt
import com.example.proyectofinal.database.CompletedExercises
import com.example.proyectofinal.database.CompletedLessons
import com.example.proyectofinal.database.Courses
import com.example.proyectofinal.database.DatabaseFactory
import com.example.proyectofinal.database.EnrolledCourses
import com.example.proyectofinal.database.Exercises
import com.example.proyectofinal.database.Lessons
import com.example.proyectofinal.database.Users
import com.example.proyectofinal.database.UserProgress as UserProgressTable
import com.example.proyectofinal.models.ChoiceOption
import com.example.proyectofinal.models.ExerciseAttemptRequest
import com.example.proyectofinal.models.Exercise
import com.example.proyectofinal.models.CreateCourseRequest
import com.example.proyectofinal.models.CreateExerciseRequest
import com.example.proyectofinal.models.CreateLessonRequest
import com.example.proyectofinal.models.CourseStudentsProgressResponse
import com.example.proyectofinal.models.ExerciseType
import com.example.proyectofinal.models.InputValuePayload
import com.example.proyectofinal.models.InputValueSubmission
import com.example.proyectofinal.models.MultiSelectPayload
import com.example.proyectofinal.models.MultiSelectSubmission
import com.example.proyectofinal.models.MultipleChoicePayload
import com.example.proyectofinal.models.MultipleChoiceSubmission
import com.example.proyectofinal.models.UpdateCourseRequest
import com.example.proyectofinal.models.UpdateExerciseRequest
import com.example.proyectofinal.models.UpdateLessonRequest
import com.example.proyectofinal.models.UserRole
import com.example.proyectofinal.seed.SeedData
import com.example.proyectofinal.service.AdminLessonMutationResult
import com.example.proyectofinal.service.AdminLessonPatchRequest
import com.example.proyectofinal.service.AuthService
import com.example.proyectofinal.service.CourseReadResult
import com.example.proyectofinal.service.CourseService
import com.example.proyectofinal.service.ExerciseAttemptResult
import com.example.proyectofinal.service.ExercisePayloadSupport
import com.example.proyectofinal.service.ExerciseService
import com.example.proyectofinal.service.FieldPatch
import com.example.proyectofinal.service.LessonListReadResult
import com.example.proyectofinal.service.LessonReadResult
import com.example.proyectofinal.service.LessonService
import com.example.proyectofinal.service.TheoryUpdateResult
import com.example.proyectofinal.service.UserService
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.sql.DriverManager
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CourseServiceTest {
    @BeforeTest
    fun setUp() {
        initServiceTestDatabase()
    }

    @Test
    fun `course service query methods return persisted data`() {
        insertUser(id = "admin-1", role = UserRole.ADMIN)
        insertUser(id = "teacher-1", role = UserRole.TEACHER)
        insertUser(id = "student-1", role = UserRole.STUDENT)
        insertCourse(id = "official-course-year-3", creatorId = "admin-1", isOfficial = true, schoolYear = 3)
        insertCourse(id = "official-course-year-4", creatorId = "admin-1", isOfficial = true, schoolYear = 4)
        insertCourse(id = "teacher-course", creatorId = "teacher-1", joinCode = "JOIN123")
        insertCourse(id = "teacher-course-2", creatorId = "teacher-1", joinCode = "JOIN456")
        insertLesson(id = "lesson-2", courseId = "teacher-course", orderIndex = 1)
        insertLesson(id = "lesson-1", courseId = "teacher-course", orderIndex = 0)
        enrollUser(userId = "student-1", courseId = "teacher-course")

        val service = CourseService()

        assertEquals(
            setOf("official-course-year-3", "official-course-year-4"),
            service.getOfficialCourses().map { it.id }.toSet()
        )
        assertEquals(listOf("official-course-year-3"), service.getOfficialCourses(3).map { it.id })
        assertEquals(3, service.getOfficialCourses(3).single().schoolYear)
        assertTrue(service.getOfficialCourses(6).isEmpty())
        assertEquals(
            setOf("teacher-course", "teacher-course-2"),
            service.getCoursesByCreator("teacher-1").map { it.id }.toSet()
        )
        assertEquals(listOf("teacher-course"), service.getEnrolledCourses("student-1").map { it.id })

        val course = service.getCourseById("teacher-course")
        assertNotNull(course)
        assertEquals(listOf("lesson-1", "lesson-2"), course.lessons.map { it.id })
        assertEquals("teacher-1", service.getCreatorId("teacher-course"))
    }

    @Test
    fun `course roster progress counts only enrolled students and course content`() {
        insertUser(id = "teacher-roster", role = UserRole.TEACHER)
        insertUser(id = "student-roster", role = UserRole.STUDENT, name = "Roster Student")
        insertUser(id = "student-not-enrolled", role = UserRole.STUDENT)
        insertCourse(id = "roster-course", creatorId = "teacher-roster", title = "Roster Course")
        insertCourse(id = "other-course", creatorId = "teacher-roster")
        insertLesson(id = "roster-lesson-1", courseId = "roster-course")
        insertLesson(id = "roster-lesson-2", courseId = "roster-course")
        insertLesson(id = "roster-lesson-3", courseId = "roster-course")
        insertLesson(id = "other-lesson", courseId = "other-course")
        insertExercise(id = "roster-exercise-1", lessonId = "roster-lesson-1")
        insertExercise(id = "roster-exercise-2", lessonId = "roster-lesson-2")
        insertExercise(id = "other-exercise", lessonId = "other-lesson")
        enrollUser(userId = "student-roster", courseId = "roster-course")
        enrollUser(userId = "teacher-roster", courseId = "roster-course")

        transaction {
            CompletedLessons.insert { it[userId] = "student-roster"; it[lessonId] = "roster-lesson-1" }
            CompletedLessons.insert { it[userId] = "student-roster"; it[lessonId] = "roster-lesson-2" }
            CompletedLessons.insert { it[userId] = "student-roster"; it[lessonId] = "other-lesson" }
            CompletedExercises.insert { it[userId] = "student-roster"; it[exerciseId] = "roster-exercise-1"; it[score] = 10 }
            CompletedExercises.insert { it[userId] = "student-roster"; it[exerciseId] = "other-exercise"; it[score] = 10 }
        }

        val response: CourseStudentsProgressResponse = CourseService().getStudentsProgress("roster-course")!!

        assertEquals("roster-course", response.courseId)
        assertEquals("Roster Course", response.courseTitle)
        assertEquals(3, response.totalLessons)
        assertEquals(1, response.students.size)
        with(response.students.single()) {
            assertEquals("student-roster", studentId)
            assertEquals("Roster Student", studentName)
            assertEquals(2, completedLessons)
            assertEquals(1, completedExercises)
            assertEquals(66, progressPercentage)
        }
    }

    @Test
    fun `course roster progress handles empty courses and missing courses`() {
        insertUser(id = "teacher-empty", role = UserRole.TEACHER)
        insertUser(id = "student-empty", role = UserRole.STUDENT)
        insertCourse(id = "empty-course", creatorId = "teacher-empty")
        enrollUser(userId = "student-empty", courseId = "empty-course")

        val response = CourseService().getStudentsProgress("empty-course")!!

        assertEquals(0, response.totalLessons)
        assertEquals(0, response.students.single().progressPercentage)
        assertEquals(null, CourseService().getStudentsProgress("missing-course"))
    }

    @Test
    fun `course service mutation methods persist changes`() {
        insertUser(id = "teacher-1", role = UserRole.TEACHER)
        insertUser(id = "student-1", role = UserRole.STUDENT)

        val service = CourseService()
        val created = service.createCourse(
            CreateCourseRequest(
                id = "created-course",
                title = "Created Course",
                description = "Created description",
                joinCode = "JOIN123",
                schoolYear = 5,
                topic = "Álgebra",
                difficulty = "Intermedio",
                durationMinutes = 30,
                xpReward = 80
            ),
            creatorId = "teacher-1"
        )

        assertEquals("created-course", created.id)
        assertEquals(5, created.schoolYear)
        assertEquals("Álgebra", created.topic)
        assertEquals("Intermedio", created.difficulty)
        assertEquals(30, created.durationMinutes)
        assertEquals(80, created.xpReward)

        val updated = service.updateCourse(
            id = "created-course",
            request = UpdateCourseRequest(
                title = "Updated Course",
                description = "Updated description",
                joinCode = "NEWCODE",
                schoolYear = 6,
                topic = "Geometría",
                difficulty = "Avanzado",
                durationMinutes = 45,
                xpReward = 120
            )
        )
        assertEquals("Updated Course", updated?.title)
        assertEquals("Updated description", updated?.description)
        assertEquals(6, updated?.schoolYear)
        assertEquals("Geometría", updated?.topic)
        assertEquals("Avanzado", updated?.difficulty)
        assertEquals(45, updated?.durationMinutes)
        assertEquals(120, updated?.xpReward)

        val joined = service.joinCourse(userId = "student-1", code = "NEWCODE")
        assertEquals("created-course", joined?.id)

        transaction {
            assertEquals(
                1L,
                EnrolledCourses.selectAll()
                    .where { EnrolledCourses.userId eq "student-1" }
                    .count()
            )
        }

        assertTrue(service.deleteCourse("created-course"))
        assertNull(service.getCourseById("created-course"))
    }
}

class AuthServiceTest {
    @BeforeTest
    fun setUp() {
        initServiceTestDatabase()
    }

    @Test
    fun `find user by email returns persisted auth record`() {
        val passwordHash = BCrypt.withDefaults().hashToString(12, "secret123".toCharArray())
        insertUser(
            id = "teacher-1",
            role = UserRole.TEACHER,
            email = "teacher@example.com",
            passwordHash = passwordHash
        )

        val user = AuthService().findUserByEmail("teacher@example.com")

        assertNotNull(user)
        assertEquals("teacher-1", user.id)
        assertEquals("teacher@example.com", user.email)
        assertEquals(passwordHash, user.passwordHash)
    }

    @Test
    fun `validate credentials accepts matching password and rejects other paths`() {
        val passwordHash = BCrypt.withDefaults().hashToString(12, "secret123".toCharArray())
        insertUser(
            id = "teacher-1",
            role = UserRole.TEACHER,
            email = "teacher@example.com",
            passwordHash = passwordHash
        )

        val service = AuthService()

        assertEquals("teacher-1", service.validateCredentials("teacher@example.com", "secret123")?.id)
        assertNull(service.validateCredentials("teacher@example.com", "wrong-password"))
        assertNull(service.validateCredentials("missing@example.com", "secret123"))
    }
}

class LessonExerciseServiceTest {
    @BeforeTest
    fun setUp() {
        initServiceTestDatabase()
    }

    @Test
    fun `lesson service supports list lookup update and delete flows`() {
        insertUser(id = "teacher-1", role = UserRole.TEACHER)
        insertCourse(id = "course-1", creatorId = "teacher-1")

        val service = LessonService()
        service.createLesson(CreateLessonRequest("lesson-1", "course-1", "First", "Theory 1"))
        service.createLesson(CreateLessonRequest("lesson-2", "course-1", "Second", "Theory 2"))
        insertExercise(id = "exercise-1", lessonId = "lesson-1", correctAnswer = "4")

        assertEquals(listOf("lesson-1", "lesson-2"), service.getLessonsByCourseId("course-1").map { it.id })

        val lesson = service.getLessonById("lesson-1", hideAnswers = false)
        assertNotNull(lesson)
        assertEquals("4", lesson.exercises.single().correctAnswer)

        val updated = service.updateLesson(
            id = "lesson-1",
            request = UpdateLessonRequest(
                title = "Updated lesson",
                theoryContent = "Updated theory"
            )
        )
        assertEquals("Updated lesson", updated?.title)
        assertEquals("teacher-1", service.getCourseCreatorId("course-1"))
        assertEquals("teacher-1", service.getCreatorId("lesson-1"))

        assertTrue(service.deleteLesson("lesson-2"))
        assertEquals(listOf("lesson-1"), service.getLessonsByCourseId("course-1").map { it.id })
    }

    @Test
    fun `lesson read access follows role and enrollment visibility`() {
        insertUser(id = "admin-1", role = UserRole.ADMIN)
        insertUser(id = "teacher-owner", role = UserRole.TEACHER)
        insertUser(id = "teacher-other", role = UserRole.TEACHER)
        insertUser(id = "learner-enrolled", role = UserRole.STUDENT)
        insertUser(id = "learner-other", role = UserRole.STUDENT)

        insertCourse(id = "official-course", creatorId = "admin-1", isOfficial = true, schoolYear = 3)
        insertCourse(id = "teacher-course", creatorId = "teacher-owner")
        insertLesson(id = "official-lesson", courseId = "official-course", theoryContent = "Official theory")
        insertLesson(id = "teacher-lesson", courseId = "teacher-course", theoryContent = "Teacher theory")
        insertExercise(
            id = "exercise-teacher",
            lessonId = "teacher-lesson",
            options = "3,4",
            correctAnswer = "4"
        )
        enrollUser(userId = "learner-enrolled", courseId = "teacher-course")
        val exerciseService = ExerciseService()
        exerciseService.createExercise(
            CreateExerciseRequest(
                id = "exercise-input",
                lessonId = "teacher-lesson",
                title = "Enter 42",
                type = ExerciseType.INPUT_VALUE,
                payload = InputValuePayload(correctValue = "42")
            )
        )
        exerciseService.createExercise(
            CreateExerciseRequest(
                id = "exercise-multi",
                lessonId = "teacher-lesson",
                title = "Choose A and C",
                type = ExerciseType.MULTI_SELECT,
                payload = MultiSelectPayload(
                    options = listOf(
                        ChoiceOption(id = "A", text = "A"),
                        ChoiceOption(id = "C", text = "C")
                    ),
                    correctOptionIds = listOf("A", "C")
                )
            )
        )

        val service = LessonService()

        assertEquals(
            "teacher-lesson",
            assertIs<LessonReadResult.Success>(
                service.getLessonByIdForUser("teacher-lesson", "teacher-owner", UserRole.TEACHER)
            ).lesson.id
        )
        val adminLesson = assertIs<LessonReadResult.Success>(
            service.getLessonByIdForUser("teacher-lesson", "admin-1", UserRole.ADMIN)
        ).lesson
        assertEquals("teacher-lesson", adminLesson.id)
        assertEquals(
            "official-lesson",
            assertIs<LessonReadResult.Success>(
                service.getLessonByIdForUser("official-lesson", "learner-other", UserRole.STUDENT)
            ).lesson.id
        )
        assertEquals(
            "official-lesson",
            assertIs<LessonReadResult.Success>(
                service.getLessonByIdForUser("official-lesson", "teacher-other", UserRole.TEACHER)
            ).lesson.id
        )
        assertEquals(
            "official-course",
            assertIs<CourseReadResult.Success>(
                CourseService().getCourseByIdForUser("official-course", "teacher-other", UserRole.TEACHER)
            ).course.id
        )

        val enrolledLesson = assertIs<LessonReadResult.Success>(
            service.getLessonByIdForUser("teacher-lesson", "learner-enrolled", UserRole.STUDENT)
        ).lesson
        val studentExercises = enrolledLesson.exercises.associateBy { it.id }
        val adminExercises = adminLesson.exercises.associateBy { it.id }
        assertNull(assertIs<MultipleChoicePayload>(studentExercises.getValue("exercise-teacher").payload).correctOptionId)
        assertNull(assertIs<InputValuePayload>(studentExercises.getValue("exercise-input").payload).correctValue)
        assertNull(assertIs<MultiSelectPayload>(studentExercises.getValue("exercise-multi").payload).correctOptionIds)
        assertEquals(
            "4",
            assertIs<MultipleChoicePayload>(adminExercises.getValue("exercise-teacher").payload).correctOptionId
        )
        assertEquals("42", assertIs<InputValuePayload>(adminExercises.getValue("exercise-input").payload).correctValue)
        assertEquals(
            listOf("A", "C"),
            assertIs<MultiSelectPayload>(adminExercises.getValue("exercise-multi").payload).correctOptionIds
        )

        assertEquals(
            LessonReadResult.Forbidden,
            service.getLessonByIdForUser("teacher-lesson", "teacher-other", UserRole.TEACHER)
        )
        assertEquals(
            LessonReadResult.Forbidden,
            service.getLessonByIdForUser("teacher-lesson", "learner-other", UserRole.STUDENT)
        )
        assertEquals(
            LessonReadResult.NotFound,
            service.getLessonByIdForUser("missing-lesson", "admin-1", UserRole.ADMIN)
        )
        assertEquals(
            LessonListReadResult.Forbidden,
            service.getLessonsByCourseIdForUser("teacher-course", "learner-other", UserRole.STUDENT)
        )
    }

    @Test
    fun `standalone lessons are visible only to admin or creator and theory updates follow ownership`() {
        insertUser(id = "admin-1", role = UserRole.ADMIN)
        insertUser(id = "teacher-owner", role = UserRole.TEACHER)
        insertUser(id = "teacher-other", role = UserRole.TEACHER)
        insertUser(id = "learner-1", role = UserRole.STUDENT)

        insertLesson(
            id = "standalone-lesson",
            courseId = null,
            creatorId = "teacher-owner",
            theoryContent = "Standalone theory"
        )
        insertExercise(id = "standalone-exercise", lessonId = "standalone-lesson", correctAnswer = "42")

        val service = LessonService()

        val adminLesson = assertIs<LessonReadResult.Success>(
            service.getLessonByIdForUser("standalone-lesson", "admin-1", UserRole.ADMIN)
        ).lesson
        val ownerLesson = assertIs<LessonReadResult.Success>(
            service.getLessonByIdForUser("standalone-lesson", "teacher-owner", UserRole.TEACHER)
        ).lesson

        assertEquals("42", adminLesson.exercises.single().correctAnswer)
        assertEquals("42", ownerLesson.exercises.single().correctAnswer)
        assertEquals(listOf("standalone-lesson"), service.listStandaloneLessons().map { it.id })
        assertEquals(
            LessonReadResult.Forbidden,
            service.getLessonByIdForUser("standalone-lesson", "teacher-other", UserRole.TEACHER)
        )
        assertEquals(
            LessonReadResult.Forbidden,
            service.getLessonByIdForUser("standalone-lesson", "learner-1", UserRole.STUDENT)
        )

        assertIs<TheoryUpdateResult.Success>(
            service.updateTheoryContent(
                lessonId = "standalone-lesson",
                content = "Updated by creator",
                userId = "teacher-owner",
                role = UserRole.TEACHER
            )
        )
        assertIs<TheoryUpdateResult.Success>(
            service.updateTheoryContent(
                lessonId = "standalone-lesson",
                content = "Updated by admin",
                userId = "admin-1",
                role = UserRole.ADMIN
            )
        )
        assertEquals(
            TheoryUpdateResult.Forbidden,
            service.updateTheoryContent(
                lessonId = "standalone-lesson",
                content = "Rejected",
                userId = "teacher-other",
                role = UserRole.TEACHER
            )
        )
    }

    @Test
    fun `admin lesson patch unassigns with fallback creator and rejects creator clears`() {
        insertUser(id = "teacher-owner", role = UserRole.TEACHER)
        insertUser(id = "teacher-other", role = UserRole.TEACHER)
        insertCourse(id = "course-1", creatorId = "teacher-owner")
        insertCourse(id = "course-2", creatorId = "teacher-other")
        insertLesson(id = "course-lesson", courseId = "course-1", creatorId = null)

        val service = LessonService()

        val detachedLesson = assertIs<AdminLessonMutationResult.Success>(
            service.adminUpdateLesson(
                id = "course-lesson",
                request = AdminLessonPatchRequest(
                    courseId = FieldPatch.Present<String?>(null)
                )
            )
        ).lesson

        assertEquals(null, detachedLesson.courseId)
        assertEquals("teacher-owner", detachedLesson.creatorId)
        assertEquals("teacher-owner", service.getCreatorId("course-lesson"))

        val reassignedLesson = assertIs<AdminLessonMutationResult.Success>(
            service.adminUpdateLesson(
                id = "course-lesson",
                request = AdminLessonPatchRequest(
                    courseId = FieldPatch.Present("course-2")
                )
            )
        ).lesson

        assertEquals("course-2", reassignedLesson.courseId)
        assertEquals("teacher-owner", reassignedLesson.creatorId)

        val clearResult = assertIs<AdminLessonMutationResult.InvalidRequest>(
            service.adminUpdateLesson(
                id = "course-lesson",
                request = AdminLessonPatchRequest(
                    creatorId = FieldPatch.Present<String?>(null)
                )
            )
        )
        assertTrue(clearResult.message.contains("cannot be cleared"))
    }

    @Test
    fun `course read access blocks private course details for outsiders`() {
        insertUser(id = "admin-1", role = UserRole.ADMIN)
        insertUser(id = "teacher-owner", role = UserRole.TEACHER)
        insertUser(id = "learner-enrolled", role = UserRole.STUDENT)
        insertUser(id = "learner-other", role = UserRole.STUDENT)

        insertCourse(id = "official-course", creatorId = "admin-1", isOfficial = true, schoolYear = 3)
        insertCourse(id = "teacher-course", creatorId = "teacher-owner")
        insertLesson(id = "teacher-lesson", courseId = "teacher-course")
        enrollUser(userId = "learner-enrolled", courseId = "teacher-course")

        val service = CourseService()

        assertIs<CourseReadResult.Success>(
            service.getCourseByIdForUser("teacher-course", "teacher-owner", UserRole.TEACHER)
        )
        assertIs<CourseReadResult.Success>(
            service.getCourseByIdForUser("teacher-course", "learner-enrolled", UserRole.STUDENT)
        )
        assertIs<CourseReadResult.Success>(
            service.getCourseByIdForUser("official-course", "learner-other", UserRole.STUDENT)
        )
        assertEquals(
            CourseReadResult.Forbidden,
            service.getCourseByIdForUser("teacher-course", "learner-other", UserRole.STUDENT)
        )
    }

    @Test
    fun `theory updates persist only for allowed roles and scopes`() {
        insertUser(id = "admin-1", role = UserRole.ADMIN)
        insertUser(id = "teacher-owner", role = UserRole.TEACHER)
        insertUser(id = "teacher-other", role = UserRole.TEACHER)

        insertCourse(id = "official-course", creatorId = "admin-1", isOfficial = true, schoolYear = 3)
        insertCourse(id = "teacher-course", creatorId = "teacher-owner")
        insertCourse(id = "other-course", creatorId = "teacher-other")

        insertLesson(id = "official-lesson", courseId = "official-course", theoryContent = "Official theory")
        insertLesson(id = "teacher-lesson", courseId = "teacher-course", theoryContent = "Teacher theory")
        insertLesson(id = "other-lesson", courseId = "other-course", theoryContent = "Other theory")
        insertExercise(id = "exercise-1", lessonId = "official-lesson", correctAnswer = "4")

        val service = LessonService()

        val adminResult = service.updateTheoryContent(
            lessonId = "official-lesson",
            content = "Updated official theory",
            userId = "admin-1",
            role = UserRole.ADMIN
        )
        val teacherResult = service.updateTheoryContent(
            lessonId = "teacher-lesson",
            content = "Updated teacher theory",
            userId = "teacher-owner",
            role = UserRole.TEACHER
        )
        val forbiddenTeacherResult = service.updateTheoryContent(
            lessonId = "other-lesson",
            content = "Should fail",
            userId = "teacher-owner",
            role = UserRole.TEACHER
        )
        val forbiddenAdminResult = service.updateTheoryContent(
            lessonId = "teacher-lesson",
            content = "Should also fail",
            userId = "admin-1",
            role = UserRole.ADMIN
        )

        assertEquals("Updated official theory", assertIs<TheoryUpdateResult.Success>(adminResult).lesson.theoryContent)
        assertEquals("4", assertIs<TheoryUpdateResult.Success>(adminResult).lesson.exercises.single().correctAnswer)
        assertEquals("Updated teacher theory", assertIs<TheoryUpdateResult.Success>(teacherResult).lesson.theoryContent)
        assertEquals(TheoryUpdateResult.Forbidden, forbiddenTeacherResult)
        assertEquals(TheoryUpdateResult.Forbidden, forbiddenAdminResult)

        transaction {
            assertEquals(
                "Updated official theory",
                Lessons.selectAll().where { Lessons.id eq "official-lesson" }.single()[Lessons.theoryContent]
            )
            assertEquals(
                "Updated teacher theory",
                Lessons.selectAll().where { Lessons.id eq "teacher-lesson" }.single()[Lessons.theoryContent]
            )
        }
    }

    @Test
    fun `exercise service supports list create update and delete flows`() {
        insertUser(id = "teacher-1", role = UserRole.TEACHER)
        insertCourse(id = "course-1", creatorId = "teacher-1")
        insertLesson(id = "lesson-1", courseId = "course-1")

        val service = ExerciseService()
        service.createExercise(
            CreateExerciseRequest(
                id = "exercise-1",
                lessonId = "lesson-1",
                title = "2 + 2 = ?",
                type = ExerciseType.MULTIPLE_CHOICE,
                payload = MultipleChoicePayload(
                    options = listOf(
                        ChoiceOption(id = "3", text = "3"),
                        ChoiceOption(id = "4", text = "4")
                    ),
                    correctOptionId = "4"
                )
            )
        )

        val hiddenExercises = service.getExercisesByLessonId("lesson-1", hideAnswers = true)
        assertEquals(1, hiddenExercises.size)
        assertEquals("", hiddenExercises.single().correctAnswer)

        val updated = service.updateExercise(
            id = "exercise-1",
            request = UpdateExerciseRequest(
                title = "3 + 3 = ?",
                payload = MultipleChoicePayload(
                    options = listOf(
                        ChoiceOption(id = "5", text = "5"),
                        ChoiceOption(id = "6", text = "6")
                    ),
                    correctOptionId = "6"
                )
            )
        )
        assertEquals("3 + 3 = ?", updated?.question)
        assertEquals(listOf("5", "6"), updated?.options)
        assertEquals("teacher-1", service.getLessonCreatorId("lesson-1"))
        assertEquals("teacher-1", service.getCreatorId("exercise-1"))

        assertTrue(service.deleteExercise("exercise-1"))
        assertTrue(service.getExercisesByLessonId("lesson-1", hideAnswers = false).isEmpty())
    }

    @Test
    fun `exercise attempt validation handles every typed submission contract`() {
        val options = listOf(
            ChoiceOption(id = "A", text = "A"),
            ChoiceOption(id = "B", text = "B"),
            ChoiceOption(id = "C", text = "C")
        )
        val multipleChoice = Exercise(
            id = "choice",
            lessonId = "lesson-1",
            title = "Choose B",
            payload = MultipleChoicePayload(options = options, correctOptionId = "B")
        )
        val inputValue = Exercise(
            id = "input",
            lessonId = "lesson-1",
            title = "Enter 42",
            payload = InputValuePayload(correctValue = "42")
        )
        val multiSelect = Exercise(
            id = "multi",
            lessonId = "lesson-1",
            title = "Choose A and C",
            payload = MultiSelectPayload(options = options, correctOptionIds = listOf("A", "C"))
        )

        assertTrue(
            ExercisePayloadSupport.evaluateAttempt(
                multipleChoice,
                MultipleChoiceSubmission("B")
            ).isCorrect
        )
        assertTrue(
            ExercisePayloadSupport.evaluateAttempt(inputValue, InputValueSubmission(" 42 ")).isCorrect
        )
        assertTrue(
            ExercisePayloadSupport.evaluateAttempt(
                multiSelect,
                MultiSelectSubmission(listOf("C", "A"))
            ).isCorrect
        )
        assertFalse(
            ExercisePayloadSupport.evaluateAttempt(
                multiSelect,
                MultiSelectSubmission(listOf("A"))
            ).isCorrect
        )
    }

    @Test
    fun `exercise ownership fallback and course delete keep standalone content intact`() {
        insertUser(id = "admin-1", role = UserRole.ADMIN)
        insertUser(id = "teacher-owner", role = UserRole.TEACHER)
        insertCourse(id = "course-1", creatorId = "teacher-owner")
        insertLesson(id = "course-lesson", courseId = "course-1", creatorId = "admin-1")
        insertLesson(id = "standalone-lesson", courseId = null, creatorId = "teacher-owner")
        insertExercise(id = "course-exercise", lessonId = "course-lesson")
        insertExercise(id = "standalone-exercise", lessonId = "standalone-lesson")

        val exerciseService = ExerciseService()
        val courseService = CourseService()

        assertEquals("teacher-owner", exerciseService.getLessonCreatorId("course-lesson"))
        assertEquals("teacher-owner", exerciseService.getLessonCreatorId("standalone-lesson"))
        assertEquals("teacher-owner", exerciseService.getCreatorId("course-exercise"))
        assertEquals("teacher-owner", exerciseService.getCreatorId("standalone-exercise"))

        assertTrue(courseService.adminDeleteCourse("course-1"))

        transaction {
            assertEquals(0L, Courses.selectAll().where { Courses.id eq "course-1" }.count())
            assertEquals(0L, Lessons.selectAll().where { Lessons.id eq "course-lesson" }.count())
            assertEquals(0L, Exercises.selectAll().where { Exercises.id eq "course-exercise" }.count())
            assertEquals(1L, Lessons.selectAll().where { Lessons.id eq "standalone-lesson" }.count())
            assertEquals(1L, Exercises.selectAll().where { Exercises.id eq "standalone-exercise" }.count())
        }
    }

    @Test
    fun `database init backfills missing course school year column and is idempotent`() {
        val url = "jdbc:h2:mem:${UUID.randomUUID()};MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE"

        DriverManager.getConnection(url, "sa", "").use { connection ->
            connection.createStatement().use { statement ->
                statement.execute(
                    """
                    CREATE TABLE users (
                        id VARCHAR(50) PRIMARY KEY,
                        name VARCHAR(100) NOT NULL,
                        email VARCHAR(100) NOT NULL,
                        password_hash VARCHAR(255) NOT NULL,
                        role VARCHAR(20) NOT NULL
                    )
                    """.trimIndent()
                )
                statement.execute("CREATE UNIQUE INDEX idx_users_email ON users (email)")
                statement.execute(
                    """
                    CREATE TABLE courses (
                        id VARCHAR(50) PRIMARY KEY,
                        title VARCHAR(200) NOT NULL,
                        description VARCHAR(1000) NOT NULL,
                        creator_id VARCHAR(50) NOT NULL,
                        is_official BOOLEAN NOT NULL DEFAULT FALSE,
                        join_code VARCHAR(20)
                    )
                    """.trimIndent()
                )
                statement.execute(
                    """
                    CREATE TABLE lessons (
                        id VARCHAR(50) PRIMARY KEY,
                        course_id VARCHAR(50) NOT NULL,
                        title VARCHAR(200) NOT NULL,
                        theory_content TEXT NOT NULL,
                        order_index INTEGER NOT NULL DEFAULT 0,
                        CONSTRAINT fk_lessons_course FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                statement.execute(
                    """
                    CREATE TABLE exercises (
                        id VARCHAR(50) PRIMARY KEY,
                        lesson_id VARCHAR(50) NOT NULL,
                        question VARCHAR(500) NOT NULL,
                        options VARCHAR(500) NOT NULL,
                        correct_answer VARCHAR(255) NOT NULL,
                        type VARCHAR(30) NOT NULL DEFAULT 'MULTIPLE_CHOICE',
                        CONSTRAINT fk_exercises_lesson FOREIGN KEY (lesson_id) REFERENCES lessons(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                statement.execute(
                    """
                    CREATE TABLE user_progress (
                        user_id VARCHAR(50) PRIMARY KEY,
                        total_score INTEGER NOT NULL DEFAULT 0,
                        CONSTRAINT fk_user_progress_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                statement.execute(
                    """
                    CREATE TABLE completed_lessons (
                        user_id VARCHAR(50) NOT NULL,
                        lesson_id VARCHAR(50) NOT NULL,
                        PRIMARY KEY (user_id, lesson_id),
                        CONSTRAINT fk_completed_lessons_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                        CONSTRAINT fk_completed_lessons_lesson FOREIGN KEY (lesson_id) REFERENCES lessons(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                statement.execute(
                    """
                    CREATE TABLE completed_exercises (
                        user_id VARCHAR(50) NOT NULL,
                        exercise_id VARCHAR(50) NOT NULL,
                        score INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY (user_id, exercise_id),
                        CONSTRAINT fk_completed_exercises_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                        CONSTRAINT fk_completed_exercises_exercise FOREIGN KEY (exercise_id) REFERENCES exercises(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                statement.execute(
                    """
                    CREATE TABLE enrolled_courses (
                        user_id VARCHAR(50) NOT NULL,
                        course_id VARCHAR(50) NOT NULL,
                        PRIMARY KEY (user_id, course_id),
                        CONSTRAINT fk_enrolled_courses_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                        CONSTRAINT fk_enrolled_courses_course FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                statement.execute(
                    """
                    INSERT INTO users (id, name, email, password_hash, role)
                    VALUES ('legacy-teacher', 'Legacy Teacher', 'legacy@example.com', 'hash', 'TEACHER')
                    """.trimIndent()
                )
                statement.execute(
                    """
                    INSERT INTO courses (id, title, description, creator_id, is_official, join_code)
                    VALUES ('legacy-course', 'Legacy Course', 'Created before school year', 'legacy-teacher', FALSE, NULL)
                    """.trimIndent()
                )
            }
        }

        DatabaseFactory.init(
            url = url,
            driver = "org.h2.Driver",
            user = "sa",
            password = ""
        )

        DatabaseFactory.init(
            url = url,
            driver = "org.h2.Driver",
            user = "sa",
            password = ""
        )

        DriverManager.getConnection(url, "sa", "").use { connection ->
            connection.metaData.getColumns(null, null, "courses", "school_year").use { columns ->
                assertTrue(columns.next())
            }

            connection.prepareStatement("SELECT school_year FROM courses WHERE id = ?").use { statement ->
                statement.setString(1, "legacy-course")

                statement.executeQuery().use { resultSet ->
                    assertTrue(resultSet.next())
                    assertEquals(0, resultSet.getInt("school_year"))
                }
            }
        }
    }

    @Test
    fun `database init backfills exercise payloads and normalizes true false rows`() {
        val url = "jdbc:h2:mem:${UUID.randomUUID()};MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE"

        DriverManager.getConnection(url, "sa", "").use { connection ->
            connection.createStatement().use { statement ->
                statement.execute(
                    """
                    CREATE TABLE users (
                        id VARCHAR(50) PRIMARY KEY,
                        name VARCHAR(100) NOT NULL,
                        email VARCHAR(100) NOT NULL,
                        password_hash VARCHAR(255) NOT NULL,
                        role VARCHAR(20) NOT NULL
                    )
                    """.trimIndent()
                )
                statement.execute("CREATE UNIQUE INDEX idx_users_email ON users (email)")
                statement.execute(
                    """
                    CREATE TABLE courses (
                        id VARCHAR(50) PRIMARY KEY,
                        title VARCHAR(200) NOT NULL,
                        description VARCHAR(1000) NOT NULL,
                        creator_id VARCHAR(50) NOT NULL,
                        is_official BOOLEAN NOT NULL DEFAULT FALSE,
                        join_code VARCHAR(20)
                    )
                    """.trimIndent()
                )
                statement.execute(
                    """
                    CREATE TABLE lessons (
                        id VARCHAR(50) PRIMARY KEY,
                        course_id VARCHAR(50) NOT NULL,
                        title VARCHAR(200) NOT NULL,
                        theory_content TEXT NOT NULL,
                        order_index INTEGER NOT NULL DEFAULT 0,
                        CONSTRAINT fk_lessons_course FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                statement.execute(
                    """
                    CREATE TABLE exercises (
                        id VARCHAR(50) PRIMARY KEY,
                        lesson_id VARCHAR(50) NOT NULL,
                        question VARCHAR(500) NOT NULL,
                        options VARCHAR(500) NOT NULL,
                        correct_answer VARCHAR(255) NOT NULL,
                        type VARCHAR(30) NOT NULL DEFAULT 'MULTIPLE_CHOICE',
                        CONSTRAINT fk_exercises_lesson FOREIGN KEY (lesson_id) REFERENCES lessons(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                statement.execute(
                    """
                    CREATE TABLE user_progress (
                        user_id VARCHAR(50) PRIMARY KEY,
                        total_score INTEGER NOT NULL DEFAULT 0,
                        CONSTRAINT fk_user_progress_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                statement.execute(
                    """
                    CREATE TABLE completed_lessons (
                        user_id VARCHAR(50) NOT NULL,
                        lesson_id VARCHAR(50) NOT NULL,
                        PRIMARY KEY (user_id, lesson_id),
                        CONSTRAINT fk_completed_lessons_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                        CONSTRAINT fk_completed_lessons_lesson FOREIGN KEY (lesson_id) REFERENCES lessons(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                statement.execute(
                    """
                    CREATE TABLE completed_exercises (
                        user_id VARCHAR(50) NOT NULL,
                        exercise_id VARCHAR(50) NOT NULL,
                        score INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY (user_id, exercise_id),
                        CONSTRAINT fk_completed_exercises_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                        CONSTRAINT fk_completed_exercises_exercise FOREIGN KEY (exercise_id) REFERENCES exercises(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                statement.execute(
                    """
                    CREATE TABLE enrolled_courses (
                        user_id VARCHAR(50) NOT NULL,
                        course_id VARCHAR(50) NOT NULL,
                        PRIMARY KEY (user_id, course_id),
                        CONSTRAINT fk_enrolled_courses_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                        CONSTRAINT fk_enrolled_courses_course FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                statement.execute("INSERT INTO users (id, name, email, password_hash, role) VALUES ('legacy-admin', 'Legacy Admin', 'legacy-admin@example.com', 'hash', 'ADMIN')")
                statement.execute("INSERT INTO courses (id, title, description, creator_id, is_official, join_code) VALUES ('legacy-course', 'Legacy Course', 'Legacy', 'legacy-admin', TRUE, 'JOIN123')")
                statement.execute("INSERT INTO lessons (id, course_id, title, theory_content, order_index) VALUES ('legacy-lesson', 'legacy-course', 'Legacy Lesson', 'Theory', 0)")
                statement.execute("INSERT INTO exercises (id, lesson_id, question, options, correct_answer, type) VALUES ('legacy-choice', 'legacy-lesson', '2 + 2 = ?', '3,4', '4', 'MULTIPLE_CHOICE')")
                statement.execute("INSERT INTO exercises (id, lesson_id, question, options, correct_answer, type) VALUES ('legacy-bool', 'legacy-lesson', 'Is 2 even?', 'True,False', 'True', 'TRUE_FALSE')")
                statement.execute("INSERT INTO exercises (id, lesson_id, question, options, correct_answer, type) VALUES ('legacy-multi-option', 'legacy-lesson', 'Pick a color', 'red,green,blue', 'green', 'MULTIPLE_CHOICE')")
                statement.execute("INSERT INTO exercises (id, lesson_id, question, options, correct_answer, type) VALUES ('legacy-spaced-option', 'legacy-lesson', 'Pick a spaced color', 'red, green, blue', ' green ', 'MULTIPLE_CHOICE')")
            }
        }

        DatabaseFactory.init(
            url = url,
            driver = "org.h2.Driver",
            user = "sa",
            password = ""
        )

        DriverManager.getConnection(url, "sa", "").use { connection ->
            connection.prepareStatement("SELECT type, payload FROM exercises WHERE id = ?").use { statement ->
                statement.setString(1, "legacy-choice")

                statement.executeQuery().use { resultSet ->
                    assertTrue(resultSet.next())
                    assertEquals("MULTIPLE_CHOICE", resultSet.getString("type"))
                    assertTrue(resultSet.getString("payload").contains("\"type\":\"multipleChoice\""))
                    assertTrue(resultSet.getString("payload").contains("\"correctOptionId\":\"4\""))
                }
            }

            connection.prepareStatement("SELECT type, payload FROM exercises WHERE id = ?").use { statement ->
                statement.setString(1, "legacy-bool")

                statement.executeQuery().use { resultSet ->
                    assertTrue(resultSet.next())
                    assertEquals("MULTIPLE_CHOICE", resultSet.getString("type"))
                    assertTrue(resultSet.getString("payload").contains("\"True\""))
                }
            }

            connection.prepareStatement("SELECT payload FROM exercises WHERE id = ?").use { statement ->
                statement.setString(1, "legacy-multi-option")

                statement.executeQuery().use { resultSet ->
                    assertTrue(resultSet.next())
                    val payload = resultSet.getString("payload")
                    assertTrue(payload.contains("\"id\":\"red\",\"text\":\"red\""))
                    assertTrue(payload.contains("\"id\":\"green\",\"text\":\"green\""))
                    assertTrue(payload.contains("\"id\":\"blue\",\"text\":\"blue\""))
                    assertTrue(payload.contains("\"correctOptionId\":\"green\""))
                }
            }

            connection.prepareStatement("SELECT payload FROM exercises WHERE id = ?").use { statement ->
                statement.setString(1, "legacy-spaced-option")

                statement.executeQuery().use { resultSet ->
                    assertTrue(resultSet.next())
                    val payload = resultSet.getString("payload")
                    assertTrue(payload.contains("\"id\":\"red\",\"text\":\"red\""))
                    assertTrue(payload.contains("\"id\":\"green\",\"text\":\"green\""))
                    assertTrue(payload.contains("\"id\":\"blue\",\"text\":\"blue\""))
                    assertTrue(payload.contains("\"correctOptionId\":\"green\""))
                    assertTrue(!payload.contains("\"id\":\" green\""))
                    assertTrue(!payload.contains("\"text\":\" green\""))
                    assertTrue(!payload.contains("\"correctOptionId\":\" green \""))
                }
            }
        }
    }

    @Test
    fun `database init fails when a pending migration cannot be applied`() {
        val url = "jdbc:h2:mem:${UUID.randomUUID()};MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE"

        DriverManager.getConnection(url, "sa", "").use { connection ->
            connection.createStatement().use { statement ->
                statement.execute(
                    """
                    CREATE TABLE legacy_placeholder (
                        id INTEGER PRIMARY KEY
                    )
                    """.trimIndent()
                )
            }
        }

        Flyway.configure()
            .dataSource(url, "sa", "")
            .locations("classpath:db/migration")
            .baselineVersion("1")
            .load()
            .baseline()

        val error = assertFailsWith<Exception> {
            DatabaseFactory.init(
                url = url,
                driver = "org.h2.Driver",
                user = "sa",
                password = ""
            )
        }

        val migrationFailureDetected = generateSequence(error as Throwable?) { it.cause }
            .mapNotNull { it.message }
            .any { message ->
                message.contains("V2__ensure_courses_school_year.sql") ||
                    (message.contains("courses", ignoreCase = true) &&
                        message.contains("school_year", ignoreCase = true))
            }

        assertTrue(migrationFailureDetected)
    }
}

class UserServiceTest {
    @BeforeTest
    fun setUp() {
        initServiceTestDatabase()
    }

    @Test
    fun `attempt exercise is first wins and completes lesson on final exercise`() {
        insertUser(id = "admin-1", role = UserRole.ADMIN)
        insertUser(id = "learner-1", role = UserRole.STUDENT)
        insertCourse(id = "official-course", creatorId = "admin-1", isOfficial = true)
        insertLesson(id = "lesson-1", courseId = "official-course")
        insertExercise(id = "exercise-1", lessonId = "lesson-1")
        insertExercise(id = "exercise-2", lessonId = "lesson-1")
        val service = UserService()
        val firstResult = service.attemptExercise(
            userId = "learner-1",
            role = UserRole.STUDENT,
            request = ExerciseAttemptRequest(
                exerciseId = "exercise-1",
                submission = MultipleChoiceSubmission(selectedOptionId = "a"),
                score = 10
            )
        )
        val duplicateResult = service.attemptExercise(
            userId = "learner-1",
            role = UserRole.STUDENT,
            request = ExerciseAttemptRequest(
                exerciseId = "exercise-1",
                submission = MultipleChoiceSubmission(selectedOptionId = "a"),
                score = 99
            )
        )
        val finalResult = service.attemptExercise(
            userId = "learner-1",
            role = UserRole.STUDENT,
            request = ExerciseAttemptRequest(
                exerciseId = "exercise-2",
                submission = MultipleChoiceSubmission(selectedOptionId = "a"),
                score = 15
            )
        )
        val firstSuccess = assertIs<ExerciseAttemptResult.Success>(firstResult)
        assertEquals(true, firstSuccess.response.isCorrect)
        assertEquals(false, firstSuccess.response.lessonCompleted)
        assertEquals(10, firstSuccess.response.progress.totalScore)
        assertEquals(setOf("exercise-1"), firstSuccess.response.progress.completedExerciseIds)
        val duplicateSuccess = assertIs<ExerciseAttemptResult.Success>(duplicateResult)
        assertEquals(true, duplicateSuccess.response.isCorrect)
        assertEquals(10, duplicateSuccess.response.progress.totalScore)
        assertEquals(setOf("exercise-1"), duplicateSuccess.response.progress.completedExerciseIds)
        assertEquals(false, duplicateSuccess.response.lessonCompleted)
        val finalSuccess = assertIs<ExerciseAttemptResult.Success>(finalResult)
        assertEquals(true, finalSuccess.response.isCorrect)
        assertEquals(true, finalSuccess.response.lessonCompleted)
        assertEquals(25, finalSuccess.response.progress.totalScore)
        assertEquals(setOf("exercise-1", "exercise-2"), finalSuccess.response.progress.completedExerciseIds)
        assertEquals(setOf("lesson-1"), finalSuccess.response.progress.completedLessonIds)
        transaction {
            val completion = CompletedExercises.selectAll()
                .where {
                    (CompletedExercises.userId eq "learner-1") and
                        (CompletedExercises.exerciseId eq "exercise-1")
                }
                .single()
            assertEquals(2L, CompletedExercises.selectAll().count())
            assertEquals(10, completion[CompletedExercises.score])
            assertEquals(
                1L,
                CompletedLessons.selectAll()
                    .where {
                        (CompletedLessons.userId eq "learner-1") and
                            (CompletedLessons.lessonId eq "lesson-1")
                    }
                    .count()
            )
        }
    }

    @Test
    fun `attempt exercise rejects private exercise access for unenrolled learner`() {
        insertUser(id = "teacher-1", role = UserRole.TEACHER)
        insertUser(id = "learner-1", role = UserRole.STUDENT)
        insertCourse(id = "private-course", creatorId = "teacher-1", isOfficial = false)
        insertLesson(id = "lesson-1", courseId = "private-course")
        insertExercise(id = "exercise-1", lessonId = "lesson-1")
        val result = UserService().attemptExercise(
            userId = "learner-1",
            role = UserRole.STUDENT,
            request = ExerciseAttemptRequest(
                exerciseId = "exercise-1",
                submission = MultipleChoiceSubmission(selectedOptionId = "a"),
                score = 10
            )
        )
        assertEquals(ExerciseAttemptResult.Forbidden, result)
        transaction {
            assertEquals(0L, CompletedExercises.selectAll().count())
            assertEquals(0L, CompletedLessons.selectAll().count())
            assertEquals(0L, UserProgressTable.selectAll().count())
        }
    }

    @Test
    fun `attempt exercise allows standalone exercise for lesson creator`() {
        insertUser(id = "learner-owner", role = UserRole.STUDENT)
        insertLesson(id = "standalone-lesson", courseId = null, creatorId = "learner-owner")
        insertExercise(id = "standalone-exercise", lessonId = "standalone-lesson")

        val result = UserService().attemptExercise(
            userId = "learner-owner",
            role = UserRole.STUDENT,
            request = ExerciseAttemptRequest(
                exerciseId = "standalone-exercise",
                submission = MultipleChoiceSubmission(selectedOptionId = "a"),
                score = 7
            )
        )

        val success = assertIs<ExerciseAttemptResult.Success>(result)
        assertEquals(true, success.response.isCorrect)
        assertEquals("standalone-exercise", success.response.exerciseId)
        assertEquals("standalone-lesson", success.response.lessonId)
        assertEquals(true, success.response.lessonCompleted)
        assertEquals(7, success.response.progress.totalScore)
        assertEquals(setOf("standalone-exercise"), success.response.progress.completedExerciseIds)
        assertEquals(setOf("standalone-lesson"), success.response.progress.completedLessonIds)
    }

    @Test
    fun `attempt exercise rejects standalone exercise for non owner learner`() {
        insertUser(id = "learner-owner", role = UserRole.STUDENT)
        insertUser(id = "learner-other", role = UserRole.STUDENT)
        insertLesson(id = "standalone-lesson", courseId = null, creatorId = "learner-owner")
        insertExercise(id = "standalone-exercise", lessonId = "standalone-lesson")

        val result = UserService().attemptExercise(
            userId = "learner-other",
            role = UserRole.STUDENT,
            request = ExerciseAttemptRequest(
                exerciseId = "standalone-exercise",
                submission = MultipleChoiceSubmission(selectedOptionId = "a"),
                score = 7
            )
        )

        assertEquals(ExerciseAttemptResult.Forbidden, result)
        transaction {
            assertEquals(0L, CompletedExercises.selectAll().count())
            assertEquals(0L, CompletedLessons.selectAll().count())
            assertEquals(0L, UserProgressTable.selectAll().count())
        }
    }

    @Test
    fun `attempt exercise validates typed submissions and only completes on correct answer`() {
        insertUser(id = "admin-1", role = UserRole.ADMIN)
        insertUser(id = "learner-1", role = UserRole.STUDENT)
        insertCourse(id = "official-course", creatorId = "admin-1", isOfficial = true)
        insertLesson(id = "lesson-1", courseId = "official-course")
        insertExercise(
            id = "exercise-choice",
            lessonId = "lesson-1",
            question = "2 + 2 = ?",
            options = "3,4",
            correctAnswer = "4",
            type = ExerciseType.MULTIPLE_CHOICE
        )
        insertExercise(
            id = "exercise-input",
            lessonId = "lesson-1",
            question = "Write four",
            options = "",
            correctAnswer = "Four",
            type = ExerciseType.INPUT_VALUE
        )
        insertExercise(
            id = "exercise-multi",
            lessonId = "lesson-1",
            question = "Select prime numbers",
            options = "2,3,4",
            correctAnswer = "2,3",
            type = ExerciseType.MULTI_SELECT
        )

        val service = UserService()

        val wrongChoice = assertIs<ExerciseAttemptResult.Success>(
            service.attemptExercise(
                userId = "learner-1",
                role = UserRole.STUDENT,
                request = ExerciseAttemptRequest(
                    exerciseId = "exercise-choice",
                    submission = MultipleChoiceSubmission(selectedOptionId = "3"),
                    score = 10
                )
            )
        )
        assertEquals(false, wrongChoice.response.isCorrect)
        assertTrue(wrongChoice.response.message?.contains("Incorrect") == true)

        val correctChoice = assertIs<ExerciseAttemptResult.Success>(
            service.attemptExercise(
                userId = "learner-1",
                role = UserRole.STUDENT,
                request = ExerciseAttemptRequest(
                    exerciseId = "exercise-choice",
                    submission = MultipleChoiceSubmission(selectedOptionId = "4"),
                    score = 10
                )
            )
        )
        assertEquals(true, correctChoice.response.isCorrect)

        val correctInput = assertIs<ExerciseAttemptResult.Success>(
            service.attemptExercise(
                userId = "learner-1",
                role = UserRole.STUDENT,
                request = ExerciseAttemptRequest(
                    exerciseId = "exercise-input",
                    submission = InputValueSubmission(value = "  four  "),
                    score = 15
                )
            )
        )
        assertEquals(true, correctInput.response.isCorrect)

        val partialMulti = assertIs<ExerciseAttemptResult.Success>(
            service.attemptExercise(
                userId = "learner-1",
                role = UserRole.STUDENT,
                request = ExerciseAttemptRequest(
                    exerciseId = "exercise-multi",
                    submission = MultiSelectSubmission(selectedOptionIds = listOf("2")),
                    score = 20
                )
            )
        )
        assertEquals(false, partialMulti.response.isCorrect)

        val correctMulti = assertIs<ExerciseAttemptResult.Success>(
            service.attemptExercise(
                userId = "learner-1",
                role = UserRole.STUDENT,
                request = ExerciseAttemptRequest(
                    exerciseId = "exercise-multi",
                    submission = MultiSelectSubmission(selectedOptionIds = listOf("3", "2")),
                    score = 20
                )
            )
        )
        assertEquals(true, correctMulti.response.isCorrect)
        assertEquals(true, correctMulti.response.lessonCompleted)
        assertEquals(45, correctMulti.response.progress.totalScore)
        assertEquals(
            setOf("exercise-choice", "exercise-input", "exercise-multi"),
            correctMulti.response.progress.completedExerciseIds
        )
    }

    @Test
    fun `attempt exercise rejects malformed typed submissions`() {
        insertUser(id = "admin-1", role = UserRole.ADMIN)
        insertUser(id = "learner-1", role = UserRole.STUDENT)
        insertCourse(id = "official-course", creatorId = "admin-1", isOfficial = true)
        insertLesson(id = "lesson-1", courseId = "official-course")
        insertExercise(
            id = "exercise-choice",
            lessonId = "lesson-1",
            question = "2 + 2 = ?",
            options = "3,4",
            correctAnswer = "4",
            type = ExerciseType.MULTIPLE_CHOICE
        )

        val result = UserService().attemptExercise(
            userId = "learner-1",
            role = UserRole.STUDENT,
            request = ExerciseAttemptRequest(
                exerciseId = "exercise-choice",
                submission = InputValueSubmission(value = "4")
            )
        )

        val invalid = assertIs<ExerciseAttemptResult.InvalidRequest>(result)
        assertTrue(invalid.message.contains("submission type"))
    }
}

class SeedDataTest {
    @BeforeTest
    fun setUp() {
        initServiceTestDatabase()
        System.setProperty("seed.admin.id", "admin-001")
        System.setProperty("seed.admin.name", "Admin")
        System.setProperty("seed.admin.email", "admin-seed@example.com")
        System.setProperty("seed.admin.password", "seed-password")
    }

    @Test
    fun `repeat seeding inserts nothing new`() {
        SeedData.seedOfficialCourses()

        val countsAfterFirstSeed = transaction {
            Triple(
                Users.selectAll().count(),
                Courses.selectAll().count(),
                Pair(Lessons.selectAll().count(), Exercises.selectAll().count())
            )
        }

        SeedData.seedOfficialCourses()

        transaction {
            assertEquals(countsAfterFirstSeed.first, Users.selectAll().count())
            assertEquals(countsAfterFirstSeed.second, Courses.selectAll().count())
            assertEquals(countsAfterFirstSeed.third.first, Lessons.selectAll().count())
            assertEquals(countsAfterFirstSeed.third.second, Exercises.selectAll().count())
        }
    }

    @Test
    fun `new seed entity is inserted after prior seeding and pre-existing admin row is unchanged`() {
        val preExistingAdminId = "pre-existing-admin"
        insertUser(id = preExistingAdminId, role = UserRole.ADMIN, email = "admin-seed@example.com")

        SeedData.seedOfficialCourses()

        transaction {
            assertEquals(
                1L,
                Users.selectAll().where { Users.email eq "admin-seed@example.com" }.count()
            )
            assertEquals(
                preExistingAdminId,
                Users.selectAll().where { Users.email eq "admin-seed@example.com" }.single()[Users.id]
            )
            assertEquals(1L, Courses.selectAll().where { Courses.id eq "course-demo-qa" }.count())
            assertEquals(1L, Lessons.selectAll().where { Lessons.id eq "lesson-demo-theory" }.count())
            assertEquals(1L, Lessons.selectAll().where { Lessons.id eq "lesson-demo-exercises" }.count())
            assertEquals(1L, Exercises.selectAll().where { Exercises.id eq "ex-demo-mc-1" }.count())
            assertEquals(1L, Exercises.selectAll().where { Exercises.id eq "ex-demo-input-1" }.count())
            assertEquals(1L, Exercises.selectAll().where { Exercises.id eq "ex-demo-multi-1" }.count())
        }
    }

    @Test
    fun `demo course content materializes every payload type and arithmetic seed stays intact`() {
        SeedData.seedOfficialCourses()

        transaction {
            val demoCourse = Courses.selectAll().where { Courses.id eq "course-demo-qa" }.single()
            assertTrue(demoCourse[Courses.isOfficial])

            val theoryLesson = Lessons.selectAll().where { Lessons.id eq "lesson-demo-theory" }.single()
            val exercisesLesson = Lessons.selectAll().where { Lessons.id eq "lesson-demo-exercises" }.single()
            assertTrue(theoryLesson[Lessons.theoryContent].isNotBlank())
            assertTrue(exercisesLesson[Lessons.theoryContent].isNotBlank())

            val mcExercise = Exercises.selectAll().where { Exercises.id eq "ex-demo-mc-1" }.single()
            val inputExercise = Exercises.selectAll().where { Exercises.id eq "ex-demo-input-1" }.single()
            val multiExercise = Exercises.selectAll().where { Exercises.id eq "ex-demo-multi-1" }.single()

            assertIs<MultipleChoicePayload>(
                ExercisePayloadSupport.materializePayload(
                    persistedType = mcExercise[Exercises.type],
                    persistedPayload = mcExercise[Exercises.payload],
                    legacyOptions = mcExercise[Exercises.options],
                    legacyCorrectAnswer = mcExercise[Exercises.correctAnswer]
                ).payload
            )
            assertIs<InputValuePayload>(
                ExercisePayloadSupport.materializePayload(
                    persistedType = inputExercise[Exercises.type],
                    persistedPayload = inputExercise[Exercises.payload],
                    legacyOptions = inputExercise[Exercises.options],
                    legacyCorrectAnswer = inputExercise[Exercises.correctAnswer]
                ).payload
            )
            assertIs<MultiSelectPayload>(
                ExercisePayloadSupport.materializePayload(
                    persistedType = multiExercise[Exercises.type],
                    persistedPayload = multiExercise[Exercises.payload],
                    legacyOptions = multiExercise[Exercises.options],
                    legacyCorrectAnswer = multiExercise[Exercises.correctAnswer]
                ).payload
            )

            assertEquals(1L, Courses.selectAll().where { Courses.id eq "course-basic-arithmetic" }.count())
            val arithmeticExercise = Exercises.selectAll().where { Exercises.id eq "ex-add-3" }.single()
            assertEquals("True,False", arithmeticExercise[Exercises.options])
            assertEquals("True", arithmeticExercise[Exercises.correctAnswer])
            assertEquals("TRUE_FALSE", arithmeticExercise[Exercises.type])
        }
    }
}

private fun initServiceTestDatabase() {
    DatabaseFactory.init(
        url = "jdbc:h2:mem:${UUID.randomUUID()};MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        driver = "org.h2.Driver",
        user = "sa",
        password = ""
    )
}

private fun insertUser(
    id: String,
    role: UserRole,
    email: String = "$id@example.com",
    passwordHash: String = "hash",
    name: String = id
) {
    transaction {
        Users.insert {
            it[Users.id] = id
            it[Users.name] = name
            it[Users.email] = email
            it[Users.passwordHash] = passwordHash
            it[Users.role] = role.name
        }
    }
}

private fun insertCourse(
    id: String,
    creatorId: String,
    isOfficial: Boolean = false,
    joinCode: String? = null,
    schoolYear: Int = 0,
    title: String = id,
    description: String = id
) {
    transaction {
        Courses.insert {
            it[Courses.id] = id
            it[Courses.title] = title
            it[Courses.description] = description
            it[Courses.creatorId] = creatorId
            it[Courses.isOfficial] = isOfficial
            it[Courses.schoolYear] = schoolYear
            it[Courses.joinCode] = joinCode
        }
    }
}

private fun insertLesson(
    id: String,
    courseId: String?,
    creatorId: String? = null,
    orderIndex: Int = 0,
    title: String = id,
    theoryContent: String = id
) {
    transaction {
        Lessons.insert {
            it[Lessons.id] = id
            it[Lessons.courseId] = courseId
            it[Lessons.creatorId] = creatorId
            it[Lessons.title] = title
            it[Lessons.theoryContent] = theoryContent
            it[Lessons.orderIndex] = orderIndex
        }
    }
}

private fun insertExercise(
    id: String,
    lessonId: String,
    question: String = id,
    options: String = "a,b",
    correctAnswer: String = "a",
    type: ExerciseType = ExerciseType.MULTIPLE_CHOICE
) {
    transaction {
        Exercises.insert {
            it[Exercises.id] = id
            it[Exercises.lessonId] = lessonId
            it[Exercises.question] = question
            it[Exercises.options] = options
            it[Exercises.correctAnswer] = correctAnswer
            it[Exercises.type] = type.name
            it[Exercises.payload] = ExercisePayloadSupport.legacyPayloadJson(
                type = type,
                optionsCsv = options,
                correctAnswer = correctAnswer
            )
        }
    }
}

private fun enrollUser(userId: String, courseId: String) {
    transaction {
        EnrolledCourses.insert {
            it[EnrolledCourses.userId] = userId
            it[EnrolledCourses.courseId] = courseId
        }
    }
}
