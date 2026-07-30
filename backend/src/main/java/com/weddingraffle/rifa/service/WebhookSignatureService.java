package com.weddingraffle.rifa.service;

public interface WebhookSignatureService {

    void validate(String paymentId, String requestId, String signature);
}
