package com.example.proyectofinal.database

import org.jetbrains.exposed.v1.core.*

object Users : Table("users") {
    val id = varchar("id", 50)
    val name = varchar("name", 100)
    val email = varchar("email", 100).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val role = varchar("role", 20).default("STUDENT")

    override val primaryKey = PrimaryKey(id)
}

object UserProfilePreferences : Table("user_profile_preferences") {
    val userId = reference("user_id", Users.id, onDelete = ReferenceOption.CASCADE)
    val notificationsEnabled = bool("notifications_enabled").default(true)
    val soundsEnabled = bool("sounds_enabled").default(true)
    val language = varchar("language", 10).nullable()
    val avatarId = varchar("avatar_id", 50).nullable()

    override val primaryKey = PrimaryKey(userId)
}

object Courses : Table("courses") {
    val id = varchar("id", 50)
    val title = varchar("title", 200)
    val description = varchar("description", 1000)
    val creatorId = varchar("creator_id", 50)
    val isOfficial = bool("is_official").default(false)
    val schoolYear = integer("school_year").default(0)
    val joinCode = varchar("join_code", 20).nullable()
    val topic = varchar("topic", 100).nullable()
    val difficulty = varchar("difficulty", 50).nullable()
    val durationMinutes = integer("duration_minutes").nullable()
    val xpReward = integer("xp_reward").nullable()

    override val primaryKey = PrimaryKey(id)
}

object Lessons : Table("lessons") {
    val id = varchar("id", 50)
    val courseId = reference("course_id", Courses.id, onDelete = ReferenceOption.CASCADE).nullable()
    val creatorId = varchar("creator_id", 50).nullable()
    val title = varchar("title", 200)
    val theoryContent = text("theory_content")
    val orderIndex = integer("order_index").default(0)

    override val primaryKey = PrimaryKey(id)
}

object Exercises : Table("exercises") {
    val id = varchar("id", 50)
    val lessonId = reference("lesson_id", Lessons.id, onDelete = ReferenceOption.CASCADE)
    val question = varchar("question", 500)
    val options = varchar("options", 500)
    val correctAnswer = varchar("correct_answer", 255)
    val type = varchar("type", 50).default("MULTIPLE_CHOICE")
    val payload = text("payload")

    override val primaryKey = PrimaryKey(id)
}

object UserProgress : Table("user_progress") {
    val userId = reference("user_id", Users.id, onDelete = ReferenceOption.CASCADE)
    val totalScore = integer("total_score").default(0)

    override val primaryKey = PrimaryKey(userId)
}

object CompletedLessons : Table("completed_lessons") {
    val userId = reference("user_id", Users.id, onDelete = ReferenceOption.CASCADE)
    val lessonId = reference("lesson_id", Lessons.id, onDelete = ReferenceOption.CASCADE)

    override val primaryKey = PrimaryKey(userId, lessonId)
}

object CompletedExercises : Table("completed_exercises") {
    val userId = reference("user_id", Users.id, onDelete = ReferenceOption.CASCADE)
    val exerciseId = reference("exercise_id", Exercises.id, onDelete = ReferenceOption.CASCADE)
    val score = integer("score").default(0)

    override val primaryKey = PrimaryKey(userId, exerciseId)
}

object EnrolledCourses : Table("enrolled_courses") {
    val userId = reference("user_id", Users.id, onDelete = ReferenceOption.CASCADE)
    val courseId = reference("course_id", Courses.id, onDelete = ReferenceOption.CASCADE)

    override val primaryKey = PrimaryKey(userId, courseId)
}
