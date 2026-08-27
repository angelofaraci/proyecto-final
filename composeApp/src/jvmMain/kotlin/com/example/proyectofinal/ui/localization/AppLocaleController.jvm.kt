package com.example.proyectofinal.ui.localization

import java.util.Locale

private val JvmLocaleProcessBaseline: Locale = Locale.getDefault()

internal actual fun createPlatformLocaleActivator(): PlatformLocaleActivator = JvmLocaleActivator()

private class JvmLocaleActivator : PlatformLocaleActivator {
    override val activationPolicy = LocaleActivationPolicy.IMMEDIATE

    override fun activate(accountId: String, language: AppLanguage?) {
        Locale.setDefault(language?.let { Locale.forLanguageTag(it.languageTag) } ?: JvmLocaleProcessBaseline)
    }
}
