package com.weddingraffle.rifa.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.core.MPRequestOptions;
import com.mercadopago.resources.preference.Preference;
import com.weddingraffle.rifa.config.AppProperties;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class MercadoPagoClientTests {

    @Test
    void sendsIdempotencyKeyWhenCreatingPreference() throws Exception {
        PreferenceClient preferenceClient = mock(PreferenceClient.class);
        Preference preference = mock(Preference.class);
        when(preference.getId()).thenReturn("preference-123");
        when(preference.getInitPoint()).thenReturn("https://checkout.example.com");
        when(preferenceClient.create(any(), any(MPRequestOptions.class))).thenReturn(preference);
        MercadoPagoClient client = new MercadoPagoClient(appProperties(), mock(PaymentClient.class), preferenceClient);

        CheckoutPreferenceResponse response = client.createPreference(
                new CheckoutPreferenceRequest("Guest User", null, 2, new BigDecimal("10.00"), "external-reference-123"),
                "checkout-key-123");

        ArgumentCaptor<MPRequestOptions> optionsCaptor = ArgumentCaptor.forClass(MPRequestOptions.class);
        verify(preferenceClient).create(any(), optionsCaptor.capture());
        assertThat(optionsCaptor.getValue().getCustomHeaders()).containsEntry("X-Idempotency-Key", "checkout-key-123");
        assertThat(response)
                .isEqualTo(new CheckoutPreferenceResponse("preference-123", "https://checkout.example.com"));
    }

    private static AppProperties appProperties() {
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
                        new AppProperties.Retry(1, 1, 1)));
    }
}
