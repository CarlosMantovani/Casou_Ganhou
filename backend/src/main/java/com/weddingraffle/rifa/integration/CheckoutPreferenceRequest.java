package com.weddingraffle.rifa.integration;

import java.math.BigDecimal;

public record CheckoutPreferenceRequest(
        String email, Integer quantity, BigDecimal unitPrice, String externalReference) {}
