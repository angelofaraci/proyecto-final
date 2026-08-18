package com.example.proyectofinal.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.proyectofinal.data.SchoolYearOption
import com.example.proyectofinal.domain.StudentTrack
import com.example.proyectofinal.ui.primitives.MButton
import com.example.proyectofinal.ui.primitives.MCard
import com.example.proyectofinal.ui.theme.AppThemeDefaults
import org.jetbrains.compose.resources.stringResource
import proyectofinal.composeapp.generated.resources.Res
import proyectofinal.composeapp.generated.resources.onboarding_action_continue
import proyectofinal.composeapp.generated.resources.onboarding_action_continue_to_courses
import proyectofinal.composeapp.generated.resources.onboarding_action_saving
import proyectofinal.composeapp.generated.resources.onboarding_step1_description
import proyectofinal.composeapp.generated.resources.onboarding_step1_title
import proyectofinal.composeapp.generated.resources.onboarding_step2_description
import proyectofinal.composeapp.generated.resources.onboarding_step2_title
import proyectofinal.composeapp.generated.resources.onboarding_step3_description
import proyectofinal.composeapp.generated.resources.onboarding_step3_title
import proyectofinal.composeapp.generated.resources.onboarding_step4_description
import proyectofinal.composeapp.generated.resources.onboarding_step4_title
import proyectofinal.composeapp.generated.resources.onboarding_summary_category
import proyectofinal.composeapp.generated.resources.onboarding_summary_province
import proyectofinal.composeapp.generated.resources.onboarding_summary_school_year
import proyectofinal.composeapp.generated.resources.onboarding_title
import proyectofinal.composeapp.generated.resources.onboarding_track_unavailable

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onCompleted: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.isCompleted) {
        if (state.isCompleted) {
            onCompleted()
        }
    }

    OnboardingContent(
        state = state,
        onProvinceSelected = viewModel::selectProvince,
        onSchoolYearSelected = viewModel::selectSchoolYear,
        onTrackSelected = viewModel::selectTrack,
        onContinue = viewModel::nextStep,
        onBack = viewModel::goBack,
        onComplete = viewModel::completeOnboarding
    )
}

@Composable
internal fun OnboardingContent(
    state: OnboardingUiState,
    onProvinceSelected: (String) -> Unit,
    onSchoolYearSelected: (Int) -> Unit,
    onTrackSelected: (StudentTrack) -> Unit,
    onContinue: () -> Unit,
    onBack: () -> Unit,
    onComplete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(Res.string.onboarding_title),
            style = MaterialTheme.typography.headlineSmall
        )

        StepSummary(state)

        if (state.errorMessage != null) {
            Text(
                text = state.errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            when (state.currentStep) {
                OnboardingStep.PROVINCE -> ProvinceStep(
                    provinces = state.provinces,
                    selectedProvince = state.selectedProvince,
                    onProvinceSelected = onProvinceSelected,
                    enabled = !state.isSaving
                )

                OnboardingStep.CATEGORY -> CategoryStep(
                    trackOptions = state.trackOptions,
                    selectedTrack = state.selectedTrack,
                    onTrackSelected = onTrackSelected,
                    enabled = !state.isSaving
                )

                OnboardingStep.SCHOOL_YEAR -> SchoolYearStep(
                    schoolYears = state.availableSchoolYears,
                    selectedSchoolYear = state.selectedSchoolYear,
                    onSchoolYearSelected = onSchoolYearSelected,
                    enabled = !state.isSaving
                )

                OnboardingStep.CONFIRMATION -> ConfirmationStep(
                    state = state,
                    onComplete = onComplete
                )
            }
        }

        if (state.currentStep != OnboardingStep.CONFIRMATION) {
            MButton(
                onClick = onContinue,
                enabled = !state.isSaving && hasCurrentStepSelection(state),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(Res.string.onboarding_action_continue))
            }
        }

        if (state.currentStep != OnboardingStep.PROVINCE) {
            Surface(
                onClick = onBack,
                enabled = !state.isSaving,
                modifier = Modifier
                    .size(38.dp)
                    .testTag("onboardingBackButton"),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = "←", style = MaterialTheme.typography.titleLarge)
                }
            }
        }
    }
}

private fun hasCurrentStepSelection(state: OnboardingUiState): Boolean =
    when (state.currentStep) {
        OnboardingStep.PROVINCE -> state.selectedProvince != null
        OnboardingStep.CATEGORY -> state.selectedTrack != null
        OnboardingStep.SCHOOL_YEAR -> state.selectedSchoolYear != null
        OnboardingStep.CONFIRMATION -> false
    }

@Composable
private fun StepSummary(state: OnboardingUiState) {
    val provinceSummary = stringResource(Res.string.onboarding_summary_province, state.selectedProvince.orEmpty())
    val schoolYearSummary = stringResource(
        Res.string.onboarding_summary_school_year,
        state.availableSchoolYears.firstOrNull { option -> option.schoolYear == state.selectedSchoolYear }
            ?.label.orEmpty()
    )
    val selectedTrackLabel = state.selectedTrack?.let { track -> track.localizedLabel() } ?: ""
    val categorySummary = stringResource(Res.string.onboarding_summary_category, selectedTrackLabel)
    val summary = buildList {
        state.selectedProvince?.let { add(provinceSummary) }
        state.availableSchoolYears.firstOrNull { option -> option.schoolYear == state.selectedSchoolYear }
            ?.let { add(schoolYearSummary) }
        state.selectedTrack?.let { add(categorySummary) }
    }

    if (summary.isNotEmpty()) {
        Surface(
            shape = RoundedCornerShape(AppThemeDefaults.shapes.pill),
            color = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Text(
                text = summary.joinToString(" • "),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun ProvinceStep(
    provinces: List<String>,
    selectedProvince: String?,
    onProvinceSelected: (String) -> Unit,
    enabled: Boolean
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StepTitle(
            title = stringResource(Res.string.onboarding_step1_title),
            description = stringResource(Res.string.onboarding_step1_description)
        )

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(provinces) { province ->
                SelectionCard(
                    title = province,
                    selected = selectedProvince == province,
                    enabled = enabled,
                    onClick = { onProvinceSelected(province) }
                )
            }
        }
    }
}

@Composable
private fun SchoolYearStep(
    schoolYears: List<SchoolYearOption>,
    selectedSchoolYear: Int?,
    onSchoolYearSelected: (Int) -> Unit,
    enabled: Boolean
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StepTitle(
            title = stringResource(Res.string.onboarding_step3_title),
            description = stringResource(Res.string.onboarding_step3_description)
        )

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(schoolYears) { option ->
                SelectionCard(
                    title = option.label,
                    subtitle = allowedTrackSummary(option.allowedTracks),
                    selected = selectedSchoolYear == option.schoolYear,
                    enabled = enabled,
                    onClick = { onSchoolYearSelected(option.schoolYear) }
                )
            }
        }
    }
}

@Composable
private fun CategoryStep(
    trackOptions: List<OnboardingTrackOption>,
    selectedTrack: StudentTrack?,
    onTrackSelected: (StudentTrack) -> Unit,
    enabled: Boolean
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StepTitle(
            title = stringResource(Res.string.onboarding_step2_title),
            description = stringResource(Res.string.onboarding_step2_description)
        )

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(trackOptions) { option ->
                val unavailableLabel = stringResource(Res.string.onboarding_track_unavailable)
                SelectionCard(
                    title = option.track.localizedLabel(),
                    subtitle = if (option.enabled) null else unavailableLabel,
                    selected = selectedTrack == option.track,
                    enabled = enabled && option.enabled,
                    onClick = { onTrackSelected(option.track) }
                )
            }
        }
    }
}

@Composable
private fun ConfirmationStep(
    state: OnboardingUiState,
    onComplete: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StepTitle(
            title = stringResource(Res.string.onboarding_step4_title),
            description = stringResource(Res.string.onboarding_step4_description)
        )

        val confirmationTrackLabel = state.selectedTrack?.let { track -> track.localizedLabel() } ?: ""

        MCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(stringResource(Res.string.onboarding_summary_province, state.selectedProvince.orEmpty()))
                Text(
                    text = stringResource(
                        Res.string.onboarding_summary_school_year,
                        state.availableSchoolYears.firstOrNull { option -> option.schoolYear == state.selectedSchoolYear }
                            ?.label.orEmpty()
                    )
                )
                Text(stringResource(Res.string.onboarding_summary_category, confirmationTrackLabel))
            }
        }

        MButton(
            onClick = onComplete,
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (state.isSaving) {
                    stringResource(Res.string.onboarding_action_saving)
                } else {
                    stringResource(Res.string.onboarding_action_continue_to_courses)
                }
            )
        }
    }
}

@Composable
private fun StepTitle(title: String, description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SelectionCard(
    title: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    subtitle: String? = null
) {
    val containerColor = when {
        !enabled -> MaterialTheme.colorScheme.surfaceVariant
        selected -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surface
    }
    val borderColor = when {
        selected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    MCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("selectionCard-$title")
            .clickable(enabled = enabled, onClick = onClick),
        border = BorderStroke(1.dp, borderColor),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RadioButton(selected = selected, onClick = null, enabled = enabled)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge)
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun allowedTrackSummary(tracks: Set<StudentTrack>): String {
    val labels = tracks.map { track -> track.localizedLabel() }
    return labels.joinToString()
}
