package com.example.proyectofinal.ui.localization

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AppLocaleControllerTest {
    @Test
    fun acceptedLanguageUpdatesStateAfterPlatformActivation() {
        val activator = RecordingLocaleActivator(LocaleActivationPolicy.IMMEDIATE)
        val controller = AppLocaleController(activator)

        controller.apply("user-1", AppLanguage.ENGLISH)

        assertEquals("user-1" to AppLanguage.ENGLISH, activator.calls.single())
        assertEquals(AppLanguage.ENGLISH, controller.state.value.language)
        assertEquals(1, controller.state.value.revision)
    }

    @Test
    fun iosPolicyCanRepresentDeferredActivationWithoutChangingControllerContract() {
        val activator = RecordingLocaleActivator(LocaleActivationPolicy.NEXT_LAUNCH)
        val controller = AppLocaleController(activator)

        controller.apply("user-1", AppLanguage.SPANISH)

        assertEquals(LocaleActivationPolicy.NEXT_LAUNCH, controller.state.value.activationPolicy)
        assertEquals(AppLanguage.SPANISH, controller.state.value.language)
    }

    @Test
    fun clearRemovesOnlyTheActiveAccountOverride() {
        val activator = RecordingLocaleActivator(LocaleActivationPolicy.IMMEDIATE)
        val controller = AppLocaleController(activator)
        controller.apply("user-1", AppLanguage.ENGLISH)

        controller.clear("user-1")

        assertEquals("user-1" to null, activator.calls.last())
        assertEquals(null, controller.state.value.language)
        assertEquals(2, controller.state.value.revision)
    }

    @Test
    fun blankAccountIdIsRejectedBeforePlatformMutation() {
        val activator = RecordingLocaleActivator(LocaleActivationPolicy.IMMEDIATE)
        val controller = AppLocaleController(activator)

        assertFailsWith<IllegalArgumentException> {
            controller.apply(" ", AppLanguage.ENGLISH)
        }
        assertEquals(emptyList(), activator.calls)
    }

    @Test
    fun deferredLocaleIsResolvedByAControllerCreatedForTheNextLaunch() {
        val store = InMemoryLocalePreferenceStore()
        NextLaunchLocaleActivator(store).activate("user-1", AppLanguage.ENGLISH)

        val relaunchedActivator = NextLaunchLocaleActivator(store)

        assertEquals("en", relaunchedActivator.languageTagForNextLaunch())
    }

    @Test
    fun clearingDeferredLocaleRemovesTheNextLaunchOverride() {
        val store = InMemoryLocalePreferenceStore()
        val activator = NextLaunchLocaleActivator(store)
        activator.activate("user-1", AppLanguage.ENGLISH)

        activator.activate("user-1", null)

        assertEquals(null, NextLaunchLocaleActivator(store).languageTagForNextLaunch())
    }

    @Test
    fun clearingInactiveAccountPreservesActiveAccountForNextLaunch() {
        val store = InMemoryLocalePreferenceStore()
        val activator = NextLaunchLocaleActivator(store)
        activator.activate("user-1", AppLanguage.SPANISH)
        activator.activate("user-2", AppLanguage.ENGLISH)

        activator.activate("user-1", null)

        assertEquals("en", NextLaunchLocaleActivator(store).languageTagForNextLaunch())
    }
}

private class RecordingLocaleActivator(
    override val activationPolicy: LocaleActivationPolicy
) : PlatformLocaleActivator {
    val calls = mutableListOf<Pair<String, AppLanguage?>>()

    override fun activate(accountId: String, language: AppLanguage?) {
        calls += accountId to language
    }
}

private class InMemoryLocalePreferenceStore : LocalePreferenceStore {
    private val values = mutableMapOf<String, String>()

    override fun read(key: String): String? = values[key]

    override fun write(key: String, value: String) {
        values[key] = value
    }

    override fun remove(key: String) {
        values.remove(key)
    }
}
