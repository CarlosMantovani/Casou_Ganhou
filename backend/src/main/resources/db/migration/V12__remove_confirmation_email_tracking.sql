ALTER TABLE transaction
    DROP COLUMN IF EXISTS confirmation_email_sent_at,
    DROP COLUMN IF EXISTS confirmation_email_failed_at,
    DROP COLUMN IF EXISTS confirmation_email_last_error;
