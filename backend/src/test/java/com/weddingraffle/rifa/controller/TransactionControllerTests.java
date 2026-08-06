package com.weddingraffle.rifa.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.weddingraffle.rifa.config.AppProperties;
import com.weddingraffle.rifa.config.SecurityConfig;
import com.weddingraffle.rifa.dto.TransactionCreateResponse;
import com.weddingraffle.rifa.dto.TransactionQuoteResponse;
import com.weddingraffle.rifa.dto.TransactionStatusResponse;
import com.weddingraffle.rifa.entity.PaymentStatus;
import com.weddingraffle.rifa.exception.ExternalPaymentException;
import com.weddingraffle.rifa.service.LuckyNumberPdfService;
import com.weddingraffle.rifa.service.TransactionService;
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

@WebMvcTest(TransactionController.class)
@Import({SecurityConfig.class, TransactionControllerTests.TestConfig.class})
class TransactionControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransactionService transactionService;

    @MockBean
    private LuckyNumberPdfService luckyNumberPdfService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    void quoteReturnsTotalWithoutAuthentication() throws Exception {
        when(transactionService.quote(any()))
                .thenReturn(new TransactionQuoteResponse(
                        "Guest User",
                        "11999999999",
                        "guest@example.com",
                        2,
                        new BigDecimal("10.00"),
                        new BigDecimal("20.00")));

        mockMvc.perform(
                        post("/transactions/quote")
                                .contentType("application/json")
                                .content(
                                        "{\"name\":\"Guest User\",\"phone\":\"(11) 99999-9999\",\"email\":\"guest@example.com\",\"quantity\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Guest User"))
                .andExpect(jsonPath("$.phone").value("11999999999"))
                .andExpect(jsonPath("$.email").value("guest@example.com"))
                .andExpect(jsonPath("$.quantity").value(2))
                .andExpect(jsonPath("$.unitPrice").value(10.00))
                .andExpect(jsonPath("$.totalAmount").value(20.00));
    }

    @Test
    void quoteReturnsValidationErrorForInvalidRequest() throws Exception {
        mockMvc.perform(post("/transactions/quote")
                        .contentType("application/json")
                        .content("{\"name\":\"\",\"phone\":\"\",\"email\":\"invalid\",\"quantity\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void createReturnsCheckoutWithoutAuthentication() throws Exception {
        when(transactionService.create(any()))
                .thenReturn(new TransactionCreateResponse(
                        "external-reference-123", "preference-123", "https://checkout.example.com"));

        mockMvc.perform(
                        post("/transactions")
                                .contentType("application/json")
                                .content(
                                        "{\"name\":\"Guest User\",\"phone\":\"(11) 99999-9999\",\"email\":\"guest@example.com\",\"quantity\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.externalReference").value("external-reference-123"))
                .andExpect(jsonPath("$.preferenceId").value("preference-123"))
                .andExpect(jsonPath("$.checkoutUrl").value("https://checkout.example.com"));
    }

    @Test
    void createReturnsBadGatewayWhenPaymentProviderFails() throws Exception {
        when(transactionService.create(any()))
                .thenThrow(new ExternalPaymentException(
                        "Unable to create Mercado Pago preference.", new RuntimeException()));

        mockMvc.perform(
                        post("/transactions")
                                .contentType("application/json")
                                .content(
                                        "{\"name\":\"Guest User\",\"phone\":\"(11) 99999-9999\",\"email\":\"guest@example.com\",\"quantity\":2}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("PAYMENT_PROVIDER_ERROR"));
    }

    @Test
    void createReturnsInternalErrorWhenUnexpectedFailureOccurs() throws Exception {
        when(transactionService.create(any())).thenThrow(new IllegalStateException("Unexpected failure."));

        mockMvc.perform(
                        post("/transactions")
                                .contentType("application/json")
                                .content(
                                        "{\"name\":\"Guest User\",\"phone\":\"(11) 99999-9999\",\"email\":\"guest@example.com\",\"quantity\":2}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
    }

    @Test
    void downloadsLuckyNumbersPdfWithoutAuthentication() throws Exception {
        when(luckyNumberPdfService.generate("external-reference-123")).thenReturn("%PDF".getBytes());

        mockMvc.perform(get("/transactions/external-reference-123/lucky-numbers.pdf"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment")));
    }

    @Test
    void statusAcceptsPaymentIdFromCheckoutReturn() throws Exception {
        when(transactionService.getStatus("external-reference-123", "456"))
                .thenReturn(new TransactionStatusResponse(
                        "external-reference-123",
                        true,
                        PaymentStatus.APPROVED,
                        2,
                        new BigDecimal("20.00"),
                        java.util.List.of("00001", "00002")));

        mockMvc.perform(get("/transactions/external-reference-123/status").param("paymentId", "456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        verify(transactionService).getStatus(eq("external-reference-123"), eq("456"));
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
                            new AppProperties.Retry(3, 500, 2)),
                    new AppProperties.Mail("no-reply@example.com"));
        }
    }
}
