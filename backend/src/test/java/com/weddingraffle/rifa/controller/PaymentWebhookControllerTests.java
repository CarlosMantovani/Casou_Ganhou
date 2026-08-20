package com.weddingraffle.rifa.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.weddingraffle.rifa.config.AppProperties;
import com.weddingraffle.rifa.config.SecurityConfig;
import com.weddingraffle.rifa.service.TransactionService;
import com.weddingraffle.rifa.service.WebhookSignatureService;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PaymentWebhookController.class)
@Import({SecurityConfig.class, PaymentWebhookControllerTests.TestConfig.class})
class PaymentWebhookControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransactionService transactionService;

    @MockBean
    private WebhookSignatureService webhookSignatureService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    void webhookProcessesPaymentNotificationWithoutAuthentication() throws Exception {
        mockMvc.perform(post("/payments/webhook")
                        .header("x-request-id", "request-123")
                        .header("x-signature", "ts=123,v1=abc")
                        .contentType("application/json")
                        .content("{\"type\":\"payment\",\"data\":{\"id\":\"123\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processed").value(true));

        verify(webhookSignatureService).validate("123", "request-123", "ts=123,v1=abc");
        verify(transactionService).processPaymentNotification("123");
    }

    @Test
    void webhookUsesQueryPaymentIdBeforeBodyPaymentId() throws Exception {
        mockMvc.perform(post("/payments/webhook")
                        .queryParam("data.id", "123")
                        .header("x-request-id", "request-123")
                        .header("x-signature", "ts=123,v1=abc")
                        .contentType("application/json")
                        .content("{\"type\":\"payment\",\"data\":{\"id\":\"ignored\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processed").value(true));

        verify(webhookSignatureService).validate("123", "request-123", "ts=123,v1=abc");
        verify(transactionService).processPaymentNotification("123");
    }

    @Test
    void webhookIgnoresNonPaymentNotification() throws Exception {
        mockMvc.perform(post("/payments/webhook")
                        .queryParam("data.id", "merchant-order-123")
                        .queryParam("type", "merchant_order")
                        .header("x-request-id", "request-123")
                        .header("x-signature", "ts=123,v1=abc")
                        .contentType("application/json")
                        .content("{\"type\":\"merchant_order\",\"data\":{\"id\":\"merchant-order-123\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processed").value(false));

        verify(webhookSignatureService).validate("merchant-order-123", "request-123", "ts=123,v1=abc");
        verifyNoInteractions(transactionService);
    }

    @Test
    void webhookReturnsBadRequestWhenPaymentIdIsMissing() throws Exception {
        mockMvc.perform(post("/payments/webhook")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        AppProperties appProperties() {
            return new AppProperties(
                    "http://localhost:5173",
                    new AppProperties.Jwt("01234567890123456789012345678901", 3600, "raffle-api-test"),
                    new AppProperties.Raffle(new BigDecimal("10.00"), "00000", "99999"),
                    new AppProperties.MercadoPago(
                            "token",
                            "http://localhost:8080/payments/webhook",
                            "",
                            "http://localhost:5173/payment-return/success",
                            "http://localhost:5173/payment-return/failure",
                            "http://localhost:5173/payment-return/pending",
                            new AppProperties.Retry(3, 500, 2)));
        }
    }
}
