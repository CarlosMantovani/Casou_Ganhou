package com.weddingraffle.rifa.integration;

public interface PaymentProviderClient {

    CheckoutPreferenceResponse createPreference(CheckoutPreferenceRequest request);

    PaymentProviderPayment getPayment(String paymentId);
}
