package com.example.proyectofinal.ui.activities

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.proyectofinal.domain.AuthRepository
import com.example.proyectofinal.domain.ExerciseRepository
import com.example.proyectofinal.domain.LessonRepository
import com.example.proyectofinal.domain.UserRepository
import com.example.proyectofinal.models.Exercise
import com.example.proyectofinal.models.ExercisePayload
import com.example.proyectofinal.models.ExerciseSubmission
import com.example.proyectofinal.models.InputValuePayload
import com.example.proyectofinal.models.InputValueSubmission
import com.example.proyectofinal.models.MultiSelectPayload
import com.example.proyectofinal.models.MultiSelectSubmission
import com.example.proyectofinal.models.MultipleChoicePayload
import com.example.proyectofinal.models.MultipleChoiceSubmission
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LessonMapViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val lessonRepository: LessonRepository,
    private val exerciseRepository: ExerciseRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(LessonMapUiState())
    val uiState: StateFlow<LessonMapUiState> = _uiState.asStateFlow()

    private var completedExerciseIds: Set<String> = emptySet()

    init {
        refresh()
    }

    fun refresh() = viewModelScope.launch {
        _uiState.value = LessonMapUiState(isLoading = true)

        _uiState.value = try {
            val user = authRepository.session.value.user
                ?: userRepository.getCurrentUser()
                ?: error("Unable to restore the authenticated user")
            val progress = userRepository.getUserProgress(user.id)
            val lessonMap = loadLessonMap(progress.enrolledCourseIds)
            completedExerciseIds = progress.completedExerciseIds

            createLoadedState(lessonMap = lessonMap, completedExerciseIds = completedExerciseIds)
        } catch (error: Exception) {
            LessonMapUiState(
                isLoading = false,
                errorMessage = error.message ?: "Unknown error"
            )
        }
    }

    fun selectExercise(exerciseId: String) {
        val currentState = _uiState.value
        val lessonMap = currentState.lessonMap ?: return
        val node = currentState.nodes.firstOrNull { it.exercise.id == exerciseId } ?: return

        if (node.state != LessonNodeState.Unlocked && node.state != LessonNodeState.Current) {
            return
        }

        _uiState.update {
            it.copy(
                selectedExerciseId = exerciseId,
                activeExerciseId = exerciseId,
                activeExerciseDraft = createDraft(node.exercise.payload),
                activeExercisePhase = ActiveExercisePhase.Drafting,
                remainingLives = 3,
                exerciseFeedback = null,
                nodes = buildLessonMapNodes(
                    exercises = lessonMap.exercises,
                    completedExerciseIds = completedExerciseIds,
                    selectedExerciseId = exerciseId
                )
            )
        }
    }

    fun dismissActiveExercise() {
        _uiState.update {
            it.copy(
                activeExerciseId = null,
                activeExerciseDraft = null,
                activeExercisePhase = ActiveExercisePhase.Drafting,
                remainingLives = 3,
                exerciseFeedback = null
            )
        }
    }

    fun selectMultipleChoiceAnswer(optionId: String) {
        _uiState.update {
            it.copy(
                activeExerciseDraft = ExerciseAnswerDraft.MultipleChoice(selectedOptionId = optionId),
                activeExercisePhase = ActiveExercisePhase.Drafting,
                exerciseFeedback = null
            )
        }
    }

    fun updateInputValueAnswer(value: String) {
        _uiState.update {
            it.copy(
                activeExerciseDraft = ExerciseAnswerDraft.InputValue(value = value),
                activeExercisePhase = ActiveExercisePhase.Drafting,
                exerciseFeedback = null
            )
        }
    }

    fun toggleMultiSelectAnswer(optionId: String) {
        _uiState.update { state ->
            val draft = state.activeExerciseDraft as? ExerciseAnswerDraft.MultiSelect
                ?: ExerciseAnswerDraft.MultiSelect()
            val selectedIds = draft.selectedOptionIds.toMutableSet().apply {
                if (!add(optionId)) {
                    remove(optionId)
                }
            }

            state.copy(
                activeExerciseDraft = ExerciseAnswerDraft.MultiSelect(selectedOptionIds = selectedIds),
                activeExercisePhase = ActiveExercisePhase.Drafting,
                exerciseFeedback = null
            )
        }
    }

    fun submitAnswer() = viewModelScope.launch {
        val currentState = _uiState.value
        val lessonMap = currentState.lessonMap ?: return@launch
        val exercise = currentState.activeExercise ?: return@launch

        if (currentState.isSubmittingAnswer) {
            return@launch
        }

        val submission = currentState.activeExerciseDraft
            ?.toSubmission()
            ?: run {
                _uiState.update {
                    it.copy(
                        activeExercisePhase = ActiveExercisePhase.Drafting,
                        exerciseFeedback = invalidDraftFeedback(exercise.payload)
                    )
                }
                return@launch
            }

        _uiState.update {
            it.copy(
                activeExercisePhase = ActiveExercisePhase.Submitting,
                exerciseFeedback = null
            )
        }

        try {
            val attempt = userRepository.attemptExercise(
                exerciseId = exercise.id,
                submission = submission,
                score = 100
            )

            if (!attempt.isCorrect) {
                _uiState.update {
                    it.copy(
                        activeExercisePhase = ActiveExercisePhase.RetryReady,
                        remainingLives = (it.remainingLives - 1).coerceAtLeast(0),
                        exerciseFeedback = ExerciseFeedbackUiState(
                            message = attempt.message ?: "Incorrect answer. Try again.",
                            tone = ExerciseFeedbackTone.Error
                        )
                    )
                }
                return@launch
            }

            completedExerciseIds = attempt.progress.completedExerciseIds
            _uiState.value = createLoadedState(
                lessonMap = lessonMap,
                completedExerciseIds = completedExerciseIds,
                exerciseFeedback = ExerciseFeedbackUiState(
                    message = "Exercise completed. Keep going.",
                    tone = ExerciseFeedbackTone.Success
                )
            )
        } catch (error: Exception) {
            _uiState.update {
                it.copy(
                    activeExercisePhase = ActiveExercisePhase.RetryReady,
                    exerciseFeedback = ExerciseFeedbackUiState(
                        message = error.message ?: "Unable to save your progress",
                        tone = ExerciseFeedbackTone.Error
                    )
                )
            }
        }
    }

    fun openTheory() {
        val lesson = _uiState.value.lessonMap?.lesson ?: return
        _uiState.update { it.copy(selectedTheoryLesson = lesson) }
    }

    fun showHint() {
        _uiState.update {
            it.copy(
                exerciseFeedback = ExerciseFeedbackUiState(
                    message = "Hint requested.",
                    tone = ExerciseFeedbackTone.Info
                )
            )
        }
    }

    fun dismissTheory() {
        _uiState.update { it.copy(selectedTheoryLesson = null) }
    }

    private suspend fun loadLessonMap(enrolledCourseIds: Set<String>): LessonMapLesson {
        val courseId = enrolledCourseIds.sorted().firstOrNull()
            ?: error("Activities are unavailable until you join a course.")
        val lesson = lessonRepository.getLessonsByCourse(courseId).firstOrNull()
            ?: error("No lessons are available for your current course yet.")
        val exercises = lesson.exercises.ifEmpty {
            exerciseRepository.getExercisesByLesson(lesson.id)
        }

        if (exercises.isEmpty()) {
            error("No exercises are available for this lesson yet.")
        }

        return LessonMapLesson(
            lesson = lesson,
            exercises = exercises
        )
    }

    private fun createLoadedState(
        lessonMap: LessonMapLesson,
        completedExerciseIds: Set<String>,
        exerciseFeedback: ExerciseFeedbackUiState? = null
    ): LessonMapUiState {
        return LessonMapUiState(
            isLoading = false,
            lessonMap = lessonMap,
            nodes = buildLessonMapNodes(
                exercises = lessonMap.exercises,
                completedExerciseIds = completedExerciseIds
            ),
            exerciseFeedback = exerciseFeedback
        )
    }

    private fun createDraft(payload: ExercisePayload): ExerciseAnswerDraft = when (payload) {
        is MultipleChoicePayload -> ExerciseAnswerDraft.MultipleChoice()
        is InputValuePayload -> ExerciseAnswerDraft.InputValue()
        is MultiSelectPayload -> ExerciseAnswerDraft.MultiSelect()
    }

    private fun invalidDraftFeedback(payload: ExercisePayload): ExerciseFeedbackUiState =
        ExerciseFeedbackUiState(
            message = when (payload) {
                is MultipleChoicePayload -> "Select one option before submitting."
                is InputValuePayload -> "Enter an answer before submitting."
                is MultiSelectPayload -> "Select at least one option before submitting."
            },
            tone = ExerciseFeedbackTone.Error
        )

    private fun ExerciseAnswerDraft.toSubmission(): ExerciseSubmission? = when (this) {
        is ExerciseAnswerDraft.MultipleChoice -> selectedOptionId
            ?.takeIf { it.isNotBlank() }
            ?.let(::MultipleChoiceSubmission)

        is ExerciseAnswerDraft.InputValue -> value
            .trim()
            .takeIf { it.isNotBlank() }
            ?.let(::InputValueSubmission)

        is ExerciseAnswerDraft.MultiSelect -> selectedOptionIds
            .map(String::trim)
            .filter(String::isNotEmpty)
            .distinct()
            .takeIf(List<String>::isNotEmpty)
            ?.let(::MultiSelectSubmission)
    }
}

internal fun buildLessonMapNodes(
    exercises: List<Exercise>,
    completedExerciseIds: Set<String>,
    selectedExerciseId: String? = null
): List<LessonMapNodeUiModel> {
    val firstAvailableExerciseId = exercises.firstOrNull { it.id !in completedExerciseIds }?.id

    return exercises.mapIndexed { index, exercise ->
        val state = when {
            exercise.id in completedExerciseIds -> LessonNodeState.Completed
            firstAvailableExerciseId == null -> LessonNodeState.Completed
            exercise.id == firstAvailableExerciseId && selectedExerciseId == exercise.id -> LessonNodeState.Current
            exercise.id == firstAvailableExerciseId -> LessonNodeState.Unlocked
            else -> LessonNodeState.Locked
        }

        LessonMapNodeUiModel(
            exercise = exercise,
            index = index + 1,
            title = "Exercise ${index + 1}",
            summary = exercise.title,
            state = state
        )
    }
}
