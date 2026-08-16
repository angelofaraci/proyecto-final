package com.example.proyectofinal.ui.activities

import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.example.proyectofinal.models.ChoiceOption
import com.example.proyectofinal.models.Exercise
import com.example.proyectofinal.models.Lesson
import com.example.proyectofinal.models.MultipleChoicePayload
import com.example.proyectofinal.ui.theme.AppTheme
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Render coverage for the `ui-redesign-sync` lesson-map slice (mapa-leccion.png). Asserts the
 * semantics-observable redesign pieces (back + title + "{N} Lecciones" header, coral "Ver teoría"
 * pill, derived progress percent + teal bar, circular node states, Canvas serpentine path) and
 * guards the behavior that must survive the visual sync (back -> onShowHome, pill -> onOpenTheory,
 * unlocked node tap -> onExerciseSelected, locked/completed taps gated off per the existing
 * `selectExercise` Unlocked/Current contract).
 *
 * Maintainer rulings locked here: percent derived from state (the PNG's "45%" at 3/8 is mock
 * math -> "37%"); subtitle is "{N} Lecciones" only (`Lesson` has no unit field); completed nodes
 * stay non-interactive (spec scenario deferred to a future contract change).
 */
class LessonMapRedesignRenderTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `header renders title lesson count theory pill and back arrow`() {
        var theoryOpens = 0
        var homeShows = 0
        val nodes = buildLessonMapNodes(
            exercises = sampleExercises(4),
            completedExerciseIds = setOf("ex-1")
        )

        renderMap(
            uiState = mapUiState(nodes),
            onShowHome = { homeShows++ },
            onOpenTheory = { theoryOpens++ }
        )

        composeTestRule.onNodeWithText("Fundamentos").assertExists()
        composeTestRule.onNodeWithText("4 Lessons").assertExists()

        composeTestRule.onNodeWithText("View theory").performClick()
        assert(theoryOpens == 1)

        composeTestRule.onNodeWithContentDescription("Back").performClick()
        assert(homeShows == 1)
    }

    @Test
    fun `progress bar derives percent and counts from node states`() {
        val nodes = buildLessonMapNodes(
            exercises = sampleExercises(8),
            completedExerciseIds = setOf("ex-1", "ex-2", "ex-3")
        )

        renderMap(uiState = mapUiState(nodes))

        composeTestRule.onNodeWithText("37% Completed").assertExists()
        composeTestRule.onNodeWithText("3/8 Lessons").assertExists()
        composeTestRule.onNode(
            SemanticsMatcher.expectValue(
                SemanticsProperties.ProgressBarRangeInfo,
                ProgressBarRangeInfo(3f / 8f, 0f..1f, 0)
            )
        ).assertExists()
    }

    @Test
    fun `node states render and locked and completed nodes are non interactive`() {
        var selections = 0
        val nodes = buildLessonMapNodes(
            exercises = sampleExercises(3),
            completedExerciseIds = setOf("ex-1"),
            selectedExerciseId = "ex-2"
        )
        // States: Completed (1), Current (2), Locked (3)

        renderMap(
            uiState = mapUiState(nodes),
            onExerciseSelected = { selections++ }
        )

        composeTestRule.onNodeWithTag("lessonMapNode-1").assertExists()
        composeTestRule.onNodeWithTag("lessonMapNode-2").assertExists()
        composeTestRule.onNodeWithTag("lessonMapNode-3").assertExists()

        // Locked node: non-interactive per spec.
        composeTestRule.onNodeWithTag("lessonMapNode-3").performClick()
        assert(selections == 0)

        // Completed node: tap gating preserved (selectExercise only accepts Unlocked/Current).
        composeTestRule.onNodeWithTag("lessonMapNode-1").performClick()
        assert(selections == 0)
    }

    @Test
    fun `tapping an unlocked node selects its exercise`() {
        var selectedExerciseId: String? = null
        val nodes = buildLessonMapNodes(
            exercises = sampleExercises(3),
            completedExerciseIds = setOf("ex-1", "ex-2")
        )
        // States: Completed (1), Completed (2), Unlocked (3)

        renderMap(
            uiState = mapUiState(nodes),
            onExerciseSelected = { selectedExerciseId = it }
        )

        composeTestRule.onNodeWithTag("lessonMapNode-3").performClick()
        assert(selectedExerciseId == "ex-3")
    }

    @Test
    fun `tapping the current node selects its exercise`() {
        var selectedExerciseId: String? = null
        val nodes = buildLessonMapNodes(sampleExercises(3), completedExerciseIds = setOf("ex-1"))

        renderMap(mapUiState(nodes), onExerciseSelected = { selectedExerciseId = it })
        composeTestRule.onNodeWithTag("lessonMapNode-2").performClick()

        assert(selectedExerciseId == "ex-2")
    }

    @Test
    fun `path segments into locked nodes are dashed`() {
        assertTrue(isDashedLessonPathSegment(LessonNodeState.Locked))
        assertFalse(isDashedLessonPathSegment(LessonNodeState.Current))
        assertFalse(isDashedLessonPathSegment(LessonNodeState.Completed))
    }

    @Test
    fun `canvas path renders with height derived from node count`() {
        val nodes = buildLessonMapNodes(
            exercises = sampleExercises(2),
            completedExerciseIds = setOf("ex-1")
        )

        renderMap(uiState = mapUiState(nodes))

        composeTestRule.onNodeWithTag("lessonMapPath")
            .assertExists()
            .assertHeightIsEqualTo((2 * 120).dp)
    }

    private fun renderMap(
        uiState: LessonMapUiState,
        onShowHome: () -> Unit = {},
        onExerciseSelected: (String) -> Unit = {},
        onOpenTheory: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            AppTheme {
                LessonMapContent(
                    uiState = uiState,
                    onRetry = {},
                    onShowHome = onShowHome,
                    onExerciseSelected = onExerciseSelected,
                    onDismissActiveExercise = {},
                    onMultipleChoiceAnswerSelected = {},
                    onInputValueChanged = {},
                    onMultiSelectAnswerToggled = {},
                    onSubmitAnswer = {},
                    onOpenTheory = onOpenTheory,
                    onDismissTheory = {}
                )
            }
        }
    }

    private fun mapUiState(nodes: List<LessonMapNodeUiModel>) = LessonMapUiState(
        isLoading = false,
        lessonMap = LessonMapLesson(
            lesson = sampleLesson(),
            exercises = nodes.map(LessonMapNodeUiModel::exercise)
        ),
        nodes = nodes
    )

    private fun sampleLesson() = Lesson(
        id = "lesson-1",
        courseId = "course-1",
        creatorId = "system",
        title = "Fundamentos",
        theoryContent = "A fraction represents a part of a whole."
    )

    private fun sampleExercises(count: Int): List<Exercise> = (1..count).map { index ->
        Exercise(
            id = "ex-$index",
            lessonId = "lesson-1",
            title = "Exercise $index",
            payload = MultipleChoicePayload(
                options = listOf(ChoiceOption(id = "opt-$index", text = "1/2"))
            )
        )
    }
}
