package com.example.proyectofinal.ui.localization

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

internal actual fun createPlatformLocaleActivator(): PlatformLocaleActivator = AndroidLocaleActivator()

private class AndroidLocaleActivator : PlatformLocaleActivator {
    override val activationPolicy = LocaleActivationPolicy.IMMEDIATE

    override fun activate(accountId: String, language: AppLanguage?) {
        val locales = LocaleListCompat.forLanguageTags(language?.languageTag.orEmpty())
        AppCompatDelegate.setApplicationLocales(locales)
    }
}
