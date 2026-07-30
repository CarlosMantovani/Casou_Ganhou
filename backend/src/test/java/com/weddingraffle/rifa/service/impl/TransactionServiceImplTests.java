package com.weddingraffle.rifa.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.weddingraffle.rifa.config.AppProperties;
import com.weddingraffle.rifa.dto.TransactionCreateRequest;
import com.weddingraffle.rifa.dto.TransactionCreateResponse;
import com.weddingraffle.rifa.dto.TransactionQuoteRequest;
import com.weddingraffle.rifa.dto.TransactionQuoteResponse;
import com.weddingraffle.rifa.entity.PaymentStatus;
import com.weddingraffle.rifa.entity.Transaction;
import com.weddingraffle.rifa.integration.CheckoutPreferenceRequest;
import com.weddingraffle.rifa.integration.CheckoutPreferenceResponse;
import com.weddingraffle.rifa.integration.PaymentProviderClient;
import com.weddingraffle.rifa.integration.PaymentProviderPayment;
import com.weddingraffle.rifa.repository.TransactionRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTests {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private PaymentProviderClient paymentProviderClient;

    @Test
    void calculatesQuoteFromConfiguredUnitPrice() {
        TransactionServiceImpl transactionService =
                new TransactionServiceImpl(appProperties(), transactionRepository, paymentProviderClient);

        TransactionQuoteResponse response =
                transactionService.quote(new TransactionQuoteRequest("guest@example.com", 3));

        assertThat(response.email()).isEqualTo("guest@example.com");
        assertThat(response.quantity()).isEqualTo(3);
        assertThat(response.unitPrice()).isEqualByComparingTo("10.00");
        assertThat(response.totalAmount()).isEqualByComparingTo("30.00");
    }

    @Test
    void createsPendingTransactionWithCheckoutPreference() {
        TransactionServiceImpl transactionService =
                new TransactionServiceImpl(appProperties(), transactionRepository, paymentProviderClient);
        when(paymentProviderClient.createPreference(any()))
                .thenReturn(new CheckoutPreferenceResponse("preference-123", "https://checkout.example.com"));

        TransactionCreateResponse response =
                transactionService.create(new TransactionCreateRequest("guest@example.com", 2));

        assertThat(response.externalReference()).isNotBlank();
        assertThat(response.preferenceId()).isEqualTo("preference-123");
        assertThat(response.checkoutUrl()).isEqualTo("https://checkout.example.com");

        ArgumentCaptor<CheckoutPreferenceRequest> preferenceCaptor =
                ArgumentCaptor.forClass(CheckoutPreferenceRequest.class);
        verify(paymentProviderClient).createPreference(preferenceCaptor.capture());
        assertThat(preferenceCaptor.getValue().email()).isEqualTo("guest@example.com");
        assertThat(preferenceCaptor.getValue().quantity()).isEqualTo(2);
        assertThat(preferenceCaptor.getValue().unitPrice()).isEqualByComparingTo("10.00");
        assertThat(preferenceCaptor.getValue().externalReference()).isEqualTo(response.externalReference());

        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(transactionCaptor.capture());
        assertThat(transactionCaptor.getValue().getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(transactionCaptor.getValue().getEmail()).isEqualTo("guest@example.com");
        assertThat(transactionCaptor.getValue().getQuantity()).isEqualTo(2);
        assertThat(transactionCaptor.getValue().getTotalAmount()).isEqualByComparingTo("20.00");
        assertThat(transactionCaptor.getValue().getExternalReference()).isEqualTo(response.externalReference());
        assertThat(transactionCaptor.getValue().getMpPreferenceId()).isEqualTo("preference-123");
    }

    @Test
    void processesApprovedPaymentNotification() {
        TransactionServiceImpl transactionService =
                new TransactionServiceImpl(appProperties(), transactionRepository, paymentProviderClient);
        Transaction transaction = new Transaction(
                "guest@example.com", 2, new BigDecimal("20.00"), PaymentStatus.PENDING, "external-reference-123");
        when(paymentProviderClient.getPayment("123"))
                .thenReturn(new PaymentProviderPayment("123", "external-reference-123", "approved"));
        when(transactionRepository.findByExternalReference("external-reference-123"))
                .thenReturn(Optional.of(transaction));

        transactionService.processPaymentNotification("123");

        assertThat(transaction.getStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(transaction.getMpPaymentId()).isEqualTo("123");
        verify(transactionRepository).save(transaction);
    }

    @Test
    void ignoresDuplicateNotificationForApprovedTransaction() {
        TransactionServiceImpl transactionService =
                new TransactionServiceImpl(appProperties(), transactionRepository, paymentProviderClient);
        Transaction transaction = new Transaction(
                "guest@example.com", 2, new BigDecimal("20.00"), PaymentStatus.APPROVED, "external-reference-123");
        when(paymentProviderClient.getPayment("123"))
                .thenReturn(new PaymentProviderPayment("123", "external-reference-123", "approved"));
        when(transactionRepository.findByExternalReference("external-reference-123"))
                .thenReturn(Optional.of(transaction));

        transactionService.processPaymentNotification("123");

        verify(transactionRepository, never()).save(transaction);
    }

    @Test
    void returnsCurrentStatusWithEmptyLuckyNumbers() {
        TransactionServiceImpl transactionService =
                new TransactionServiceImpl(appProperties(), transactionRepository, paymentProviderClient);
        Transaction transaction = new Transaction(
                "guest@example.com", 2, new BigDecimal("20.00"), PaymentStatus.REJECTED, "external-reference-123");
        when(transactionRepository.findByExternalReference("external-reference-123"))
                .thenReturn(Optional.of(transaction));

        var response = transactionService.getStatus("external-reference-123");

        assertThat(response.externalReference()).isEqualTo("external-reference-123");
        assertThat(response.status()).isEqualTo(PaymentStatus.REJECTED);
        assertThat(response.quantity()).isEqualTo(2);
        assertThat(response.totalAmount()).isEqualByComparingTo("20.00");
        assertThat(response.luckyNumbers()).isEmpty();
    }

    private static AppProperties appProperties() {
        return new AppProperties(
                "http://localhost:5173",
                new AppProperties.Jwt("01234567890123456789012345678901", 3600, "raffle-api-test"),
                new AppProperties.Raffle(new BigDecimal("10.00"), "00000", "99999"),
                new AppProperties.MercadoPago(
                        "token",
                        "http://localhost:8080/payments/webhook",
                        "",
                        "http://localhost:5173/payment-return/success",
                        "http://localhost:5173/payment-return/failure",
                        "http://localhost:5173/payment-return/pending",
                        new AppProperties.Retry(3, 500, 2)),
                new AppProperties.Mail("no-reply@example.com"));
    }
}
