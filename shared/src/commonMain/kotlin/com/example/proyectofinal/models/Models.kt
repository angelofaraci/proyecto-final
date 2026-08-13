package com.example.proyectofinal.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String,
    val name: String,
    val email: String,
    val role: UserRole = UserRole.STUDENT
)

@Serializable
data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class AuthResponse(
    val token: String,
    val user: User
)

@Serializable
enum class UserRole {
    ADMIN,
    TEACHER,
    STUDENT;

    companion object {
        fun parse(value: String): UserRole? = when (value.trim().uppercase()) {
            "LEARNER", "STUDENT" -> STUDENT
            "TEACHER" -> TEACHER
            "ADMIN" -> ADMIN
            else -> null
        }
    }
}

@Serializable
data class Course(
    val id: String,
    val title: String,
    val description: String,
    val creatorId: String,
    val isOfficial: Boolean = false,
    val joinCode: String? = null,
    val lessons: List<Lesson> = emptyList(),
    val schoolYear: Int = 0,
    val topic: String? = null,
    val difficulty: String? = null,
    val durationMinutes: Int? = null,
    val xpReward: Int? = null
)

@Serializable
data class CreateCourseRequest(
    val id: String,
    val title: String,
    val description: String,
    val joinCode: String? = null,
    val schoolYear: Int = 0,
    val topic: String? = null,
    val difficulty: String? = null,
    val durationMinutes: Int? = null,
    val xpReward: Int? = null
)

@Serializable
data class Lesson(
    val id: String,
    val courseId: String? = null,
    val creatorId: String? = null,
    val title: String,
    val theoryContent: String,
    val exercises: List<Exercise> = emptyList()
)

@Serializable
data class Exercise(
    val id: String,
    val lessonId: String,
    val title: String,
    val type: ExerciseType = ExerciseType.MULTIPLE_CHOICE,
    val payload: ExercisePayload
) {
    constructor(
        id: String,
        lessonId: String,
        question: String,
        options: List<String>,
        correctAnswer: String,
        type: ExerciseType = ExerciseType.MULTIPLE_CHOICE
    ) : this(
        id = id,
        lessonId = lessonId,
        title = question,
        type = normalizedExerciseType(type),
        payload = legacyPayloadFrom(type = type, options = options, correctAnswer = correctAnswer)
    )

    val question: String
        get() = title

    val options: List<String>
        get() = when (val currentPayload = payload) {
            is MultipleChoicePayload -> currentPayload.options.map(ChoiceOption::text)
            is InputValuePayload -> emptyList()
            is MultiSelectPayload -> currentPayload.options.map(ChoiceOption::text)
        }

    val correctAnswer: String
        get() = legacyCorrectAnswerFrom(payload)

    fun copy(
        id: String = this.id,
        lessonId: String = this.lessonId,
        question: String = this.question,
        options: List<String> = this.options,
        correctAnswer: String = this.correctAnswer,
        type: ExerciseType = this.type
    ): Exercise = Exercise(
        id = id,
        lessonId = lessonId,
        question = question,
        options = options,
        correctAnswer = correctAnswer,
        type = type
    )
}

@Serializable
enum class ExerciseType {
    MULTIPLE_CHOICE,
    @Deprecated("Use MULTIPLE_CHOICE with a boolean payload instead")
    TRUE_FALSE,
    INPUT_VALUE,
    MULTI_SELECT
}

@Serializable
data class ChoiceOption(
    val id: String,
    val text: String
)

@Serializable
sealed interface ExercisePayload

@Serializable
@SerialName("multipleChoice")
data class MultipleChoicePayload(
    val options: List<ChoiceOption>,
    val correctOptionId: String? = null
) : ExercisePayload

@Serializable
@SerialName("inputValue")
data class InputValuePayload(
    val placeholder: String? = null,
    val correctValue: String? = null
) : ExercisePayload

@Serializable
@SerialName("multiSelect")
data class MultiSelectPayload(
    val options: List<ChoiceOption>,
    val correctOptionIds: List<String>? = null
) : ExercisePayload

@Serializable
data class UserProgress(
    val userId: String,
    val completedLessonIds: Set<String> = emptySet(),
    val completedExerciseIds: Set<String> = emptySet(),
    val totalScore: Int = 0,
    val enrolledCourseIds: Set<String> = emptySet()
)

@Serializable
data class CompleteExerciseRequest(
    val exerciseId: String,
    val score: Int = 0
)

@Serializable
data class ExerciseAttemptRequest(
    val exerciseId: String,
    val submission: ExerciseSubmission,
    val score: Int = 100
)

@Serializable
sealed interface ExerciseSubmission

@Serializable
@SerialName("multipleChoice")
data class MultipleChoiceSubmission(
    val selectedOptionId: String
) : ExerciseSubmission

@Serializable
@SerialName("inputValue")
data class InputValueSubmission(
    val value: String
) : ExerciseSubmission

@Serializable
@SerialName("multiSelect")
data class MultiSelectSubmission(
    val selectedOptionIds: List<String>
) : ExerciseSubmission

@Serializable
data class ExerciseAttemptResponse(
    val exerciseId: String,
    val lessonId: String,
    val isCorrect: Boolean,
    val message: String? = null,
    val lessonCompleted: Boolean = false,
    val progress: UserProgress
)

@Serializable
data class ExerciseCompletionResponse(
    val exerciseId: String,
    val lessonId: String,
    val lessonCompleted: Boolean,
    val progress: UserProgress
)
@Serializable
data class CompleteLessonRequest(
    val userId: String,
    val lessonId: String,
    val score: Int = 0
)

@Serializable
data class TheoryUpdateRequest(
    val lessonId: String,
    val theoryContent: String
)

private fun normalizedExerciseType(type: ExerciseType): ExerciseType =
    if (type == ExerciseType.TRUE_FALSE) {
        ExerciseType.MULTIPLE_CHOICE
    } else {
        type
    }

private fun legacyPayloadFrom(
    type: ExerciseType,
    options: List<String>,
    correctAnswer: String
): ExercisePayload {
    val normalizedType = normalizedExerciseType(type)
    val normalizedOptions = options.map { option -> option.trim() }

    return when (normalizedType) {
        ExerciseType.MULTIPLE_CHOICE -> {
            val choiceOptions = normalizedOptions.map { option -> ChoiceOption(id = option, text = option) }
                .let { optionsWithIds ->
                    if (correctAnswer.trim().isNotEmpty() && optionsWithIds.none { it.id == correctAnswer.trim() }) {
                        optionsWithIds + ChoiceOption(id = correctAnswer.trim(), text = correctAnswer.trim())
                    } else {
                        optionsWithIds
                    }
                }
            MultipleChoicePayload(
                options = choiceOptions,
                correctOptionId = choiceOptions.firstOrNull { it.id == correctAnswer.trim() }?.id
            )
        }

        ExerciseType.INPUT_VALUE -> InputValuePayload(correctValue = correctAnswer)
        ExerciseType.MULTI_SELECT -> {
            val choiceOptions = normalizedOptions.map { option -> ChoiceOption(id = option, text = option) }
            val selectedIds = correctAnswer
                .split(',')
                .map(String::trim)
                .filter(String::isNotEmpty)
            MultiSelectPayload(
                options = choiceOptions,
                correctOptionIds = selectedIds.ifEmpty { null }
            )
        }

        ExerciseType.TRUE_FALSE -> error("TRUE_FALSE must be normalized before payload creation")
    }
}

private fun legacyCorrectAnswerFrom(payload: ExercisePayload): String =
    when (payload) {
        is MultipleChoicePayload -> payload.correctOptionId
            ?.let { correctId -> payload.options.firstOrNull { it.id == correctId }?.text }
            .orEmpty()

        is InputValuePayload -> payload.correctValue.orEmpty()
        is MultiSelectPayload -> payload.correctOptionIds
            ?.mapNotNull { correctId -> payload.options.firstOrNull { it.id == correctId }?.text }
            ?.joinToString(",")
            .orEmpty()
    }
