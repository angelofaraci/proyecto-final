package com.example.proyectofinal.domain

import com.example.proyectofinal.models.ExerciseAttemptResponse
import com.example.proyectofinal.models.ExerciseSubmission
import com.example.proyectofinal.models.ChangePasswordRequest
import com.example.proyectofinal.models.ProfilePreferences
import com.example.proyectofinal.models.UpdateAvatarRequest
import com.example.proyectofinal.models.UpdateIdentityRequest
import com.example.proyectofinal.models.User
import com.example.proyectofinal.models.UserProgress
import com.example.proyectofinal.models.UserRole

interface UserRepository {
    /**
     * Returns the currently logged-in user.
     */
    suspend fun getCurrentUser(): User?

    /**
     * Checks if the user has permission to perform admin/teacher tasks.
     */
    suspend fun getUserRole(userId: String): UserRole

    /**
     * Updates user profile information.
     */
    suspend fun updateUser(user: User)

    suspend fun updateIdentity(request: UpdateIdentityRequest): User

    suspend fun changePassword(request: ChangePasswordRequest)

    suspend fun getProfilePreferences(): ProfilePreferences

    suspend fun updateProfilePreferences(preferences: ProfilePreferences): ProfilePreferences

    suspend fun updateAvatar(request: UpdateAvatarRequest): ProfilePreferences

    /**
     * Fetches and syncs the user's cumulative progress.
     */
    suspend fun getUserProgress(userId: String): UserProgress

    /**
     * Submits a typed attempt for the authenticated learner.
     */
    suspend fun attemptExercise(
        exerciseId: String,
        submission: ExerciseSubmission,
        score: Int = 100
    ): ExerciseAttemptResponse
}
