package com.example.proyectofinal.service

import com.example.proyectofinal.database.EnrolledCourses
import com.example.proyectofinal.database.dbQuery
import com.example.proyectofinal.models.UserRole
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll

internal data class CourseContentAccess(
    val courseId: String,
    val creatorId: String,
    val isOfficial: Boolean
)

internal sealed interface LessonContentAccess {
    data class CourseLinked(val courseAccess: CourseContentAccess) : LessonContentAccess
    data class Standalone(val creatorId: String) : LessonContentAccess
}

internal fun canReadCourseContent(access: CourseContentAccess, userId: String, role: UserRole): Boolean =
    when (role) {
        UserRole.ADMIN -> true
        UserRole.TEACHER -> access.creatorId == userId || access.isOfficial
        UserRole.STUDENT -> access.isOfficial || isUserEnrolledInCourse(userId, access.courseId)
    }

internal fun canReadLessonContent(access: LessonContentAccess, userId: String, role: UserRole): Boolean =
    when (access) {
        is LessonContentAccess.CourseLinked -> canReadCourseContent(access.courseAccess, userId, role)
        is LessonContentAccess.Standalone -> when (role) {
            UserRole.ADMIN -> true
            UserRole.TEACHER, UserRole.STUDENT -> access.creatorId == userId
        }
    }

internal fun shouldHideLessonAnswers(access: LessonContentAccess, role: UserRole): Boolean =
    when (access) {
        is LessonContentAccess.CourseLinked,
        is LessonContentAccess.Standalone -> role == UserRole.STUDENT
    }

private fun isUserEnrolledInCourse(userId: String, courseId: String): Boolean = dbQuery {
    EnrolledCourses.selectAll()
        .where {
            (EnrolledCourses.userId eq userId) and
                (EnrolledCourses.courseId eq courseId)
        }
        .count() > 0
}
