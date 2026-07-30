ALTER TABLE transaction
    ADD COLUMN confirmation_email_sent_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN confirmation_email_failed_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN confirmation_email_last_error VARCHAR(500);
