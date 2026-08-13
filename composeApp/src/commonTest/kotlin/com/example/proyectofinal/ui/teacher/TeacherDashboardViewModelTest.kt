package com.example.proyectofinal.ui.teacher

import com.example.proyectofinal.domain.CourseRepository
import com.example.proyectofinal.models.Course
import com.example.proyectofinal.models.CourseStudentProgress
import com.example.proyectofinal.models.CourseStudentsProgressResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals

@OptIn(ExperimentalCoroutinesApi::class)
class TeacherDashboardViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() = Dispatchers.setMain(dispatcher)
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `view model key is stable per teacher and isolated across accounts`() {
        assertEquals(
            teacherDashboardViewModelKey("teacher-a"),
            teacherDashboardViewModelKey("teacher-a")
        )
        assertNotEquals(
            teacherDashboardViewModelKey("teacher-a"),
            teacherDashboardViewModelKey("teacher-b")
        )
    }

    @Test
    fun `loads teacher owned courses`() = runTest(dispatcher) {
        val repository = FakeTeacherCourseRepository(mutableListOf(testCourse("course-1", "Fractions")))
        val viewModel = TeacherDashboardViewModel(repository, "teacher-1")
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(listOf("Fractions"), viewModel.uiState.value.courses.map { it.title })
    }

    @Test
    fun `creates a course and refreshes owned courses`() = runTest(dispatcher) {
        val repository = FakeTeacherCourseRepository()
        val viewModel = TeacherDashboardViewModel(repository, "teacher-1")
        advanceUntilIdle()

        viewModel.createCourse("Algebra", "Linear equations")
        advanceUntilIdle()

        assertEquals("Algebra", viewModel.uiState.value.courses.single().title)
        assertEquals("teacher-1", repository.created.single().creatorId)
        assertFalse(repository.created.single().isOfficial)
    }

    @Test
    fun `selecting a course loads its student progress`() = runTest(dispatcher) {
        val roster = CourseStudentsProgressResponse(
            "course-1", "Fractions", 2,
            listOf(CourseStudentProgress("student-1", "Ada", 1, 3, 50))
        )
        val repository = FakeTeacherCourseRepository(
            mutableListOf(testCourse("course-1", "Fractions")), roster
        )
        val viewModel = TeacherDashboardViewModel(repository, "teacher-1")
        advanceUntilIdle()

        viewModel.selectCourse("course-1")
        advanceUntilIdle()

        assertEquals(roster, viewModel.uiState.value.selectedCourseProgress)
    }
}

private class FakeTeacherCourseRepository(
    private val courses: MutableList<Course> = mutableListOf(),
    private val roster: CourseStudentsProgressResponse? = null
) : CourseRepository {
    val created = mutableListOf<Course>()
    override suspend fun getMyCreatedCourses(creatorId: String) = courses.toList()
    override suspend fun createCourse(course: Course) = course.also { created += it; courses += it }
    override suspend fun getStudentsProgress(courseId: String) = roster ?: error("Roster unavailable")
    override suspend fun getOfficialCourses(schoolYear: Int?) = emptyList<Course>()
    override suspend fun getCourseById(id: String) = courses.firstOrNull { it.id == id }
    override suspend fun getEnrolledCourses(userId: String) = emptyList<Course>()
    override suspend fun updateCourse(course: Course) = course
    override suspend fun deleteCourse(id: String) = Unit
    override suspend fun joinCourseByCode(userId: String, code: String) = null
}

private fun testCourse(id: String, title: String) = Course(id, title, title, "teacher-1")
