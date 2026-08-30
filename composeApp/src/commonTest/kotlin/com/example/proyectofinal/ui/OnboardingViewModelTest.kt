package com.example.proyectofinal.ui

import androidx.lifecycle.SavedStateHandle
import com.example.proyectofinal.domain.AuthRepository
import com.example.proyectofinal.domain.AuthSession
import com.example.proyectofinal.domain.LearnerProfile
import com.example.proyectofinal.domain.LearnerProfileRepository
import com.example.proyectofinal.domain.StudentTrack
import com.example.proyectofinal.models.User
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
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {
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
    fun `selecting a province stays on the step until Continue advances and enables all categories`() = runTest(dispatcher) {
        val viewModel = OnboardingViewModel(OnboardingFakeAuthRepository(testUser), FakeLearnerProfileRepository())

        viewModel.selectProvince("Buenos Aires")

        assertEquals(OnboardingStep.PROVINCE, viewModel.uiState.value.currentStep)
        assertEquals("Buenos Aires", viewModel.uiState.value.selectedProvince)
        assertTrue(viewModel.uiState.value.trackOptions.all(OnboardingTrackOption::enabled))
        assertNull(viewModel.uiState.value.selectedSchoolYear)

        viewModel.nextStep()

        assertEquals(OnboardingStep.CATEGORY, viewModel.uiState.value.currentStep)
    }

    @Test
    fun `Continue requires a valid selection at every onboarding step`() = runTest(dispatcher) {
        val viewModel = OnboardingViewModel(OnboardingFakeAuthRepository(testUser), FakeLearnerProfileRepository())

        viewModel.nextStep()

        assertEquals(OnboardingStep.PROVINCE, viewModel.uiState.value.currentStep)
        assertEquals("Select a valid province", viewModel.uiState.value.errorMessage)

        viewModel.selectProvince("Buenos Aires")
        viewModel.nextStep()
        viewModel.nextStep()

        assertEquals(OnboardingStep.CATEGORY, viewModel.uiState.value.currentStep)
        assertEquals(
            "Selected category is not available",
            viewModel.uiState.value.errorMessage
        )

        viewModel.selectTrack(StudentTrack.SECONDARY)
        viewModel.nextStep()
        viewModel.nextStep()

        assertEquals(OnboardingStep.SCHOOL_YEAR, viewModel.uiState.value.currentStep)
        assertEquals("Select a valid school year", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `selecting a track populates the school-year step with only years valid for that track`() = runTest(dispatcher) {
        val viewModel = OnboardingViewModel(OnboardingFakeAuthRepository(testUser), FakeLearnerProfileRepository())

        viewModel.selectProvince("Buenos Aires")
        viewModel.nextStep()
        viewModel.selectTrack(StudentTrack.PRIMARY)
        viewModel.nextStep()

        val availableYears = viewModel.uiState.value.availableSchoolYears

        assertEquals(OnboardingStep.SCHOOL_YEAR, viewModel.uiState.value.currentStep)
        assertTrue(availableYears.all { StudentTrack.PRIMARY in it.allowedTracks })
        assertTrue(availableYears.none { it.schoolYear == 13 })
    }

    @Test
    fun `province boundary rules shift secondary start and technical extra year`() = runTest(dispatcher) {
        val viewModel = OnboardingViewModel(OnboardingFakeAuthRepository(testUser), FakeLearnerProfileRepository())

        viewModel.selectProvince("CABA")
        viewModel.nextStep()
        viewModel.selectTrack(StudentTrack.SECONDARY)
        val cabaSecondaryYears = viewModel.uiState.value.availableSchoolYears.map { it.schoolYear }

        assertEquals((8..12).toList(), cabaSecondaryYears)

        viewModel.selectProvince("Buenos Aires")
        viewModel.selectTrack(StudentTrack.TECHNICAL_SECONDARY)
        val buenosAiresTechnicalYears = viewModel.uiState.value.availableSchoolYears.map { it.schoolYear }

        assertEquals((7..13).toList(), buenosAiresTechnicalYears)
    }

    @Test
    fun `secondary year labels are relative to each province structure without changing persisted years`() = runTest(dispatcher) {
        val viewModel = OnboardingViewModel(OnboardingFakeAuthRepository(testUser), FakeLearnerProfileRepository())

        viewModel.selectProvince("Entre Ríos")
        viewModel.selectTrack(StudentTrack.SECONDARY)

        assertEquals((7..12).toList(), viewModel.uiState.value.availableSchoolYears.map { it.schoolYear })
        assertEquals(
            listOf("1er Año", "2do Año", "3er Año", "4to Año", "5to Año", "6to Año"),
            viewModel.uiState.value.availableSchoolYears.map { it.label }
        )

        viewModel.selectProvince("CABA")
        viewModel.selectTrack(StudentTrack.SECONDARY)

        assertEquals((8..12).toList(), viewModel.uiState.value.availableSchoolYears.map { it.schoolYear })
        assertEquals(
            listOf("1er Año", "2do Año", "3er Año", "4to Año", "5to Año"),
            viewModel.uiState.value.availableSchoolYears.map { it.label }
        )

        viewModel.selectProvince("Buenos Aires")
        viewModel.selectTrack(StudentTrack.TECHNICAL_SECONDARY)

        assertEquals(
            listOf("1er Año", "2do Año", "3er Año", "4to Año", "5to Año", "6to Año", "7° Año"),
            viewModel.uiState.value.availableSchoolYears.map { it.label }
        )

        viewModel.selectTrack(StudentTrack.SELF_DIRECTED)

        assertEquals("12° Año", viewModel.uiState.value.availableSchoolYears.last().label)
        assertEquals(12, viewModel.uiState.value.availableSchoolYears.last().schoolYear)
    }

    @Test
    fun `completing onboarding persists the selected learner profile`() = runTest(dispatcher) {
        val repository = FakeLearnerProfileRepository()
        val viewModel = OnboardingViewModel(OnboardingFakeAuthRepository(testUser), repository)

        viewModel.selectProvince("Buenos Aires")
        viewModel.nextStep()
        viewModel.selectTrack(StudentTrack.SECONDARY)
        viewModel.nextStep()
        viewModel.selectSchoolYear(7)
        viewModel.nextStep()
        viewModel.completeOnboarding()
        advanceUntilIdle()

        assertEquals(OnboardingStep.CONFIRMATION, viewModel.uiState.value.currentStep)
        assertTrue(viewModel.uiState.value.isCompleted)
        assertFalse(viewModel.uiState.value.isSaving)
        assertEquals(
            LearnerProfile(
                province = "Buenos Aires",
                schoolYear = 7,
                studentTrack = StudentTrack.SECONDARY,
                onboardingComplete = true
            ),
            repository.savedProfile
        )
    }

    @Test
    fun `completing onboarding without all required selections shows an error and does not persist`() = runTest(dispatcher) {
        val repository = FakeLearnerProfileRepository()
        val viewModel = OnboardingViewModel(OnboardingFakeAuthRepository(testUser), repository)

        viewModel.selectProvince("Buenos Aires")
        viewModel.completeOnboarding()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isCompleted)
        assertFalse(viewModel.uiState.value.isSaving)
        assertEquals(
            "Complete every onboarding step before continuing",
            viewModel.uiState.value.errorMessage
        )
        assertNull(repository.savedProfile)
    }

    @Test
    fun `selecting an out-of-range school year is rejected and does not persist`() = runTest(dispatcher) {
        val repository = FakeLearnerProfileRepository()
        val viewModel = OnboardingViewModel(OnboardingFakeAuthRepository(testUser), repository)

        viewModel.selectProvince("Buenos Aires")
        viewModel.nextStep()
        viewModel.selectTrack(StudentTrack.PRIMARY)
        viewModel.nextStep()
        viewModel.selectSchoolYear(13)
        advanceUntilIdle()

        assertEquals(OnboardingStep.SCHOOL_YEAR, viewModel.uiState.value.currentStep)
        assertNull(viewModel.uiState.value.selectedSchoolYear)
        assertEquals(
            "Select a valid school year",
            viewModel.uiState.value.errorMessage
        )
        assertNull(repository.savedProfile)
    }

    @Test
    fun `self-directed category yields the full unfiltered year range`() = runTest(dispatcher) {
        val viewModel = OnboardingViewModel(OnboardingFakeAuthRepository(testUser), FakeLearnerProfileRepository())

        viewModel.selectProvince("CABA")
        viewModel.nextStep()
        viewModel.selectTrack(StudentTrack.SELF_DIRECTED)
        val selfDirectedYears = viewModel.uiState.value.availableSchoolYears.map { it.schoolYear }

        assertEquals((1..12).toList(), selfDirectedYears)
        assertFalse(selfDirectedYears.contains(13))
    }

    @Test
    fun `back from school-year clears the selected year, returns to category, and keeps the track`() = runTest(dispatcher) {
        val viewModel = OnboardingViewModel(OnboardingFakeAuthRepository(testUser), FakeLearnerProfileRepository())

        viewModel.selectProvince("Buenos Aires")
        viewModel.nextStep()
        viewModel.selectTrack(StudentTrack.SECONDARY)
        viewModel.nextStep()
        viewModel.selectSchoolYear(7)

        viewModel.goBack()

        assertEquals(OnboardingStep.CATEGORY, viewModel.uiState.value.currentStep)
        assertNull(viewModel.uiState.value.selectedSchoolYear)
        assertEquals(StudentTrack.SECONDARY, viewModel.uiState.value.selectedTrack)
    }

    @Test
    fun `back from category clears the selected track and returns to province`() = runTest(dispatcher) {
        val viewModel = OnboardingViewModel(OnboardingFakeAuthRepository(testUser), FakeLearnerProfileRepository())

        viewModel.selectProvince("Buenos Aires")
        viewModel.nextStep()
        viewModel.selectTrack(StudentTrack.SECONDARY)

        viewModel.goBack()

        assertEquals(OnboardingStep.PROVINCE, viewModel.uiState.value.currentStep)
        assertNull(viewModel.uiState.value.selectedTrack)
        assertEquals("Buenos Aires", viewModel.uiState.value.selectedProvince)
    }

    @Test
    fun `changing category after selecting a year clears the stale year`() = runTest(dispatcher) {
        val viewModel = OnboardingViewModel(OnboardingFakeAuthRepository(testUser), FakeLearnerProfileRepository())

        viewModel.selectProvince("Buenos Aires")
        viewModel.nextStep()
        viewModel.selectTrack(StudentTrack.SECONDARY)
        viewModel.nextStep()
        viewModel.selectSchoolYear(7)

        viewModel.goBack()
        viewModel.selectTrack(StudentTrack.PRIMARY)

        assertNull(viewModel.uiState.value.selectedSchoolYear)
        assertTrue(viewModel.uiState.value.availableSchoolYears.all { StudentTrack.PRIMARY in it.allowedTracks })
    }

    @Test
    fun `partial selections survive saved-state recreation`() = runTest(dispatcher) {
        val handle = SavedStateHandle()
        val repository = FakeLearnerProfileRepository()
        val first = OnboardingViewModel(OnboardingFakeAuthRepository(testUser), repository, handle)
        first.selectProvince("Buenos Aires")
        first.nextStep()
        first.selectTrack(StudentTrack.SECONDARY)
        first.nextStep()
        first.selectSchoolYear(7)
        advanceUntilIdle()

        val recreated = OnboardingViewModel(OnboardingFakeAuthRepository(testUser), repository, handle)

        with(recreated.uiState.value) {
            assertEquals(OnboardingStep.SCHOOL_YEAR, currentStep)
            assertEquals("Buenos Aires", selectedProvince)
            assertEquals(StudentTrack.SECONDARY, selectedTrack)
            assertEquals(7, selectedSchoolYear)
        }
    }

    @Test
    fun `unknown saved enum values fall back without crashing`() = runTest(dispatcher) {
        val handle = SavedStateHandle(
            mapOf(
                "onboarding.step" to "REMOVED_STEP",
                "onboarding.track" to "REMOVED_TRACK"
            )
        )

        val recreated = OnboardingViewModel(
            OnboardingFakeAuthRepository(testUser),
            FakeLearnerProfileRepository(),
            handle
        )

        assertEquals(OnboardingStep.PROVINCE, recreated.uiState.value.currentStep)
        assertNull(recreated.uiState.value.selectedTrack)
    }

    @Test
    fun `completing onboarding upserts the profile under the session user id`() = runTest(dispatcher) {
        val repository = FakeLearnerProfileRepository()
        val viewModel = OnboardingViewModel(OnboardingFakeAuthRepository(testUser), repository)

        viewModel.selectProvince("Buenos Aires")
        viewModel.nextStep()
        viewModel.selectTrack(StudentTrack.SECONDARY)
        viewModel.nextStep()
        viewModel.selectSchoolYear(7)
        viewModel.nextStep()
        viewModel.completeOnboarding()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isCompleted)
        assertEquals(
            LearnerProfile(
                province = "Buenos Aires",
                schoolYear = 7,
                studentTrack = StudentTrack.SECONDARY,
                onboardingComplete = true
            ),
            repository.profilesByUserId[testUser.id]
        )
    }

    @Test
    fun `completing onboarding with a null session user sets an error and does not persist`() = runTest(dispatcher) {
        val repository = FakeLearnerProfileRepository()
        val viewModel = OnboardingViewModel(OnboardingFakeAuthRepository(user = null), repository)

        viewModel.selectProvince("Buenos Aires")
        viewModel.nextStep()
        viewModel.selectTrack(StudentTrack.SECONDARY)
        viewModel.nextStep()
        viewModel.selectSchoolYear(7)
        viewModel.nextStep()
        viewModel.completeOnboarding()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isCompleted)
        assertFalse(viewModel.uiState.value.isSaving)
        assertNotNull(viewModel.uiState.value.errorMessage)
        assertTrue(repository.profilesByUserId.isEmpty())
    }
}

private val testUser = User(
    id = "user-1",
    name = "Test Student",
    email = "student@example.com",
    role = UserRole.STUDENT
)

private class OnboardingFakeAuthRepository(user: User?) : AuthRepository {
    private val state = MutableStateFlow(AuthSession(token = "token-123", user = user))
    override val session: StateFlow<AuthSession> = state
    override suspend fun login(email: String, password: String): Result<User> = Result.success(requireNotNull(state.value.user))
    override suspend fun register(name: String, email: String, password: String): Result<User> = Result.success(requireNotNull(state.value.user))
    override fun replaceSessionUser(user: User, expectedToken: String?) { state.value = state.value.copy(user = user) }
    override fun logout() = Unit
}

private class FakeLearnerProfileRepository : LearnerProfileRepository {
    val profilesByUserId = mutableMapOf<String, LearnerProfile>()
    var savedProfile: LearnerProfile? = null

    override suspend fun getProfile(userId: String): LearnerProfile? = profilesByUserId[userId]

    override suspend fun isOnboardingComplete(userId: String): Boolean =
        profilesByUserId[userId]?.onboardingComplete == true

    override suspend fun upsertProfile(userId: String, profile: LearnerProfile) {
        savedProfile = profile
        profilesByUserId[userId] = profile
    }
}
