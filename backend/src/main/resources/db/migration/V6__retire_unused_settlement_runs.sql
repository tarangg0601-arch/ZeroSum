-- Settlement plans are read-only projections; only recorded payments create
-- financial history. Preserve the unused legacy table and any rows without
-- letting it appear as an active application feature.
ALTER TABLE settlement_runs RENAME TO settlement_runs_legacy;
