package com.example.proyectofinal

import com.example.proyectofinal.database.DatabaseFactory
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.MigrationVersion
import java.sql.DriverManager
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ProfilePreferencesMigrationTest {
    @Test
    fun `V7 backfills isolated defaults cascades deletion and survives repeat startup`() {
        val url = testDbUrl()
        migrateToV6(url)
        execute(
            url,
            "INSERT INTO users (id, name, email, password_hash, role) VALUES " +
                "('user-a', 'User A', 'a@example.com', 'hash', 'STUDENT'), " +
                "('user-b', 'User B', 'b@example.com', 'hash', 'TEACHER')",
            "INSERT INTO user_progress (user_id, total_score) VALUES ('user-a', 120)"
        )

        migrateAll(url)
        migrateAll(url)

        DriverManager.getConnection(url, "sa", "").use { connection ->
            connection.prepareStatement(
                """
                SELECT notifications_enabled, sounds_enabled, language, avatar_id
                FROM user_profile_preferences
                WHERE user_id = ?
                """.trimIndent()
            ).use { query ->
                query.setString(1, "user-a")
                query.executeQuery().use { row ->
                    assertTrue(row.next())
                    assertTrue(row.getBoolean("notifications_enabled"))
                    assertTrue(row.getBoolean("sounds_enabled"))
                    assertEquals(null, row.getString("language"))
                    assertEquals(null, row.getString("avatar_id"))
                }
            }

            connection.createStatement().use { statement ->
                statement.executeUpdate(
                    "UPDATE user_profile_preferences SET language = 'en', avatar_id = 'avatar_2' " +
                        "WHERE user_id = 'user-a'"
                )
            }
            assertEquals("en", scalarString(connection, "user-a", "language"))
            assertEquals(null, scalarString(connection, "user-b", "language"))

            connection.createStatement().use { it.executeUpdate("DELETE FROM users WHERE id = 'user-a'") }
            assertEquals(0, scalarInt(connection, "SELECT COUNT(*) FROM user_profile_preferences WHERE user_id = 'user-a'"))
            assertEquals(1, scalarInt(connection, "SELECT COUNT(*) FROM user_profile_preferences WHERE user_id = 'user-b'"))
        }
    }

    @Test
    fun `V7 fresh schema contains constrained preference columns`() {
        val url = testDbUrl()

        migrateAll(url)

        DriverManager.getConnection(url, "sa", "").use { connection ->
            val columns = connection.metaData.getColumns(null, null, "user_profile_preferences", null)
                .use { result -> buildSet { while (result.next()) add(result.getString("COLUMN_NAME")) } }
            assertEquals(
                setOf("user_id", "notifications_enabled", "sounds_enabled", "language", "avatar_id"),
                columns
            )
        }
    }

    @Test
    fun `V7 failure prevents database startup`() {
        val url = testDbUrl()
        migrateToV6(url)
        execute(url, "CREATE TABLE user_profile_preferences (user_id VARCHAR(50) PRIMARY KEY)")

        val error = assertFailsWith<Exception> { migrateAll(url) }

        assertTrue(
            generateSequence(error as Throwable?) { it.cause }
                .mapNotNull(Throwable::message)
                .any { it.contains("V7__profile_preferences.sql") || it.contains("already exists", ignoreCase = true) }
        )
    }

    private fun migrateToV6(url: String) {
        Flyway.configure()
            .dataSource(url, "sa", "")
            .locations("classpath:db/migration")
            .target(MigrationVersion.fromVersion("6"))
            .load()
            .migrate()
    }

    private fun migrateAll(url: String) {
        DatabaseFactory.init(url, "org.h2.Driver", "sa", "")
    }

    private fun execute(url: String, vararg statements: String) {
        DriverManager.getConnection(url, "sa", "").use { connection ->
            connection.createStatement().use { statement -> statements.forEach(statement::execute) }
        }
    }

    private fun scalarString(connection: java.sql.Connection, userId: String, column: String): String? =
        connection.prepareStatement("SELECT $column FROM user_profile_preferences WHERE user_id = ?").use { query ->
            query.setString(1, userId)
            query.executeQuery().use { row ->
                assertTrue(row.next())
                row.getString(column)
            }
        }

    private fun scalarInt(connection: java.sql.Connection, sql: String): Int =
        connection.createStatement().use { statement ->
            statement.executeQuery(sql).use { row ->
                assertTrue(row.next())
                row.getInt(1)
            }
        }

    private fun testDbUrl() =
        "jdbc:h2:mem:${UUID.randomUUID()};MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE"
}
