CREATE TABLE auth_email_attempts (
    bucket_hash CHAR(64) PRIMARY KEY,
    attempt_count INTEGER NOT NULL CHECK (attempt_count > 0),
    window_started_at TIMESTAMPTZ NOT NULL
);
