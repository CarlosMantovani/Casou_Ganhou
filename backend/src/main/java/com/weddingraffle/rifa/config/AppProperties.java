package com.weddingraffle.rifa.config;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(String frontendOrigin, Jwt jwt, Raffle raffle, MercadoPago mercadoPago, Mail mail) {

    public record Jwt(String secret, long expirationSeconds, String issuer) {}

    public record Raffle(String numberMin, String numberMax) {
        public Raffle(BigDecimal ignoredUnitPrice, String numberMin, String numberMax) {
            this(numberMin, numberMax);
        }
    }

    public record MercadoPago(
            String accessToken,
            String webhookUrl,
            String webhookSecret,
            String successUrl,
            String failureUrl,
            String pendingUrl,
            Retry retry) {}

    public record Retry(int maxAttempts, long delayMillis, double multiplier) {}

    public record Mail(String from) {}
}
