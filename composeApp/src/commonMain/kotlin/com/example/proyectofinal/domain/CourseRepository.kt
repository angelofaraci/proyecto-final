package com.example.proyectofinal.domain

import com.example.proyectofinal.models.Course
import com.example.proyectofinal.models.CourseStudentsProgressResponse

interface CourseRepository {
    /**
     * Fetches only official courses (those provided by the app for everyone).
     */
    suspend fun getOfficialCourses(schoolYear: Int? = null): List<Course>

    /**
     * Fetches a specific course by its ID.
     */
    suspend fun getCourseById(id: String): Course?

    /**
     * Fetches courses created by a specific user (their private dashboard).
     */
    suspend fun getMyCreatedCourses(creatorId: String): List<Course>

    /**
     * Fetches courses a user has joined via join codes.
     */
    suspend fun getEnrolledCourses(userId: String): List<Course>

    /** Fetches the network-only progress roster for a course owned by the caller. */
    suspend fun getStudentsProgress(courseId: String): CourseStudentsProgressResponse

    /**
     * Creates a new user course.
     */
    suspend fun createCourse(course: Course): Course

    /**
     * Updates an existing course.
     */
    suspend fun updateCourse(course: Course): Course

    /**
     * Deletes a course.
     */
    suspend fun deleteCourse(id: String)

    /**
     * Joins a course using a code.
     */
    suspend fun joinCourseByCode(userId: String, code: String): Course?
}
