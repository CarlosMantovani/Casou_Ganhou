package com.weddingraffle.rifa.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.mercadopago.client.preference.PreferencePaymentTypeRequest;
import org.junit.jupiter.api.Test;

class MercadoPagoClientTests {

    @Test
    void excludesPaymentTypesOutsideCreditDebitAndPix() {
        var excludedPaymentTypes = MercadoPagoClient.paymentMethods().getExcludedPaymentTypes().stream()
                .map(PreferencePaymentTypeRequest::getId)
                .toList();

        assertThat(excludedPaymentTypes).containsExactlyInAnyOrder("ticket", "digital_currency", "atm", "prepaid_card");
    }
}
