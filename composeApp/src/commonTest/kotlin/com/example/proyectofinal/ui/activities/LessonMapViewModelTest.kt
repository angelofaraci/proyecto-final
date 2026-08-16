package com.example.proyectofinal.ui.activities

import com.example.proyectofinal.domain.AuthRepository
import com.example.proyectofinal.domain.AuthSession
import com.example.proyectofinal.domain.ExerciseRepository
import com.example.proyectofinal.domain.LessonRepository
import com.example.proyectofinal.domain.UserRepository
import com.example.proyectofinal.models.ChoiceOption
import com.example.proyectofinal.models.Exercise
import com.example.proyectofinal.models.ExerciseAttemptResponse
import com.example.proyectofinal.models.ExerciseSubmission
import com.example.proyectofinal.models.InputValuePayload
import com.example.proyectofinal.models.InputValueSubmission
import com.example.proyectofinal.models.Lesson
import com.example.proyectofinal.models.MultiSelectPayload
import com.example.proyectofinal.models.MultiSelectSubmission
import com.example.proyectofinal.models.MultipleChoicePayload
import com.example.proyectofinal.models.MultipleChoiceSubmission
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
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class LessonMapViewModelTest {
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
    fun `initial load unlocks only the first exercise`() = runTest(dispatcher) {
        val viewModel = LessonMapViewModel(
            authRepository = FakeLessonMapAuthRepository(testUser),
            userRepository = FakeLessonMapUserRepository(
                progress = testProgress()
            ),
            lessonRepository = FakeLessonRepository(),
            exerciseRepository = FakeExerciseRepository()
        )

        advanceUntilIdle()

        assertEquals(
            listOf(
                LessonNodeState.Unlocked,
                LessonNodeState.Locked,
                LessonNodeState.Locked,
                LessonNodeState.Locked
            ),
            viewModel.uiState.value.nodes.map(LessonMapNodeUiModel::state)
        )
        assertEquals("exercise-fractions-1", viewModel.uiState.value.activeNode?.exercise?.id)
    }

    @Test
    fun `completed progress unlocks the next sequential exercise`() = runTest(dispatcher) {
        val viewModel = LessonMapViewModel(
            authRepository = FakeLessonMapAuthRepository(testUser),
            userRepository = FakeLessonMapUserRepository(
                progress = UserProgress(
                    userId = testUser.id,
                    completedExerciseIds = setOf("exercise-fractions-1"),
                    enrolledCourseIds = setOf("course-fractions")
                )
            ),
            lessonRepository = FakeLessonRepository(),
            exerciseRepository = FakeExerciseRepository()
        )

        advanceUntilIdle()

        assertEquals(
            listOf(
                LessonNodeState.Completed,
                LessonNodeState.Unlocked,
                LessonNodeState.Locked,
                LessonNodeState.Locked
            ),
            viewModel.uiState.value.nodes.map(LessonMapNodeUiModel::state)
        )
    }

    @Test
    fun `selecting a locked exercise is ignored while unlocked exercise becomes current`() = runTest(dispatcher) {
        val viewModel = LessonMapViewModel(
            authRepository = FakeLessonMapAuthRepository(testUser),
            userRepository = FakeLessonMapUserRepository(
                progress = testProgress()
            ),
            lessonRepository = FakeLessonRepository(),
            exerciseRepository = FakeExerciseRepository()
        )

        advanceUntilIdle()
        viewModel.selectExercise("exercise-fractions-3")

        assertNull(viewModel.uiState.value.selectedExerciseId)

        viewModel.selectExercise("exercise-fractions-1")

        assertEquals("exercise-fractions-1", viewModel.uiState.value.selectedExerciseId)
        assertEquals(LessonNodeState.Current, viewModel.uiState.value.nodes.first().state)
    }

    @Test
    fun `theory action opens and dismisses the lesson theory`() = runTest(dispatcher) {
        val viewModel = LessonMapViewModel(
            authRepository = FakeLessonMapAuthRepository(testUser),
            userRepository = FakeLessonMapUserRepository(
                progress = testProgress()
            ),
            lessonRepository = FakeLessonRepository(),
            exerciseRepository = FakeExerciseRepository()
        )

        advanceUntilIdle()
        viewModel.openTheory()

        assertEquals("lesson-map-fractions", viewModel.uiState.value.selectedTheoryLesson?.id)

        viewModel.dismissTheory()

        assertNull(viewModel.uiState.value.selectedTheoryLesson)
    }

    @Test
    fun `wrong answer keeps exercise active until a correct retry advances`() = runTest(dispatcher) {
        val userRepository = FakeLessonMapUserRepository(
            progress = testProgress(),
            attemptResponses = listOf(
                ExerciseAttemptResponse(
                    exerciseId = "exercise-fractions-1",
                    lessonId = "lesson-map-fractions",
                    isCorrect = false,
                    message = "Incorrect answer. Try again.",
                    progress = testProgress()
                ),
                ExerciseAttemptResponse(
                    exerciseId = "exercise-fractions-1",
                    lessonId = "lesson-map-fractions",
                    isCorrect = true,
                    lessonCompleted = false,
                    progress = UserProgress(
                        userId = testUser.id,
                        completedExerciseIds = setOf("exercise-fractions-1"),
                        totalScore = 100,
                        enrolledCourseIds = setOf("course-fractions")
                    )
                )
            )
        )
        val viewModel = LessonMapViewModel(
            authRepository = FakeLessonMapAuthRepository(testUser),
            userRepository = userRepository,
            lessonRepository = FakeLessonRepository(),
            exerciseRepository = FakeExerciseRepository()
        )

        advanceUntilIdle()
        viewModel.selectExercise("exercise-fractions-1")
        viewModel.selectMultipleChoiceAnswer("one-third")
        viewModel.submitAnswer()
        advanceUntilIdle()

        assertEquals("exercise-fractions-1", viewModel.uiState.value.activeExerciseId)
        assertEquals(ActiveExercisePhase.RetryReady, viewModel.uiState.value.activeExercisePhase)
        assertEquals("Incorrect answer. Try again.", viewModel.uiState.value.exerciseFeedbackMessage)
        assertEquals(2, viewModel.uiState.value.remainingLives)

        viewModel.selectMultipleChoiceAnswer("one-quarter")
        viewModel.submitAnswer()
        advanceUntilIdle()

        assertEquals(
            listOf<ExerciseSubmission>(
                MultipleChoiceSubmission("one-third"),
                MultipleChoiceSubmission("one-quarter")
            ),
            userRepository.attemptCalls
        )
        assertEquals("Exercise completed. Keep going.", viewModel.uiState.value.exerciseFeedbackMessage)
        assertEquals(
            listOf(
                LessonNodeState.Completed,
                LessonNodeState.Unlocked,
                LessonNodeState.Locked,
                LessonNodeState.Locked
            ),
            viewModel.uiState.value.nodes.map(LessonMapNodeUiModel::state)
        )
    }

    @Test
    fun `multiple wrong attempts keep retry available until a correct answer advances`() = runTest(dispatcher) {
        val initialProgress = testProgress()
        val completedProgress = testProgress(completedExerciseIds = setOf("exercise-fractions-1"))
        val userRepository = FakeLessonMapUserRepository(
            progress = initialProgress,
            attemptResponses = listOf(
                ExerciseAttemptResponse(
                    exerciseId = "exercise-fractions-1",
                    lessonId = "lesson-map-fractions",
                    isCorrect = false,
                    progress = initialProgress
                ),
                ExerciseAttemptResponse(
                    exerciseId = "exercise-fractions-1",
                    lessonId = "lesson-map-fractions",
                    isCorrect = false,
                    progress = initialProgress
                ),
                ExerciseAttemptResponse(
                    exerciseId = "exercise-fractions-1",
                    lessonId = "lesson-map-fractions",
                    isCorrect = false,
                    progress = initialProgress
                ),
                ExerciseAttemptResponse(
                    exerciseId = "exercise-fractions-1",
                    lessonId = "lesson-map-fractions",
                    isCorrect = true,
                    progress = completedProgress
                )
            )
        )
        val viewModel = lessonMapViewModel(userRepository)

        advanceUntilIdle()
        viewModel.selectExercise("exercise-fractions-1")
        listOf("one-half", "one-third", "four-over-one").forEach { optionId ->
            viewModel.selectMultipleChoiceAnswer(optionId)
            viewModel.submitAnswer()
            advanceUntilIdle()
            assertEquals("exercise-fractions-1", viewModel.uiState.value.activeExerciseId)
            assertEquals(ActiveExercisePhase.RetryReady, viewModel.uiState.value.activeExercisePhase)
        }

        assertEquals(0, viewModel.uiState.value.remainingLives)
        viewModel.selectMultipleChoiceAnswer("one-quarter")
        viewModel.submitAnswer()
        advanceUntilIdle()

        assertEquals(4, userRepository.attemptCalls.size)
        assertEquals("Exercise completed. Keep going.", viewModel.uiState.value.exerciseFeedbackMessage)
        assertEquals(LessonNodeState.Completed, viewModel.uiState.value.nodes.first().state)
    }

    @Test
    fun `input value submission is trimmed before the attempt`() = runTest(dispatcher) {
        val initialProgress = testProgress(completedExerciseIds = setOf("exercise-fractions-1"))
        val userRepository = FakeLessonMapUserRepository(
            progress = initialProgress,
            attemptResponses = listOf(
                ExerciseAttemptResponse(
                    exerciseId = "exercise-fractions-2",
                    lessonId = "lesson-map-fractions",
                    isCorrect = true,
                    progress = testProgress(
                        completedExerciseIds = setOf("exercise-fractions-1", "exercise-fractions-2")
                    )
                )
            )
        )
        val viewModel = lessonMapViewModel(userRepository)

        advanceUntilIdle()
        viewModel.selectExercise("exercise-fractions-2")
        viewModel.updateInputValueAnswer(" 1/2 ")
        viewModel.submitAnswer()
        advanceUntilIdle()

        assertEquals(listOf<ExerciseSubmission>(InputValueSubmission("1/2")), userRepository.attemptCalls)
    }

    @Test
    fun `multi select submits the partial set then the exact changed set on retry`() = runTest(dispatcher) {
        val initialProgress = testProgress(
            completedExerciseIds = setOf("exercise-fractions-1", "exercise-fractions-2")
        )
        val userRepository = FakeLessonMapUserRepository(
            progress = initialProgress,
            attemptResponses = listOf(
                ExerciseAttemptResponse(
                    exerciseId = "exercise-fractions-3",
                    lessonId = "lesson-map-fractions",
                    isCorrect = false,
                    progress = initialProgress
                ),
                ExerciseAttemptResponse(
                    exerciseId = "exercise-fractions-3",
                    lessonId = "lesson-map-fractions",
                    isCorrect = true,
                    progress = testProgress(
                        completedExerciseIds = setOf(
                            "exercise-fractions-1",
                            "exercise-fractions-2",
                            "exercise-fractions-3"
                        )
                    )
                )
            )
        )
        val viewModel = lessonMapViewModel(userRepository)

        advanceUntilIdle()
        viewModel.selectExercise("exercise-fractions-3")
        viewModel.toggleMultiSelectAnswer("one-half")
        viewModel.submitAnswer()
        advanceUntilIdle()
        viewModel.toggleMultiSelectAnswer("two-fourths")
        viewModel.submitAnswer()
        advanceUntilIdle()

        assertEquals(
            listOf<ExerciseSubmission>(
                MultiSelectSubmission(listOf("one-half")),
                MultiSelectSubmission(listOf("one-half", "two-fourths"))
            ),
            userRepository.attemptCalls
        )
        assertEquals("Exercise completed. Keep going.", viewModel.uiState.value.exerciseFeedbackMessage)
    }

    @Test
    fun `blank input answer is rejected client side`() = runTest(dispatcher) {
        val userRepository = FakeLessonMapUserRepository(progress = testProgress(completedExerciseIds = setOf("exercise-fractions-1")))
        val viewModel = LessonMapViewModel(
            authRepository = FakeLessonMapAuthRepository(testUser),
            userRepository = userRepository,
            lessonRepository = FakeLessonRepository(),
            exerciseRepository = FakeExerciseRepository()
        )

        advanceUntilIdle()
        viewModel.selectExercise("exercise-fractions-2")
        viewModel.updateInputValueAnswer("   ")
        viewModel.submitAnswer()
        advanceUntilIdle()

        assertEquals(emptyList(), userRepository.attemptCalls)
        assertEquals("Enter an answer before submitting.", viewModel.uiState.value.exerciseFeedbackMessage)
        assertEquals(ActiveExercisePhase.Drafting, viewModel.uiState.value.activeExercisePhase)
    }

    private fun lessonMapViewModel(userRepository: UserRepository) = LessonMapViewModel(
        authRepository = FakeLessonMapAuthRepository(testUser),
        userRepository = userRepository,
        lessonRepository = FakeLessonRepository(),
        exerciseRepository = FakeExerciseRepository()
    )

    @Test
    fun `refresh restores current user when session user is missing`() = runTest(dispatcher) {
        val viewModel = LessonMapViewModel(
            authRepository = FakeLessonMapAuthRepository(user = null),
            userRepository = FakeLessonMapUserRepository(
                progress = testProgress(),
                currentUser = testUser
            ),
            lessonRepository = FakeLessonRepository(),
            exerciseRepository = FakeExerciseRepository()
        )

        advanceUntilIdle()

        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals("exercise-fractions-1", viewModel.uiState.value.activeNode?.exercise?.id)
    }
}

private val testUser = User(
    id = "user-1",
    name = "Alice Student",
    email = "alice@example.com",
    role = UserRole.STUDENT
)

private class FakeLessonMapAuthRepository(user: User?) : AuthRepository {
    private val state = MutableStateFlow(AuthSession(token = "token-123", user = user))

    override val session: StateFlow<AuthSession> = state

    override suspend fun login(email: String, password: String): Result<User> = Result.success(testUser)

    override suspend fun register(name: String, email: String, password: String): Result<User> = Result.success(testUser)

    override fun logout() = Unit
}

private class FakeLessonMapUserRepository(
    private val progress: UserProgress,
    private val currentUser: User? = testUser,
    attemptResponses: List<ExerciseAttemptResponse> = emptyList()
) : UserRepository {
    val attemptCalls = mutableListOf<ExerciseSubmission>()
    private val queuedAttemptResponses = ArrayDeque(attemptResponses)

    override suspend fun getCurrentUser(): User? = currentUser

    override suspend fun getUserRole(userId: String): UserRole = UserRole.STUDENT

    override suspend fun updateUser(user: User) = Unit

    override suspend fun getUserProgress(userId: String): UserProgress = progress

    override suspend fun attemptExercise(
        exerciseId: String,
        submission: ExerciseSubmission,
        score: Int
    ): ExerciseAttemptResponse {
        attemptCalls += submission
        return queuedAttemptResponses.removeFirstOrNull() ?: error("Attempt response not configured")
    }
}

private class FakeLessonRepository : LessonRepository {
    override suspend fun getLessonsByCourse(courseId: String): List<Lesson> = listOf(sampleLesson())

    override suspend fun getLessonById(id: String): Lesson? = sampleLesson()

    override suspend fun createLesson(lesson: Lesson): Lesson = lesson

    override suspend fun updateLesson(lesson: Lesson): Lesson = lesson

    override suspend fun updateTheory(lessonId: String, content: String): Lesson = sampleLesson()

    override suspend fun deleteLesson(id: String) = Unit
}

private class FakeExerciseRepository : ExerciseRepository {
    override suspend fun getExercisesByLesson(lessonId: String): List<Exercise> = sampleExercises()

    override suspend fun createExercise(exercise: Exercise): Exercise = exercise

    override suspend fun updateExercise(exercise: Exercise): Exercise = exercise

    override suspend fun deleteExercise(id: String) = Unit
}

private fun sampleLesson() = Lesson(
    id = "lesson-map-fractions",
    courseId = "course-fractions",
    creatorId = "system",
    title = "Fractions Foundations",
    theoryContent = "A fraction represents a part of a whole."
)

private fun sampleExercises() = listOf(
    Exercise(
        id = "exercise-fractions-1",
        lessonId = "lesson-map-fractions",
        title = "Which fraction shows one shaded part out of four equal parts?",
        payload = MultipleChoicePayload(
            options = listOf(
                ChoiceOption(id = "one-half", text = "1/2"),
                ChoiceOption(id = "one-third", text = "1/3"),
                ChoiceOption(id = "one-quarter", text = "1/4"),
                ChoiceOption(id = "four-over-one", text = "4/1")
            )
        )
    ),
    Exercise(
        id = "exercise-fractions-2",
        lessonId = "lesson-map-fractions",
        title = "What is 2/4 equivalent to?",
        payload = InputValuePayload(placeholder = "Type the simplified fraction")
    ),
    Exercise(
        id = "exercise-fractions-3",
        lessonId = "lesson-map-fractions",
        title = "Select the fractions equivalent to one half.",
        payload = MultiSelectPayload(
            options = listOf(
                ChoiceOption(id = "one-half", text = "1/2"),
                ChoiceOption(id = "two-fourths", text = "2/4"),
                ChoiceOption(id = "three-fourths", text = "3/4")
            )
        )
    ),
    Exercise(
        id = "exercise-fractions-4",
        lessonId = "lesson-map-fractions",
        title = "Which value is greater than one whole?",
        payload = MultipleChoicePayload(
            options = listOf(
                ChoiceOption(id = "three-fourths", text = "3/4"),
                ChoiceOption(id = "five-fourths", text = "5/4")
            )
        )
    )
)

private fun testProgress(
    completedExerciseIds: Set<String> = emptySet()
) = UserProgress(
    userId = testUser.id,
    completedExerciseIds = completedExerciseIds,
    enrolledCourseIds = setOf("course-fractions")
)
