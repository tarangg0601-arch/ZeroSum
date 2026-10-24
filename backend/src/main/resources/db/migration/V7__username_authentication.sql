-- New accounts use normalized usernames. Existing accounts remain untouched with NULL usernames
-- until their identity can be verified through a separate, safe migration process.
ALTER TABLE users ADD COLUMN username VARCHAR(32);

ALTER TABLE users ADD CONSTRAINT users_username_format_check
    CHECK (username IS NULL OR username ~ '^[a-z0-9_]{3,32}$');

CREATE UNIQUE INDEX users_username_case_insensitive_unique_idx
    ON users (LOWER(username))
    WHERE username IS NOT NULL;
