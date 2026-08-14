package com.weddingraffle.rifa.controller;

import com.weddingraffle.rifa.dto.PaymentWebhookRequest;
import com.weddingraffle.rifa.dto.PaymentWebhookResponse;
import com.weddingraffle.rifa.service.TransactionService;
import com.weddingraffle.rifa.service.WebhookSignatureService;
import io.swagger.v3.oas.annotations.Operation;
import java.util.Map;
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
        webhookSignatureService.validate(paymentId, requestId, signature);
        transactionService.processPaymentNotification(paymentId);
        return ResponseEntity.ok(new PaymentWebhookResponse(true));
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
