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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class MercadoPagoClient implements PaymentProviderClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(MercadoPagoClient.class);
    private static final String ITEM_TITLE = "Lucky number";
    private static final int ERROR_RESPONSE_LOG_LIMIT = 1_000;

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
            maxAttemptsExpression = "${app.mercado-pago.retry.max-attempts}",
            backoff =
                    @Backoff(
                            delayExpression = "${app.mercado-pago.retry.delay-millis}",
                            multiplierExpression = "${app.mercado-pago.retry.multiplier}"))
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
            maxAttemptsExpression = "${app.mercado-pago.retry.max-attempts}",
            backoff =
                    @Backoff(
                            delayExpression = "${app.mercado-pago.retry.delay-millis}",
                            multiplierExpression = "${app.mercado-pago.retry.multiplier}"))
    public PaymentProviderPayment getPayment(String paymentId) {
        try {
            LOGGER.info("Fetching Mercado Pago payment paymentId={}", paymentId);
            Payment payment = paymentClient.get(Long.valueOf(paymentId));
            LOGGER.info(
                    "Fetched Mercado Pago payment paymentId={} status={} statusDetail={} externalReference={} paymentMethodId={} dateCreated={} dateApproved={} dateLastUpdated={}",
                    payment.getId(),
                    payment.getStatus(),
                    payment.getStatusDetail(),
                    payment.getExternalReference(),
                    payment.getPaymentMethodId(),
                    payment.getDateCreated(),
                    payment.getDateApproved(),
                    payment.getDateLastUpdated());
            return new PaymentProviderPayment(
                    String.valueOf(payment.getId()), payment.getExternalReference(), payment.getStatus());
        } catch (MPApiException exception) {
            LOGGER.warn(
                    "Failed to fetch Mercado Pago payment paymentId={} statusCode={} responseBody={}",
                    paymentId,
                    exception.getStatusCode(),
                    truncated(
                            exception.getApiResponse() == null
                                    ? null
                                    : exception.getApiResponse().getContent()));
            throw new ExternalPaymentException("Unable to get Mercado Pago payment.", exception);
        } catch (MPException | NumberFormatException exception) {
            throw new ExternalPaymentException("Unable to get Mercado Pago payment.", exception);
        }
    }

    private static String truncated(String value) {
        if (!StringUtils.hasText(value) || value.length() <= ERROR_RESPONSE_LOG_LIMIT) {
            return value;
        }
        return value.substring(0, ERROR_RESPONSE_LOG_LIMIT) + "...";
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
