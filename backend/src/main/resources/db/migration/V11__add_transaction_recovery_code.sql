ALTER TABLE transaction
    ADD COLUMN recovery_code VARCHAR(4);

WITH numbered_transactions AS (
    SELECT id, lpad(((row_number() OVER (ORDER BY id) - 1) % 10000)::text, 4, '0') AS generated_code
    FROM transaction
)
UPDATE transaction
SET recovery_code = numbered_transactions.generated_code
FROM numbered_transactions
WHERE transaction.id = numbered_transactions.id;

ALTER TABLE transaction
    ALTER COLUMN recovery_code SET NOT NULL,
    ADD CONSTRAINT chk_transaction_recovery_code_format CHECK (recovery_code ~ '^[0-9]{4}$'),
    ADD CONSTRAINT uq_transaction_recovery_code UNIQUE (recovery_code);
