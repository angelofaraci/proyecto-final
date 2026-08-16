package com.example.proyectofinal.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.material3.MaterialTheme
import com.example.proyectofinal.models.ChoiceOption
import com.example.proyectofinal.models.Exercise
import com.example.proyectofinal.models.Lesson
import com.example.proyectofinal.models.MultipleChoicePayload
import com.example.proyectofinal.ui.activities.ExerciseAnswerDraft
import com.example.proyectofinal.ui.activities.LessonMapContent
import com.example.proyectofinal.ui.activities.LessonMapLesson
import com.example.proyectofinal.ui.activities.LessonMapNodeUiModel
import com.example.proyectofinal.ui.activities.LessonMapUiState
import com.example.proyectofinal.ui.activities.LessonNodeState
import com.example.proyectofinal.ui.activities.TheorySheetContent
import com.example.proyectofinal.ui.activities.buildLessonMapNodes
import com.example.proyectofinal.ui.home.HomeCourseProgress
import com.example.proyectofinal.ui.home.HomeDashboardContent
import com.example.proyectofinal.ui.home.HomeDashboardUiState
import com.example.proyectofinal.ui.theme.AppTheme
import java.io.File
import java.security.MessageDigest
import javax.imageio.ImageIO
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test

class Slice6VisualAcceptanceCaptureTest {
    @get:Rule val composeTestRule = createComposeRule()

    private val approvedBaselines = mapOf(
        "task-1.9-login" to "cadea2c39a31ec2cbca6880d379123fcac0680ca3d2ce0db10cb071f319aa65d",
        "task-2.4-register-step1" to "e55a6cdcd5f1d66fde7a618ac40510af34336530d6e336c178555a56856754f7",
        "task-2.4-register-step2" to "cc4ae60de0ef205ac6493ae0910cc798de8fc6f682b71870d263a1b1f7570c67",
        "task-2.4-register-step3" to "bfbd5c8d6d0d1d6fb6513c3f397ae9582e2c1b769c966993a13794f46bbeab8a",
        "task-4.5-home" to "7d4558b3977828108a49360599aa2b868741233732f913a21096481cd7c3b9df",
        "task-5.6-lesson-map" to "ef2f2fc957bbc34147324c52d317de77236427619cc9232061cd265cff833511",
        "task-6.7-empty" to "8a933d4b949dfdc572227855d2411a5f47c44ffa25f2926ebd7046224fe9eb65",
        "task-6.7-exercise" to "ab1f435200ba74cc4932c61b7c6c39c5a7e09bc81b2debb9c9d03d346fd9f9f0",
        "task-6.7-loading" to "7d2330e6ffb2585cb8ee35fc0f6734f6abb5c1579c9423f3dcbfdc6a8118a1a5",
        "task-6.7-onboarding" to "4a95851f2e868c8b220775f8f046fba07788015fbefad4d6c8ef3caffe27b8fd",
        "task-6.7-theory" to "3ee3b50203d233850d51aa50ce1dc1e3c7f67b6bcc5973fec8433b0860d3e5ea"
    )

    @Test
    fun `captures Slice 6 visual acceptance surfaces`() {
        capture("task-6.7-exercise") {
            LessonMapContent(
                uiState = exerciseState(), onRetry = {}, onShowHome = {}, onExerciseSelected = {},
                onDismissActiveExercise = {}, onMultipleChoiceAnswerSelected = {}, onInputValueChanged = {},
                onMultiSelectAnswerToggled = {}, onSubmitAnswer = {}, onOpenTheory = {}, onDismissTheory = {}
            )
        }
        capture("task-6.7-theory") { TheorySheetContent(lesson()) }
        capture("task-6.7-onboarding") {
            OnboardingContent(OnboardingUiState(provinces = listOf("Buenos Aires")), {}, {}, {}, {}, {}, {})
        }
        capture("task-6.7-empty") { PlaceholderScreen("Progreso", "Todavía no hay actividad") }
        capture("task-6.7-loading") { PlaceholderScreen("Cargando actividad", state = PlaceholderState.Loading) }
    }

    @Test
    fun `captures remaining redesign acceptance surfaces`() {
        capture("task-1.9-login") {
            LoginScreen(LoginViewModel(FakeAuthRepository()), onSwitchToRegister = {})
        }
        (1..3).forEach { step ->
            val viewModel = RegisterViewModel(FakeAuthRepository()).apply {
                if (step > 1) { onNameChange("Ana"); continueStep() }
                if (step > 2) { onEmailChange("ana@correo.com"); onPasswordChange("Password1!"); continueStep() }
            }
            capture("task-2.4-register-step$step") { RegisterScreen(viewModel) }
        }
        capture("task-4.5-home") {
            HomeDashboardContent(
                HomeDashboardUiState(
                    isLoading = false, greeting = "Hola, María", level = 5, streak = 7,
                    currentXp = 340, xpForNextLevel = 500, hasEnrolledCourse = true,
                    inProgressCourses = listOf(HomeCourseProgress("course-1", "Fracciones - Básico", 45))
                ), {}, {}, {}, {}
            )
        }
        capture("task-5.6-lesson-map") {
            val exercises = (1..4).map { index ->
                Exercise("map-$index", "lesson-1", "Lección $index", payload = MultipleChoicePayload(listOf(ChoiceOption("$index", "1/2"))))
            }
            LessonMapContent(
                LessonMapUiState(
                    isLoading = false, lessonMap = LessonMapLesson(lesson(), exercises),
                    nodes = buildLessonMapNodes(exercises, setOf("map-1"))
                ), {}, {}, {}, {}, {}, {}, {}, {}, {}, {}
            )
        }
    }

    private fun capture(name: String, content: @Composable () -> Unit): File {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                AppTheme {
                    Box(
                        Modifier
                            .size(300.dp, 624.dp)
                            .background(MaterialTheme.colorScheme.background)
                            .testTag("visualCapture")
                    ) { content() }
                }
            }
        }
        val file = File("build/visual-acceptance/current/$name.png").apply { parentFile.mkdirs() }
        ImageIO.write(composeTestRule.onNodeWithTag("visualCapture").captureToImage().toAwtImage(), "png", file)
        val actualHash = MessageDigest.getInstance("SHA-256").digest(file.readBytes())
            .joinToString("") { "%02x".format(it) }
        assertEquals(approvedBaselines.getValue(name), actualHash, "visual baseline changed for $name")
        return file
    }

    private fun lesson() = Lesson("lesson-1", "course-1", "system", "Fracciones", "Una fracción representa una parte.")

    private fun exerciseState(): LessonMapUiState {
        val exercise = Exercise(
            "exercise-1", "lesson-1", "¿Cuánto es la mitad de 8?",
            payload = MultipleChoicePayload(listOf(ChoiceOption("a", "3"), ChoiceOption("b", "4")))
        )
        return LessonMapUiState(
            isLoading = false,
            lessonMap = LessonMapLesson(lesson(), listOf(exercise)),
            nodes = listOf(LessonMapNodeUiModel(exercise, 1, exercise.title, "", LessonNodeState.Current)),
            selectedExerciseId = exercise.id,
            activeExerciseId = exercise.id,
            activeExerciseDraft = ExerciseAnswerDraft.MultipleChoice()
        )
    }
}
