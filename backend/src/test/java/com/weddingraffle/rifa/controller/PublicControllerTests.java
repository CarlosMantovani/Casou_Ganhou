package com.weddingraffle.rifa.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.weddingraffle.rifa.config.AppProperties;
import com.weddingraffle.rifa.config.SecurityConfig;
import com.weddingraffle.rifa.dto.HomeSummaryResponse;
import com.weddingraffle.rifa.dto.TopBuyerResponse;
import com.weddingraffle.rifa.service.HomeSummaryService;
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

@WebMvcTest(PublicController.class)
@Import({SecurityConfig.class, PublicControllerTests.TestConfig.class})
class PublicControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private HomeSummaryService homeSummaryService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    void returnsHomeSummaryWithoutAuthenticationAndWithoutPersonalData() throws Exception {
        when(homeSummaryService.getSummary())
                .thenReturn(new HomeSummaryResponse(
                        OffsetDateTime.parse("2026-09-05T21:00:00-03:00"),
                        List.of(new TopBuyerResponse("🎁", "#B75D46", 4L))));

        mockMvc.perform(get("/public/home-summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scheduledDrawAt").value("2026-09-05T21:00:00-03:00"))
                .andExpect(jsonPath("$.topBuyers[0].avatarEmoji").value("🎁"))
                .andExpect(jsonPath("$.topBuyers[0].avatarColor").value("#B75D46"))
                .andExpect(jsonPath("$.topBuyers[0].quantity").value(4))
                .andExpect(jsonPath("$.topBuyers[0].name").doesNotExist())
                .andExpect(jsonPath("$.topBuyers[0].phone").doesNotExist())
                .andExpect(jsonPath("$.topBuyers[0].email").doesNotExist());
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        AppProperties appProperties() {
            return new AppProperties(
                    "http://localhost:5173",
                    new AppProperties.Jwt("01234567890123456789012345678901", 3600, "raffle-api-test"),
                    new AppProperties.Raffle("00000", "99999"),
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
