CREATE TYPE payment_method AS ENUM (
    'MERCADO_PAGO',
    'CASH'
);

ALTER TABLE transaction
    ADD COLUMN name VARCHAR(255),
    ADD COLUMN phone VARCHAR(20),
    ADD COLUMN payment_method payment_method NOT NULL DEFAULT 'MERCADO_PAGO';

UPDATE transaction
SET name = email,
    phone = '0000000000'
WHERE name IS NULL
   OR phone IS NULL;

ALTER TABLE transaction
    ALTER COLUMN name SET NOT NULL,
    ALTER COLUMN phone SET NOT NULL,
    ALTER COLUMN email DROP NOT NULL;

ALTER TABLE lucky_number
    ALTER COLUMN email DROP NOT NULL;

ALTER TABLE raffle_draw
    ADD COLUMN winner_name VARCHAR(255);

UPDATE raffle_draw
SET winner_name = winner_email
WHERE winner_name IS NULL;

ALTER TABLE raffle_draw
    ALTER COLUMN winner_name SET NOT NULL,
    ALTER COLUMN winner_email DROP NOT NULL;

CREATE INDEX idx_transaction_name
    ON transaction (name);
