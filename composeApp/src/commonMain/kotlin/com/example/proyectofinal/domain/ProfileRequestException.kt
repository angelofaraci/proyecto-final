package com.example.proyectofinal.domain

import com.example.proyectofinal.models.ProfileError

class ProfileRequestException(
    val error: ProfileError,
    val statusCode: Int
) : Exception(error.message)
