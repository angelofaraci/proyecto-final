package com.example.proyectofinal.service

import com.example.proyectofinal.database.Courses
import com.example.proyectofinal.database.CompletedExercises
import com.example.proyectofinal.database.CompletedLessons
import com.example.proyectofinal.database.EnrolledCourses
import com.example.proyectofinal.database.Lessons
import com.example.proyectofinal.database.Exercises
import com.example.proyectofinal.database.Users
import com.example.proyectofinal.database.dbQuery
import com.example.proyectofinal.models.AdminCourseResponse
import com.example.proyectofinal.models.Course
import com.example.proyectofinal.models.CourseStudentProgress
import com.example.proyectofinal.models.CourseStudentsProgressResponse
import com.example.proyectofinal.models.CreateCourseRequest
import com.example.proyectofinal.models.UpdateCourseRequest
import com.example.proyectofinal.models.UserRole
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update

sealed interface CourseReadResult {
    data class Success(val course: Course) : CourseReadResult
    object Forbidden : CourseReadResult
    object NotFound : CourseReadResult
}

sealed interface AdminCourseMutationResult {
    data class Success(val course: Course) : AdminCourseMutationResult
    data class InvalidRequest(val message: String) : AdminCourseMutationResult
    object NotFound : AdminCourseMutationResult
}

class CourseService {
    fun getOfficialCourses(schoolYear: Int? = null): List<Course> = dbQuery {
        val filter = if (schoolYear == null) {
            Courses.isOfficial eq true
        } else {
            (Courses.isOfficial eq true) and (Courses.schoolYear eq schoolYear)
        }

        Courses.selectAll()
            .where { filter }
            .map { it.toCourse() }
    }

    fun getAllCoursesAdmin(): List<AdminCourseResponse> = dbQuery {
        Courses.selectAll().map { courseRow ->
            val courseId = courseRow[Courses.id]
            val creatorName = Users.selectAll()
                .where { Users.id eq courseRow[Courses.creatorId] }
                .firstOrNull()
                ?.get(Users.name) ?: "Unknown"

            val enrollmentCount = EnrolledCourses.selectAll()
                .where { EnrolledCourses.courseId eq courseId }
                .count()

            AdminCourseResponse(
                id = courseId,
                title = courseRow[Courses.title],
                description = courseRow[Courses.description],
                creatorId = courseRow[Courses.creatorId],
                creatorName = creatorName,
                enrollmentCount = enrollmentCount.toInt(),
                isOfficial = courseRow[Courses.isOfficial],
                schoolYear = courseRow[Courses.schoolYear]
            )
        }
    }

    fun getCourseById(id: String): Course? = dbQuery {
        val course = Courses.selectAll()
            .where { Courses.id eq id }
            .firstOrNull()
            ?: return@dbQuery null

        val lessons = Lessons.selectAll()
            .where { Lessons.courseId eq id }
            .orderBy(Lessons.orderIndex)
            .map { it.toLesson() }

        course.toCourse(lessons)
    }

    fun getCourseByIdForUser(id: String, userId: String, role: UserRole): CourseReadResult {
        val courseAccess = dbQuery {
            Courses.select(Courses.id, Courses.creatorId, Courses.isOfficial)
                .where { Courses.id eq id }
                .firstOrNull()
                ?.let {
                    CourseContentAccess(
                        courseId = it[Courses.id],
                        creatorId = it[Courses.creatorId],
                        isOfficial = it[Courses.isOfficial]
                    )
                }
        } ?: return CourseReadResult.NotFound

        if (!canReadCourseContent(courseAccess, userId, role)) {
            return CourseReadResult.Forbidden
        }

        return getCourseById(id)
            ?.let { CourseReadResult.Success(it) }
            ?: CourseReadResult.NotFound
    }

    fun getCoursesByCreator(creatorId: String): List<Course> = dbQuery {
        Courses.selectAll()
            .where { Courses.creatorId eq creatorId }
            .map { it.toCourse() }
    }

    fun getEnrolledCourses(userId: String): List<Course> = dbQuery {
        val courseIds = EnrolledCourses.selectAll()
            .where { EnrolledCourses.userId eq userId }
            .map { it[EnrolledCourses.courseId] }

        if (courseIds.isEmpty()) {
            emptyList()
        } else {
            Courses.selectAll()
                .where { Courses.id inList courseIds }
                .map { it.toCourse() }
        }
    }

    fun getStudentsProgress(courseId: String): CourseStudentsProgressResponse? = dbQuery {
        val course = Courses.selectAll()
            .where { Courses.id eq courseId }
            .firstOrNull()
            ?: return@dbQuery null

        val lessonIds = Lessons.select(Lessons.id)
            .where { Lessons.courseId eq courseId }
            .map { it[Lessons.id] }
        val exerciseIds = if (lessonIds.isEmpty()) {
            emptyList()
        } else {
            Exercises.select(Exercises.id)
                .where { Exercises.lessonId inList lessonIds }
                .map { it[Exercises.id] }
        }
        val enrolledUserIds = EnrolledCourses.select(EnrolledCourses.userId)
            .where { EnrolledCourses.courseId eq courseId }
            .map { it[EnrolledCourses.userId] }

        val students = enrolledUserIds.mapNotNull { studentId ->
            val student = Users.select(Users.id, Users.name, Users.role)
                .where { Users.id eq studentId }
                .firstOrNull()
                ?: return@mapNotNull null
            if (UserRole.parse(student[Users.role]) != UserRole.STUDENT) {
                return@mapNotNull null
            }
            val completedLessonCount = if (lessonIds.isEmpty()) 0 else {
                CompletedLessons.select(CompletedLessons.lessonId)
                    .where {
                        (CompletedLessons.userId eq studentId) and
                            (CompletedLessons.lessonId inList lessonIds)
                    }
                    .map { it[CompletedLessons.lessonId] }
                    .distinct()
                    .size
            }
            val completedExerciseCount = if (exerciseIds.isEmpty()) 0 else {
                CompletedExercises.select(CompletedExercises.exerciseId)
                    .where {
                        (CompletedExercises.userId eq studentId) and
                            (CompletedExercises.exerciseId inList exerciseIds)
                    }
                    .map { it[CompletedExercises.exerciseId] }
                    .distinct()
                    .size
            }

            CourseStudentProgress(
                studentId = student[Users.id],
                studentName = student[Users.name],
                completedLessons = completedLessonCount,
                completedExercises = completedExerciseCount,
                progressPercentage = if (lessonIds.isEmpty()) 0 else completedLessonCount * 100 / lessonIds.size
            )
        }.sortedWith(compareBy(CourseStudentProgress::studentName, CourseStudentProgress::studentId))

        CourseStudentsProgressResponse(
            courseId = course[Courses.id],
            courseTitle = course[Courses.title],
            totalLessons = lessonIds.size,
            students = students
        )
    }

    fun createCourse(
        request: CreateCourseRequest,
        creatorId: String,
        isOfficial: Boolean = false
    ): Course = dbQuery {
        Courses.insert {
            it[Courses.id] = request.id
            it[Courses.title] = request.title
            it[Courses.description] = request.description
            it[Courses.creatorId] = creatorId
            it[Courses.isOfficial] = isOfficial
            it[Courses.schoolYear] = request.schoolYear
            it[Courses.joinCode] = request.joinCode
            it[Courses.topic] = request.topic
            it[Courses.difficulty] = request.difficulty
            it[Courses.durationMinutes] = request.durationMinutes
            it[Courses.xpReward] = request.xpReward
        }

        Course(
            id = request.id,
            title = request.title,
            description = request.description,
            creatorId = creatorId,
            isOfficial = isOfficial,
            joinCode = request.joinCode,
            schoolYear = request.schoolYear,
            topic = request.topic,
            difficulty = request.difficulty,
            durationMinutes = request.durationMinutes,
            xpReward = request.xpReward
        )
    }

    fun adminCreateCourse(
        request: com.example.proyectofinal.models.CreateAdminCourseRequest,
        authenticatedUserId: String
    ): AdminCourseMutationResult {
        if (request.title.isBlank()) {
            return AdminCourseMutationResult.InvalidRequest("title is required")
        }

        if (request.description.isBlank()) {
            return AdminCourseMutationResult.InvalidRequest("description is required")
        }

        return AdminCourseMutationResult.Success(
            createCourse(
                CreateCourseRequest(
                    id = request.id,
                    title = request.title,
                    description = request.description,
                    joinCode = null,
                    schoolYear = request.schoolYear,
                    topic = request.topic,
                    difficulty = request.difficulty,
                    durationMinutes = request.durationMinutes,
                    xpReward = request.xpReward
                ),
                creatorId = authenticatedUserId,
                isOfficial = request.isOfficial
            )
        )
    }

    fun updateCourse(id: String, request: UpdateCourseRequest): Course? {
        val updated = dbQuery {
            Courses.update({ Courses.id eq id }) { row ->
                request.title?.let { row[Courses.title] = it }
                request.description?.let { row[Courses.description] = it }
                request.joinCode?.let { row[Courses.joinCode] = it }
                request.schoolYear?.let { row[Courses.schoolYear] = it }
                request.topic?.let { row[Courses.topic] = it }
                request.difficulty?.let { row[Courses.difficulty] = it }
                request.durationMinutes?.let { row[Courses.durationMinutes] = it }
                request.xpReward?.let { row[Courses.xpReward] = it }
            }
        }

        if (updated == 0) {
            return null
        }

        return dbQuery {
            Courses.selectAll()
                .where { Courses.id eq id }
                .first()
                .toCourse()
        }
    }

    fun adminUpdateCourse(
        id: String,
        request: com.example.proyectofinal.models.UpdateAdminCourseRequest
    ): AdminCourseMutationResult {
        if (request.title != null && request.title.isBlank()) {
            return AdminCourseMutationResult.InvalidRequest("title cannot be blank")
        }

        if (request.description != null && request.description.isBlank()) {
            return AdminCourseMutationResult.InvalidRequest("description cannot be blank")
        }

        val updated = dbQuery {
            Courses.update({ Courses.id eq id }) { row ->
                request.title?.let { row[Courses.title] = it }
                request.description?.let { row[Courses.description] = it }
                request.isOfficial?.let { row[Courses.isOfficial] = it }
                request.schoolYear?.let { row[Courses.schoolYear] = it }
                request.topic?.let { row[Courses.topic] = it }
                request.difficulty?.let { row[Courses.difficulty] = it }
                request.durationMinutes?.let { row[Courses.durationMinutes] = it }
                request.xpReward?.let { row[Courses.xpReward] = it }
            }
        }

        if (updated == 0) {
            return AdminCourseMutationResult.NotFound
        }

        val course = dbQuery {
            Courses.selectAll()
                .where { Courses.id eq id }
                .firstOrNull()
                ?.toCourse()
        } ?: return AdminCourseMutationResult.NotFound

        return AdminCourseMutationResult.Success(course)
    }

    fun adminDeleteCourse(id: String): Boolean = deleteCourse(id)

    fun deleteCourse(id: String): Boolean = dbQuery {
        Courses.deleteWhere { Courses.id eq id } > 0
    }

    fun joinCourse(userId: String, code: String): Course? = dbQuery {
        val course = Courses.selectAll()
            .where { Courses.joinCode eq code }
            .firstOrNull()
            ?: return@dbQuery null

        EnrolledCourses.insert {
            it[EnrolledCourses.userId] = userId
            it[EnrolledCourses.courseId] = course[Courses.id]
        }

        course.toCourse()
    }

    fun getCreatorId(id: String): String? = dbQuery {
        Courses.selectAll()
            .where { Courses.id eq id }
            .firstOrNull()
            ?.get(Courses.creatorId)
    }
}
