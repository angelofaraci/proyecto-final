package com.example.proyectofinal.ui

import com.example.proyectofinal.domain.AuthRepository
import com.example.proyectofinal.domain.AuthSession
import com.example.proyectofinal.domain.LearnerProfile
import com.example.proyectofinal.domain.LearnerProfileRepository
import com.example.proyectofinal.domain.StudentTrack
import com.example.proyectofinal.domain.UserRepository
import com.example.proyectofinal.models.ExerciseAttemptResponse
import com.example.proyectofinal.models.ExerciseSubmission
import com.example.proyectofinal.models.ChangePasswordRequest
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
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `view model derives profile metrics from user progress`() = runTest(dispatcher) {
        val viewModel = ProfileViewModel(
            authRepository = ProfileFakeAuthRepository(testUser),
            userRepository = FakeUserRepository(
                progress = UserProgress(
                    userId = testUser.id,
                    completedLessonIds = (1..12).map { "lesson-$it" }.toSet(),
                    totalScore = 350
                )
            ),
            learnerProfileRepository = ProfileFakeLearnerProfileRepository()
        )

        advanceUntilIdle()

        with(viewModel.uiState.value) {
            assertFalse(isLoading)
            assertEquals("Alice Student", displayName)
            assertEquals("alice@example.com", email)
            assertEquals(UserRole.STUDENT, role)
            assertEquals(7, schoolYear)
            assertEquals(StudentTrack.SECONDARY, studentTrack)
            assertEquals(3, level)
            assertEquals(50, currentXp)
            assertEquals(100, xpForNextLevel)
            assertEquals(7, streak)
            assertEquals(12, completedLessons)
            assertEquals(listOf(true, true, true, true), achievements.map { it.isUnlocked })
        }
    }

    @Test
    fun `view model keeps below-cap streak and locked achievements when thresholds are not met`() = runTest(dispatcher) {
        val viewModel = ProfileViewModel(
            authRepository = ProfileFakeAuthRepository(testUser),
            userRepository = FakeUserRepository(
                progress = UserProgress(
                    userId = testUser.id,
                    completedLessonIds = setOf("lesson-1", "lesson-2", "lesson-3"),
                    totalScore = 0
                )
            ),
            learnerProfileRepository = ProfileFakeLearnerProfileRepository()
        )

        advanceUntilIdle()

        with(viewModel.uiState.value) {
            assertEquals(0, level)
            assertEquals(0, currentXp)
            assertEquals(3, streak)
            assertEquals(3, completedLessons)
            assertEquals(listOf(true, false, false, false), achievements.map { it.isUnlocked })
        }
    }

    @Test
    fun `view model exposes error message when repositories fail`() = runTest(dispatcher) {
        val viewModel = ProfileViewModel(
            authRepository = ProfileFakeAuthRepository(testUser),
            userRepository = FakeUserRepository(errorMessage = "Progress unavailable"),
            learnerProfileRepository = ProfileFakeLearnerProfileRepository()
        )

        advanceUntilIdle()

        with(viewModel.uiState.value) {
            assertFalse(isLoading)
            assertEquals("Alice Student", displayName)
            assertEquals("", email)
            assertEquals(UserRole.STUDENT, role)
            assertEquals("Progress unavailable", errorMessage)
            assertTrue(achievements.isEmpty())
        }
    }
}

private val testUser = User(
    id = "user-1",
    name = "Alice Student",
    email = "alice@example.com",
    role = UserRole.STUDENT
)

private class ProfileFakeAuthRepository(user: User) : AuthRepository {
    private val state = MutableStateFlow(AuthSession(token = "token-123", user = user))
    override val session: StateFlow<AuthSession> = state
    override suspend fun login(email: String, password: String): Result<User> = Result.success(testUser)
    override suspend fun register(name: String, email: String, password: String): Result<User> = Result.success(testUser)
    override fun replaceSessionUser(user: User, expectedToken: String?) { state.value = state.value.copy(user = user) }
    override fun logout() = Unit
}

private class FakeUserRepository(
    private val progress: UserProgress? = null,
    private val errorMessage: String? = null
) : UserRepository {
    override suspend fun getCurrentUser(): User? = testUser
    override suspend fun getUserRole(userId: String): UserRole = UserRole.STUDENT
    override suspend fun updateUser(user: User) = Unit
    override suspend fun updateIdentity(request: UpdateIdentityRequest): User = error("Not used")
    override suspend fun changePassword(request: ChangePasswordRequest) = error("Not used")
    override suspend fun getProfilePreferences(): ProfilePreferences = error("Not used")
    override suspend fun updateProfilePreferences(preferences: ProfilePreferences): ProfilePreferences = error("Not used")
    override suspend fun updateAvatar(request: UpdateAvatarRequest): ProfilePreferences = error("Not used")
    override suspend fun getUserProgress(userId: String): UserProgress {
        errorMessage?.let { throw IllegalStateException(it) }
        return requireNotNull(progress)
    }

    override suspend fun attemptExercise(
        exerciseId: String,
        submission: ExerciseSubmission,
        score: Int
    ): ExerciseAttemptResponse = error("Not used in these tests")
}

private class ProfileFakeLearnerProfileRepository : LearnerProfileRepository {
    override suspend fun getProfile(userId: String): LearnerProfile = LearnerProfile("Buenos Aires", 7, StudentTrack.SECONDARY, true)
    override suspend fun isOnboardingComplete(userId: String): Boolean = true
    override suspend fun upsertProfile(userId: String, profile: LearnerProfile) = Unit
}
