CREATE TABLE raffle_config (
    id BIGINT PRIMARY KEY,
    unit_price NUMERIC(19, 2) NOT NULL CHECK (unit_price > 0),
    scheduled_draw_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_raffle_config_single_row CHECK (id = 1)
);

INSERT INTO raffle_config (id, unit_price)
VALUES (1, ${raffle_unit_price});

ALTER TABLE transaction
    ADD COLUMN unit_price NUMERIC(19, 2);

UPDATE transaction
SET unit_price = ${raffle_unit_price}
WHERE unit_price IS NULL;

ALTER TABLE transaction
    ALTER COLUMN unit_price SET NOT NULL,
    ADD CONSTRAINT chk_transaction_unit_price_positive CHECK (unit_price > 0);

CREATE FUNCTION set_raffle_config_updated_at()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_raffle_config_updated_at
    BEFORE UPDATE ON raffle_config
    FOR EACH ROW
    EXECUTE FUNCTION set_raffle_config_updated_at();
