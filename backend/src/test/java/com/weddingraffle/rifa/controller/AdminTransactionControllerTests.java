package com.weddingraffle.rifa.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.weddingraffle.rifa.config.AppProperties;
import com.weddingraffle.rifa.config.SecurityConfig;
import com.weddingraffle.rifa.dto.AdminTransactionResponse;
import com.weddingraffle.rifa.dto.AdminTransactionSummaryResponse;
import com.weddingraffle.rifa.dto.CashTransactionCreateResponse;
import com.weddingraffle.rifa.dto.PaymentStatusResponse;
import com.weddingraffle.rifa.entity.PaymentMethod;
import com.weddingraffle.rifa.service.AdminTransactionService;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminTransactionController.class)
@Import({SecurityConfig.class, AdminTransactionControllerTests.TestConfig.class})
class AdminTransactionControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminTransactionService adminTransactionService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    void listRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/transactions")).andExpect(status().isUnauthorized());
    }

    @Test
    void summaryReturnsGlobalMetricsForAdmin() throws Exception {
        when(adminTransactionService.getSummary())
                .thenReturn(new AdminTransactionSummaryResponse(12, 48, new BigDecimal("480.00")));

        mockMvc.perform(get("/transactions/summary").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTransactions").value(12))
                .andExpect(jsonPath("$.approvedLuckyNumbers").value(48))
                .andExpect(jsonPath("$.approvedRevenue").value(480.00));
    }

    @Test
    void listReturnsPagedTransactionsForAdmin() throws Exception {
        when(adminTransactionService.list(eq("guest"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(new AdminTransactionResponse(
                        "external",
                        OffsetDateTime.parse("2026-08-14T18:00:00-03:00"),
                        "Guest User",
                        "11999999999",
                        "guest@example.com",
                        PaymentMethod.MERCADO_PAGO,
                        2,
                        new BigDecimal("20.00"),
                        PaymentStatusResponse.APROVADO,
                        List.of("00001", "00002")))));

        mockMvc.perform(get("/transactions")
                        .param("query", "guest")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].externalReference").value("external"))
                .andExpect(jsonPath("$.content[0].createdAt").value("2026-08-14T18:00:00-03:00"))
                .andExpect(jsonPath("$.content[0].name").value("Guest User"))
                .andExpect(jsonPath("$.content[0].status").value("APROVADO"))
                .andExpect(jsonPath("$.content[0].luckyNumbers[0]").value("00001"));
    }

    @Test
    void createCashTransactionRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/transactions/cash")
                        .contentType("application/json")
                        .content("{\"name\":\"Guest User\",\"phone\":\"(11) 99999-9999\",\"quantity\":2}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createCashTransactionReturnsApprovedNumbersForAdmin() throws Exception {
        when(adminTransactionService.createCashTransaction(any()))
                .thenReturn(new CashTransactionCreateResponse(
                        "external",
                        "4821",
                        "Guest User",
                        "11999999999",
                        null,
                        PaymentMethod.CASH,
                        PaymentStatusResponse.APROVADO,
                        2,
                        new BigDecimal("20.00"),
                        List.of("00003", "00004"),
                        List.of("00001", "00002"),
                        4));

        mockMvc.perform(post("/transactions/cash")
                        .contentType("application/json")
                        .content("{\"name\":\"Guest User\",\"phone\":\"(11) 99999-9999\",\"quantity\":2}")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.externalReference").value("external"))
                .andExpect(jsonPath("$.recoveryCode").value("4821"))
                .andExpect(jsonPath("$.paymentMethod").value("CASH"))
                .andExpect(jsonPath("$.status").value("APROVADO"))
                .andExpect(jsonPath("$.luckyNumbers[0]").value("00003"))
                .andExpect(jsonPath("$.previousLuckyNumbers[0]").value("00001"))
                .andExpect(jsonPath("$.totalLuckyNumbers").value(4));
    }

    @Test
    void deleteCashTransactionRequiresAuthentication() throws Exception {
        mockMvc.perform(delete("/transactions/external")).andExpect(status().isUnauthorized());
    }

    @Test
    void deleteCashTransactionReturnsNoContentForAdmin() throws Exception {
        mockMvc.perform(delete("/transactions/cash-reference")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isNoContent());

        verify(adminTransactionService).deleteCashTransaction("cash-reference");
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
