ALTER TABLE transaction
    DROP CONSTRAINT IF EXISTS uq_transaction_recovery_code;

WITH phone_recovery_codes AS (
    SELECT DISTINCT ON (phone)
        phone,
        recovery_code
    FROM transaction
    ORDER BY phone, id
)
UPDATE transaction
SET recovery_code = phone_recovery_codes.recovery_code
FROM phone_recovery_codes
WHERE transaction.phone = phone_recovery_codes.phone;

CREATE INDEX idx_transaction_phone_recovery_code
    ON transaction (phone, recovery_code);
