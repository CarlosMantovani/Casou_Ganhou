package com.weddingraffle.rifa.integration;

import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.merchantorder.MerchantOrderClient;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.preference.PreferenceBackUrlsRequest;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferencePayerRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.core.MPRequestOptions;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.merchantorder.MerchantOrder;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.preference.Preference;
import com.weddingraffle.rifa.config.AppProperties;
import com.weddingraffle.rifa.exception.ExternalPaymentException;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class MercadoPagoClient implements PaymentProviderClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(MercadoPagoClient.class);

    private static final String ITEM_TITLE = "Número(s) da sorte";

    private final AppProperties appProperties;
    private final PaymentClient paymentClient;
    private final MerchantOrderClient merchantOrderClient;
    private final PreferenceClient preferenceClient;

    @Autowired
    public MercadoPagoClient(AppProperties appProperties) {
        this(appProperties, new PaymentClient(), new PreferenceClient(), new MerchantOrderClient());
    }

    MercadoPagoClient(
            AppProperties appProperties,
            PaymentClient paymentClient,
            PreferenceClient preferenceClient,
            MerchantOrderClient merchantOrderClient) {
        this.appProperties = appProperties;
        MercadoPagoConfig.setAccessToken(appProperties.mercadoPago().accessToken());
        this.paymentClient = paymentClient;
        this.preferenceClient = preferenceClient;
        this.merchantOrderClient = merchantOrderClient;
    }

    @Override
    @Retryable(
            retryFor = ExternalPaymentException.class,
            maxAttemptsExpression = "${app.mercado-pago.retry.max-attempts}",
            backoff =
                    @Backoff(
                            delayExpression = "${app.mercado-pago.retry.delay-millis}",
                            multiplierExpression = "${app.mercado-pago.retry.multiplier}"))
    public CheckoutPreferenceResponse createPreference(CheckoutPreferenceRequest request, String idempotencyKey) {
        try {
            MPRequestOptions requestOptions = MPRequestOptions.builder()
                    .customHeaders(Map.of("X-Idempotency-Key", idempotencyKey))
                    .build();
            Preference preference = preferenceClient.create(toPreferenceRequest(request), requestOptions);
            return new CheckoutPreferenceResponse(
                    preference.getId(), preference.getInitPoint(), asString(preference.getCollectorId()));
        } catch (MPApiException | MPException exception) {
            throw new ExternalPaymentException("Unable to create Mercado Pago preference.", exception);
        }
    }

    @Override
    @Retryable(
            retryFor = ExternalPaymentException.class,
            maxAttemptsExpression = "${app.mercado-pago.retry.max-attempts}",
            backoff =
                    @Backoff(
                            delayExpression = "${app.mercado-pago.retry.delay-millis}",
                            multiplierExpression = "${app.mercado-pago.retry.multiplier}"))
    public PaymentProviderPayment getPayment(String paymentId) {
        LOGGER.info("Mercado Pago payment status request paymentId={}", paymentId);
        try {
            Payment payment = paymentClient.get(Long.valueOf(paymentId));
            MerchantOrder merchantOrder = getMerchantOrder(payment);
            LOGGER.info(
                    "Mercado Pago payment status response paymentId={} externalReference={} status={}",
                    payment.getId(),
                    payment.getExternalReference(),
                    payment.getStatus());
            return new PaymentProviderPayment(
                    asString(payment.getId()),
                    payment.getExternalReference(),
                    merchantOrder != null ? merchantOrder.getExternalReference() : null,
                    merchantOrder != null ? merchantOrder.getPreferenceId() : null,
                    asString(payment.getCollectorId()),
                    payment.getTransactionAmount(),
                    payment.getCurrencyId(),
                    payment.getStatus(),
                    payment.getStatusDetail(),
                    payment.getDateCreated(),
                    payment.getDateLastUpdated());
        } catch (MPApiException | MPException | NumberFormatException exception) {
            LOGGER.warn("Mercado Pago payment status request failed paymentId={}", paymentId, exception);
            throw new ExternalPaymentException("Unable to get Mercado Pago payment.", exception);
        }
    }

    private MerchantOrder getMerchantOrder(Payment payment) throws MPException, MPApiException {
        if (payment.getOrder() == null || payment.getOrder().getId() == null) {
            return null;
        }
        return merchantOrderClient.get(payment.getOrder().getId());
    }

    private static String asString(Long value) {
        return value != null ? String.valueOf(value) : null;
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

        PreferencePayerRequest payer = StringUtils.hasText(request.email())
                ? PreferencePayerRequest.builder().email(request.email()).build()
                : null;

        PreferenceRequest.PreferenceRequestBuilder builder = PreferenceRequest.builder()
                .items(List.of(item))
                .backUrls(backUrls)
                .notificationUrl(appProperties.mercadoPago().webhookUrl())
                .externalReference(request.externalReference());

        if (payer != null) {
            builder.payer(payer);
        }

        return builder.build();
    }
}
