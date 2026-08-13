package com.example.proyectofinal.routes

import com.example.proyectofinal.models.CreateCourseRequest
import com.example.proyectofinal.models.JoinCourseRequest
import com.example.proyectofinal.models.UpdateCourseRequest
import com.example.proyectofinal.models.UserRole
import com.example.proyectofinal.plugins.currentRole
import com.example.proyectofinal.plugins.currentUserId
import com.example.proyectofinal.plugins.requireSelfOrAdmin
import com.example.proyectofinal.service.CourseReadResult
import com.example.proyectofinal.service.CourseService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.routing

fun Application.courseRoutes(service: CourseService) {
    routing {
        authenticate("auth-jwt") {
            get("/courses/official") {
                val schoolYearParam = call.request.queryParameters["schoolYear"]
                val schoolYear = schoolYearParam?.toIntOrNull()

                if (schoolYearParam != null && schoolYear == null) {
                    return@get call.respond(HttpStatusCode.BadRequest, "schoolYear must be numeric")
                }

                call.respond(service.getOfficialCourses(schoolYear))
            }

            get("/courses/{id}") {
                val courseId = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val userId = call.currentUserId()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, "Invalid or expired token")
                val role = call.currentRole()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, "Invalid or expired token")

                when (val result = service.getCourseByIdForUser(courseId, userId, role)) {
                    is CourseReadResult.Success -> call.respond(result.course)
                    CourseReadResult.Forbidden -> call.respond(HttpStatusCode.Forbidden, "Forbidden")
                    CourseReadResult.NotFound -> call.respond(HttpStatusCode.NotFound)
                }
            }

            get("/courses/creator/{creatorId}") {
                val creatorId = call.parameters["creatorId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                if (!call.requireSelfOrAdmin(creatorId)) return@get

                call.respond(service.getCoursesByCreator(creatorId))
            }

            get("/courses/{courseId}/students/progress") {
                val courseId = call.parameters["courseId"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest)
                val creatorId = service.getCreatorId(courseId)
                    ?: return@get call.respond(HttpStatusCode.NotFound)
                val userId = call.currentUserId()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, "Invalid or expired token")
                val role = call.currentRole()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, "Invalid or expired token")
                val canReadRoster = role == UserRole.ADMIN ||
                    (role == UserRole.TEACHER && userId == creatorId)
                if (!canReadRoster) {
                    return@get call.respond(HttpStatusCode.Forbidden, "Forbidden")
                }

                val progress = service.getStudentsProgress(courseId)
                    ?: return@get call.respond(HttpStatusCode.NotFound)
                call.respond(progress)
            }

            get("/courses/enrolled/{userId}") {
                val userId = call.parameters["userId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                if (!call.requireSelfOrAdmin(userId)) return@get

                call.respond(service.getEnrolledCourses(userId))
            }

            post("/courses") {
                val userId = call.currentUserId()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, "Invalid or expired token")
                val role = call.currentRole()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, "Invalid or expired token")

                if (role != UserRole.TEACHER && role != UserRole.ADMIN) {
                    return@post call.respond(HttpStatusCode.Forbidden, "Forbidden")
                }

                val request = call.receive<CreateCourseRequest>()
                call.respond(service.createCourse(request, creatorId = userId))
            }

            put("/courses/{id}") {
                val courseId = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
                val request = call.receive<UpdateCourseRequest>()

                val creatorId = service.getCreatorId(courseId) ?: return@put call.respond(HttpStatusCode.NotFound)

                if (!call.requireSelfOrAdmin(creatorId)) return@put

                val course = service.updateCourse(courseId, request) ?: return@put call.respond(HttpStatusCode.NotFound)
                call.respond(course)
            }

            delete("/courses/{id}") {
                val courseId = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)

                val creatorId = service.getCreatorId(courseId) ?: return@delete call.respond(HttpStatusCode.NotFound)

                if (!call.requireSelfOrAdmin(creatorId)) return@delete

                if (!service.deleteCourse(courseId)) {
                    call.respond(HttpStatusCode.NotFound)
                    return@delete
                }

                call.respond(HttpStatusCode.NoContent)
            }

            post("/courses/join") {
                val request = call.receive<JoinCourseRequest>()
                if (!call.requireSelfOrAdmin(request.userId)) return@post

                val course = service.joinCourse(request.userId, request.code)
                    ?: return@post call.respond(HttpStatusCode.NotFound, "Invalid join code")

                call.respond(course)
            }
        }
    }
}
