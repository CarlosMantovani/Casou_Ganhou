package com.weddingraffle.rifa.service.impl;

import com.weddingraffle.rifa.config.AppProperties;
import com.weddingraffle.rifa.exception.InvalidWebhookSignatureException;
import com.weddingraffle.rifa.service.WebhookSignatureService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MercadoPagoWebhookSignatureService implements WebhookSignatureService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final AppProperties appProperties;

    public MercadoPagoWebhookSignatureService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    public void validate(String paymentId, String requestId, String signature) {
        String secret = appProperties.mercadoPago().webhookSecret();
        if (!StringUtils.hasText(secret)) {
            return;
        }
        String timestamp = signatureValue(signature, "ts");
        String expectedSignature = signatureValue(signature, "v1");
        if (!StringUtils.hasText(paymentId)
                || !StringUtils.hasText(requestId)
                || !StringUtils.hasText(timestamp)
                || !StringUtils.hasText(expectedSignature)) {
            throw new InvalidWebhookSignatureException();
        }

        String manifest = "id:%s;request-id:%s;ts:%s;".formatted(paymentId, requestId, timestamp);
        if (!MessageDigest.isEqual(hmacSha256(manifest, secret), hexToBytes(expectedSignature))) {
            throw new InvalidWebhookSignatureException();
        }
    }

    private static String signatureValue(String signature, String key) {
        if (!StringUtils.hasText(signature)) {
            return null;
        }
        return Arrays.stream(signature.split(","))
                .map(String::trim)
                .filter(part -> part.startsWith(key + "="))
                .map(part -> part.substring(key.length() + 1))
                .findFirst()
                .orElse(null);
    }

    private static byte[] hmacSha256(String value, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
        } catch (java.security.GeneralSecurityException exception) {
            throw new InvalidWebhookSignatureException();
        }
    }

    private static byte[] hexToBytes(String hex) {
        if (hex.length() % 2 != 0) {
            throw new InvalidWebhookSignatureException();
        }
        byte[] bytes = new byte[hex.length() / 2];
        for (int index = 0; index < hex.length(); index += 2) {
            bytes[index / 2] = (byte) Integer.parseInt(hex.substring(index, index + 2), 16);
        }
        return bytes;
    }
}
