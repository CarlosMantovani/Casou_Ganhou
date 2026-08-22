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
    private static final String RAFFLE_UNIT_PRICE = "10.00";
    private static final String RAFFLE_NUMBER_MIN = "00000";
    private static final String RAFFLE_NUMBER_MAX = "99999";

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
                        "admin_username", "admin",
                        "admin_password_hash", ADMIN_PASSWORD_HASH,
                        "raffle_unit_price", RAFFLE_UNIT_PRICE,
                        "raffle_number_min", RAFFLE_NUMBER_MIN,
                        "raffle_number_max", RAFFLE_NUMBER_MAX))
                .load();

        flyway.migrate();

        try (Connection connection = POSTGRES.createConnection("");
                Statement statement = connection.createStatement()) {
            assertThat(tableExists(statement, "transaction")).isTrue();
            assertThat(tableExists(statement, "lucky_number")).isTrue();
            assertThat(tableExists(statement, "raffle_draw")).isTrue();
            assertThat(tableExists(statement, "admin_user")).isTrue();
            assertThat(tableExists(statement, "raffle_config")).isTrue();
            assertThat(tableExists(statement, "raffle_capacity")).isTrue();
            assertThat(tableExists(statement, "capacity_reservation")).isTrue();
            assertThat(tableExists(statement, "purchase_intent")).isTrue();
            assertThat(indexExists(statement, "idx_transaction_email")).isTrue();
            assertThat(indexExists(statement, "idx_transaction_external_reference"))
                    .isTrue();
            assertThat(indexExists(statement, "idx_transaction_status")).isTrue();
            assertThat(indexExists(statement, "idx_lucky_number_email")).isTrue();
            assertThat(columnExists(statement, "transaction", "confirmation_email_sent_at"))
                    .isFalse();
            assertThat(columnExists(statement, "transaction", "confirmation_email_failed_at"))
                    .isFalse();
            assertThat(columnExists(statement, "transaction", "confirmation_email_last_error"))
                    .isFalse();
            assertThat(columnExists(statement, "transaction", "unit_price")).isTrue();
            assertThat(columnExists(statement, "purchase_intent", "request_hash"))
                    .isTrue();
            assertThat(columnExists(statement, "purchase_intent", "response_payload"))
                    .isTrue();
            assertThat(adminSeedExists(statement)).isTrue();
            assertThat(approvedFlagRankingQueryWorks(statement)).isTrue();
            assertThat(adminTransactionSummaryQueryWorks(statement)).isTrue();
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

    private static boolean approvedFlagRankingQueryWorks(Statement statement) throws SQLException {
        statement.executeUpdate(
                """
                insert into transaction (
                    name,
                    phone,
                    email,
                    quantity,
                    total_amount,
                    unit_price,
                    status,
                    payment_method,
                    external_reference,
                    recovery_code,
                    participant_flag_code,
                    participant_flag_name,
                    participant_flag_emoji
                ) values (
                    'Older Buyer',
                    '44999999999',
                    'older@example.com',
                    3,
                    30.00,
                    10.00,
                    'APPROVED',
                    'CASH',
                    'external-reference-ranking-older',
                    '4821',
                    'BRAZIL',
                    'Brasil',
                    '🇧🇷'
                )
                """);
        statement.executeUpdate(
                """
                insert into transaction (
                    name,
                    phone,
                    email,
                    quantity,
                    total_amount,
                    unit_price,
                    status,
                    payment_method,
                    external_reference,
                    recovery_code,
                    participant_flag_code,
                    participant_flag_name,
                    participant_flag_emoji,
                    created_at
                ) values (
                    'Recent Buyer',
                    '44999999998',
                    'recent@example.com',
                    3,
                    30.00,
                    10.00,
                    'APPROVED',
                    'CASH',
                    'external-reference-ranking-recent',
                    '4822',
                    'CANADA',
                    'Canada',
                    'CA',
                    '2030-08-21T10:00:00-03:00'
                )
                """);
        insertCapacityReviewTransaction(
                statement,
                "capacity-review-contribution",
                "CONTRIBUTION_WITHOUT_NUMBERS",
                100,
                "1000.00",
                "UNITED_STATES");
        insertCapacityReviewTransaction(
                statement, "capacity-review-refund", "REFUND_COMPLETED", 50, "500.00", "MEXICO");
        insertCapacityReviewTransaction(statement, "capacity-review-pending", "PENDING", 20, "200.00", "FRANCE");

        try (ResultSet resultSet = statement.executeQuery(
                """
                select
                    participant_flag_code as code,
                    participant_flag_name as name,
                    participant_flag_emoji as emoji,
                    cast(sum(quantity) as bigint) as total_numbers
                from transaction
                where status = 'APPROVED'
                  and capacity_review_status is null
                group by participant_flag_code, participant_flag_name, participant_flag_emoji
                order by sum(quantity) desc, max(created_at) desc, participant_flag_name asc
                """)) {
            return resultSet.next()
                    && "CANADA".equals(resultSet.getString("code"))
                    && resultSet.getLong("total_numbers") == 3L;
        }
    }

    private static boolean adminTransactionSummaryQueryWorks(Statement statement) throws SQLException {
        try (ResultSet resultSet = statement.executeQuery(
                """
                select
                    cast(count(id) as bigint) as total_transactions,
                    cast(coalesce(sum(case
                        when status = 'APPROVED' and capacity_review_status is null then quantity
                        else 0
                    end), 0) as bigint)
                        as approved_lucky_numbers,
                    coalesce(sum(case
                        when status = 'APPROVED'
                            and capacity_review_status is distinct from 'REFUND_COMPLETED'
                        then total_amount
                        else 0
                    end), 0)
                        as approved_revenue
                from transaction
                """)) {
            return resultSet.next()
                    && resultSet.getLong("total_transactions") == 5L
                    && resultSet.getLong("approved_lucky_numbers") == 6L
                    && resultSet.getBigDecimal("approved_revenue").compareTo(new java.math.BigDecimal("1260.00")) == 0;
        }
    }

    private static void insertCapacityReviewTransaction(
            Statement statement,
            String externalReference,
            String capacityReviewStatus,
            int quantity,
            String totalAmount,
            String flagCode)
            throws SQLException {
        statement.executeUpdate(
                """
                insert into transaction (
                    name,
                    phone,
                    quantity,
                    total_amount,
                    unit_price,
                    status,
                    payment_method,
                    external_reference,
                    recovery_code,
                    participant_flag_code,
                    participant_flag_name,
                    participant_flag_emoji,
                    capacity_review_status
                ) values (
                    'Capacity Review Buyer',
                    '11999999999',
                    %d,
                    %s,
                    10.00,
                    'APPROVED',
                    'MERCADO_PAGO',
                    '%s',
                    '4821',
                    '%s',
                    '%s',
                    'FLAG',
                    '%s'
                )
                """
                        .formatted(quantity, totalAmount, externalReference, flagCode, flagCode, capacityReviewStatus));
    }
}
