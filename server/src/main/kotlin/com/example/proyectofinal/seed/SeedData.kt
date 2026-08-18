package com.example.proyectofinal.seed

import at.favre.lib.crypto.bcrypt.BCrypt
import com.example.proyectofinal.database.*
import com.example.proyectofinal.models.ChoiceOption
import com.example.proyectofinal.models.ExercisePayload
import com.example.proyectofinal.models.ExerciseType
import com.example.proyectofinal.models.InputValuePayload
import com.example.proyectofinal.models.MultiSelectPayload
import com.example.proyectofinal.models.MultipleChoicePayload
import com.example.proyectofinal.service.ExercisePayloadSupport
import org.jetbrains.exposed.v1.core.statements.InsertStatement
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.core.eq

object SeedData {
    const val DEMO_COURSE_ID = "course-demo-qa"

    private data class AdminSeedConfig(
        val id: String,
        val name: String,
        val email: String,
        val password: String
    )

    private fun configuredValue(
        envName: String,
        propertyName: String,
        defaultValue: String? = null
    ): String =
        System.getenv(envName)?.takeIf { it.isNotBlank() }
            ?: System.getProperty(propertyName)?.takeIf { it.isNotBlank() }
            ?: defaultValue
            ?: error("$envName or -D$propertyName must be configured")

    private fun adminConfig(): AdminSeedConfig = AdminSeedConfig(
        id = configuredValue("ADMIN_SEED_ID", "seed.admin.id", "admin-001"),
        name = configuredValue("ADMIN_SEED_NAME", "seed.admin.name", "Admin"),
        email = configuredValue("ADMIN_SEED_EMAIL", "seed.admin.email"),
        password = configuredValue("ADMIN_SEED_PASSWORD", "seed.admin.password")
    )

    private fun ensureAdminUser(admin: AdminSeedConfig): String {
        val existingByEmail = Users.selectAll().where { Users.email eq admin.email }.firstOrNull()
        if (existingByEmail != null) {
            return existingByEmail[Users.id]
        }

        val existingById = Users.selectAll().where { Users.id eq admin.id }.firstOrNull()
        if (existingById != null) {
            return existingById[Users.id]
        }

        Users.insert {
            it[Users.id] = admin.id
            it[Users.name] = admin.name
            it[Users.email] = admin.email
            it[Users.passwordHash] = BCrypt.withDefaults().hashToString(12, admin.password.toCharArray())
            it[Users.role] = "ADMIN"
        }
        return admin.id
    }

    private fun ensureCourse(id: String, block: Courses.(InsertStatement<Number>) -> Unit) {
        if (Courses.selectAll().where { Courses.id eq id }.empty()) {
            Courses.insert(block)
        }
    }

    private fun ensureLesson(id: String, block: Lessons.(InsertStatement<Number>) -> Unit) {
        if (Lessons.selectAll().where { Lessons.id eq id }.empty()) {
            Lessons.insert(block)
        }
    }

    private fun ensureLegacyExercise(
        id: String,
        lessonId: String,
        question: String,
        optionsCsv: String,
        correctAnswer: String,
        type: ExerciseType
    ) {
        if (Exercises.selectAll().where { Exercises.id eq id }.empty()) {
            Exercises.insert {
                it[Exercises.id] = id
                it[Exercises.lessonId] = lessonId
                it[Exercises.question] = question
                it[Exercises.options] = optionsCsv
                it[Exercises.correctAnswer] = correctAnswer
                it[Exercises.type] = type.name
                it[Exercises.payload] = ExercisePayloadSupport.legacyPayloadJson(
                    type = type,
                    optionsCsv = optionsCsv,
                    correctAnswer = correctAnswer
                )
            }
        }
    }

    private fun ensureTypedExercise(
        id: String,
        lessonId: String,
        question: String,
        type: ExerciseType,
        payload: ExercisePayload
    ) {
        if (Exercises.selectAll().where { Exercises.id eq id }.empty()) {
            Exercises.insert {
                it[Exercises.id] = id
                it[Exercises.lessonId] = lessonId
                it[Exercises.question] = question
                it[Exercises.options] = ExercisePayloadSupport.toLegacyOptionsCsv(payload)
                it[Exercises.correctAnswer] = ExercisePayloadSupport.toLegacyCorrectAnswer(payload)
                it[Exercises.type] = type.name
                it[Exercises.payload] = ExercisePayloadSupport.serializePayload(payload)
            }
        }
    }

    fun seedOfficialCourses() {
        val admin = adminConfig()

        transaction {
            println("Seeding official courses...")

            val adminId = ensureAdminUser(admin)

            seedBasicArithmetic(adminId)
            seedDemoCourse(adminId)

            println("Seed data created successfully!")
        }
    }

    private fun seedBasicArithmetic(adminId: String) {
        val basicArithmeticId = "course-basic-arithmetic"
        ensureCourse(basicArithmeticId) {
            it[Courses.id] = basicArithmeticId
            it[Courses.title] = "Basic Arithmetic"
            it[Courses.description] = "Learn the fundamentals of addition, subtraction, multiplication, and division."
            it[Courses.creatorId] = adminId
            it[Courses.isOfficial] = true
            it[Courses.schoolYear] = 3
            it[Courses.joinCode] = "ARITH101"
            it[Courses.topic] = "Fracciones"
            it[Courses.difficulty] = "Fácil"
            it[Courses.durationMinutes] = 20
            it[Courses.xpReward] = 50
        }

        val additionLessonId = "lesson-addition"
        ensureLesson(additionLessonId) {
            it[Lessons.id] = additionLessonId
            it[Lessons.courseId] = basicArithmeticId
            it[Lessons.title] = "Addition"
            it[Lessons.theoryContent] = """
                # Introduction to Addition

                Addition is one of the fundamental operations in mathematics. 
                When you add numbers, you combine them to get a total (sum).

                ## Example
                2 + 3 = 5

                Here, 2 and 3 are called "addends" and 5 is the "sum".

                ## Properties of Addition
                - **Commutative**: a + b = b + a
                - **Associative**: (a + b) + c = a + (b + c)
                - **Identity**: a + 0 = a
            """.trimIndent()
            it[Lessons.orderIndex] = 0
        }

        ensureLegacyExercise(
            id = "ex-add-1",
            lessonId = additionLessonId,
            question = "What is 5 + 3?",
            optionsCsv = "6,7,8,9",
            correctAnswer = "8",
            type = ExerciseType.MULTIPLE_CHOICE
        )

        ensureLegacyExercise(
            id = "ex-add-2",
            lessonId = additionLessonId,
            question = "What is 12 + 8?",
            optionsCsv = "18,19,20,21",
            correctAnswer = "20",
            type = ExerciseType.MULTIPLE_CHOICE
        )

        ensureLegacyExercise(
            id = "ex-add-3",
            lessonId = additionLessonId,
            question = "Is 4 + 4 = 8?",
            optionsCsv = "True,False",
            correctAnswer = "True",
            type = ExerciseType.TRUE_FALSE
        )

        val subtractionLessonId = "lesson-subtraction"
        ensureLesson(subtractionLessonId) {
            it[Lessons.id] = subtractionLessonId
            it[Lessons.courseId] = basicArithmeticId
            it[Lessons.title] = "Subtraction"
            it[Lessons.theoryContent] = """
                # Introduction to Subtraction

                Subtraction is the opposite of addition. When you subtract, 
                you find the difference between two numbers.

                ## Example
                7 - 3 = 4

                Here, 7 is the "minuend", 3 is the "subtrahend", and 4 is the "difference".

                ## Important Note
                In subtraction, order matters! a - b is not the same as b - a.
            """.trimIndent()
            it[Lessons.orderIndex] = 1
        }

        ensureLegacyExercise(
            id = "ex-sub-1",
            lessonId = subtractionLessonId,
            question = "What is 10 - 4?",
            optionsCsv = "4,5,6,7",
            correctAnswer = "6",
            type = ExerciseType.MULTIPLE_CHOICE
        )

        val multiplicationLessonId = "lesson-multiplication"
        ensureLesson(multiplicationLessonId) {
            it[Lessons.id] = multiplicationLessonId
            it[Lessons.courseId] = basicArithmeticId
            it[Lessons.title] = "Multiplication"
            it[Lessons.theoryContent] = """
                # Introduction to Multiplication

                Multiplication is repeated addition. Instead of adding the same number 
                multiple times, we multiply.

                ## Example
                3 x 4 = 12 (same as 4 + 4 + 4 = 12)

                ## Properties of Multiplication
                - **Commutative**: a × b = b × a
                - **Associative**: (a × b) × c = a × (b × c)
                - **Identity**: a × 1 = a
                - **Zero Property**: a × 0 = 0
            """.trimIndent()
            it[Lessons.orderIndex] = 2
        }

        ensureLegacyExercise(
            id = "ex-mul-1",
            lessonId = multiplicationLessonId,
            question = "What is 6 × 7?",
            optionsCsv = "36,40,42,48",
            correctAnswer = "42",
            type = ExerciseType.MULTIPLE_CHOICE
        )

        val divisionLessonId = "lesson-division"
        ensureLesson(divisionLessonId) {
            it[Lessons.id] = divisionLessonId
            it[Lessons.courseId] = basicArithmeticId
            it[Lessons.title] = "Division"
            it[Lessons.theoryContent] = """
                # Introduction to Division

                Division is the opposite of multiplication. It tells us how many times 
                one number fits into another.

                ## Example
                12 ÷ 3 = 4

                Here, 12 is the "dividend", 3 is the "divisor", and 4 is the "quotient".

                ## Important Note
                You cannot divide by zero!
            """.trimIndent()
            it[Lessons.orderIndex] = 3
        }

        ensureLegacyExercise(
            id = "ex-div-1",
            lessonId = divisionLessonId,
            question = "What is 20 ÷ 4?",
            optionsCsv = "4,5,6,7",
            correctAnswer = "5",
            type = ExerciseType.MULTIPLE_CHOICE
        )
    }

    private fun seedDemoCourse(adminId: String) {
        val demoCourseId = DEMO_COURSE_ID
        ensureCourse(demoCourseId) {
            it[Courses.id] = demoCourseId
            it[Courses.title] = "Demo QA Course"
            it[Courses.description] = "Demo/QA course exercising every lesson and exercise rendering path."
            it[Courses.creatorId] = adminId
            it[Courses.isOfficial] = true
            it[Courses.schoolYear] = 3
            it[Courses.joinCode] = "DEMOQA1"
            it[Courses.topic] = "Demo"
            it[Courses.difficulty] = "Fácil"
            it[Courses.durationMinutes] = 15
            it[Courses.xpReward] = 10
        }

        val theoryLessonId = "lesson-demo-theory"
        ensureLesson(theoryLessonId) {
            it[Lessons.id] = theoryLessonId
            it[Lessons.courseId] = demoCourseId
            it[Lessons.title] = "Demo Theory"
            it[Lessons.theoryContent] = """
                # Demo QA Theory Lesson

                This lesson exists to exercise the theory-rendering path for manual QA
                and demos. It has no exercises.
            """.trimIndent()
            it[Lessons.orderIndex] = 0
        }

        val exercisesLessonId = "lesson-demo-exercises"
        ensureLesson(exercisesLessonId) {
            it[Lessons.id] = exercisesLessonId
            it[Lessons.courseId] = demoCourseId
            it[Lessons.title] = "Demo Exercises"
            it[Lessons.theoryContent] = """
                # Demo QA Exercises Lesson

                This lesson carries one exercise per payload type: MultipleChoice,
                InputValue, and MultiSelect.
            """.trimIndent()
            it[Lessons.orderIndex] = 1
        }

        ensureTypedExercise(
            id = "ex-demo-mc-1",
            lessonId = exercisesLessonId,
            question = "Which planet is known as the Red Planet?",
            type = ExerciseType.MULTIPLE_CHOICE,
            payload = MultipleChoicePayload(
                options = listOf(
                    ChoiceOption(id = "mars", text = "Mars"),
                    ChoiceOption(id = "venus", text = "Venus"),
                    ChoiceOption(id = "jupiter", text = "Jupiter")
                ),
                correctOptionId = "mars"
            )
        )

        ensureTypedExercise(
            id = "ex-demo-input-1",
            lessonId = exercisesLessonId,
            question = "Type the capital of France.",
            type = ExerciseType.INPUT_VALUE,
            payload = InputValuePayload(
                placeholder = "Capital city",
                correctValue = "Paris"
            )
        )

        ensureTypedExercise(
            id = "ex-demo-multi-1",
            lessonId = exercisesLessonId,
            question = "Select every even number.",
            type = ExerciseType.MULTI_SELECT,
            payload = MultiSelectPayload(
                options = listOf(
                    ChoiceOption(id = "two", text = "2"),
                    ChoiceOption(id = "three", text = "3"),
                    ChoiceOption(id = "four", text = "4")
                ),
                correctOptionIds = listOf("two", "four")
            )
        )
    }
}
