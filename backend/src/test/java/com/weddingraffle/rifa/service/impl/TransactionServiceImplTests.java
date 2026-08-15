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
import com.weddingraffle.rifa.entity.ParticipantFlag;
import com.weddingraffle.rifa.entity.PaymentStatus;
import com.weddingraffle.rifa.entity.Transaction;
import com.weddingraffle.rifa.integration.CheckoutPreferenceRequest;
import com.weddingraffle.rifa.integration.CheckoutPreferenceResponse;
import com.weddingraffle.rifa.integration.PaymentProviderClient;
import com.weddingraffle.rifa.integration.PaymentProviderPayment;
import com.weddingraffle.rifa.repository.TransactionRepository;
import com.weddingraffle.rifa.service.LuckyNumberService;
import com.weddingraffle.rifa.service.ParticipantFlagService;
import com.weddingraffle.rifa.service.PaymentApprovedEvent;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTests {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private PaymentProviderClient paymentProviderClient;

    @Mock
    private LuckyNumberService luckyNumberService;

    @Mock
    private ParticipantFlagService participantFlagService;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Test
    void calculatesQuoteFromConfiguredUnitPrice() {
        TransactionServiceImpl transactionService = new TransactionServiceImpl(
                appProperties(),
                transactionRepository,
                paymentProviderClient,
                luckyNumberService,
                participantFlagService,
                applicationEventPublisher);

        TransactionQuoteResponse response = transactionService.quote(
                new TransactionQuoteRequest("Guest User", "(11) 99999-9999", "guest@example.com", 3));

        assertThat(response.name()).isEqualTo("Guest User");
        assertThat(response.phone()).isEqualTo("11999999999");
        assertThat(response.email()).isEqualTo("guest@example.com");
        assertThat(response.quantity()).isEqualTo(3);
        assertThat(response.unitPrice()).isEqualByComparingTo("10.00");
        assertThat(response.totalAmount()).isEqualByComparingTo("30.00");
    }

    @Test
    void createsPendingTransactionWithCheckoutPreference() {
        TransactionServiceImpl transactionService = new TransactionServiceImpl(
                appProperties(),
                transactionRepository,
                paymentProviderClient,
                luckyNumberService,
                participantFlagService,
                applicationEventPublisher);
        when(paymentProviderClient.createPreference(any()))
                .thenReturn(new CheckoutPreferenceResponse("preference-123", "https://checkout.example.com"));
        when(participantFlagService.resolveForPhone("11999999999"))
                .thenReturn(new ParticipantFlag("BRAZIL", "Brasil", "🇧🇷"));

        TransactionCreateResponse response = transactionService.create(
                new TransactionCreateRequest("Guest User", "(11) 99999-9999", "guest@example.com", 2));

        assertThat(response.externalReference()).isNotBlank();
        assertThat(response.preferenceId()).isEqualTo("preference-123");
        assertThat(response.checkoutUrl()).isEqualTo("https://checkout.example.com");

        ArgumentCaptor<CheckoutPreferenceRequest> preferenceCaptor =
                ArgumentCaptor.forClass(CheckoutPreferenceRequest.class);
        verify(paymentProviderClient).createPreference(preferenceCaptor.capture());
        assertThat(preferenceCaptor.getValue().name()).isEqualTo("Guest User");
        assertThat(preferenceCaptor.getValue().email()).isEqualTo("guest@example.com");
        assertThat(preferenceCaptor.getValue().quantity()).isEqualTo(2);
        assertThat(preferenceCaptor.getValue().unitPrice()).isEqualByComparingTo("10.00");
        assertThat(preferenceCaptor.getValue().externalReference()).isEqualTo(response.externalReference());

        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(transactionCaptor.capture());
        assertThat(transactionCaptor.getValue().getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(transactionCaptor.getValue().getName()).isEqualTo("Guest User");
        assertThat(transactionCaptor.getValue().getPhone()).isEqualTo("11999999999");
        assertThat(transactionCaptor.getValue().getEmail()).isEqualTo("guest@example.com");
        assertThat(transactionCaptor.getValue().getQuantity()).isEqualTo(2);
        assertThat(transactionCaptor.getValue().getTotalAmount()).isEqualByComparingTo("20.00");
        assertThat(transactionCaptor.getValue().getExternalReference()).isEqualTo(response.externalReference());
        assertThat(transactionCaptor.getValue().getMpPreferenceId()).isEqualTo("preference-123");
        assertThat(transactionCaptor.getValue().getParticipantFlagCode()).isEqualTo("BRAZIL");
        assertThat(transactionCaptor.getValue().getParticipantFlagName()).isEqualTo("Brasil");
        assertThat(transactionCaptor.getValue().getParticipantFlagEmoji()).isEqualTo("🇧🇷");
    }

    @Test
    void processesApprovedPaymentNotification() {
        TransactionServiceImpl transactionService = new TransactionServiceImpl(
                appProperties(),
                transactionRepository,
                paymentProviderClient,
                luckyNumberService,
                participantFlagService,
                applicationEventPublisher);
        Transaction transaction = new Transaction(
                "guest@example.com", 2, new BigDecimal("20.00"), PaymentStatus.PENDING, "external-reference-123");
        when(paymentProviderClient.getPayment("123"))
                .thenReturn(new PaymentProviderPayment("123", "external-reference-123", "approved"));
        when(transactionRepository.findByExternalReference("external-reference-123"))
                .thenReturn(Optional.of(transaction));

        transactionService.processPaymentNotification("123");

        assertThat(transaction.getStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(transaction.getMpPaymentId()).isEqualTo("123");
        verify(luckyNumberService).generateFor(transaction);
        verify(transactionRepository).save(transaction);
        ArgumentCaptor<PaymentApprovedEvent> eventCaptor = ArgumentCaptor.forClass(PaymentApprovedEvent.class);
        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().externalReference()).isEqualTo("external-reference-123");
    }

    @Test
    void processesRejectedPaymentNotificationWithoutGeneratingLuckyNumbers() {
        TransactionServiceImpl transactionService = new TransactionServiceImpl(
                appProperties(),
                transactionRepository,
                paymentProviderClient,
                luckyNumberService,
                participantFlagService,
                applicationEventPublisher);
        Transaction transaction = new Transaction(
                "guest@example.com", 2, new BigDecimal("20.00"), PaymentStatus.PENDING, "external-reference-123");
        when(paymentProviderClient.getPayment("123"))
                .thenReturn(new PaymentProviderPayment("123", "external-reference-123", "rejected"));
        when(transactionRepository.findByExternalReference("external-reference-123"))
                .thenReturn(Optional.of(transaction));

        transactionService.processPaymentNotification("123");

        assertThat(transaction.getStatus()).isEqualTo(PaymentStatus.REJECTED);
        verify(luckyNumberService, never()).generateFor(transaction);
        verify(transactionRepository).save(transaction);
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void ignoresDuplicateNotificationForApprovedTransaction() {
        TransactionServiceImpl transactionService = new TransactionServiceImpl(
                appProperties(),
                transactionRepository,
                paymentProviderClient,
                luckyNumberService,
                participantFlagService,
                applicationEventPublisher);
        Transaction transaction = new Transaction(
                "guest@example.com", 2, new BigDecimal("20.00"), PaymentStatus.APPROVED, "external-reference-123");
        transaction.markPayment(PaymentStatus.APPROVED, "123");
        when(paymentProviderClient.getPayment("123"))
                .thenReturn(new PaymentProviderPayment("123", "external-reference-123", "approved"));
        when(transactionRepository.findByExternalReference("external-reference-123"))
                .thenReturn(Optional.of(transaction));

        transactionService.processPaymentNotification("123");

        verify(luckyNumberService, never()).generateFor(transaction);
        verify(transactionRepository, never()).save(transaction);
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void updatesApprovedTransactionWhenPaymentIsRefundedWithoutGeneratingLuckyNumbers() {
        TransactionServiceImpl transactionService = new TransactionServiceImpl(
                appProperties(),
                transactionRepository,
                paymentProviderClient,
                luckyNumberService,
                participantFlagService,
                applicationEventPublisher);
        Transaction transaction = new Transaction(
                "guest@example.com", 2, new BigDecimal("20.00"), PaymentStatus.APPROVED, "external-reference-123");
        transaction.markPayment(PaymentStatus.APPROVED, "123");
        when(paymentProviderClient.getPayment("123"))
                .thenReturn(new PaymentProviderPayment("123", "external-reference-123", "refunded"));
        when(transactionRepository.findByExternalReference("external-reference-123"))
                .thenReturn(Optional.of(transaction));

        transactionService.processPaymentNotification("123");

        assertThat(transaction.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        verify(luckyNumberService, never()).generateFor(transaction);
        verify(transactionRepository).save(transaction);
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void mapsChargebackAndMediationStatusesWithoutGeneratingLuckyNumbers() {
        TransactionServiceImpl transactionService = new TransactionServiceImpl(
                appProperties(),
                transactionRepository,
                paymentProviderClient,
                luckyNumberService,
                participantFlagService,
                applicationEventPublisher);
        Transaction chargebackTransaction = new Transaction(
                "guest@example.com", 2, new BigDecimal("20.00"), PaymentStatus.APPROVED, "external-reference-123");
        chargebackTransaction.markPayment(PaymentStatus.APPROVED, "123");
        Transaction mediationTransaction = new Transaction(
                "guest@example.com", 2, new BigDecimal("20.00"), PaymentStatus.APPROVED, "external-reference-456");
        mediationTransaction.markPayment(PaymentStatus.APPROVED, "456");
        when(paymentProviderClient.getPayment("123"))
                .thenReturn(new PaymentProviderPayment("123", "external-reference-123", "charged_back"));
        when(paymentProviderClient.getPayment("456"))
                .thenReturn(new PaymentProviderPayment("456", "external-reference-456", "in_mediation"));
        when(transactionRepository.findByExternalReference("external-reference-123"))
                .thenReturn(Optional.of(chargebackTransaction));
        when(transactionRepository.findByExternalReference("external-reference-456"))
                .thenReturn(Optional.of(mediationTransaction));

        transactionService.processPaymentNotification("123");
        transactionService.processPaymentNotification("456");

        assertThat(chargebackTransaction.getStatus()).isEqualTo(PaymentStatus.CHARGED_BACK);
        assertThat(mediationTransaction.getStatus()).isEqualTo(PaymentStatus.IN_MEDIATION);
        verify(luckyNumberService, never()).generateFor(any());
        verify(transactionRepository).save(chargebackTransaction);
        verify(transactionRepository).save(mediationTransaction);
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void returnsCurrentStatusWithLuckyNumbers() {
        TransactionServiceImpl transactionService = new TransactionServiceImpl(
                appProperties(),
                transactionRepository,
                paymentProviderClient,
                luckyNumberService,
                participantFlagService,
                applicationEventPublisher);
        Transaction transaction = new Transaction(
                "guest@example.com", 2, new BigDecimal("20.00"), PaymentStatus.APPROVED, "external-reference-123");
        when(transactionRepository.findByExternalReference("external-reference-123"))
                .thenReturn(Optional.of(transaction));
        when(luckyNumberService.findNumbers("external-reference-123")).thenReturn(List.of("00001", "00002"));

        var response = transactionService.getStatus("external-reference-123");

        assertThat(response.externalReference()).isEqualTo("external-reference-123");
        assertThat(response.emailProvided()).isTrue();
        assertThat(response.status()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(response.quantity()).isEqualTo(2);
        assertThat(response.totalAmount()).isEqualByComparingTo("20.00");
        assertThat(response.luckyNumbers()).containsExactly("00001", "00002");
    }

    @Test
    void statusFallbackPublishesEventWhenPaymentBecomesApproved() {
        TransactionServiceImpl transactionService = new TransactionServiceImpl(
                appProperties(),
                transactionRepository,
                paymentProviderClient,
                luckyNumberService,
                participantFlagService,
                applicationEventPublisher);
        Transaction transaction = new Transaction(
                "guest@example.com", 2, new BigDecimal("20.00"), PaymentStatus.PENDING, "external-reference-123");
        transaction.markPayment(PaymentStatus.PENDING, "123");
        when(transactionRepository.findByExternalReference("external-reference-123"))
                .thenReturn(Optional.of(transaction));
        when(paymentProviderClient.getPayment("123"))
                .thenReturn(new PaymentProviderPayment("123", "external-reference-123", "approved"));
        when(luckyNumberService.findNumbers("external-reference-123")).thenReturn(List.of("00001", "00002"));

        var response = transactionService.getStatus("external-reference-123");

        assertThat(response.status()).isEqualTo(PaymentStatus.APPROVED);
        verify(luckyNumberService).generateFor(transaction);
        verify(transactionRepository).save(transaction);
        verify(applicationEventPublisher).publishEvent(any(PaymentApprovedEvent.class));
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
