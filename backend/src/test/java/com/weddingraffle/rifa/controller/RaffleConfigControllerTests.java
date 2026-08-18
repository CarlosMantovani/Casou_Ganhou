package com.weddingraffle.rifa.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.weddingraffle.rifa.config.AppProperties;
import com.weddingraffle.rifa.config.SecurityConfig;
import com.weddingraffle.rifa.dto.RaffleConfigResponse;
import com.weddingraffle.rifa.dto.WeddingPaletteResponse;
import com.weddingraffle.rifa.dto.WeddingProfileResponse;
import com.weddingraffle.rifa.service.RaffleConfigService;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RaffleConfigController.class)
@Import({SecurityConfig.class, RaffleConfigControllerTests.TestConfig.class})
class RaffleConfigControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RaffleConfigService raffleConfigService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    void getConfigRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/admin/raffle-config")).andExpect(status().isUnauthorized());
    }

    @Test
    void getConfigReturnsCurrentUnitPriceForAdmin() throws Exception {
        when(raffleConfigService.getConfig())
                .thenReturn(new RaffleConfigResponse(
                        new BigDecimal("10.00"),
                        null,
                        weddingProfile(),
                        OffsetDateTime.parse("2026-08-14T18:00:00-03:00")));

        mockMvc.perform(get("/admin/raffle-config").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unitPrice").value(10.00))
                .andExpect(jsonPath("$.updatedAt").value("2026-08-14T18:00:00-03:00"));
    }

    @Test
    void updateUnitPriceRequiresAuthentication() throws Exception {
        mockMvc.perform(put("/admin/raffle-config/unit-price")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"unitPrice\":15.00}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateUnitPriceRejectsInvalidValue() throws Exception {
        mockMvc.perform(put("/admin/raffle-config/unit-price")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"unitPrice\":0}")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateUnitPriceReturnsUpdatedConfigForAdmin() throws Exception {
        when(raffleConfigService.updateUnitPrice(any()))
                .thenReturn(new RaffleConfigResponse(
                        new BigDecimal("15.00"),
                        null,
                        weddingProfile(),
                        OffsetDateTime.parse("2026-08-14T18:00:00-03:00")));

        mockMvc.perform(put("/admin/raffle-config/unit-price")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"unitPrice\":15.00}")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unitPrice").value(15.00));
    }

    @Test
    void updateScheduledDrawAtReturnsUpdatedConfigForAdmin() throws Exception {
        when(raffleConfigService.updateScheduledDrawAt(any()))
                .thenReturn(new RaffleConfigResponse(
                        new BigDecimal("10.00"),
                        OffsetDateTime.parse("2026-09-05T20:00:00-03:00"),
                        weddingProfile(),
                        OffsetDateTime.parse("2026-08-14T18:00:00-03:00")));

        mockMvc.perform(put("/admin/raffle-config/scheduled-at")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scheduledDrawAt\":\"2026-09-05T20:00:00-03:00\"}")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scheduledDrawAt").value("2026-09-05T20:00:00-03:00"));
    }

    @Test
    void updateWeddingProfileReturnsUpdatedConfigForAdmin() throws Exception {
        when(raffleConfigService.updateWeddingProfile(any()))
                .thenReturn(new RaffleConfigResponse(
                        new BigDecimal("10.00"),
                        null,
                        weddingProfile(),
                        OffsetDateTime.parse("2026-08-14T18:00:00-03:00")));

        mockMvc.perform(put("/admin/raffle-config/wedding-profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {
                                  "groomName": "Jose Carlos",
                                  "brideName": "Paula",
                                  "palette": {
                                    "ivory": "#F7F1E6",
                                    "ivoryDeep": "#F0E8D8",
                                    "ink": "#2B2419",
                                    "inkSoft": "#5B5140",
                                    "green": "#24402E",
                                    "greenDeep": "#152A1D",
                                    "wine": "#7A2E33",
                                    "gold": "#B8935A",
                                    "goldSoft": "#DCC79A",
                                    "line": "#D9CBAA"
                                  }
                                }
                                """)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weddingProfile.groomName").value("Jose Carlos"))
                .andExpect(jsonPath("$.weddingProfile.brideName").value("Paula"))
                .andExpect(jsonPath("$.weddingProfile.palette.green").value("#24402E"));
    }

    private static WeddingProfileResponse weddingProfile() {
        return new WeddingProfileResponse(
                "Jose Carlos",
                "Paula",
                new WeddingPaletteResponse(
                        "#F7F1E6", "#F0E8D8", "#2B2419", "#5B5140", "#24402E", "#152A1D", "#7A2E33", "#B8935A",
                        "#DCC79A", "#D9CBAA"));
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
