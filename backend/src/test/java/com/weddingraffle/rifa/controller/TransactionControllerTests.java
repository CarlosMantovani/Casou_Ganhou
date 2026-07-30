package com.weddingraffle.rifa.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.weddingraffle.rifa.config.AppProperties;
import com.weddingraffle.rifa.config.SecurityConfig;
import com.weddingraffle.rifa.dto.TransactionQuoteResponse;
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
    private UserDetailsService userDetailsService;

    @Test
    void quoteReturnsTotalWithoutAuthentication() throws Exception {
        when(transactionService.quote(any()))
                .thenReturn(new TransactionQuoteResponse(
                        "guest@example.com", 2, new BigDecimal("10.00"), new BigDecimal("20.00")));

        mockMvc.perform(post("/transactions/quote")
                        .contentType("application/json")
                        .content("{\"email\":\"guest@example.com\",\"quantity\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("guest@example.com"))
                .andExpect(jsonPath("$.quantity").value(2))
                .andExpect(jsonPath("$.unitPrice").value(10.00))
                .andExpect(jsonPath("$.totalAmount").value(20.00));
    }

    @Test
    void quoteReturnsValidationErrorForInvalidRequest() throws Exception {
        mockMvc.perform(post("/transactions/quote")
                        .contentType("application/json")
                        .content("{\"email\":\"invalid\",\"quantity\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        AppProperties appProperties() {
            return new AppProperties(
                    "http://localhost:5173",
                    new AppProperties.Jwt("01234567890123456789012345678901", 3600, "raffle-api-test"),
                    new AppProperties.Raffle(new BigDecimal("10.00"), "00000", "99999"),
                    new AppProperties.MercadoPago("token", "http://localhost:8080/payments/webhook"),
                    new AppProperties.Mail("no-reply@example.com"));
        }
    }
}
