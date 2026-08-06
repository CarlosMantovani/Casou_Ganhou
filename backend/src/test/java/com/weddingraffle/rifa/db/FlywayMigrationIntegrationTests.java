package com.weddingraffle.rifa.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class FlywayMigrationIntegrationTests {

    private static final String ADMIN_PASSWORD_HASH = "$2a$12$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("wedding_raffle")
            .withUsername("wedding_raffle")
            .withPassword("wedding_raffle");

    @Test
    void appliesAllDatabaseMigrations() throws SQLException {
        Flyway flyway = Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .placeholders(Map.of(
                        "admin_username",
                        "admin",
                        "admin_password_hash",
                        ADMIN_PASSWORD_HASH,
                        "raffle_unit_price",
                        "10.00"))
                .load();

        flyway.migrate();

        try (Connection connection = POSTGRES.createConnection("");
                Statement statement = connection.createStatement()) {
            assertThat(tableExists(statement, "transaction")).isTrue();
            assertThat(tableExists(statement, "lucky_number")).isTrue();
            assertThat(tableExists(statement, "raffle_draw")).isTrue();
            assertThat(tableExists(statement, "admin_user")).isTrue();
            assertThat(tableExists(statement, "raffle_config")).isTrue();
            assertThat(indexExists(statement, "idx_transaction_email")).isTrue();
            assertThat(indexExists(statement, "idx_transaction_external_reference"))
                    .isTrue();
            assertThat(indexExists(statement, "idx_transaction_status")).isTrue();
            assertThat(indexExists(statement, "idx_lucky_number_email")).isTrue();
            assertThat(columnExists(statement, "transaction", "confirmation_email_sent_at"))
                    .isTrue();
            assertThat(columnExists(statement, "transaction", "confirmation_email_failed_at"))
                    .isTrue();
            assertThat(columnExists(statement, "transaction", "confirmation_email_last_error"))
                    .isTrue();
            assertThat(columnExists(statement, "transaction", "unit_price")).isTrue();
            assertThat(raffleConfigSeedExists(statement)).isTrue();
            assertThat(adminSeedExists(statement)).isTrue();
        }
    }

    private static boolean tableExists(Statement statement, String tableName) throws SQLException {
        try (ResultSet resultSet = statement.executeQuery(
                "select exists (select 1 from information_schema.tables where table_schema = 'public' and table_name = '"
                        + tableName + "')")) {
            resultSet.next();
            return resultSet.getBoolean(1);
        }
    }

    private static boolean indexExists(Statement statement, String indexName) throws SQLException {
        try (ResultSet resultSet = statement.executeQuery(
                "select exists (select 1 from pg_indexes where schemaname = 'public' and indexname = '" + indexName
                        + "')")) {
            resultSet.next();
            return resultSet.getBoolean(1);
        }
    }

    private static boolean columnExists(Statement statement, String tableName, String columnName) throws SQLException {
        try (ResultSet resultSet = statement.executeQuery(
                "select exists (select 1 from information_schema.columns where table_schema = 'public' and table_name = '"
                        + tableName + "' and column_name = '" + columnName + "')")) {
            resultSet.next();
            return resultSet.getBoolean(1);
        }
    }

    private static boolean adminSeedExists(Statement statement) throws SQLException {
        try (ResultSet resultSet = statement.executeQuery(
                "select exists (select 1 from admin_user where username = 'admin' and char_length(password_hash) = 60)")) {
            resultSet.next();
            return resultSet.getBoolean(1);
        }
    }

    private static boolean raffleConfigSeedExists(Statement statement) throws SQLException {
        try (ResultSet resultSet = statement.executeQuery(
                "select exists (select 1 from raffle_config where id = 1 and unit_price = 10.00)")) {
            resultSet.next();
            return resultSet.getBoolean(1);
        }
    }
}
