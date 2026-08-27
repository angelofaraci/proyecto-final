package com.example.proyectofinal

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.proyectofinal.di.DatabaseDriverFactory
import com.example.proyectofinal.di.initializeKoin
import com.example.proyectofinal.domain.StudentTrack
import com.example.proyectofinal.ui.home.HomeDashboardContent
import com.example.proyectofinal.ui.home.HomeDashboardUiState
import org.koin.dsl.module

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        initializeKoin(
            module {
                single { DatabaseDriverFactory(applicationContext) }
            }
        )

        setContent {
            App()
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    MaterialTheme {
        HomeDashboardContent(
            uiState = HomeDashboardUiState(
                isLoading = false,
                greeting = "Buenos días, María",
                schoolYear = 7,
                studentTrack = StudentTrack.SECONDARY,
                level = 2,
                completedLessons = 5,
                hasEnrolledCourse = true
            ),
            onContinueLearning = {},
            onOpenLessonMap = {},
            onJoinCourse = {},
            onLogout = {}
        )
    }
}
