package com.example.proyectofinal.models

import kotlinx.serialization.Serializable

@Serializable
data class UpdateCourseRequest(
    val title: String? = null,
    val description: String? = null,
    val joinCode: String? = null,
    val schoolYear: Int? = null,
    val topic: String? = null,
    val difficulty: String? = null,
    val durationMinutes: Int? = null,
    val xpReward: Int? = null
)

@Serializable
data class JoinCourseRequest(
    val userId: String,
    val code: String
)
