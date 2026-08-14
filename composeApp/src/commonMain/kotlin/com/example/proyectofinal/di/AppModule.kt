package com.example.proyectofinal.di

import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.EnumColumnAdapter
import com.example.proyectofinal.data.AuthApi
import com.example.proyectofinal.data.CourseApi
import com.example.proyectofinal.data.ExerciseApi
import com.example.proyectofinal.data.KtorCourseRepository
import com.example.proyectofinal.data.KtorAuthRepository
import com.example.proyectofinal.data.KtorExerciseRepository
import com.example.proyectofinal.data.KtorLessonRepository
import com.example.proyectofinal.data.KtorUserRepository
import com.example.proyectofinal.data.LessonApi
import com.example.proyectofinal.data.SqlDelightLearnerProfileRepository
import com.example.proyectofinal.data.UserApi
import com.example.proyectofinal.db.AppDatabase
import com.example.proyectofinal.db.CourseEntity
import com.example.proyectofinal.db.ExerciseEntity
import com.example.proyectofinal.db.UserEntity
import com.example.proyectofinal.db.UserProgressEntity
import com.example.proyectofinal.domain.AuthRepository
import com.example.proyectofinal.domain.CourseRepository
import com.example.proyectofinal.domain.ExerciseRepository
import com.example.proyectofinal.domain.LearnerProfileRepository
import com.example.proyectofinal.domain.LessonRepository
import com.example.proyectofinal.domain.UserRepository
import com.example.proyectofinal.models.UserRole
import com.example.proyectofinal.ui.AuthGateViewModel
import com.example.proyectofinal.ui.CourseViewModel
import com.example.proyectofinal.ui.activities.LessonMapViewModel
import com.example.proyectofinal.ui.home.HomeDashboardViewModel
import com.example.proyectofinal.ui.LoginViewModel
import com.example.proyectofinal.ui.MainRouterViewModel
import com.example.proyectofinal.ui.OnboardingViewModel
import com.example.proyectofinal.ui.ProfileViewModel
import com.example.proyectofinal.ui.RegisterViewModel
import com.example.proyectofinal.ui.teacher.TeacherDashboardViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    includes(networkModule)

    single { createAppDatabase(get()) }

    single { CourseApi(get(), get()) }
    single { LessonApi(get(), get()) }
    single { ExerciseApi(get(), get()) }
    single { UserApi(get(), get()) }
    single { AuthApi(get(), get()) }

    single<CourseRepository> { KtorCourseRepository(get(), get()) }
    single<AuthRepository> { KtorAuthRepository(get(), get()) }
    single<LessonRepository> { KtorLessonRepository(get(), get()) }
    single<ExerciseRepository> { KtorExerciseRepository(get(), get()) }
    single<UserRepository> { KtorUserRepository(get(), get(), get()) }
    single<LearnerProfileRepository> { SqlDelightLearnerProfileRepository(get()) }

    viewModelOf(::CourseViewModel)
    viewModelOf(::AuthGateViewModel)
    viewModelOf(::LessonMapViewModel)
    viewModelOf(::HomeDashboardViewModel)
    viewModelOf(::LoginViewModel)
    viewModel { MainRouterViewModel() }
    viewModelOf(::OnboardingViewModel)
    viewModelOf(::ProfileViewModel)
    viewModelOf(::RegisterViewModel)
    viewModel { (teacherId: String) -> TeacherDashboardViewModel(get(), teacherId) }
}

internal val userRoleColumnAdapter = object : ColumnAdapter<UserRole, String> {
    override fun decode(databaseValue: String): UserRole =
        UserRole.parse(databaseValue)
            ?: error("Unknown persisted user role: $databaseValue")

    override fun encode(value: UserRole): String = value.name
}

private fun createAppDatabase(driverFactory: DatabaseDriverFactory): AppDatabase {
    val intAdapter = object : ColumnAdapter<Int, Long> {
        override fun decode(databaseValue: Long): Int = databaseValue.toInt()

        override fun encode(value: Int): Long = value.toLong()
    }

    val driver = driverFactory.createDriver().applyPendingLocalSchemaFixes()

    return AppDatabase(
        driver = driver,
        CourseEntityAdapter = CourseEntity.Adapter(
            schoolYearAdapter = intAdapter,
            durationMinutesAdapter = intAdapter,
            xpRewardAdapter = intAdapter
        ),
        ExerciseEntityAdapter = ExerciseEntity.Adapter(
            typeAdapter = EnumColumnAdapter()
        ),
        UserProgressEntityAdapter = UserProgressEntity.Adapter(
            totalScoreAdapter = intAdapter
        ),
        UserEntityAdapter = UserEntity.Adapter(
            roleAdapter = userRoleColumnAdapter
        )
    )
}
