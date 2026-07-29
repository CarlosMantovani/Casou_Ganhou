-- Set these Flyway placeholders from environment variables before running migrations.
-- Flyway CLI: FLYWAY_PLACEHOLDERS_ADMIN_USERNAME and
--             FLYWAY_PLACEHOLDERS_ADMIN_PASSWORD_HASH
-- Spring Boot: SPRING_FLYWAY_PLACEHOLDERS_ADMIN_USERNAME and
--              SPRING_FLYWAY_PLACEHOLDERS_ADMIN_PASSWORD_HASH
-- ADMIN_PASSWORD_HASH must contain a BCrypt hash; no plaintext password is stored.
INSERT INTO admin_user (username, password_hash)
VALUES ('${admin_username}', '${admin_password_hash}');
