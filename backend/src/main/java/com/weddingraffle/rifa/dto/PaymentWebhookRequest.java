package com.weddingraffle.rifa.dto;

public record PaymentWebhookRequest(String type, String action, PaymentWebhookDataRequest data) {}
