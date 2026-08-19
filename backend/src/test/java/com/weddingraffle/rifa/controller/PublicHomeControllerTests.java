package com.weddingraffle.rifa.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.weddingraffle.rifa.config.AppProperties;
import com.weddingraffle.rifa.config.SecurityConfig;
import com.weddingraffle.rifa.dto.FlagRankingResponse;
import com.weddingraffle.rifa.dto.HomeSummaryResponse;
import com.weddingraffle.rifa.dto.RaffleDrawResponse;
import com.weddingraffle.rifa.service.PublicHomeService;
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
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PublicHomeController.class)
@Import({SecurityConfig.class, PublicHomeControllerTests.TestConfig.class})
class PublicHomeControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PublicHomeService publicHomeService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    void returnsHomeSummaryWithoutAuthentication() throws Exception {
        when(publicHomeService.getSummary())
                .thenReturn(new HomeSummaryResponse(
                        OffsetDateTime.parse("2026-09-05T20:00:00-03:00"),
                        List.of(new FlagRankingResponse("BRAZIL", "Brasil", "BR", 12)),
                        new RaffleDrawResponse(
                                "00042",
                                "Winner Guest",
                                OffsetDateTime.parse("2026-09-05T23:00:00-03:00"),
                                "Brasil",
                                "BR")));

        mockMvc.perform(get("/public/home-summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scheduledDrawAt").value("2026-09-05T20:00:00-03:00"))
                .andExpect(jsonPath("$.flagRanking[0].code").value("BRAZIL"))
                .andExpect(jsonPath("$.flagRanking[0].name").value("Brasil"))
                .andExpect(jsonPath("$.flagRanking[0].totalNumbers").value(12))
                .andExpect(jsonPath("$.raffleResult.winningNumber").value("00042"))
                .andExpect(jsonPath("$.raffleResult.winnerName").value("Winner Guest"))
                .andExpect(jsonPath("$.raffleResult.participantFlagName").value("Brasil"))
                .andExpect(jsonPath("$.raffleResult.participantFlagEmoji").value("BR"));
    }

    @Test
    void returnsFlagRankingWithoutAuthentication() throws Exception {
        when(publicHomeService.getFlagRanking())
                .thenReturn(List.of(
                        new FlagRankingResponse("BRAZIL", "Brasil", "BR", 12),
                        new FlagRankingResponse("NICARAGUA", "Nicarágua", "NI", 8)));

        mockMvc.perform(get("/public/flag-ranking"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("BRAZIL"))
                .andExpect(jsonPath("$[0].name").value("Brasil"))
                .andExpect(jsonPath("$[0].totalNumbers").value(12))
                .andExpect(jsonPath("$[1].code").value("NICARAGUA"))
                .andExpect(jsonPath("$[1].name").value("Nicarágua"))
                .andExpect(jsonPath("$[1].totalNumbers").value(8));
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
