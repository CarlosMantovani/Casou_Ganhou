package com.weddingraffle.rifa.controller;

import com.weddingraffle.rifa.dto.PaymentWebhookRequest;
import com.weddingraffle.rifa.dto.PaymentWebhookResponse;
import com.weddingraffle.rifa.service.TransactionService;
import com.weddingraffle.rifa.service.WebhookSignatureService;
import io.swagger.v3.oas.annotations.Operation;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payments")
public class PaymentWebhookController {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentWebhookController.class);

    private final TransactionService transactionService;
    private final WebhookSignatureService webhookSignatureService;

    public PaymentWebhookController(
            TransactionService transactionService, WebhookSignatureService webhookSignatureService) {
        this.transactionService = transactionService;
        this.webhookSignatureService = webhookSignatureService;
    }

    @Operation(summary = "Process Mercado Pago payment webhook")
    @PostMapping("/webhook")
    public ResponseEntity<PaymentWebhookResponse> process(
            @RequestBody(required = false) PaymentWebhookRequest request,
            @RequestParam Map<String, String> queryParams,
            @RequestHeader(value = "x-request-id", required = false) String requestId,
            @RequestHeader(value = "x-signature", required = false) String signature) {
        String paymentId = paymentId(request, queryParams);
        String type = notificationType(request, queryParams);
        String action = notificationAction(request, queryParams);
        String topic = queryParams.get("topic");
        LOGGER.info(
                "Received Mercado Pago webhook type={} topic={} action={} paymentId={} requestIdPresent={} signaturePresent={}",
                type,
                topic,
                action,
                paymentId,
                StringUtils.hasText(requestId),
                StringUtils.hasText(signature));
        webhookSignatureService.validate(paymentId, requestId, signature);
        if (!isPaymentNotification(request, queryParams)) {
            LOGGER.info(
                    "Ignored Mercado Pago webhook because notification is not payment type={} topic={} action={} paymentId={}",
                    type,
                    topic,
                    action,
                    paymentId);
            return ResponseEntity.ok(new PaymentWebhookResponse(false));
        }
        transactionService.processPaymentNotification(paymentId);
        LOGGER.info("Processed Mercado Pago payment webhook paymentId={}", paymentId);
        return ResponseEntity.ok(new PaymentWebhookResponse(true));
    }

    private static boolean isPaymentNotification(PaymentWebhookRequest request, Map<String, String> queryParams) {
        String type = notificationType(request, queryParams);
        return !StringUtils.hasText(type) || "payment".equalsIgnoreCase(type);
    }

    private static String notificationType(PaymentWebhookRequest request, Map<String, String> queryParams) {
        String type = queryParams.get("type");
        if (!StringUtils.hasText(type)) {
            type = queryParams.get("topic");
        }
        if (!StringUtils.hasText(type) && request != null) {
            type = request.type();
        }
        return type;
    }

    private static String notificationAction(PaymentWebhookRequest request, Map<String, String> queryParams) {
        String action = queryParams.get("action");
        if (!StringUtils.hasText(action) && request != null) {
            action = request.action();
        }
        return action;
    }

    private static String paymentId(PaymentWebhookRequest request, Map<String, String> queryParams) {
        String dataId = queryParams.get("data.id");
        if (StringUtils.hasText(dataId)) {
            return dataId;
        }
        String id = queryParams.get("id");
        if (StringUtils.hasText(id)) {
            return id;
        }
        if (request != null
                && request.data() != null
                && StringUtils.hasText(request.data().id())) {
            return request.data().id();
        }
        throw new IllegalArgumentException("Payment id is required.");
    }
}
