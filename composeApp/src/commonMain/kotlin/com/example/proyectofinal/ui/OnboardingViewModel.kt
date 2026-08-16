package com.example.proyectofinal.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.proyectofinal.data.ProvinceSchoolCatalog
import com.example.proyectofinal.data.SchoolYearOption
import com.example.proyectofinal.domain.AuthRepository
import com.example.proyectofinal.domain.LearnerProfile
import com.example.proyectofinal.domain.LearnerProfileRepository
import com.example.proyectofinal.domain.StudentTrack
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect

enum class OnboardingStep {
    PROVINCE,
    CATEGORY,
    SCHOOL_YEAR,
    CONFIRMATION
}

data class OnboardingTrackOption(
    val track: StudentTrack,
    val enabled: Boolean
)

data class OnboardingUiState(
    val currentStep: OnboardingStep = OnboardingStep.PROVINCE,
    val provinces: List<String> = ProvinceSchoolCatalog.provinces,
    val availableSchoolYears: List<SchoolYearOption> = emptyList(),
    val trackOptions: List<OnboardingTrackOption> = StudentTrack.entries.map {
        OnboardingTrackOption(track = it, enabled = false)
    },
    val selectedProvince: String? = null,
    val selectedSchoolYear: Int? = null,
    val selectedTrack: StudentTrack? = null,
    val isSaving: Boolean = false,
    val isCompleted: Boolean = false,
    val errorMessage: String? = null
)

class OnboardingViewModel(
    private val authRepository: AuthRepository,
    private val learnerProfileRepository: LearnerProfileRepository,
    private val savedStateHandle: SavedStateHandle = SavedStateHandle()
) : ViewModel() {

    private val _uiState = MutableStateFlow(restoredState(savedStateHandle))
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.collect { state ->
                savedStateHandle[STEP_KEY] = state.currentStep.name
                savedStateHandle[PROVINCE_KEY] = state.selectedProvince
                savedStateHandle[TRACK_KEY] = state.selectedTrack?.name
                savedStateHandle[YEAR_KEY] = state.selectedSchoolYear
            }
        }
    }

    fun selectProvince(province: String) {
        val schoolYears = ProvinceSchoolCatalog.schoolYearOptionsFor(province)
        if (schoolYears.isEmpty()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Select a valid province")
            return
        }

        _uiState.value = _uiState.value.copy(
            trackOptions = buildTrackOptions(StudentTrack.entries.toSet()),
            selectedProvince = province,
            selectedTrack = null,
            selectedSchoolYear = null,
            availableSchoolYears = emptyList(),
            errorMessage = null
        )
    }

    fun selectTrack(track: StudentTrack) {
        val state = _uiState.value
        val province = state.selectedProvince

        if (province == null || track !in StudentTrack.entries) {
            _uiState.value = state.copy(
                errorMessage = "Selected category is not available"
            )
            return
        }

        _uiState.value = state.copy(
            selectedTrack = track,
            availableSchoolYears = ProvinceSchoolCatalog.schoolYearOptionsFor(province, track),
            selectedSchoolYear = null,
            errorMessage = null
        )
    }

    fun selectSchoolYear(schoolYear: Int) {
        val state = _uiState.value
        val selectedOption = state.availableSchoolYears.firstOrNull { option ->
            option.schoolYear == schoolYear
        }

        if (selectedOption == null) {
            _uiState.value = state.copy(errorMessage = "Select a valid school year")
            return
        }

        _uiState.value = state.copy(
            selectedSchoolYear = schoolYear,
            errorMessage = null
        )
    }

    fun nextStep() {
        val state = _uiState.value
        when (state.currentStep) {
            OnboardingStep.PROVINCE -> {
                val province = state.selectedProvince
                if (province == null || ProvinceSchoolCatalog.schoolYearOptionsFor(province).isEmpty()) {
                    _uiState.value = state.copy(errorMessage = "Select a valid province")
                    return
                }

                _uiState.value = state.copy(
                    currentStep = OnboardingStep.CATEGORY,
                    errorMessage = null
                )
            }

            OnboardingStep.CATEGORY -> {
                val track = state.selectedTrack
                if (track == null) {
                    _uiState.value = state.copy(
                        errorMessage = "Selected category is not available"
                    )
                    return
                }

                _uiState.value = state.copy(
                    currentStep = OnboardingStep.SCHOOL_YEAR,
                    errorMessage = null
                )
            }

            OnboardingStep.SCHOOL_YEAR -> {
                val schoolYear = state.selectedSchoolYear
                if (schoolYear == null || state.availableSchoolYears.none { it.schoolYear == schoolYear }) {
                    _uiState.value = state.copy(errorMessage = "Select a valid school year")
                    return
                }

                _uiState.value = state.copy(
                    currentStep = OnboardingStep.CONFIRMATION,
                    errorMessage = null
                )
            }

            OnboardingStep.CONFIRMATION -> Unit
        }
    }

    fun goBack() {
        val state = _uiState.value
        _uiState.value = when (state.currentStep) {
            OnboardingStep.PROVINCE -> state
            OnboardingStep.CATEGORY -> state.copy(
                currentStep = OnboardingStep.PROVINCE,
                selectedTrack = null,
                availableSchoolYears = emptyList(),
                selectedSchoolYear = null,
                errorMessage = null
            )

            OnboardingStep.SCHOOL_YEAR -> state.copy(
                currentStep = OnboardingStep.CATEGORY,
                selectedSchoolYear = null,
                errorMessage = null
            )

            OnboardingStep.CONFIRMATION -> state.copy(
                currentStep = OnboardingStep.SCHOOL_YEAR,
                errorMessage = null
            )
        }
    }

    fun completeOnboarding() {
        val state = _uiState.value
        val province = state.selectedProvince
        val schoolYear = state.selectedSchoolYear
        val track = state.selectedTrack

        if (province == null || schoolYear == null || track == null) {
            _uiState.value = state.copy(
                errorMessage = "Complete every onboarding step before continuing"
            )
            return
        }

        if (!ProvinceSchoolCatalog.isValidSelection(province, schoolYear, track)) {
            _uiState.value = state.copy(
                errorMessage = "Selected category does not match the chosen school year"
            )
            return
        }

        val userId = authRepository.session.value.user?.id
        if (userId.isNullOrBlank()) {
            _uiState.value = state.copy(
                errorMessage = "Your session could not be restored. Please sign in again."
            )
            return
        }

        _uiState.value = state.copy(isSaving = true, errorMessage = null)

        viewModelScope.launch {
            runCatching {
                learnerProfileRepository.upsertProfile(
                    userId,
                    LearnerProfile(
                        province = province,
                        schoolYear = schoolYear,
                        studentTrack = track,
                        onboardingComplete = true
                    )
                )
            }.onSuccess {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    isCompleted = true,
                    errorMessage = null
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = error.message ?: "Unable to save onboarding"
                )
            }
        }
    }

    private fun buildTrackOptions(allowedTracks: Set<StudentTrack>): List<OnboardingTrackOption> =
        StudentTrack.entries.map { track ->
            OnboardingTrackOption(track = track, enabled = track in allowedTracks)
        }

    private companion object {
        const val STEP_KEY = "onboarding.step"
        const val PROVINCE_KEY = "onboarding.province"
        const val TRACK_KEY = "onboarding.track"
        const val YEAR_KEY = "onboarding.year"

        fun restoredState(handle: SavedStateHandle): OnboardingUiState {
            val province = handle.get<String>(PROVINCE_KEY)
            val track = handle.get<String>(TRACK_KEY)?.let { saved ->
                StudentTrack.entries.firstOrNull { it.name == saved }
            }
            val years = if (province != null && track != null) {
                ProvinceSchoolCatalog.schoolYearOptionsFor(province, track)
            } else emptyList()
            val year = handle.get<Int>(YEAR_KEY)?.takeIf { selected -> years.any { it.schoolYear == selected } }
            return OnboardingUiState(
                currentStep = handle.get<String>(STEP_KEY)?.let { saved ->
                    OnboardingStep.entries.firstOrNull { it.name == saved }
                } ?: OnboardingStep.PROVINCE,
                trackOptions = StudentTrack.entries.map { OnboardingTrackOption(it, province != null) },
                selectedProvince = province,
                selectedTrack = track,
                availableSchoolYears = years,
                selectedSchoolYear = year
            )
        }
    }
}
