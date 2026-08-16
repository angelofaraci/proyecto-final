package com.example.proyectofinal.ui

import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.example.proyectofinal.models.Lesson
import com.example.proyectofinal.ui.activities.TheorySheet
import com.example.proyectofinal.ui.theme.AppTheme
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test

class SupportingSurfacesRedesignRenderTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `theory sheet separates the lesson title from the readable content section`() {
        composeTestRule.setContent {
            AppTheme {
                TheorySheet(
                    lesson = Lesson(
                        id = "lesson-1",
                        courseId = "course-1",
                        creatorId = "teacher-1",
                        title = "Fractions",
                        theoryContent = "A fraction represents part of a whole."
                    ),
                    onDismiss = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("theorySheetTitle").assertIsDisplayed()
        composeTestRule.onNodeWithTag("theorySheetConcept").assertIsDisplayed()
        composeTestRule.onNodeWithTag("theorySheetSteps").assertIsDisplayed()
        composeTestRule.onNodeWithTag("theorySheetExample").assertIsDisplayed()
        composeTestRule.onNodeWithTag("theorySheetPrevious").assertIsDisplayed()
            .assertIsNotEnabled()
        composeTestRule.onNodeWithTag("theorySheetNext").assertIsDisplayed()
            .assertIsNotEnabled()
        composeTestRule.onNodeWithTag("theorySheetChapter").assertIsDisplayed()
        composeTestRule.onNodeWithTag("theorySheetStep1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("theorySheetStep2").assertIsDisplayed()
        composeTestRule.onNodeWithTag("theorySheetStep3").assertIsDisplayed()
    }

    @Test
    fun `onboarding exposes tappable selection cards and a compact back action`() {
        var selectedProvince: String? = null

        composeTestRule.setContent {
            AppTheme {
                OnboardingContent(
                    state = OnboardingUiState(provinces = listOf("Buenos Aires")),
                    onProvinceSelected = { selectedProvince = it },
                    onSchoolYearSelected = {},
                    onTrackSelected = {},
                    onContinue = {},
                    onBack = {},
                    onComplete = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("selectionCard-Buenos Aires")
            .assertIsDisplayed()
            .performClick()
        assertEquals("Buenos Aires", selectedProvince)
    }

    @Test
    fun `onboarding back action is a 38dp square after the first step`() {
        composeTestRule.setContent {
            AppTheme {
                OnboardingContent(
                    state = OnboardingUiState(
                        currentStep = OnboardingStep.CATEGORY,
                        selectedProvince = "Buenos Aires"
                    ),
                    onProvinceSelected = {},
                    onSchoolYearSelected = {},
                    onTrackSelected = {},
                    onContinue = {},
                    onBack = {},
                    onComplete = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("onboardingBackButton")
            .assertWidthIsEqualTo(38.dp)
            .assertHeightIsEqualTo(38.dp)
    }

    @Test
    fun `placeholder renders a branded loading surface when content is pending`() {
        composeTestRule.setContent {
            AppTheme {
                PlaceholderScreen(
                    title = "Progress",
                    message = "Loading progress",
                    state = PlaceholderState.Loading
                )
            }
        }

        composeTestRule.onNodeWithTag("placeholderLoading").assertIsDisplayed()
        composeTestRule.onNodeWithTag("placeholderIllustration").assertIsDisplayed()
    }

    @Test
    fun `placeholder keeps empty-state copy visible without a loading indicator`() {
        composeTestRule.setContent {
            AppTheme {
                PlaceholderScreen(
                    title = "Progress",
                    message = "No activity yet"
                )
            }
        }

        composeTestRule.onNodeWithText("Progress").assertIsDisplayed()
        composeTestRule.onNodeWithText("No activity yet").assertIsDisplayed()
        composeTestRule.onAllNodesWithTag("placeholderLoading").assertCountEquals(0)
    }
    @Test
    fun `empty state renders a branded illustration and an action surface`() {
        val router = MainRouterViewModel(MainTab.PROGRESS)
        composeTestRule.setContent {
            AppTheme {
                PlaceholderScreen(
                    title = "Progress",
                    message = "No activity yet",
                    onExploreActivities = router::showActivities
                )
            }
        }

        composeTestRule.onNodeWithTag("placeholderEmptyArtwork").assertIsDisplayed()
        composeTestRule.onNodeWithTag("placeholderEmptyAction").assertIsDisplayed()
            .performClick()
        assertEquals(MainTab.ACTIVITIES, router.target.value)
        composeTestRule.onNodeWithTag("activityEmptyCard").assertIsDisplayed()
        composeTestRule.onNodeWithTag("activityEmptyTitle").assertIsDisplayed()
    }

    @Test
    fun `loading state renders content skeleton rather than only a spinner`() {
        composeTestRule.setContent {
            AppTheme {
                PlaceholderScreen(
                    title = "Progress",
                    message = "Loading progress",
                    state = PlaceholderState.Loading
                )
            }
        }

        composeTestRule.onNodeWithTag("placeholderLoadingSkeleton").assertIsDisplayed()
        composeTestRule.onNodeWithTag("placeholderLoadingAction").assertIsDisplayed()
        composeTestRule.onNodeWithTag("activityLoadingHeader").assertIsDisplayed()
        composeTestRule.onAllNodesWithTag("activityLoadingOption").assertCountEquals(2)
        composeTestRule.onNodeWithTag("activityLoadingConfirm").assertIsDisplayed()
    }

    @Test
    fun `bottom navigation remains available outside the player and hides within it`() {
        assertTrue(homeBottomNavigationVisible(isExercisePlayerActive = false))
        assertFalse(homeBottomNavigationVisible(isExercisePlayerActive = true))
    }

}
