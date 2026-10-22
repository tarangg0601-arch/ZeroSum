CREATE TABLE group_join_attempts (
    bucket_hash CHAR(64) PRIMARY KEY,
    attempt_count INTEGER NOT NULL CHECK (attempt_count > 0),
    window_started_at TIMESTAMPTZ NOT NULL
);

ALTER TABLE recurring_rules
    ADD COLUMN anchor_day SMALLINT,
    ADD COLUMN anchor_month SMALLINT,
    ADD COLUMN anchor_month_end BOOLEAN;

-- Backfill from the current next date. Older dates already clamped by earlier
-- releases cannot reveal the originally requested day, so current next_run_date
-- is the explicit anchor for those existing rows.
UPDATE recurring_rules
SET anchor_day = EXTRACT(DAY FROM next_run_date)::SMALLINT,
    anchor_month = EXTRACT(MONTH FROM next_run_date)::SMALLINT,
    anchor_month_end = next_run_date = (date_trunc('month', next_run_date + INTERVAL '1 month') - INTERVAL '1 day')::date;

ALTER TABLE recurring_rules
    ALTER COLUMN anchor_day SET NOT NULL,
    ALTER COLUMN anchor_month SET NOT NULL,
    ALTER COLUMN anchor_month_end SET NOT NULL,
    ADD CONSTRAINT chk_recurring_anchor_day CHECK (anchor_day BETWEEN 1 AND 31),
    ADD CONSTRAINT chk_recurring_anchor_month CHECK (anchor_month BETWEEN 1 AND 12);
