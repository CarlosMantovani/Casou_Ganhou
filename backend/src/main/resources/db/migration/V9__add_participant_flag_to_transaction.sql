ALTER TABLE transaction
    ADD COLUMN participant_flag_code VARCHAR(50),
    ADD COLUMN participant_flag_name VARCHAR(100),
    ADD COLUMN participant_flag_emoji VARCHAR(20);

UPDATE transaction
SET
    participant_flag_code = CASE mod(abs(hashtext(phone)), 12)
        WHEN 0 THEN 'BRAZIL'
        WHEN 1 THEN 'ARGENTINA'
        WHEN 2 THEN 'JAPAN'
        WHEN 3 THEN 'ITALY'
        WHEN 4 THEN 'CANADA'
        WHEN 5 THEN 'PORTUGAL'
        WHEN 6 THEN 'SPAIN'
        WHEN 7 THEN 'FRANCE'
        WHEN 8 THEN 'GERMANY'
        WHEN 9 THEN 'MEXICO'
        WHEN 10 THEN 'URUGUAY'
        ELSE 'CHILE'
    END,
    participant_flag_name = CASE mod(abs(hashtext(phone)), 12)
        WHEN 0 THEN 'Brasil'
        WHEN 1 THEN 'Argentina'
        WHEN 2 THEN 'Japao'
        WHEN 3 THEN 'Italia'
        WHEN 4 THEN 'Canada'
        WHEN 5 THEN 'Portugal'
        WHEN 6 THEN 'Espanha'
        WHEN 7 THEN 'Franca'
        WHEN 8 THEN 'Alemanha'
        WHEN 9 THEN 'Mexico'
        WHEN 10 THEN 'Uruguai'
        ELSE 'Chile'
    END,
    participant_flag_emoji = CASE mod(abs(hashtext(phone)), 12)
        WHEN 0 THEN 'BR'
        WHEN 1 THEN 'AR'
        WHEN 2 THEN 'JP'
        WHEN 3 THEN 'IT'
        WHEN 4 THEN 'CA'
        WHEN 5 THEN 'PT'
        WHEN 6 THEN 'ES'
        WHEN 7 THEN 'FR'
        WHEN 8 THEN 'DE'
        WHEN 9 THEN 'MX'
        WHEN 10 THEN 'UY'
        ELSE 'CL'
    END
WHERE participant_flag_code IS NULL;

ALTER TABLE transaction
    ALTER COLUMN participant_flag_code SET NOT NULL,
    ALTER COLUMN participant_flag_name SET NOT NULL,
    ALTER COLUMN participant_flag_emoji SET NOT NULL;

CREATE INDEX idx_transaction_participant_flag_code
    ON transaction (participant_flag_code);

CREATE INDEX idx_transaction_phone_participant_flag
    ON transaction (phone, participant_flag_code);
