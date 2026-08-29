package com.example.proyectofinal

import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.EnumColumnAdapter
import com.example.proyectofinal.data.KtorCourseRepository
import com.example.proyectofinal.db.AppDatabase
import com.example.proyectofinal.db.CourseEntity
import com.example.proyectofinal.db.ExerciseEntity
import com.example.proyectofinal.db.UserEntity
import com.example.proyectofinal.db.UserProgressEntity
import com.example.proyectofinal.db.createTestDriver
import com.example.proyectofinal.di.appModule
import com.example.proyectofinal.di.userRoleColumnAdapter
import com.example.proyectofinal.domain.AuthRepository
import com.example.proyectofinal.domain.AuthSession
import com.example.proyectofinal.domain.CourseRepository
import com.example.proyectofinal.domain.ExerciseRepository
import com.example.proyectofinal.domain.LearnerProfile
import com.example.proyectofinal.domain.LearnerProfileRepository
import com.example.proyectofinal.domain.LessonRepository
import com.example.proyectofinal.domain.StudentTrack
import com.example.proyectofinal.domain.UserRepository
import com.example.proyectofinal.models.Course
import com.example.proyectofinal.models.ChangePasswordRequest
import com.example.proyectofinal.models.Exercise
import com.example.proyectofinal.models.ExerciseAttemptResponse
import com.example.proyectofinal.models.ExerciseSubmission
import com.example.proyectofinal.models.ExerciseType
import com.example.proyectofinal.models.Lesson
import com.example.proyectofinal.models.ProfilePreferences
import com.example.proyectofinal.models.UpdateAvatarRequest
import com.example.proyectofinal.models.UpdateIdentityRequest
import com.example.proyectofinal.models.User
import com.example.proyectofinal.models.UserProgress
import com.example.proyectofinal.models.UserRole
import com.example.proyectofinal.ui.CourseUiState
import com.example.proyectofinal.ui.CourseViewModel
import com.example.proyectofinal.ui.ProfileViewModel
import com.example.proyectofinal.ui.activities.LessonMapViewModel
import io.ktor.client.HttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertSame

@OptIn(ExperimentalCoroutinesApi::class)
class AppModuleTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `app module wires http client and ktor course repository`() {
        val koinApp = koinApplication {
            allowOverride(true)
            modules(
                appModule,
                module {
                    single { createTestAppDatabase() }
                }
            )
        }

        try {
            val koin = koinApp.koin

            assertNotNull(koin.get<HttpClient>())
            assertIs<KtorCourseRepository>(koin.get<CourseRepository>())
        } finally {
            koinApp.close()
        }
    }

    @Test
    fun `app module resolves view models with controlled course repository`() = runTest(dispatcher) {
        val controlledCourseRepository = FakeCourseRepository { emptyList() }
        val koinApp = koinApplication {
            allowOverride(true)
            modules(
                appModule,
                module {
                    single { createTestAppDatabase() }
                    single<AuthRepository> { AppModuleTestAuthRepository }
                    single<UserRepository> { AppModuleTestUserRepository }
                    single<CourseRepository> { controlledCourseRepository }
                    single<LessonRepository> { AppModuleTestLessonRepository }
                    single<ExerciseRepository> { AppModuleTestExerciseRepository }
                    single<LearnerProfileRepository> { FakeLearnerProfileRepository(7) }
                }
            )
        }

        try {
            val koin = koinApp.koin

            assertSame(controlledCourseRepository, koin.get<CourseRepository>())
            val courseViewModel = koin.get<CourseViewModel>()
            val lessonMapViewModel = koin.get<LessonMapViewModel>()
            val profileViewModel = koin.get<ProfileViewModel>()
            advanceUntilIdle()

            assertNotNull(courseViewModel)
            assertNotNull(lessonMapViewModel)
            assertNotNull(profileViewModel)
        } finally {
            advanceUntilIdle()
            koinApp.close()
            advanceUntilIdle()
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class CourseViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `view model transitions from loading to success`() = runTest(dispatcher) {
        val expectedSchoolYear = 7
        val expectedCourses = listOf(
            Course(
                id = "course-1",
                title = "Fractions",
                description = "Learn fractions",
                creatorId = "teacher-1",
                isOfficial = true
            )
        )
        val emittedStates = mutableListOf<CourseUiState>()
        val fakeCourseRepository = FakeCourseRepository { schoolYear ->
            assertEquals(expectedSchoolYear, schoolYear)
            expectedCourses
        }
        val viewModel = CourseViewModel(
            fakeCourseRepository,
            FakeLearnerProfileRepository(expectedSchoolYear),
            AppModuleTestAuthRepository
        )

        val collectionJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.take(2).toList(emittedStates)
        }

        advanceUntilIdle()

        assertEquals(
            listOf(CourseUiState.Loading, CourseUiState.Success(expectedCourses)),
            emittedStates
        )
        collectionJob.cancel()
    }

    @Test
    fun `view model transitions from loading to error`() = runTest(dispatcher) {
        val emittedStates = mutableListOf<CourseUiState>()
        val viewModel = CourseViewModel(
            FakeCourseRepository { throw IllegalStateException("Network unavailable") },
            FakeLearnerProfileRepository(null),
            AppModuleTestAuthRepository
        )

        val collectionJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.take(2).toList(emittedStates)
        }

        advanceUntilIdle()

        assertEquals(CourseUiState.Loading, emittedStates.first())
        assertEquals(CourseUiState.Error("Network unavailable"), emittedStates.last())
        collectionJob.cancel()
    }
}

private class FakeCourseRepository(
    private val officialCoursesProvider: suspend (Int?) -> List<Course>
) : CourseRepository {
    override suspend fun getOfficialCourses(schoolYear: Int?): List<Course> = officialCoursesProvider(schoolYear)

    override suspend fun getCourseById(id: String): Course? = null

    override suspend fun getMyCreatedCourses(creatorId: String): List<Course> = emptyList()

    override suspend fun getEnrolledCourses(userId: String): List<Course> = emptyList()

    override suspend fun getStudentsProgress(courseId: String) = error("Not used")

    override suspend fun createCourse(course: Course): Course = course

    override suspend fun updateCourse(course: Course): Course = course

    override suspend fun deleteCourse(id: String) = Unit

    override suspend fun joinCourseByCode(userId: String, code: String): Course? = null
}

private class FakeLearnerProfileRepository(
    schoolYear: Int?
) : LearnerProfileRepository {
    private val profile = schoolYear?.let { year ->
        LearnerProfile(
            province = "Buenos Aires",
            schoolYear = year,
            studentTrack = StudentTrack.SECONDARY,
            onboardingComplete = true
        )
    }

    override suspend fun getProfile(userId: String): LearnerProfile? = profile

    override suspend fun isOnboardingComplete(userId: String): Boolean = profile?.onboardingComplete == true

    override suspend fun upsertProfile(userId: String, profile: LearnerProfile) = Unit
}

private fun createTestAppDatabase(): AppDatabase {
    val intAdapter = object : ColumnAdapter<Int, Long> {
        override fun decode(databaseValue: Long): Int = databaseValue.toInt()

        override fun encode(value: Int): Long = value.toLong()
    }

    return AppDatabase(
        driver = createTestDriver(),
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

private val appModuleTestUser = User(
    id = "user-1",
    name = "Test Student",
    email = "student@example.com",
    role = UserRole.STUDENT
)

private object AppModuleTestAuthRepository : AuthRepository {
    override val session = MutableStateFlow(AuthSession(token = "token-123", user = appModuleTestUser))

    override suspend fun login(email: String, password: String): Result<User> = Result.success(appModuleTestUser)

    override suspend fun register(name: String, email: String, password: String): Result<User> = Result.success(appModuleTestUser)

    override fun replaceSessionUser(user: User, expectedToken: String?) = Unit

    override fun logout() = Unit
}

private object AppModuleTestUserRepository : UserRepository {
    override suspend fun getCurrentUser(): User? = appModuleTestUser

    override suspend fun getUserRole(userId: String): UserRole = UserRole.STUDENT

    override suspend fun updateUser(user: User) = Unit
    override suspend fun updateIdentity(request: UpdateIdentityRequest): User = error("Not used")
    override suspend fun changePassword(request: ChangePasswordRequest) = error("Not used")
    override suspend fun getProfilePreferences(): ProfilePreferences = error("Not used")
    override suspend fun updateProfilePreferences(preferences: ProfilePreferences): ProfilePreferences = error("Not used")
    override suspend fun updateAvatar(request: UpdateAvatarRequest): ProfilePreferences = error("Not used")

    override suspend fun getUserProgress(userId: String): UserProgress = UserProgress(
        userId = userId,
        enrolledCourseIds = setOf("course-1")
    )

    override suspend fun attemptExercise(
        exerciseId: String,
        submission: ExerciseSubmission,
        score: Int
    ): ExerciseAttemptResponse = ExerciseAttemptResponse(
        exerciseId = exerciseId,
        lessonId = "lesson-1",
        isCorrect = true,
        progress = UserProgress(userId = appModuleTestUser.id, completedExerciseIds = setOf(exerciseId))
    )
}

private object AppModuleTestLessonRepository : LessonRepository {
    override suspend fun getLessonsByCourse(courseId: String): List<Lesson> = listOf(
        Lesson(
            id = "lesson-1",
            courseId = courseId,
            title = "Lesson 1",
            theoryContent = "Theory"
        )
    )

    override suspend fun getLessonById(id: String): Lesson? = null

    override suspend fun createLesson(lesson: Lesson): Lesson = lesson

    override suspend fun updateLesson(lesson: Lesson): Lesson = lesson

    override suspend fun updateTheory(lessonId: String, content: String): Lesson =
        Lesson(id = lessonId, courseId = "course-1", title = "Lesson 1", theoryContent = content)

    override suspend fun deleteLesson(id: String) = Unit
}

private object AppModuleTestExerciseRepository : ExerciseRepository {
    override suspend fun getExercisesByLesson(lessonId: String): List<Exercise> = listOf(
        Exercise(
            id = "exercise-1",
            lessonId = lessonId,
            question = "Question",
            options = listOf("A", "B"),
            correctAnswer = "A",
            type = ExerciseType.MULTIPLE_CHOICE
        )
    )

    override suspend fun createExercise(exercise: Exercise): Exercise = exercise

    override suspend fun updateExercise(exercise: Exercise): Exercise = exercise

    override suspend fun deleteExercise(id: String) = Unit
}
