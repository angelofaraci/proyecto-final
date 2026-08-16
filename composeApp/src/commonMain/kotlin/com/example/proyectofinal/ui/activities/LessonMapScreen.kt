package com.example.proyectofinal.ui.activities

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.proyectofinal.models.ChoiceOption
import com.example.proyectofinal.models.Exercise
import com.example.proyectofinal.models.ExercisePayload
import com.example.proyectofinal.models.InputValuePayload
import com.example.proyectofinal.models.MultiSelectPayload
import com.example.proyectofinal.models.MultipleChoicePayload
import com.example.proyectofinal.ui.primitives.MButton
import com.example.proyectofinal.ui.primitives.MButtonStyle
import com.example.proyectofinal.ui.primitives.MCard
import com.example.proyectofinal.ui.primitives.MLinearProgressIndicator
import com.example.proyectofinal.ui.primitives.MProgressIndicator
import com.example.proyectofinal.ui.primitives.MTextField
import com.example.proyectofinal.ui.PlaceholderScreen
import com.example.proyectofinal.ui.PlaceholderState
import com.example.proyectofinal.ui.theme.AppThemeDefaults
import com.example.proyectofinal.ui.theme.BrandCoralShadow
import com.example.proyectofinal.ui.theme.BrandLock
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import proyectofinal.composeapp.generated.resources.Res
import proyectofinal.composeapp.generated.resources.ic_arrow_left
import proyectofinal.composeapp.generated.resources.lesson_map_action_back_to_map
import proyectofinal.composeapp.generated.resources.lesson_map_action_checking_answer
import proyectofinal.composeapp.generated.resources.lesson_map_action_hint
import proyectofinal.composeapp.generated.resources.lesson_map_action_go_home
import proyectofinal.composeapp.generated.resources.lesson_map_action_retry
import proyectofinal.composeapp.generated.resources.lesson_map_action_submit_answer
import proyectofinal.composeapp.generated.resources.lesson_map_action_try_again
import proyectofinal.composeapp.generated.resources.lesson_map_action_view_theory
import proyectofinal.composeapp.generated.resources.lesson_map_answer_label
import proyectofinal.composeapp.generated.resources.lesson_map_back_content_description
import proyectofinal.composeapp.generated.resources.lesson_map_close_exercise_content_description
import proyectofinal.composeapp.generated.resources.lesson_map_exercise_unsupported
import proyectofinal.composeapp.generated.resources.lesson_map_header_lessons_count
import proyectofinal.composeapp.generated.resources.lesson_map_progress_lessons
import proyectofinal.composeapp.generated.resources.lesson_map_progress_percent
import proyectofinal.composeapp.generated.resources.lesson_map_question_counter

@Composable
fun LessonMapScreen(
    onShowHome: () -> Unit = {},
    onExercisePlayerActiveChanged: (Boolean) -> Unit = {},
    viewModel: LessonMapViewModel = koinViewModel<LessonMapViewModel>()
) {
    val uiState by viewModel.uiState.collectAsState()

    LessonMapContent(
        uiState = uiState,
        onRetry = viewModel::refresh,
        onShowHome = onShowHome,
        onExerciseSelected = viewModel::selectExercise,
        onDismissActiveExercise = viewModel::dismissActiveExercise,
        onMultipleChoiceAnswerSelected = viewModel::selectMultipleChoiceAnswer,
        onInputValueChanged = viewModel::updateInputValueAnswer,
        onMultiSelectAnswerToggled = viewModel::toggleMultiSelectAnswer,
        onSubmitAnswer = viewModel::submitAnswer,
        onOpenTheory = viewModel::openTheory,
        onDismissTheory = viewModel::dismissTheory,
        onHintRequested = viewModel::showHint,
        onExercisePlayerActiveChanged = onExercisePlayerActiveChanged
    )
}

@Composable
internal fun LessonMapContent(
    uiState: LessonMapUiState,
    onRetry: () -> Unit,
    onShowHome: () -> Unit,
    onExerciseSelected: (String) -> Unit,
    onDismissActiveExercise: () -> Unit,
    onMultipleChoiceAnswerSelected: (String) -> Unit,
    onInputValueChanged: (String) -> Unit,
    onMultiSelectAnswerToggled: (String) -> Unit,
    onSubmitAnswer: () -> Unit,
    onOpenTheory: () -> Unit,
    onDismissTheory: () -> Unit,
    onHintRequested: () -> Unit = {},
    onExercisePlayerActiveChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    LaunchedEffect(uiState.activeExerciseId) {
        onExercisePlayerActiveChanged(uiState.activeExerciseId != null)
    }
    when {
        uiState.isLoading -> PlaceholderScreen(
            title = "Cargando actividad",
            state = PlaceholderState.Loading,
            modifier = modifier
        )

        uiState.errorMessage != null -> Box(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = uiState.errorMessage.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge
                )
                MButton(onClick = onRetry) {
                    Text(stringResource(Res.string.lesson_map_action_retry))
                }
                MButton(onClick = onShowHome, style = MButtonStyle.Outline) {
                    Text(stringResource(Res.string.lesson_map_action_go_home))
                }
            }
        }

        else -> {
            val lessonMap = uiState.lessonMap ?: return
            val activeExercise = uiState.activeExercise

            if (activeExercise != null) {
                ExercisePlayerContent(
                    exercise = activeExercise,
                    lessonTitle = lessonMap.lesson.title,
                    exerciseNumber = uiState.activeNode?.index,
                    exerciseCount = uiState.nodes.size,
                    remainingLives = uiState.remainingLives,
                    draft = uiState.activeExerciseDraft,
                    phase = uiState.activeExercisePhase,
                    feedback = uiState.exerciseFeedback,
                    onBack = onDismissActiveExercise,
                    onMultipleChoiceAnswerSelected = onMultipleChoiceAnswerSelected,
                    onInputValueChanged = onInputValueChanged,
                    onMultiSelectAnswerToggled = onMultiSelectAnswerToggled,
                    onSubmitAnswer = onSubmitAnswer,
                    onHintRequested = onHintRequested,
                    modifier = modifier
                )
            } else {
                Column(modifier = modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                    ) {
                        Spacer(Modifier.height(20.dp))
                        LessonMapHeader(
                            title = lessonMap.lesson.title,
                            totalExercises = uiState.nodes.size,
                            isTheoryAvailable = uiState.isTheoryAvailable,
                            onBack = onShowHome,
                            onOpenTheory = onOpenTheory
                        )
                        Spacer(Modifier.height(20.dp))
                        LessonMapProgress(nodes = uiState.nodes)
                        uiState.exerciseFeedback?.let { feedback ->
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = feedback.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = feedbackColor(feedback.tone),
                                fontStyle = FontStyle.Italic
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp)
                    ) {
                        Spacer(Modifier.height(24.dp))
                        LessonMapPath(
                            nodes = uiState.nodes,
                            onNodeClick = { node -> onExerciseSelected(node.exercise.id) }
                        )
                        Spacer(Modifier.height(32.dp))
                    }
                }
            }

            uiState.selectedTheoryLesson?.let { lesson ->
                TheorySheet(
                    lesson = lesson,
                    onDismiss = onDismissTheory
                )
            }
        }
    }
}

@Composable
private fun ExercisePlayerContent(
    exercise: Exercise,
    lessonTitle: String,
    exerciseNumber: Int?,
    exerciseCount: Int,
    remainingLives: Int,
    draft: ExerciseAnswerDraft?,
    phase: ActiveExercisePhase,
    feedback: ExerciseFeedbackUiState?,
    onBack: () -> Unit,
    onMultipleChoiceAnswerSelected: (String) -> Unit,
    onInputValueChanged: (String) -> Unit,
    onMultiSelectAnswerToggled: (String) -> Unit,
    onSubmitAnswer: () -> Unit,
    onHintRequested: () -> Unit,
    modifier: Modifier = Modifier
) {
    val questionNumber = exerciseNumber ?: 1
    val questionCount = exerciseCount.coerceAtLeast(1)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(Modifier.height(20.dp))
            ExercisePlayerHeader(
                lessonTitle = lessonTitle,
                questionNumber = questionNumber,
                questionCount = questionCount,
                remainingLives = remainingLives,
                onClose = onBack
            )
            MLinearProgressIndicator(
                progress = { questionNumber.toFloat() / questionCount },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("exerciseProgress")
            )
            MCard(
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = exercise.title,
                        modifier = Modifier.testTag("exerciseQuestion"),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            Text(
                text = "ELIGE LA RESPUESTA CORRECTA",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            ExerciseAnswerSection(
                payload = exercise.payload,
                draft = draft,
                onMultipleChoiceAnswerSelected = onMultipleChoiceAnswerSelected,
                onInputValueChanged = onInputValueChanged,
                onMultiSelectAnswerToggled = onMultiSelectAnswerToggled
            )
            Text(
                text = "💡 ${stringResource(Res.string.lesson_map_action_hint)}",
                modifier = Modifier
                    .testTag("exerciseHint")
                    .clickable(onClick = onHintRequested),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            feedback?.let {
                Text(
                    text = it.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = feedbackColor(it.tone)
                )
            }
            Spacer(Modifier.height(4.dp))
        }
        MButton(
            onClick = onSubmitAnswer,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .testTag("exerciseConfirmButton"),
            enabled = canSubmitDraft(draft) && phase != ActiveExercisePhase.Submitting,
            shape = RoundedCornerShape(18.dp)
        ) {
            Text(
                when (phase) {
                    ActiveExercisePhase.Submitting -> stringResource(Res.string.lesson_map_action_checking_answer)
                    ActiveExercisePhase.RetryReady -> stringResource(Res.string.lesson_map_action_try_again)
                    ActiveExercisePhase.Drafting -> stringResource(Res.string.lesson_map_action_submit_answer)
                }
            )
        }
    }
}

@Composable
private fun ExercisePlayerHeader(
    lessonTitle: String,
    questionNumber: Int,
    questionCount: Int,
    remainingLives: Int,
    onClose: () -> Unit
) {
    val closeContentDescription = stringResource(Res.string.lesson_map_close_exercise_content_description)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .semantics { contentDescription = closeContentDescription }
                .clickable(onClick = onClose),
            contentAlignment = Alignment.Center
        ) {
            Text("×", style = MaterialTheme.typography.titleLarge)
        }
        Text(
            text = "$lessonTitle\n${stringResource(Res.string.lesson_map_question_counter, questionNumber, questionCount)}",
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge
        )
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            repeat(remainingLives.coerceIn(0, 3)) {
                Text(
                    text = "♥",
                    modifier = Modifier.testTag("exerciseHeart"),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
private fun ExerciseAnswerSection(
    payload: ExercisePayload,
    draft: ExerciseAnswerDraft?,
    onMultipleChoiceAnswerSelected: (String) -> Unit,
    onInputValueChanged: (String) -> Unit,
    onMultiSelectAnswerToggled: (String) -> Unit
) {
    when {
        payload is MultipleChoicePayload && draft is ExerciseAnswerDraft.MultipleChoice -> {
            AnswerGrid(optionCount = payload.options.size) {
                itemsIndexed(payload.options, key = { _, option -> option.id }) { index, option ->
                    ChoiceOptionCard(
                        option = option,
                        label = ('A' + index).toString(),
                        isSelected = draft.selectedOptionId == option.id,
                        onClick = { onMultipleChoiceAnswerSelected(option.id) }
                    )
                }
            }
        }

        payload is InputValuePayload && draft is ExerciseAnswerDraft.InputValue -> {
            MTextField(
                value = draft.value,
                onValueChange = onInputValueChanged,
                modifier = Modifier.testTag("exerciseInputValue"),
                singleLine = true,
                label = { Text(stringResource(Res.string.lesson_map_answer_label)) },
                placeholder = payload.placeholder?.let { placeholder -> { Text(placeholder) } }
            )
        }

        payload is MultiSelectPayload && draft is ExerciseAnswerDraft.MultiSelect -> {
            AnswerGrid(optionCount = payload.options.size) {
                itemsIndexed(payload.options, key = { _, option -> option.id }) { index, option ->
                    ChoiceOptionCard(
                        option = option,
                        label = ('A' + index).toString(),
                        isSelected = option.id in draft.selectedOptionIds,
                        onClick = { onMultiSelectAnswerToggled(option.id) }
                    )
                }
            }
        }

        else -> {
            MCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Text(
                    text = stringResource(Res.string.lesson_map_exercise_unsupported),
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun AnswerGrid(
    optionCount: Int,
    content: androidx.compose.foundation.lazy.grid.LazyGridScope.() -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxWidth()
            .height(((optionCount + 1) / 2 * 96).dp)
            .testTag("exerciseAnswerGrid"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        userScrollEnabled = false,
        content = content
    )
}

@Composable
private fun ChoiceOptionCard(
    option: ChoiceOption,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    MCard(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isSelected) 6.dp else 0.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = BrandCoralShadow,
                spotColor = BrandCoralShadow
            )
            .semantics { selected = isSelected }
            .testTag("exerciseAnswer-${option.id}")
            .clickable(onClick = onClick),
        border = BorderStroke(
            1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("exerciseAnswerCard")
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "$label: ${option.text}",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private fun canSubmitDraft(draft: ExerciseAnswerDraft?): Boolean = when (draft) {
    is ExerciseAnswerDraft.MultipleChoice -> draft.selectedOptionId != null
    is ExerciseAnswerDraft.InputValue -> draft.value.trim().isNotEmpty()
    is ExerciseAnswerDraft.MultiSelect -> draft.selectedOptionIds.isNotEmpty()
    null -> false
}

@Composable
private fun feedbackColor(tone: ExerciseFeedbackTone) = when (tone) {
    ExerciseFeedbackTone.Info -> MaterialTheme.colorScheme.secondary
    ExerciseFeedbackTone.Success -> MaterialTheme.colorScheme.primary
    ExerciseFeedbackTone.Error -> MaterialTheme.colorScheme.error
}

// Serpentine geometry per design.md: 120dp vertical step, node centers y = i*120+60dp,
// alternating x at 72dp from each edge. Parity is read on the node's 1-based index so the
// first node sits on the right, matching the authoritative mapa-leccion.png (design's
// "even -> left, odd -> right" wording holds against that index).
private const val PathStepDp = 120
private const val NodeCenterXDp = 72

@Composable
private fun LessonMapHeader(
    title: String,
    totalExercises: Int,
    isTheoryAvailable: Boolean,
    onBack: () -> Unit,
    onOpenTheory: () -> Unit
) {
    val backContentDescription = stringResource(Res.string.lesson_map_back_content_description)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .semantics { contentDescription = backContentDescription }
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_arrow_left),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.size(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = stringResource(Res.string.lesson_map_header_lessons_count, totalExercises),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Surface(
            shape = RoundedCornerShape(AppThemeDefaults.shapes.pill),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .testTag("theoryPill")
                .clip(RoundedCornerShape(AppThemeDefaults.shapes.pill))
                .clickable(enabled = isTheoryAvailable, onClick = onOpenTheory)
                .alpha(if (isTheoryAvailable) 1f else 0.5f)
        ) {
            Text(
                text = stringResource(Res.string.lesson_map_action_view_theory),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
private fun LessonMapProgress(nodes: List<LessonMapNodeUiModel>) {
    val total = nodes.size
    val completed = nodes.count { it.state == LessonNodeState.Completed }
    // Percent is derived from node state; the PNG's "45%" at 3/8 is mock math (3/8 = 37%).
    val percent = if (total > 0) completed * 100 / total else 0
    val progress = if (total > 0) completed.toFloat() / total else 0f

    Column(modifier = Modifier.fillMaxWidth()) {
        MLinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(Res.string.lesson_map_progress_percent, percent),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            Text(
                text = stringResource(Res.string.lesson_map_progress_lessons, completed, total),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LessonMapPath(
    nodes: List<LessonMapNodeUiModel>,
    onNodeClick: (LessonMapNodeUiModel) -> Unit
) {
    if (nodes.isEmpty()) return

    val completedColor = MaterialTheme.colorScheme.secondary
    val actionableColor = MaterialTheme.colorScheme.primary

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height((nodes.size * PathStepDp).dp)
    ) {
        val width = maxWidth

        Canvas(
            modifier = Modifier
                .matchParentSize()
                .testTag("lessonMapPath")
        ) {
            val strokeWidth = 4.dp.toPx()
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx()))
            val leftCenterX = NodeCenterXDp.dp.toPx()
            val rightCenterX = size.width - NodeCenterXDp.dp.toPx()

            fun centerX(nodeIndex: Int) = if (nodeIndex % 2 == 0) leftCenterX else rightCenterX

            // Dash rule per mapa-leccion.png: a segment is dashed when the node it runs INTO is
            // locked; solid teal after a completed node; solid coral otherwise.
            for (i in 0 until nodes.lastIndex) {
                val from = nodes[i]
                val to = nodes[i + 1]
                val dashed = isDashedLessonPathSegment(to.state)
                val color = when {
                    dashed -> BrandLock
                    from.state == LessonNodeState.Completed -> completedColor
                    else -> actionableColor
                }
                drawLine(
                    color = color,
                    start = Offset(centerX(from.index), (i * PathStepDp + PathStepDp / 2).dp.toPx()),
                    end = Offset(centerX(to.index), ((i + 1) * PathStepDp + PathStepDp / 2).dp.toPx()),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round,
                    pathEffect = if (dashed) dashEffect else null
                )
            }
        }

        nodes.forEach { node ->
            val isLeft = node.index % 2 == 0
            LessonMapNode(
                node = node,
                onClick = { onNodeClick(node) },
                modifier = Modifier.absoluteOffset(
                    x = if (isLeft) {
                        (NodeCenterXDp - LessonNodeSizeDp / 2).dp
                    } else {
                        width - (NodeCenterXDp + LessonNodeSizeDp / 2).dp
                    },
                    y = ((node.index - 1) * PathStepDp + (PathStepDp - LessonNodeSizeDp) / 2).dp
                )
            )
        }
    }
}

internal fun isDashedLessonPathSegment(destination: LessonNodeState): Boolean =
    destination == LessonNodeState.Locked
