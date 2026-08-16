package com.example.proyectofinal.ui.activities

import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.proyectofinal.models.ChoiceOption
import com.example.proyectofinal.models.Exercise
import com.example.proyectofinal.models.ExercisePayload
import com.example.proyectofinal.models.InputValuePayload
import com.example.proyectofinal.models.Lesson
import com.example.proyectofinal.models.MultiSelectPayload
import com.example.proyectofinal.models.MultipleChoicePayload
import com.example.proyectofinal.ui.theme.AppTheme
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test

class ExercisePlayerRedesignRenderTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun `header derives lesson question progress and remaining hearts from state`() {
        var dismissals = 0
        renderPlayer(remainingLives = 2, onDismiss = { dismissals++ })
        composeTestRule.onNodeWithContentDescription("Close exercise").performClick()
        composeTestRule.onNodeWithText("Fractions", substring = true).assertExists()
        composeTestRule.onNodeWithText("Question 1/4", substring = true).assertExists()
        composeTestRule.onAllNodesWithTag("exerciseHeart").assertCountEquals(2)
        composeTestRule.onNodeWithTag("exerciseProgress").assertExists()
        composeTestRule.onNode(SemanticsMatcher.expectValue(
            SemanticsProperties.ProgressBarRangeInfo, ProgressBarRangeInfo(1f / 4f, 0f..1f, 0)
        )).assertExists()
        assertEquals(1, dismissals)
    }

    @Test
    fun `header renders a later question progress and one remaining heart`() {
        renderPlayer(remainingLives = 1, activeExerciseIndex = 3)

        composeTestRule.onNodeWithText("Question 3/4", substring = true).assertExists()
        composeTestRule.onAllNodesWithTag("exerciseHeart").assertCountEquals(1)
        composeTestRule.onNode(SemanticsMatcher.expectValue(
            SemanticsProperties.ProgressBarRangeInfo, ProgressBarRangeInfo(3f / 4f, 0f..1f, 0)
        )).assertExists()
    }

    @Test
    fun `multiple choice renders in grid and exposes selected answer`() {
        var selectedOptionId: String? = null
        renderPlayer(draft = ExerciseAnswerDraft.MultipleChoice("b"), onChoice = { selectedOptionId = it })
        composeTestRule.onNodeWithText("Question 1/4", substring = true).assertExists()
        composeTestRule.onAllNodesWithTag("exerciseAnswerCard", useUnmergedTree = true).assertCountEquals(4)
        composeTestRule.onNodeWithTag("exerciseAnswer-b").assertIsSelected()
        composeTestRule.onNodeWithTag("exerciseAnswer-a").performClick()
        assertEquals("a", selectedOptionId)
    }

    @Test
    fun `exercise keeps one answer label per option and exposes question hierarchy`() {
        renderPlayer(draft = ExerciseAnswerDraft.MultipleChoice("b"))

        composeTestRule.onNodeWithTag("exerciseQuestion").assertExists()
        listOf("A: 1/3", "B: 1/2", "C: 2/3", "D: 3/4").forEach { option ->
            composeTestRule.onAllNodesWithText(option, useUnmergedTree = true).assertCountEquals(1)
        }
    }

    @Test
    fun `hint invokes its callback and bottom confirmation action remains available`() {
        var submissions = 0
        var hintRequests = 0
        renderPlayer(
            draft = ExerciseAnswerDraft.MultipleChoice("a"),
            onSubmit = { submissions++ },
            onHint = { hintRequests++ }
        )
        composeTestRule.onNodeWithTag("exerciseHint").performClick()
        assertEquals(0, submissions)
        composeTestRule.onNodeWithTag("exerciseConfirmButton").performClick()
        assertEquals(1, hintRequests)
        assertEquals(1, submissions)
    }

    @Test
    fun `input value renders text input and dispatches typed text`() {
        var typedValue = ""
        renderPlayer(
            payload = InputValuePayload(placeholder = "Type the simplified fraction"),
            draft = ExerciseAnswerDraft.InputValue(),
            onInputValueChanged = { typedValue = it }
        )

        composeTestRule.onNodeWithTag("exerciseInputValue").assertExists()
        composeTestRule.onNodeWithTag("exerciseInputValue").performTextInput(" 42 ")

        assertEquals(" 42 ", typedValue)
    }

    @Test
    fun `multi select renders grid and dispatches independent option toggles`() {
        val toggledIds = mutableListOf<String>()
        renderPlayer(
            payload = MultiSelectPayload(sampleOptions),
            draft = ExerciseAnswerDraft.MultiSelect(setOf("a", "c")),
            onMultiSelectAnswerToggled = toggledIds::add
        )

        composeTestRule.onAllNodesWithTag("exerciseAnswerCard", useUnmergedTree = true).assertCountEquals(4)
        composeTestRule.onNodeWithTag("exerciseAnswer-a").assertIsSelected()
        composeTestRule.onNodeWithTag("exerciseAnswer-c").assertIsSelected()
        composeTestRule.onNodeWithTag("exerciseAnswer-b").performClick()

        assertEquals(listOf("b"), toggledIds)
    }

    @Test
    fun `incompatible payload and draft render fallback without crashing`() {
        renderPlayer(
            payload = InputValuePayload(),
            draft = ExerciseAnswerDraft.MultiSelect()
        )

        composeTestRule.onNodeWithText(
            "This exercise type is not supported in this app version."
        ).assertExists()
    }

    private fun renderPlayer(
        payload: ExercisePayload = MultipleChoicePayload(sampleOptions),
        draft: ExerciseAnswerDraft = ExerciseAnswerDraft.MultipleChoice(),
        remainingLives: Int = 3,
        activeExerciseIndex: Int = 1,
        onChoice: (String) -> Unit = {},
        onDismiss: () -> Unit = {},
        onSubmit: () -> Unit = {},
        onHint: () -> Unit = {},
        onInputValueChanged: (String) -> Unit = {},
        onMultiSelectAnswerToggled: (String) -> Unit = {}
    ) = composeTestRule.setContent {
        AppTheme {
            LessonMapContent(
                uiState = playerUiState(payload, draft, remainingLives, activeExerciseIndex), onRetry = {}, onShowHome = {}, onExerciseSelected = {},
                onDismissActiveExercise = onDismiss, onMultipleChoiceAnswerSelected = onChoice,
                onInputValueChanged = onInputValueChanged,
                onMultiSelectAnswerToggled = onMultiSelectAnswerToggled,
                onSubmitAnswer = onSubmit,
                onOpenTheory = {}, onDismissTheory = {}, onHintRequested = onHint
            )
        }
    }

    private fun playerUiState(
        payload: ExercisePayload,
        draft: ExerciseAnswerDraft,
        remainingLives: Int,
        activeExerciseIndex: Int
    ): LessonMapUiState {
        val exercises = (1..4).map { index -> Exercise(
            "exercise-$index", "lesson-1", "What is one half?",
            payload = if (index == activeExerciseIndex) payload else MultipleChoicePayload(sampleOptions)
        ) }
        val activeExercise = exercises[activeExerciseIndex - 1]
        return LessonMapUiState(
            isLoading = false,
            lessonMap = LessonMapLesson(Lesson("lesson-1", "course-1", "system", "Fractions", ""), exercises),
            nodes = exercises.mapIndexed { index, exercise ->
                LessonMapNodeUiModel(exercise, index + 1, exercise.title, "", LessonNodeState.Current)
            },
            selectedExerciseId = activeExercise.id,
            activeExerciseId = activeExercise.id,
            activeExerciseDraft = draft,
            remainingLives = remainingLives
        )
    }

    private companion object {
        val sampleOptions = listOf(
            ChoiceOption("a", "1/3"),
            ChoiceOption("b", "1/2"),
            ChoiceOption("c", "2/3"),
            ChoiceOption("d", "3/4")
        )
    }
}
