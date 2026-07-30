package com.weddingraffle.rifa.integration;

import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.preference.PreferenceBackUrlsRequest;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferencePayerRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.preference.Preference;
import com.weddingraffle.rifa.config.AppProperties;
import com.weddingraffle.rifa.exception.ExternalPaymentException;
import java.util.List;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

@Component
public class MercadoPagoClient implements PaymentProviderClient {

    private static final String ITEM_TITLE = "Lucky number";

    private final AppProperties appProperties;
    private final PaymentClient paymentClient;
    private final PreferenceClient preferenceClient;

    public MercadoPagoClient(AppProperties appProperties) {
        this.appProperties = appProperties;
        MercadoPagoConfig.setAccessToken(appProperties.mercadoPago().accessToken());
        this.paymentClient = new PaymentClient();
        this.preferenceClient = new PreferenceClient();
    }

    @Override
    @Retryable(
            retryFor = ExternalPaymentException.class,
            maxAttemptsExpression = "#{@appProperties.mercadoPago().retry().maxAttempts()}",
            backoff =
                    @Backoff(
                            delayExpression = "#{@appProperties.mercadoPago().retry().delayMillis()}",
                            multiplierExpression = "#{@appProperties.mercadoPago().retry().multiplier()}"))
    public CheckoutPreferenceResponse createPreference(CheckoutPreferenceRequest request) {
        try {
            Preference preference = preferenceClient.create(toPreferenceRequest(request));
            return new CheckoutPreferenceResponse(preference.getId(), preference.getInitPoint());
        } catch (MPApiException | MPException exception) {
            throw new ExternalPaymentException("Unable to create Mercado Pago preference.", exception);
        }
    }

    @Override
    @Retryable(
            retryFor = ExternalPaymentException.class,
            maxAttemptsExpression = "#{@appProperties.mercadoPago().retry().maxAttempts()}",
            backoff =
                    @Backoff(
                            delayExpression = "#{@appProperties.mercadoPago().retry().delayMillis()}",
                            multiplierExpression = "#{@appProperties.mercadoPago().retry().multiplier()}"))
    public PaymentProviderPayment getPayment(String paymentId) {
        try {
            Payment payment = paymentClient.get(Long.valueOf(paymentId));
            return new PaymentProviderPayment(
                    String.valueOf(payment.getId()), payment.getExternalReference(), payment.getStatus());
        } catch (MPApiException | MPException | NumberFormatException exception) {
            throw new ExternalPaymentException("Unable to get Mercado Pago payment.", exception);
        }
    }

    private PreferenceRequest toPreferenceRequest(CheckoutPreferenceRequest request) {
        PreferenceItemRequest item = PreferenceItemRequest.builder()
                .title(ITEM_TITLE)
                .quantity(request.quantity())
                .unitPrice(request.unitPrice())
                .build();

        PreferenceBackUrlsRequest backUrls = PreferenceBackUrlsRequest.builder()
                .success(appProperties.mercadoPago().successUrl())
                .failure(appProperties.mercadoPago().failureUrl())
                .pending(appProperties.mercadoPago().pendingUrl())
                .build();

        PreferencePayerRequest payer =
                PreferencePayerRequest.builder().email(request.email()).build();

        return PreferenceRequest.builder()
                .items(List.of(item))
                .payer(payer)
                .backUrls(backUrls)
                .notificationUrl(appProperties.mercadoPago().webhookUrl())
                .externalReference(request.externalReference())
                .build();
    }
}
