package com.example.proyectofinal.ui.teacher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.proyectofinal.domain.CourseRepository
import com.example.proyectofinal.models.Course
import com.example.proyectofinal.models.CourseStudentsProgressResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

internal fun teacherDashboardViewModelKey(userId: String): String = "teacher-dashboard:$userId"

data class TeacherDashboardUiState(
    val courses: List<Course> = emptyList(),
    val selectedCourseProgress: CourseStudentsProgressResponse? = null,
    val isLoading: Boolean = true,
    val isCreating: Boolean = false,
    val errorMessage: String? = null
)

class TeacherDashboardViewModel(
    private val courseRepository: CourseRepository,
    private val teacherId: String
) : ViewModel() {
    private val _uiState = MutableStateFlow(TeacherDashboardUiState())
    val uiState = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching { courseRepository.getMyCreatedCourses(teacherId) }
                .onSuccess { _uiState.value = _uiState.value.copy(courses = it, isLoading = false) }
                .onFailure { _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = it.message) }
        }
    }

    fun createCourse(title: String, description: String) {
        if (title.isBlank() || description.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCreating = true, errorMessage = null)
            val course = Course(
                id = "teacher-${Random.nextLong().toString().replace('-', '0')}",
                title = title.trim(),
                description = description.trim(),
                creatorId = teacherId
            )
            runCatching {
                courseRepository.createCourse(course)
                courseRepository.getMyCreatedCourses(teacherId)
            }.onSuccess { courses ->
                    _uiState.value = _uiState.value.copy(courses = courses, isCreating = false)
                }
                .onFailure { _uiState.value = _uiState.value.copy(isCreating = false, errorMessage = it.message) }
        }
    }

    fun selectCourse(courseId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching { courseRepository.getStudentsProgress(courseId) }
                .onSuccess {
                    _uiState.value = _uiState.value.copy(selectedCourseProgress = it, isLoading = false)
                }
                .onFailure { _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = it.message) }
        }
    }
}
