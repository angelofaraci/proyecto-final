package com.example.proyectofinal.models

import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProfileContractsTest {
    private val json = Json

    @Test
    fun identityRequestSerializesOnlyPermittedFields() {
        val encoded = json.encodeToJsonElement(
            UpdateIdentityRequest(name = "Ada Lovelace", email = "ada@example.com")
        ) as JsonObject

        assertEquals(setOf("name", "email"), encoded.keys)
        assertFalse("role" in encoded)
    }

    @Test
    fun sensitiveRequestsSerializeExplicitPasswordConfirmations() {
        val passwordChange = json.encodeToJsonElement(
            ChangePasswordRequest(currentPassword = "old-secret", newPassword = "new-secret")
        ) as JsonObject
        val deletion = json.encodeToJsonElement(
            DeleteAccountRequest(currentPassword = "current-secret")
        ) as JsonObject

        assertEquals(setOf("currentPassword", "newPassword"), passwordChange.keys)
        assertEquals("\"old-secret\"", passwordChange.getValue("currentPassword").toString())
        assertEquals(setOf("currentPassword"), deletion.keys)
        assertEquals("\"current-secret\"", deletion.getValue("currentPassword").toString())
    }

    @Test
    fun profilePreferencesRoundTripEverySupportedValue() {
        SupportedLanguage.entries.forEach { language ->
            AvatarId.entries.forEach { avatarId ->
                val preferences = ProfilePreferences(
                    notificationsEnabled = false,
                    soundsEnabled = false,
                    language = language,
                    avatarId = avatarId
                )

                assertEquals(
                    preferences,
                    json.decodeFromString<ProfilePreferences>(json.encodeToString(preferences))
                )
            }
        }
    }

    @Test
    fun nullablePreferenceCatalogValuesRoundTrip() {
        val preferences = ProfilePreferences(language = null, avatarId = null)

        assertEquals(
            preferences,
            json.decodeFromString<ProfilePreferences>(json.encodeToString(preferences))
        )
    }

    @Test
    fun unsupportedLanguageIsRejected() {
        val payload = json.parseToJsonElement(
            """{"notificationsEnabled":true,"soundsEnabled":true,"language":"fr"}"""
        )

        assertFailsWith<SerializationException> {
            json.decodeFromJsonElement<ProfilePreferences>(payload)
        }
    }

    @Test
    fun unsupportedAvatarIsRejected() {
        assertFailsWith<SerializationException> {
            json.decodeFromString<UpdateAvatarRequest>("""{"avatarId":"avatar_99"}""")
        }
    }

    @Test
    fun avatarAndTypedErrorContractsRoundTrip() {
        val avatarRequest = UpdateAvatarRequest(AvatarId.AVATAR_3)
        val error = ProfileError(ProfileErrorCode.COURSE_OWNERSHIP, "Transfer courses first")

        assertEquals(
            avatarRequest,
            json.decodeFromString<UpdateAvatarRequest>(json.encodeToString(avatarRequest))
        )
        assertEquals(error, json.decodeFromString<ProfileError>(json.encodeToString(error)))
        assertTrue(json.encodeToString(error).contains("COURSE_OWNERSHIP"))
    }
}
