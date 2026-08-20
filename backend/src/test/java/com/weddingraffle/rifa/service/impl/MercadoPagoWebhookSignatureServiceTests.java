package com.weddingraffle.rifa.service.impl;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.weddingraffle.rifa.config.AppProperties;
import com.weddingraffle.rifa.exception.InvalidWebhookSignatureException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

class MercadoPagoWebhookSignatureServiceTests {

    @Test
    void acceptsValidSignatureWhenSecretIsConfigured() throws Exception {
        MercadoPagoWebhookSignatureService service = new MercadoPagoWebhookSignatureService(appProperties("secret"));
        String manifest = "id:123;request-id:request-123;ts:456;";
        String signature = "ts=456,v1=" + hmacSha256(manifest, "secret");

        assertThatCode(() -> service.validate("123", "request-123", signature)).doesNotThrowAnyException();
    }

    @Test
    void rejectsInvalidSignatureWhenSecretIsConfigured() {
        MercadoPagoWebhookSignatureService service = new MercadoPagoWebhookSignatureService(appProperties("secret"));

        assertThatThrownBy(() -> service.validate("123", "request-123", "ts=456,v1=bad"))
                .isInstanceOf(InvalidWebhookSignatureException.class);
    }

    @Test
    void skipsValidationWhenSecretIsBlank() {
        MercadoPagoWebhookSignatureService service = new MercadoPagoWebhookSignatureService(appProperties(""));

        assertThatCode(() -> service.validate("123", null, null)).doesNotThrowAnyException();
    }

    private static String hmacSha256(String value, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] digest = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte item : digest) {
            hex.append("%02x".formatted(item));
        }
        return hex.toString();
    }

    private static AppProperties appProperties(String webhookSecret) {
        return new AppProperties(
                "http://localhost:5173",
                new AppProperties.Jwt("01234567890123456789012345678901", 3600, "raffle-api-test"),
                new AppProperties.Raffle(new BigDecimal("10.00"), "00000", "99999"),
                new AppProperties.MercadoPago(
                        "token",
                        "http://localhost:8080/payments/webhook",
                        webhookSecret,
                        "http://localhost:5173/payment-return/success",
                        "http://localhost:5173/payment-return/failure",
                        "http://localhost:5173/payment-return/pending",
                        new AppProperties.Retry(3, 500, 2)));
    }
}
