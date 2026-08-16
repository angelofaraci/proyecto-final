package com.example.proyectofinal.ui.activities

import com.example.proyectofinal.models.Exercise
import com.example.proyectofinal.models.Lesson

sealed interface ExerciseAnswerDraft {
    data class MultipleChoice(val selectedOptionId: String? = null) : ExerciseAnswerDraft

    data class InputValue(val value: String = "") : ExerciseAnswerDraft

    data class MultiSelect(val selectedOptionIds: Set<String> = emptySet()) : ExerciseAnswerDraft
}

enum class ActiveExercisePhase {
    Drafting,
    RetryReady,
    Submitting,
}

enum class ExerciseFeedbackTone {
    Info,
    Success,
    Error,
}

data class ExerciseFeedbackUiState(
    val message: String,
    val tone: ExerciseFeedbackTone,
)

enum class LessonNodeState {
    Locked,
    Unlocked,
    Completed,
    Current
}

data class LessonMapLesson(
    val lesson: Lesson,
    val exercises: List<Exercise>
)

data class LessonMapNodeUiModel(
    val exercise: Exercise,
    val index: Int,
    val title: String,
    val summary: String,
    val state: LessonNodeState
)

data class LessonMapUiState(
    val isLoading: Boolean = true,
    val lessonMap: LessonMapLesson? = null,
    val nodes: List<LessonMapNodeUiModel> = emptyList(),
    val selectedExerciseId: String? = null,
    val activeExerciseId: String? = null,
    val activeExerciseDraft: ExerciseAnswerDraft? = null,
    val activeExercisePhase: ActiveExercisePhase = ActiveExercisePhase.Drafting,
    val remainingLives: Int = 3,
    val exerciseFeedback: ExerciseFeedbackUiState? = null,
    val selectedTheoryLesson: Lesson? = null,
    val errorMessage: String? = null
) {
    val activeNode: LessonMapNodeUiModel?
        get() = nodes.firstOrNull { it.exercise.id == selectedExerciseId }
            ?: nodes.firstOrNull { it.state == LessonNodeState.Unlocked || it.state == LessonNodeState.Current }

    val activeExercise: Exercise?
        get() = lessonMap?.exercises?.firstOrNull { it.id == activeExerciseId }

    val isSubmittingAnswer: Boolean
        get() = activeExercisePhase == ActiveExercisePhase.Submitting

    val exerciseFeedbackMessage: String?
        get() = exerciseFeedback?.message

    val isTheoryAvailable: Boolean
        get() = lessonMap != null
}
