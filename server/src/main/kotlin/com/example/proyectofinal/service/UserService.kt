package com.example.proyectofinal.service

import at.favre.lib.crypto.bcrypt.BCrypt
import com.example.proyectofinal.database.CompletedExercises
import com.example.proyectofinal.database.CompletedLessons
import com.example.proyectofinal.database.Courses
import com.example.proyectofinal.database.EnrolledCourses
import com.example.proyectofinal.database.Exercises
import com.example.proyectofinal.database.Lessons
import com.example.proyectofinal.database.UserProgress as UserProgressTable
import com.example.proyectofinal.database.UserProfilePreferences
import com.example.proyectofinal.database.Users
import com.example.proyectofinal.database.dbQuery
import com.example.proyectofinal.models.AdminUserResponse
import com.example.proyectofinal.models.CompleteLessonRequest
import com.example.proyectofinal.models.ExerciseAttemptRequest
import com.example.proyectofinal.models.ExerciseAttemptResponse
import com.example.proyectofinal.models.ExerciseCompletionResponse
import com.example.proyectofinal.models.PageResponse
import com.example.proyectofinal.models.ChangePasswordRequest
import com.example.proyectofinal.models.AvatarId
import com.example.proyectofinal.models.ProfilePreferences
import com.example.proyectofinal.models.SupportedLanguage
import com.example.proyectofinal.models.UpdateAvatarRequest
import com.example.proyectofinal.models.UpdateIdentityRequest
import com.example.proyectofinal.models.UpdateUserRequest
import com.example.proyectofinal.models.User
import com.example.proyectofinal.models.UserProgress
import com.example.proyectofinal.models.UserRole
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update

sealed interface ExerciseAttemptResult {
    data class Success(val response: ExerciseAttemptResponse) : ExerciseAttemptResult
    data class InvalidRequest(val message: String) : ExerciseAttemptResult
    object Forbidden : ExerciseAttemptResult
    object NotFound : ExerciseAttemptResult
}

sealed interface IdentityUpdateResult {
    data class Success(val user: User) : IdentityUpdateResult
    data object InvalidValue : IdentityUpdateResult
    data object EmailConflict : IdentityUpdateResult
    data object NotFound : IdentityUpdateResult
}

sealed interface PasswordChangeResult {
    data object Success : PasswordChangeResult
    data object InvalidValue : PasswordChangeResult
    data object InvalidPassword : PasswordChangeResult
    data object NotFound : PasswordChangeResult
}

class UserService {
    fun getUserById(id: String): User? = dbQuery {
        Users.selectAll()
            .where { Users.id eq id }
            .firstOrNull()
            ?.toUser()
    }

    fun listUsers(query: String? = null, page: Int = 0, size: Int = 20): PageResponse<AdminUserResponse> = dbQuery {
        val searchPattern = query?.let { "%$it%" }

        val totalElements = if (searchPattern != null) {
            Users.selectAll()
                .where { (Users.name like searchPattern) or (Users.email like searchPattern) }
                .count()
        } else {
            Users.selectAll().count()
        }

        val items = (if (searchPattern != null) {
            Users.selectAll()
                .where { (Users.name like searchPattern) or (Users.email like searchPattern) }
        } else {
            Users.selectAll()
        }).orderBy(Users.name)
            .limit(size)
            .offset((page * size).toLong())
            .map { row ->
                AdminUserResponse(
                    id = row[Users.id],
                    name = row[Users.name],
                    email = row[Users.email],
                    role = UserRole.parse(row[Users.role]) ?: UserRole.STUDENT
                )
            }

        PageResponse(
            items = items,
            page = page,
            size = size,
            totalElements = totalElements,
            totalPages = if (size > 0) ((totalElements + size - 1) / size).toInt() else 0
        )
    }

    fun updateUser(id: String, request: UpdateUserRequest): User? {
        val updated = dbQuery {
            Users.update({ Users.id eq id }) { row ->
                request.name?.let { row[Users.name] = it }
                request.email?.let { row[Users.email] = it }
                request.role?.let { row[Users.role] = it.name }
            }
        }

        if (updated == 0) {
            return null
        }

        return getUserById(id)
    }

    fun updateIdentity(userId: String, request: UpdateIdentityRequest): IdentityUpdateResult {
        val name = request.name.trim()
        val email = request.email.trim()
        if (name.isEmpty() || name.length > 100 || !EMAIL_PATTERN.matches(email)) {
            return IdentityUpdateResult.InvalidValue
        }
        return try {
            dbQuery {
                val updated = Users.update({ Users.id eq userId }) {
                    it[Users.name] = name
                    it[Users.email] = email
                }
                if (updated == 0) return@dbQuery IdentityUpdateResult.NotFound
                IdentityUpdateResult.Success(
                    Users.selectAll().where { Users.id eq userId }.single().toUser()
                )
            }
        } catch (exception: ExposedSQLException) {
            if (exception.sqlState == UNIQUE_VIOLATION_SQL_STATE) IdentityUpdateResult.EmailConflict
            else throw exception
        }
    }

    fun changePassword(userId: String, request: ChangePasswordRequest): PasswordChangeResult {
        val currentBytes = request.currentPassword.encodeToByteArray().size
        val newBytes = request.newPassword.encodeToByteArray().size
        if (currentBytes !in 1..BCRYPT_MAX_BYTES || request.newPassword.length < 8 || newBytes > BCRYPT_MAX_BYTES) {
            return PasswordChangeResult.InvalidValue
        }
        val currentHash = dbQuery {
            Users.selectAll().where { Users.id eq userId }.firstOrNull()?.get(Users.passwordHash)
        } ?: return PasswordChangeResult.NotFound
        if (!BCrypt.verifyer().verify(request.currentPassword.toCharArray(), currentHash).verified) {
            return PasswordChangeResult.InvalidPassword
        }
        val newHash = BCrypt.withDefaults().hashToString(12, request.newPassword.toCharArray())
        val updated = dbQuery {
            Users.update({ (Users.id eq userId) and (Users.passwordHash eq currentHash) }) {
                it[Users.passwordHash] = newHash
            }
        }
        return if (updated == 1) PasswordChangeResult.Success else PasswordChangeResult.InvalidPassword
    }

    fun getProfilePreferences(userId: String): ProfilePreferences? = dbQuery {
        readProfilePreferences(userId)
    }

    fun updateProfilePreferences(userId: String, preferences: ProfilePreferences): ProfilePreferences? = dbQuery {
        val updated = UserProfilePreferences.update({ UserProfilePreferences.userId eq userId }) {
            it[UserProfilePreferences.notificationsEnabled] = preferences.notificationsEnabled
            it[UserProfilePreferences.soundsEnabled] = preferences.soundsEnabled
            it[UserProfilePreferences.language] = preferences.language?.toStorageValue()
            it[UserProfilePreferences.avatarId] = preferences.avatarId?.toStorageValue()
        }
        if (updated == 0) null else readProfilePreferences(userId)
    }

    fun updateAvatar(userId: String, request: UpdateAvatarRequest): ProfilePreferences? = dbQuery {
        val updated = UserProfilePreferences.update({ UserProfilePreferences.userId eq userId }) {
            it[UserProfilePreferences.avatarId] = request.avatarId.toStorageValue()
        }
        if (updated == 0) null else readProfilePreferences(userId)
    }

    private fun readProfilePreferences(userId: String): ProfilePreferences? =
        UserProfilePreferences.selectAll()
            .where { UserProfilePreferences.userId eq userId }
            .firstOrNull()
            ?.let { row ->
                ProfilePreferences(
                    notificationsEnabled = row[UserProfilePreferences.notificationsEnabled],
                    soundsEnabled = row[UserProfilePreferences.soundsEnabled],
                    language = row[UserProfilePreferences.language]?.let(::storedLanguage),
                    avatarId = row[UserProfilePreferences.avatarId]?.let(::storedAvatar)
                )
            }

    fun getUserProgress(userId: String): UserProgress = dbQuery {
        readUserProgress(userId)
    }

    fun attemptExercise(
        userId: String,
        role: UserRole,
        request: ExerciseAttemptRequest
    ): ExerciseAttemptResult = dbQuery {
        if (role != UserRole.STUDENT) {
            return@dbQuery ExerciseAttemptResult.Forbidden
        }

        val exerciseRow = (Exercises innerJoin Lessons)
            .selectAll()
            .where { Exercises.id eq request.exerciseId }
            .firstOrNull()
            ?: return@dbQuery ExerciseAttemptResult.NotFound

        val lessonAccess = resolveLessonContentAccess(
            courseId = exerciseRow[Lessons.courseId],
            standaloneCreatorId = exerciseRow[Lessons.creatorId]
        ) ?: return@dbQuery ExerciseAttemptResult.NotFound

        if (!canReadLessonContent(lessonAccess, userId, role)) {
            return@dbQuery ExerciseAttemptResult.Forbidden
        }

        val exercise = ExercisePayloadSupport.toExercise(
            id = exerciseRow[Exercises.id],
            lessonId = exerciseRow[Exercises.lessonId],
            title = exerciseRow[Exercises.question],
            persistedType = exerciseRow[Exercises.type],
            persistedPayload = exerciseRow[Exercises.payload],
            legacyOptions = exerciseRow[Exercises.options],
            legacyCorrectAnswer = exerciseRow[Exercises.correctAnswer],
            hideAnswers = false
        )

        val evaluation = try {
            ExercisePayloadSupport.evaluateAttempt(exercise, request.submission)
        } catch (exception: IllegalArgumentException) {
            return@dbQuery ExerciseAttemptResult.InvalidRequest(exception.message ?: "Invalid exercise submission")
        }

        if (!evaluation.isCorrect) {
            return@dbQuery ExerciseAttemptResult.Success(
                ExerciseAttemptResponse(
                    exerciseId = exercise.id,
                    lessonId = exercise.lessonId,
                    isCorrect = false,
                    message = evaluation.message,
                    progress = readUserProgress(userId)
                )
            )
        }

        val completion = recordExerciseCompletion(
            userId = userId,
            exerciseId = exercise.id,
            lessonId = exercise.lessonId,
            score = request.score
        )

        ExerciseAttemptResult.Success(
            ExerciseAttemptResponse(
                exerciseId = completion.exerciseId,
                lessonId = completion.lessonId,
                isCorrect = true,
                lessonCompleted = completion.lessonCompleted,
                progress = completion.progress
            )
        )
    }

    private fun recordExerciseCompletion(
        userId: String,
        exerciseId: String,
        lessonId: String,
        score: Int
    ): ExerciseCompletionResponse {
        val existingCompletion = CompletedExercises.selectAll()
            .where {
                (CompletedExercises.userId eq userId) and
                    (CompletedExercises.exerciseId eq exerciseId)
            }
            .firstOrNull()
        if (existingCompletion == null) {
            CompletedExercises.insert {
                it[CompletedExercises.userId] = userId
                it[CompletedExercises.exerciseId] = exerciseId
                it[CompletedExercises.score] = score
            }
            val existingProgress = UserProgressTable.selectAll()
                .where { UserProgressTable.userId eq userId }
                .firstOrNull()
            if (existingProgress == null) {
                UserProgressTable.insert {
                    it[UserProgressTable.userId] = userId
                    it[UserProgressTable.totalScore] = score
                }
            } else {
                val currentScore = existingProgress[UserProgressTable.totalScore]
                UserProgressTable.update({ UserProgressTable.userId eq userId }) { row ->
                    row[UserProgressTable.totalScore] = currentScore + score
                }
            }
        }
        val totalExercisesInLesson = Exercises.selectAll()
            .where { Exercises.lessonId eq lessonId }
            .count()
        val completedExercisesInLesson = (CompletedExercises innerJoin Exercises)
            .selectAll()
            .where {
                (CompletedExercises.userId eq userId) and
                    (Exercises.lessonId eq lessonId)
            }
            .count()
        val lessonCompleted = totalExercisesInLesson > 0 && completedExercisesInLesson == totalExercisesInLesson
        if (lessonCompleted) {
            val existingLessonCompletion = CompletedLessons.selectAll()
                .where {
                    (CompletedLessons.userId eq userId) and
                        (CompletedLessons.lessonId eq lessonId)
                }
                .firstOrNull()
            if (existingLessonCompletion == null) {
                CompletedLessons.insert {
                    it[CompletedLessons.userId] = userId
                    it[CompletedLessons.lessonId] = lessonId
                }
            }
        }
        return ExerciseCompletionResponse(
            exerciseId = exerciseId,
            lessonId = lessonId,
            lessonCompleted = lessonCompleted,
            progress = readUserProgress(userId)
        )
    }

    private fun readUserProgress(userId: String): UserProgress {
        val progressRow = UserProgressTable.selectAll()
            .where { UserProgressTable.userId eq userId }
            .firstOrNull()
        val completedLessons = CompletedLessons.selectAll()
            .where { CompletedLessons.userId eq userId }
            .map { it[CompletedLessons.lessonId] }
            .toSet()
        val completedExercises = CompletedExercises.selectAll()
            .where { CompletedExercises.userId eq userId }
            .map { it[CompletedExercises.exerciseId] }
            .toSet()
        val enrolledCourses = EnrolledCourses.selectAll()
            .where { EnrolledCourses.userId eq userId }
            .map { it[EnrolledCourses.courseId] }
            .toSet()
        return UserProgress(
            userId = userId,
            totalScore = progressRow?.get(UserProgressTable.totalScore) ?: 0,
            completedLessonIds = completedLessons,
            completedExerciseIds = completedExercises,
            enrolledCourseIds = enrolledCourses
        )
    }

    fun updateProgress(request: CompleteLessonRequest) {
        dbQuery {
            val existingProgress = UserProgressTable.selectAll()
                .where { UserProgressTable.userId eq request.userId }
                .firstOrNull()

            if (existingProgress == null) {
                UserProgressTable.insert {
                    it[UserProgressTable.userId] = request.userId
                    it[UserProgressTable.totalScore] = request.score
                }
            } else {
                val currentScore = existingProgress[UserProgressTable.totalScore]
                UserProgressTable.update({ UserProgressTable.userId eq request.userId }) { row ->
                    row[UserProgressTable.totalScore] = currentScore + request.score
                }
            }
            CompletedLessons.insert {
                it[CompletedLessons.userId] = request.userId
                it[CompletedLessons.lessonId] = request.lessonId
            }
        }
    }
    private fun resolveLessonContentAccess(
        courseId: String?,
        standaloneCreatorId: String?
    ): LessonContentAccess? =
        if (courseId != null) {
            Courses.select(Courses.creatorId, Courses.isOfficial)
                .where { Courses.id eq courseId }
                .firstOrNull()
                ?.let {
                    LessonContentAccess.CourseLinked(
                        CourseContentAccess(
                            courseId = courseId,
                            creatorId = it[Courses.creatorId],
                            isOfficial = it[Courses.isOfficial]
                        )
                    )
                }
        } else {
            standaloneCreatorId?.let { LessonContentAccess.Standalone(it) }
        }

    private companion object {
        const val BCRYPT_MAX_BYTES = 72
        const val UNIQUE_VIOLATION_SQL_STATE = "23505"
        val EMAIL_PATTERN = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
    }
}

private fun SupportedLanguage.toStorageValue() = when (this) {
    SupportedLanguage.SPANISH -> "es"
    SupportedLanguage.ENGLISH -> "en"
}

private fun AvatarId.toStorageValue() = "avatar_${ordinal + 1}"

private fun storedLanguage(value: String) = when (value) {
    "es" -> SupportedLanguage.SPANISH
    "en" -> SupportedLanguage.ENGLISH
    else -> error("Unsupported stored language: $value")
}

private fun storedAvatar(value: String) =
    AvatarId.entries.getOrNull(value.removePrefix("avatar_").toIntOrNull()?.minus(1) ?: -1)
        ?: error("Unsupported stored avatar: $value")
