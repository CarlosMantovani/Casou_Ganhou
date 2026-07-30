package com.weddingraffle.rifa.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.weddingraffle.rifa.config.AppProperties;
import com.weddingraffle.rifa.dto.TransactionQuoteRequest;
import com.weddingraffle.rifa.dto.TransactionQuoteResponse;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class TransactionServiceImplTests {

    @Test
    void calculatesQuoteFromConfiguredUnitPrice() {
        TransactionServiceImpl transactionService = new TransactionServiceImpl(appProperties());

        TransactionQuoteResponse response =
                transactionService.quote(new TransactionQuoteRequest("guest@example.com", 3));

        assertThat(response.email()).isEqualTo("guest@example.com");
        assertThat(response.quantity()).isEqualTo(3);
        assertThat(response.unitPrice()).isEqualByComparingTo("10.00");
        assertThat(response.totalAmount()).isEqualByComparingTo("30.00");
    }

    private static AppProperties appProperties() {
        return new AppProperties(
                "http://localhost:5173",
                new AppProperties.Jwt("01234567890123456789012345678901", 3600, "raffle-api-test"),
                new AppProperties.Raffle(new BigDecimal("10.00"), "00000", "99999"),
                new AppProperties.MercadoPago("token", "http://localhost:8080/payments/webhook"),
                new AppProperties.Mail("no-reply@example.com"));
    }
}
