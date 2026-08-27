CREATE TABLE user_profile_preferences (
    user_id VARCHAR(50) NOT NULL,
    notifications_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    sounds_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    language VARCHAR(10),
    avatar_id VARCHAR(50),
    CONSTRAINT pk_user_profile_preferences PRIMARY KEY (user_id),
    CONSTRAINT fk_user_profile_preferences_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_user_profile_preferences_language
        CHECK (language IS NULL OR language IN ('es', 'en')),
    CONSTRAINT chk_user_profile_preferences_avatar
        CHECK (avatar_id IS NULL OR avatar_id IN ('avatar_1', 'avatar_2', 'avatar_3', 'avatar_4'))
);

INSERT INTO user_profile_preferences (user_id)
SELECT users.id
FROM users
WHERE NOT EXISTS (
    SELECT 1
    FROM user_profile_preferences
    WHERE user_profile_preferences.user_id = users.id
);
