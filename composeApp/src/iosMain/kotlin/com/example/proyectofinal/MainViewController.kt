package com.example.proyectofinal

import androidx.compose.ui.window.ComposeUIViewController
import com.example.proyectofinal.di.DatabaseDriverFactory
import com.example.proyectofinal.di.initializeKoin
import com.example.proyectofinal.ui.localization.bootstrapIosAppLocale
import org.koin.dsl.module

fun MainViewController() = run {
    bootstrapIosAppLocale()
    initializeKoin(
        module {
            single { DatabaseDriverFactory() }
        }
    )

    ComposeUIViewController { App() }
}
