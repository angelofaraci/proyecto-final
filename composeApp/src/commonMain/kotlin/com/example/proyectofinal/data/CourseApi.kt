package com.example.proyectofinal.data

import com.example.proyectofinal.di.ApiConfig
import com.example.proyectofinal.models.Course
import com.example.proyectofinal.models.CreateCourseRequest
import com.example.proyectofinal.models.CourseStudentsProgressResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

class CourseApi(
    private val client: HttpClient,
    private val apiConfig: ApiConfig
) {

    private val baseUrl: String = apiConfig.baseUrl

    suspend fun fetchOfficialCourses(schoolYear: Int? = null): List<Course> {
        return client.get("$baseUrl/courses/official") {
            schoolYear?.let { parameter("schoolYear", it) }
        }.body()
    }

    suspend fun fetchCourseById(id: String): Course {
        return client.get("$baseUrl/courses/$id").body()
    }

    suspend fun fetchMyCourses(creatorId: String): List<Course> {
        return client.get("$baseUrl/courses/creator/$creatorId").body()
    }

    suspend fun fetchEnrolledCourses(userId: String): List<Course> {
        return client.get("$baseUrl/courses/enrolled/$userId").body()
    }

    suspend fun fetchStudentsProgress(courseId: String): CourseStudentsProgressResponse {
        return client.get("$baseUrl/courses/$courseId/students/progress").body()
    }

    suspend fun createCourse(course: Course): Course {
        return client.post("$baseUrl/courses") {
            contentType(ContentType.Application.Json)
            setBody(
                CreateCourseRequest(
                    id = course.id,
                    title = course.title,
                    description = course.description,
                    joinCode = course.joinCode,
                    schoolYear = course.schoolYear,
                    topic = course.topic,
                    difficulty = course.difficulty,
                    durationMinutes = course.durationMinutes,
                    xpReward = course.xpReward
                )
            )
        }.body()
    }

    suspend fun updateCourse(course: Course): Course {
        return client.put("$baseUrl/courses/${course.id}") {
            contentType(ContentType.Application.Json)
            setBody(course)
        }.body()
    }

    suspend fun deleteCourse(id: String) {
        client.delete("$baseUrl/courses/$id")
    }

    suspend fun joinCourse(userId: String, code: String): Course {
        return client.post("$baseUrl/courses/join") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("userId" to userId, "code" to code))
        }.body()
    }
}
