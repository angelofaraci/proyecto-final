package com.example.proyectofinal.ui.home

import com.example.proyectofinal.domain.AuthRepository
import com.example.proyectofinal.domain.AuthSession
import com.example.proyectofinal.domain.CourseRepository
import com.example.proyectofinal.domain.LearnerProfile
import com.example.proyectofinal.domain.LearnerProfileRepository
import com.example.proyectofinal.domain.StudentTrack
import com.example.proyectofinal.domain.UserRepository
import com.example.proyectofinal.models.ExerciseAttemptResponse
import com.example.proyectofinal.models.ExerciseSubmission
import com.example.proyectofinal.models.ChangePasswordRequest
import com.example.proyectofinal.models.Course
import com.example.proyectofinal.models.Lesson
import com.example.proyectofinal.models.ProfilePreferences
import com.example.proyectofinal.models.UpdateAvatarRequest
import com.example.proyectofinal.models.UpdateIdentityRequest
import com.example.proyectofinal.models.User
import com.example.proyectofinal.models.UserProgress
import com.example.proyectofinal.models.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class HomeDashboardViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `salutation changes with the hour of day`() {
        assertEquals("Buenos días", salutation(hour = 8))
        assertEquals("Buenas tardes", salutation(hour = 14))
        assertEquals("Buenas noches", salutation(hour = 21))
    }

    @Test
    fun `streak preserves activity count below the seven-day cap`() {
        assertEquals(3, activityStreak(3))
        assertEquals(7, activityStreak(12))
    }

    @Test
    fun `view model falls back to a generic greeting when display name is blank`() = runTest(dispatcher) {
        val viewModel = HomeDashboardViewModel(
            authRepository = HomeDashboardFakeAuthRepository(testUser.copy(name = "   ")),
            courseRepository = FakeHomeDashboardCourseRepository(),
            userRepository = FakeHomeDashboardUserRepository(
                progress = UserProgress(userId = testUser.id, completedLessonIds = setOf("lesson-1"), totalScore = 50)
            ),
            learnerProfileRepository = HomeDashboardFakeLearnerProfileRepository()
        )

        advanceUntilIdle()

        with(viewModel.uiState.value) {
            assertFalse(isLoading)
            assertTrue(greeting in setOf("Buenos días", "Buenas tardes", "Buenas noches"))
            assertEquals(7, schoolYear)
            assertEquals(StudentTrack.SECONDARY, studentTrack)
        }
    }

    @Test
    fun `view model derives level math and caps streak at seven`() = runTest(dispatcher) {
        val viewModel = HomeDashboardViewModel(
            authRepository = HomeDashboardFakeAuthRepository(testUser),
            courseRepository = FakeHomeDashboardCourseRepository(),
            userRepository = FakeHomeDashboardUserRepository(
                progress = UserProgress(
                    userId = testUser.id,
                    completedLessonIds = (1..12).map { "lesson-$it" }.toSet(),
                    totalScore = 350
                )
            ),
            learnerProfileRepository = HomeDashboardFakeLearnerProfileRepository()
        )

        advanceUntilIdle()

        with(viewModel.uiState.value) {
            assertTrue(greeting.endsWith("Alice Student"))
            assertEquals(3, level)
            assertEquals(7, streak)
            assertEquals(50, currentXp)
            assertEquals(100, xpForNextLevel)
            assertEquals(12, completedLessons)
        }
    }

    @Test
    fun `view model keeps the progress card state at zero when progress is empty`() = runTest(dispatcher) {
        val viewModel = HomeDashboardViewModel(
            authRepository = HomeDashboardFakeAuthRepository(testUser),
            courseRepository = FakeHomeDashboardCourseRepository(),
            userRepository = FakeHomeDashboardUserRepository(
                progress = UserProgress(
                    userId = testUser.id,
                    completedLessonIds = emptySet(),
                    totalScore = 0
                )
            ),
            learnerProfileRepository = HomeDashboardFakeLearnerProfileRepository()
        )

        advanceUntilIdle()

        with(viewModel.uiState.value) {
            assertFalse(isLoading)
            assertEquals(0, level)
            assertEquals(0, streak)
            assertEquals(0, currentXp)
            assertEquals(100, xpForNextLevel)
            assertEquals(0, completedLessons)
            assertTrue(greeting.endsWith("Alice Student"))
        }
    }

    @Test
    fun `view model exposes per-course completion percentages for enrolled courses`() = runTest(dispatcher) {
        val enrolledCourses = listOf(
            Course(
                id = "course-1",
                title = "Fracciones - Básico",
                description = "Learn fractions",
                creatorId = "teacher-1",
                lessons = (1..4).map { Lesson(id = "lesson-$it", title = "Lesson $it", theoryContent = "") }
            ),
            Course(
                id = "course-2",
                title = "Álgebra Inicial",
                description = "Learn algebra",
                creatorId = "teacher-1",
                lessons = (5..6).map { Lesson(id = "lesson-$it", title = "Lesson $it", theoryContent = "") }
            )
        )
        val viewModel = HomeDashboardViewModel(
            authRepository = HomeDashboardFakeAuthRepository(testUser),
            courseRepository = FakeHomeDashboardCourseRepository(enrolledCourses = enrolledCourses),
            userRepository = FakeHomeDashboardUserRepository(
                progress = UserProgress(
                    userId = testUser.id,
                    enrolledCourseIds = setOf("course-1", "course-2"),
                    completedLessonIds = setOf("lesson-1", "lesson-2"),
                    totalScore = 20
                )
            ),
            learnerProfileRepository = HomeDashboardFakeLearnerProfileRepository()
        )

        advanceUntilIdle()

        with(viewModel.uiState.value) {
            assertFalse(isLoading)
            assertEquals(
                listOf(
                    HomeCourseProgress(courseId = "course-1", title = "Fracciones - Básico", progressPercent = 50),
                    HomeCourseProgress(courseId = "course-2", title = "Álgebra Inicial", progressPercent = 0)
                ),
                inProgressCourses
            )
        }
    }

    @Test
    fun `view model degrades to an empty course list when the course fetch fails`() = runTest(dispatcher) {
        val viewModel = HomeDashboardViewModel(
            authRepository = HomeDashboardFakeAuthRepository(testUser),
            courseRepository = FakeHomeDashboardCourseRepository(coursesErrorMessage = "Courses unavailable"),
            userRepository = FakeHomeDashboardUserRepository(
                progress = UserProgress(
                    userId = testUser.id,
                    enrolledCourseIds = setOf("course-1"),
                    completedLessonIds = setOf("lesson-1"),
                    totalScore = 10
                )
            ),
            learnerProfileRepository = HomeDashboardFakeLearnerProfileRepository()
        )

        advanceUntilIdle()

        with(viewModel.uiState.value) {
            assertFalse(isLoading)
            assertEquals(null, errorMessage)
            assertTrue(hasEnrolledCourse)
            assertEquals(emptyList(), inProgressCourses)
        }
    }

    @Test
    fun `course fetch cancellation is propagated instead of degrading to an empty list`() = runTest(dispatcher) {
        val viewModel = HomeDashboardViewModel(
            authRepository = HomeDashboardFakeAuthRepository(testUser),
            courseRepository = FakeHomeDashboardCourseRepository(cancelCourseFetch = true),
            userRepository = FakeHomeDashboardUserRepository(
                progress = UserProgress(userId = testUser.id, enrolledCourseIds = setOf("course-1"))
            ),
            learnerProfileRepository = HomeDashboardFakeLearnerProfileRepository()
        )

        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `view model exposes error state when progress loading fails`() = runTest(dispatcher) {
        val viewModel = HomeDashboardViewModel(
            authRepository = HomeDashboardFakeAuthRepository(testUser),
            courseRepository = FakeHomeDashboardCourseRepository(),
            userRepository = FakeHomeDashboardUserRepository(errorMessage = "Progress unavailable"),
            learnerProfileRepository = HomeDashboardFakeLearnerProfileRepository()
        )

        advanceUntilIdle()

        with(viewModel.uiState.value) {
            assertFalse(isLoading)
            assertEquals("Progress unavailable", errorMessage)
            assertTrue(greeting.endsWith("Alice Student"))
        }
    }

    @Test
    fun `view model restores the current user when token session has no embedded user`() = runTest(dispatcher) {
        val viewModel = HomeDashboardViewModel(
            authRepository = HomeDashboardFakeAuthRepository(user = null),
            courseRepository = FakeHomeDashboardCourseRepository(),
            userRepository = FakeHomeDashboardUserRepository(
                progress = UserProgress(userId = testUser.id, enrolledCourseIds = emptySet()),
                currentUser = testUser
            ),
            learnerProfileRepository = HomeDashboardFakeLearnerProfileRepository()
        )

        advanceUntilIdle()

        with(viewModel.uiState.value) {
            assertFalse(isLoading)
            assertTrue(greeting.endsWith("Alice Student"))
            assertFalse(hasEnrolledCourse)
            assertEquals(null, errorMessage)
        }
    }

    @Test
    fun `join course updates the dashboard when the user was not enrolled`() = runTest(dispatcher) {
        val courseRepository = FakeHomeDashboardCourseRepository(
            joinedCourse = Course(
                id = "course-1",
                title = "Fractions",
                description = "Learn fractions",
                creatorId = "teacher-1",
                joinCode = "FRACTIONS-7A"
            )
        )
        val userRepository = FakeHomeDashboardUserRepository(
            progress = UserProgress(userId = testUser.id, enrolledCourseIds = emptySet()),
            currentUser = testUser,
            progressProvider = {
                if (courseRepository.joinCalls.isEmpty()) {
                    UserProgress(userId = testUser.id, enrolledCourseIds = emptySet())
                } else {
                    UserProgress(userId = testUser.id, enrolledCourseIds = setOf("course-1"))
                }
            }
        )
        val viewModel = HomeDashboardViewModel(
            authRepository = HomeDashboardFakeAuthRepository(user = null),
            courseRepository = courseRepository,
            userRepository = userRepository,
            learnerProfileRepository = HomeDashboardFakeLearnerProfileRepository()
        )

        advanceUntilIdle()
        viewModel.joinCourse(" FRACTIONS-7A ")
        advanceUntilIdle()

        assertEquals(listOf("user-1:FRACTIONS-7A"), courseRepository.joinCalls)
        assertTrue(viewModel.uiState.value.hasEnrolledCourse)
        assertEquals(null, viewModel.uiState.value.joinCourseMessage)
    }
}

private val testUser = User(
    id = "user-1",
    name = "Alice Student",
    email = "alice@example.com",
    role = UserRole.STUDENT
)

private class HomeDashboardFakeAuthRepository(user: User?) : AuthRepository {
    private val state = MutableStateFlow(AuthSession(token = "token-123", user = user))
    override val session: StateFlow<AuthSession> = state
    override suspend fun login(email: String, password: String): Result<User> = Result.success(testUser)
    override suspend fun register(name: String, email: String, password: String): Result<User> = Result.success(testUser)
    override fun replaceSessionUser(user: User, expectedToken: String?) { state.value = state.value.copy(user = user) }
    override fun logout() = Unit
}

private class FakeHomeDashboardCourseRepository(
    private val joinedCourse: Course? = null,
    private val enrolledCourses: List<Course> = emptyList(),
    private val coursesErrorMessage: String? = null,
    private val cancelCourseFetch: Boolean = false
) : CourseRepository {
    val joinCalls = mutableListOf<String>()

    override suspend fun getOfficialCourses(schoolYear: Int?): List<Course> = emptyList()

    override suspend fun getCourseById(id: String): Course? = null

    override suspend fun getMyCreatedCourses(creatorId: String): List<Course> = emptyList()

    override suspend fun getStudentsProgress(courseId: String) = error("Not used")

    override suspend fun getEnrolledCourses(userId: String): List<Course> {
        if (cancelCourseFetch) throw CancellationException("Course fetch cancelled")
        coursesErrorMessage?.let { throw IllegalStateException(it) }
        return enrolledCourses
    }

    override suspend fun createCourse(course: Course): Course = course

    override suspend fun updateCourse(course: Course): Course = course

    override suspend fun deleteCourse(id: String) = Unit

    override suspend fun joinCourseByCode(userId: String, code: String): Course? {
        joinCalls += "$userId:$code"
        return joinedCourse
    }
}

private class FakeHomeDashboardUserRepository(
    private val progress: UserProgress? = null,
    private val errorMessage: String? = null,
    private val currentUser: User? = testUser,
    private val progressProvider: (() -> UserProgress)? = null
) : UserRepository {
    override suspend fun getCurrentUser(): User? = currentUser
    override suspend fun getUserRole(userId: String): UserRole = UserRole.STUDENT
    override suspend fun updateUser(user: User) = Unit
    override suspend fun updateIdentity(request: UpdateIdentityRequest): User = error("Not used")
    override suspend fun changePassword(request: ChangePasswordRequest) = error("Not used")
    override suspend fun getProfilePreferences(): ProfilePreferences = error("Not used")
    override suspend fun updateProfilePreferences(preferences: ProfilePreferences): ProfilePreferences = error("Not used")
    override suspend fun updateAvatar(request: UpdateAvatarRequest): ProfilePreferences = error("Not used")

    override suspend fun getUserProgress(userId: String): UserProgress {
        errorMessage?.let { throw IllegalStateException(it) }
        return progressProvider?.invoke() ?: requireNotNull(progress)
    }

    override suspend fun attemptExercise(
        exerciseId: String,
        submission: ExerciseSubmission,
        score: Int
    ): ExerciseAttemptResponse = error("Not used in these tests")
}

private class HomeDashboardFakeLearnerProfileRepository : LearnerProfileRepository {
    override suspend fun getProfile(userId: String): LearnerProfile = LearnerProfile("Buenos Aires", 7, StudentTrack.SECONDARY, true)
    override suspend fun isOnboardingComplete(userId: String): Boolean = true
    override suspend fun upsertProfile(userId: String, profile: LearnerProfile) = Unit
}
