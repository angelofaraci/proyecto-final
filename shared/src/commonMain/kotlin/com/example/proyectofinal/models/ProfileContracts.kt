package com.example.proyectofinal.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UpdateIdentityRequest(
    val name: String,
    val email: String
)

@Serializable
data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String
)

@Serializable
data class DeleteAccountRequest(
    val currentPassword: String
)

@Serializable
data class UpdateAvatarRequest(
    val avatarId: AvatarId
)

@Serializable
data class ProfilePreferences(
    val notificationsEnabled: Boolean = true,
    val soundsEnabled: Boolean = true,
    val language: SupportedLanguage? = null,
    val avatarId: AvatarId? = null
)

@Serializable
enum class SupportedLanguage {
    @SerialName("es")
    SPANISH,

    @SerialName("en")
    ENGLISH
}

@Serializable
enum class AvatarId {
    @SerialName("avatar_1")
    AVATAR_1,

    @SerialName("avatar_2")
    AVATAR_2,

    @SerialName("avatar_3")
    AVATAR_3,

    @SerialName("avatar_4")
    AVATAR_4
}

@Serializable
data class ProfileError(
    val code: ProfileErrorCode,
    val message: String
)

@Serializable
enum class ProfileErrorCode {
    EMAIL_CONFLICT,
    INVALID_PASSWORD,
    COURSE_OWNERSHIP,
    INVALID_VALUE
}
