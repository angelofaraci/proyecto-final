package com.example.proyectofinal.data

import com.example.proyectofinal.domain.CourseRepository
import com.example.proyectofinal.db.AppDatabase
import com.example.proyectofinal.models.Course
import com.example.proyectofinal.models.CourseStudentsProgressResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

class KtorCourseRepository(
    private val api: CourseApi,
    private val database: AppDatabase
) : CourseRepository {

    override suspend fun getOfficialCourses(schoolYear: Int?): List<Course> = withContext(Dispatchers.IO) {
        val courses = api.fetchOfficialCourses(schoolYear)
        courses.forEach { insertCourseToLocal(it) }
        courses
    }

    override suspend fun getCourseById(id: String): Course? = withContext(Dispatchers.IO) {
        try {
            val remote = api.fetchCourseById(id)
            insertCourseToLocal(remote)
            remote
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getMyCreatedCourses(creatorId: String): List<Course> = withContext(Dispatchers.IO) {
        val courses = api.fetchMyCourses(creatorId)
        courses.forEach { insertCourseToLocal(it) }
        courses
    }

    override suspend fun getEnrolledCourses(userId: String): List<Course> = withContext(Dispatchers.IO) {
        val courses = api.fetchEnrolledCourses(userId)
        courses.forEach { insertCourseToLocal(it) }
        courses
    }

    override suspend fun getStudentsProgress(courseId: String): CourseStudentsProgressResponse =
        withContext(Dispatchers.IO) {
            api.fetchStudentsProgress(courseId)
        }

    override suspend fun createCourse(course: Course): Course = withContext(Dispatchers.IO) {
        val created = api.createCourse(course)
        insertCourseToLocal(created)
        created
    }

    override suspend fun updateCourse(course: Course): Course = withContext(Dispatchers.IO) {
        val updated = api.updateCourse(course)
        insertCourseToLocal(updated)
        updated
    }

    override suspend fun deleteCourse(id: String) {
        withContext(Dispatchers.IO) {
            api.deleteCourse(id)
            database.appDatabaseQueries.deleteCourse(id)
        }
    }

    override suspend fun joinCourseByCode(userId: String, code: String): Course? = withContext(Dispatchers.IO) {
        try {
            val joined = api.joinCourse(userId, code)
            insertCourseToLocal(joined)
            joined
        } catch (e: Exception) {
            null
        }
    }

    private fun insertCourseToLocal(course: Course) {
        database.appDatabaseQueries.insertCourse(
            id = course.id,
            title = course.title,
            description = course.description,
            creatorId = course.creatorId,
            isOfficial = course.isOfficial,
            schoolYear = course.schoolYear,
            joinCode = course.joinCode,
            topic = course.topic,
            difficulty = course.difficulty,
            durationMinutes = course.durationMinutes,
            xpReward = course.xpReward
        )
    }
}
