package com.example.proyectofinal.ui.localization

import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import java.util.Locale
import org.jetbrains.compose.resources.stringResource
import org.junit.Rule
import proyectofinal.composeapp.generated.resources.Res
import proyectofinal.composeapp.generated.resources.teacher_dashboard_title
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AppLocaleControllerJvmTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val originalLocale = Locale.getDefault()
    private val expectedBaseline = Locale.forLanguageTag("pt-BR")

    @AfterTest
    fun restoreLocale() {
        Locale.setDefault(originalLocale)
    }

    @Test
    fun switchingLanguageRefreshesMountedComposeResourceText() {
        Locale.setDefault(expectedBaseline)
        val controller = AppLocaleController()
        controller.apply("user-1", AppLanguage.SPANISH)
        composeRule.setContent {
            AppLocaleHost(controller) {
                Text(stringResource(Res.string.teacher_dashboard_title))
            }
        }
        composeRule.onNodeWithText("Panel docente").assertExists()

        composeRule.runOnIdle {
            controller.apply("user-1", AppLanguage.ENGLISH)
        }
        composeRule.onNodeWithText("Teacher dashboard").assertExists()
        assertEquals(2, controller.state.value.revision)
    }

    @Test
    fun clearFromLaterControllerRestoresProcessBaseline() {
        Locale.setDefault(expectedBaseline)
        val firstController = AppLocaleController()
        firstController.apply("user-1", AppLanguage.ENGLISH)

        AppLocaleController().clear("user-1")

        assertEquals(expectedBaseline, Locale.getDefault())
    }
}
