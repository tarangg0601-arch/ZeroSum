ALTER TABLE recurring_rules
    ADD COLUMN paid_by BIGINT,
    ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE;

UPDATE recurring_rules rule
SET paid_by = groups.created_by
FROM groups
WHERE rule.group_id = groups.id;

ALTER TABLE recurring_rules
    ADD CONSTRAINT fk_recurring_rules_paid_by
        FOREIGN KEY (paid_by) REFERENCES users(id),
    ALTER COLUMN paid_by SET NOT NULL;

ALTER TABLE expenses
    ADD COLUMN recurring_rule_id BIGINT,
    ADD CONSTRAINT fk_expenses_recurring_rule
        FOREIGN KEY (recurring_rule_id) REFERENCES recurring_rules(id) ON DELETE SET NULL,
    ADD CONSTRAINT uq_expenses_recurring_rule_date
        UNIQUE (recurring_rule_id, expense_date);

CREATE INDEX idx_recurring_rules_due
    ON recurring_rules(is_active, next_run_date);
