package com.weddingraffle.rifa.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.mercadopago.client.preference.PreferencePaymentTypeRequest;
import com.weddingraffle.rifa.config.AppProperties;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MercadoPagoClientTests {

    @Test
    void excludesPaymentTypesOutsideCreditDebitAndPix() {
        var excludedPaymentTypes = MercadoPagoClient.paymentMethods().getExcludedPaymentTypes().stream()
                .map(PreferencePaymentTypeRequest::getId)
                .toList();

        assertThat(excludedPaymentTypes).containsExactlyInAnyOrder("ticket", "digital_currency", "atm", "prepaid_card");
    }

    @Test
    void buildsPreferenceWithPublicReturnSettingsAndBrazilianCurrency() {
        MercadoPagoClient client = new MercadoPagoClient(appProperties());

        var preferenceRequest = client.toPreferenceRequest(new CheckoutPreferenceRequest(
                "Guest User", "guest@example.com", 2, new BigDecimal("10.00"), "external-reference-123"));

        assertThat(preferenceRequest.getAutoReturn()).isEqualTo("approved");
        assertThat(preferenceRequest.getNotificationUrl()).isEqualTo("https://api.example.com/payments/webhook");
        assertThat(preferenceRequest.getBackUrls().getSuccess())
                .isEqualTo("https://app.example.com/payment-return/success");
        assertThat(preferenceRequest.getBackUrls().getFailure())
                .isEqualTo("https://app.example.com/payment-return/failure");
        assertThat(preferenceRequest.getBackUrls().getPending())
                .isEqualTo("https://app.example.com/payment-return/pending");
        assertThat(preferenceRequest.getItems()).singleElement().satisfies(item -> {
            assertThat(item.getCurrencyId()).isEqualTo("BRL");
            assertThat(item.getQuantity()).isEqualTo(2);
            assertThat(item.getUnitPrice()).isEqualByComparingTo("10.00");
        });
    }

    private static AppProperties appProperties() {
        return new AppProperties(
                "https://app.example.com",
                new AppProperties.Jwt("01234567890123456789012345678901", 3600, "raffle-api-test"),
                new AppProperties.Raffle(new BigDecimal("10.00"), "00000", "99999"),
                new AppProperties.MercadoPago(
                        "token",
                        "https://api.example.com/payments/webhook",
                        "",
                        "https://app.example.com/payment-return/success",
                        "https://app.example.com/payment-return/failure",
                        "https://app.example.com/payment-return/pending",
                        new AppProperties.Retry(3, 500, 2)),
                new AppProperties.Mail("no-reply@example.com"));
    }
}
