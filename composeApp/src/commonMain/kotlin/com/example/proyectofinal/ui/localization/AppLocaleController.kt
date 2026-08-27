package com.example.proyectofinal.ui.localization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppLanguage(val languageTag: String) {
    SPANISH("es"),
    ENGLISH("en");

    companion object {
        fun fromLanguageTag(languageTag: String?): AppLanguage? =
            entries.firstOrNull { it.languageTag == languageTag }
    }
}

enum class LocaleActivationPolicy {
    IMMEDIATE,
    NEXT_LAUNCH
}

data class AppLocaleState(
    val language: AppLanguage? = null,
    val activationPolicy: LocaleActivationPolicy,
    val revision: Long = 0
)

internal interface PlatformLocaleActivator {
    val activationPolicy: LocaleActivationPolicy

    fun activate(accountId: String, language: AppLanguage?)
}

internal interface LocalePreferenceStore {
    fun read(key: String): String?
    fun write(key: String, value: String)
    fun remove(key: String)
}

internal class NextLaunchLocaleActivator(
    private val store: LocalePreferenceStore
) : PlatformLocaleActivator {
    override val activationPolicy = LocaleActivationPolicy.NEXT_LAUNCH

    override fun activate(accountId: String, language: AppLanguage?) {
        if (language == null) {
            store.remove(accountLocaleKey(accountId))
            if (store.read(ActiveLocaleAccountKey) == accountId) {
                store.remove(ActiveLocaleAccountKey)
            }
        } else {
            store.write(accountLocaleKey(accountId), language.languageTag)
            store.write(ActiveLocaleAccountKey, accountId)
        }
    }

    fun languageTagForNextLaunch(): String? {
        val accountId = store.read(ActiveLocaleAccountKey) ?: return null
        return store.read(accountLocaleKey(accountId))
    }

    private fun accountLocaleKey(accountId: String) = AccountLocaleKeyPrefix + accountId

    private companion object {
        const val ActiveLocaleAccountKey = "profile.locale.activeAccount"
        const val AccountLocaleKeyPrefix = "profile.locale.account."
    }
}

/** Recreates the resource-consuming subtree after an immediate platform locale change. */
@Composable
internal fun AppLocaleHost(
    controller: AppLocaleController,
    content: @Composable () -> Unit
) {
    val state by controller.state.collectAsState()
    key(state.revision) {
        content()
    }
}

internal expect fun createPlatformLocaleActivator(): PlatformLocaleActivator

/** Applies an authenticated account's language at the platform-defined activation point. */
class AppLocaleController internal constructor(
    private val activator: PlatformLocaleActivator = createPlatformLocaleActivator()
) {
    private val mutableState = MutableStateFlow(
        AppLocaleState(activationPolicy = activator.activationPolicy)
    )

    val state: StateFlow<AppLocaleState> = mutableState.asStateFlow()

    fun apply(accountId: String, language: AppLanguage) {
        require(accountId.isNotBlank()) { "Account id must not be blank" }
        activator.activate(accountId, language)
        mutableState.value = mutableState.value.copy(
            language = language,
            revision = mutableState.value.revision + 1
        )
    }

    fun clear(accountId: String) {
        require(accountId.isNotBlank()) { "Account id must not be blank" }
        activator.activate(accountId, null)
        mutableState.value = mutableState.value.copy(
            language = null,
            revision = mutableState.value.revision + 1
        )
    }
}
