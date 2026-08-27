package com.example.proyectofinal.ui.localization

import platform.Foundation.NSUserDefaults

private const val AppleLanguagesKey = "AppleLanguages"

internal actual fun createPlatformLocaleActivator(): PlatformLocaleActivator =
    NextLaunchLocaleActivator(IosLocalePreferenceStore)

private object IosLocalePreferenceStore : LocalePreferenceStore {
    private val defaults get() = NSUserDefaults.standardUserDefaults

    override fun read(key: String): String? = defaults.stringForKey(key)

    override fun write(key: String, value: String) {
        defaults.setObject(value, forKey = key)
        defaults.synchronize()
    }

    override fun remove(key: String) {
        defaults.removeObjectForKey(key)
        defaults.synchronize()
    }
}

/** Installs the saved account language before Compose reads its resource environment. */
internal fun bootstrapIosAppLocale() {
    val defaults = NSUserDefaults.standardUserDefaults
    val languageTag = NextLaunchLocaleActivator(IosLocalePreferenceStore).languageTagForNextLaunch()
    if (languageTag == null) {
        defaults.removeObjectForKey(AppleLanguagesKey)
        defaults.synchronize()
        return
    }
    defaults.setObject(listOf(languageTag), forKey = AppleLanguagesKey)
    defaults.synchronize()
}
