package com.weddingraffle.rifa.integration;

import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.preference.PreferenceBackUrlsRequest;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferencePayerRequest;
import com.mercadopago.client.preference.PreferencePaymentMethodsRequest;
import com.mercadopago.client.preference.PreferencePaymentTypeRequest;
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

    private static final String AUTO_RETURN_APPROVED = "approved";
    private static final String CURRENCY_ID_BRL = "BRL";
    private static final String ITEM_TITLE = "Lucky number";
    private static final String ATM_PAYMENT_TYPE = "atm";
    private static final String DIGITAL_CURRENCY_PAYMENT_TYPE = "digital_currency";
    private static final String PREPAID_CARD_PAYMENT_TYPE = "prepaid_card";
    private static final String TICKET_PAYMENT_TYPE = "ticket";

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
            maxAttemptsExpression = "${app.mercado-pago.retry.max-attempts:3}",
            backoff =
                    @Backoff(
                            delayExpression = "${app.mercado-pago.retry.delay-millis:500}",
                            multiplierExpression = "${app.mercado-pago.retry.multiplier:2}"))
    public CheckoutPreferenceResponse createPreference(CheckoutPreferenceRequest request) {
        try {
            Preference preference = preferenceClient.create(toPreferenceRequest(request));
            return new CheckoutPreferenceResponse(preference.getId(), preference.getInitPoint());
        } catch (MPApiException exception) {
            logMercadoPagoApiError("create preference", exception);
            throw new ExternalPaymentException("Unable to create Mercado Pago preference.", exception);
        } catch (MPException exception) {
            throw new ExternalPaymentException("Unable to create Mercado Pago preference.", exception);
        }
    }

    @Override
    @Retryable(
            retryFor = ExternalPaymentException.class,
            maxAttemptsExpression = "${app.mercado-pago.retry.max-attempts:3}",
            backoff =
                    @Backoff(
                            delayExpression = "${app.mercado-pago.retry.delay-millis:500}",
                            multiplierExpression = "${app.mercado-pago.retry.multiplier:2}"))
    public PaymentProviderPayment getPayment(String paymentId) {
        try {
            Payment payment = paymentClient.get(Long.valueOf(paymentId));
            return new PaymentProviderPayment(
                    String.valueOf(payment.getId()), payment.getExternalReference(), payment.getStatus());
        } catch (MPApiException exception) {
            logMercadoPagoApiError("get payment", exception);
            throw new ExternalPaymentException("Unable to get Mercado Pago payment.", exception);
        } catch (MPException | NumberFormatException exception) {
            throw new ExternalPaymentException("Unable to get Mercado Pago payment.", exception);
        }
    }

    PreferenceRequest toPreferenceRequest(CheckoutPreferenceRequest request) {
        PreferenceItemRequest item = PreferenceItemRequest.builder()
                .title(ITEM_TITLE)
                .currencyId(CURRENCY_ID_BRL)
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
                .autoReturn(AUTO_RETURN_APPROVED)
                .backUrls(backUrls)
                .paymentMethods(paymentMethods())
                .notificationUrl(appProperties.mercadoPago().webhookUrl())
                .externalReference(request.externalReference());

        if (payer != null) {
            builder.payer(payer);
        }

        return builder.build();
    }

    static PreferencePaymentMethodsRequest paymentMethods() {
        return PreferencePaymentMethodsRequest.builder()
                .excludedPaymentTypes(List.of(
                        excludedPaymentType(TICKET_PAYMENT_TYPE),
                        excludedPaymentType(DIGITAL_CURRENCY_PAYMENT_TYPE),
                        excludedPaymentType(ATM_PAYMENT_TYPE),
                        excludedPaymentType(PREPAID_CARD_PAYMENT_TYPE)))
                .build();
    }

    private static PreferencePaymentTypeRequest excludedPaymentType(String paymentType) {
        return PreferencePaymentTypeRequest.builder().id(paymentType).build();
    }

    private static void logMercadoPagoApiError(String operation, MPApiException exception) {
        String responseContent =
                exception.getApiResponse() != null ? exception.getApiResponse().getContent() : null;
        LOGGER.error(
                "Mercado Pago API error during {}. statusCode={}, responseBody={}",
                operation,
                exception.getStatusCode(),
                responseContent);
    }
}
