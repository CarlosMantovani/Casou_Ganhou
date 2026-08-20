package com.weddingraffle.rifa.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.weddingraffle.rifa.dto.PaymentStatusResponse;
import com.weddingraffle.rifa.dto.TransactionCreateRequest;
import com.weddingraffle.rifa.dto.TransactionCreateResponse;
import com.weddingraffle.rifa.dto.TransactionQuoteRequest;
import com.weddingraffle.rifa.dto.TransactionQuoteResponse;
import com.weddingraffle.rifa.dto.TransactionRecoveryRequest;
import com.weddingraffle.rifa.entity.ParticipantFlag;
import com.weddingraffle.rifa.entity.PaymentMethod;
import com.weddingraffle.rifa.entity.PaymentStatus;
import com.weddingraffle.rifa.entity.Transaction;
import com.weddingraffle.rifa.exception.InvalidRaffleStateException;
import com.weddingraffle.rifa.integration.CheckoutPreferenceRequest;
import com.weddingraffle.rifa.integration.CheckoutPreferenceResponse;
import com.weddingraffle.rifa.integration.PaymentProviderClient;
import com.weddingraffle.rifa.integration.PaymentProviderPayment;
import com.weddingraffle.rifa.repository.TransactionRepository;
import com.weddingraffle.rifa.service.LuckyNumberService;
import com.weddingraffle.rifa.service.ParticipantFlagService;
import com.weddingraffle.rifa.service.RaffleConfigService;
import com.weddingraffle.rifa.service.RecoveryCodeService;
import java.math.BigDecimal;
import java.util.List;
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

    @Mock
    private LuckyNumberService luckyNumberService;

    @Mock
    private ParticipantFlagService participantFlagService;

    @Mock
    private RaffleConfigService raffleConfigService;

    @Mock
    private RecoveryCodeService recoveryCodeService;

    @Test
    void calculatesQuoteFromConfiguredUnitPrice() {
        TransactionServiceImpl transactionService = transactionService();
        when(raffleConfigService.getCurrentUnitPrice()).thenReturn(new BigDecimal("10.00"));

        TransactionQuoteResponse response =
                transactionService.quote(new TransactionQuoteRequest("Guest User", "(11) 99999-9999", 3));

        assertThat(response.name()).isEqualTo("Guest User");
        assertThat(response.phone()).isEqualTo("11999999999");
        assertThat(response.quantity()).isEqualTo(3);
        assertThat(response.unitPrice()).isEqualByComparingTo("10.00");
        assertThat(response.totalAmount()).isEqualByComparingTo("30.00");
    }

    @Test
    void quoteRejectsPurchaseAfterDrawIsClosed() {
        TransactionServiceImpl transactionService = transactionService();
        when(raffleConfigService.isDrawClosed()).thenReturn(true);

        assertThatThrownBy(
                        () -> transactionService.quote(new TransactionQuoteRequest("Guest User", "(11) 99999-9999", 3)))
                .isInstanceOf(InvalidRaffleStateException.class)
                .hasMessage("Draw is closed. No more numbers can be purchased.");
    }

    @Test
    void createsPendingTransactionWithCheckoutPreference() {
        TransactionServiceImpl transactionService = transactionService();
        when(raffleConfigService.getCurrentUnitPrice()).thenReturn(new BigDecimal("10.00"));
        when(paymentProviderClient.createPreference(any()))
                .thenReturn(new CheckoutPreferenceResponse("preference-123", "https://checkout.example.com"));
        when(participantFlagService.resolveForPhone("11999999999"))
                .thenReturn(new ParticipantFlag("BRAZIL", "Brasil", "🇧🇷"));
        when(recoveryCodeService.resolveForPhone("11999999999")).thenReturn("4821");

        TransactionCreateResponse response =
                transactionService.create(new TransactionCreateRequest("Guest User", "(11) 99999-9999", 2));

        assertThat(response.externalReference()).isNotBlank();
        assertThat(response.recoveryCode()).isEqualTo("4821");
        assertThat(response.preferenceId()).isEqualTo("preference-123");
        assertThat(response.checkoutUrl()).isEqualTo("https://checkout.example.com");

        ArgumentCaptor<CheckoutPreferenceRequest> preferenceCaptor =
                ArgumentCaptor.forClass(CheckoutPreferenceRequest.class);
        verify(paymentProviderClient).createPreference(preferenceCaptor.capture());
        assertThat(preferenceCaptor.getValue().name()).isEqualTo("Guest User");
        assertThat(preferenceCaptor.getValue().email()).isNull();
        assertThat(preferenceCaptor.getValue().quantity()).isEqualTo(2);
        assertThat(preferenceCaptor.getValue().unitPrice()).isEqualByComparingTo("10.00");
        assertThat(preferenceCaptor.getValue().externalReference()).isEqualTo(response.externalReference());

        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(transactionCaptor.capture());
        assertThat(transactionCaptor.getValue().getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(transactionCaptor.getValue().getName()).isEqualTo("Guest User");
        assertThat(transactionCaptor.getValue().getPhone()).isEqualTo("11999999999");
        assertThat(transactionCaptor.getValue().getEmail()).isNull();
        assertThat(transactionCaptor.getValue().getQuantity()).isEqualTo(2);
        assertThat(transactionCaptor.getValue().getTotalAmount()).isEqualByComparingTo("20.00");
        assertThat(transactionCaptor.getValue().getExternalReference()).isEqualTo(response.externalReference());
        assertThat(transactionCaptor.getValue().getRecoveryCode()).isEqualTo("4821");
        assertThat(transactionCaptor.getValue().getMpPreferenceId()).isEqualTo("preference-123");
        assertThat(transactionCaptor.getValue().getParticipantFlagCode()).isEqualTo("BRAZIL");
        assertThat(transactionCaptor.getValue().getParticipantFlagName()).isEqualTo("Brasil");
        assertThat(transactionCaptor.getValue().getParticipantFlagEmoji()).isEqualTo("🇧🇷");
    }

    @Test
    void createRejectsPurchaseAfterDrawIsClosed() {
        TransactionServiceImpl transactionService = transactionService();
        when(raffleConfigService.isDrawClosed()).thenReturn(true);

        assertThatThrownBy(() ->
                        transactionService.create(new TransactionCreateRequest("Guest User", "(11) 99999-9999", 2)))
                .isInstanceOf(InvalidRaffleStateException.class)
                .hasMessage("Draw is closed. No more numbers can be purchased.");
        verify(paymentProviderClient, never()).createPreference(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void processesApprovedPaymentNotification() {
        TransactionServiceImpl transactionService = transactionService();
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
    }

    @Test
    void processesRejectedPaymentNotificationWithoutGeneratingLuckyNumbers() {
        TransactionServiceImpl transactionService = transactionService();
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
    }

    @Test
    void ignoresDuplicateNotificationForApprovedTransaction() {
        TransactionServiceImpl transactionService = transactionService();
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
    }

    @Test
    void updatesApprovedTransactionWhenPaymentIsRefundedWithoutGeneratingLuckyNumbers() {
        TransactionServiceImpl transactionService = transactionService();
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
    }

    @Test
    void mapsChargebackAndMediationStatusesWithoutGeneratingLuckyNumbers() {
        TransactionServiceImpl transactionService = transactionService();
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
    }

    @Test
    void returnsCurrentStatusWithLuckyNumbers() {
        TransactionServiceImpl transactionService = transactionService();
        Transaction transaction = new Transaction(
                "guest@example.com", 2, new BigDecimal("20.00"), PaymentStatus.APPROVED, "external-reference-123");
        transaction.assignRecoveryCode("4821");
        when(transactionRepository.findByExternalReference("external-reference-123"))
                .thenReturn(Optional.of(transaction));
        when(luckyNumberService.findNumbers("external-reference-123")).thenReturn(List.of("00001", "00002"));
        when(luckyNumberService.findPreviousApprovedNumbers("0000000000", "external-reference-123"))
                .thenReturn(List.of("00090", "00091"));

        var response = transactionService.getStatus("external-reference-123");

        assertThat(response.externalReference()).isEqualTo("external-reference-123");
        assertThat(response.recoveryCode()).isEqualTo("4821");
        assertThat(response.status()).isEqualTo(PaymentStatusResponse.APROVADO);
        assertThat(response.quantity()).isEqualTo(2);
        assertThat(response.totalAmount()).isEqualByComparingTo("20.00");
        assertThat(response.luckyNumbers()).containsExactly("00001", "00002");
        assertThat(response.previousLuckyNumbers()).containsExactly("00090", "00091");
        assertThat(response.totalLuckyNumbers()).isEqualTo(4);
    }

    @Test
    void statusFallbackGeneratesLuckyNumbersWhenPaymentBecomesApproved() {
        TransactionServiceImpl transactionService = transactionService();
        Transaction transaction = new Transaction(
                "guest@example.com", 2, new BigDecimal("20.00"), PaymentStatus.PENDING, "external-reference-123");
        transaction.markPayment(PaymentStatus.PENDING, "123");
        when(transactionRepository.findByExternalReference("external-reference-123"))
                .thenReturn(Optional.of(transaction));
        when(paymentProviderClient.getPayment("123"))
                .thenReturn(new PaymentProviderPayment("123", "external-reference-123", "approved"));
        when(luckyNumberService.findNumbers("external-reference-123")).thenReturn(List.of("00001", "00002"));
        when(luckyNumberService.findPreviousApprovedNumbers("0000000000", "external-reference-123"))
                .thenReturn(List.of());

        var response = transactionService.getStatus("external-reference-123");

        assertThat(response.status()).isEqualTo(PaymentStatusResponse.APROVADO);
        verify(luckyNumberService).generateFor(transaction);
        verify(transactionRepository).save(transaction);
    }

    @Test
    void recoversTransactionByPhoneAndRecoveryCode() {
        TransactionServiceImpl transactionService = transactionService();
        Transaction transaction = new Transaction(
                "Guest User",
                "11999999999",
                null,
                1,
                new BigDecimal("10.00"),
                PaymentStatus.APPROVED,
                PaymentMethod.MERCADO_PAGO,
                "external-reference-123");
        transaction.assignRecoveryCode("4821");
        when(transactionRepository.findByPhoneAndRecoveryCodeOrderByCreatedAtDesc("11999999999", "4821"))
                .thenReturn(List.of(transaction));
        when(luckyNumberService.findApprovedNumbersByPhone("11999999999"))
                .thenReturn(List.of("00042", "00090", "00091"));

        var response = transactionService.recover(new TransactionRecoveryRequest("(11) 99999-9999", "4821"));

        assertThat(response.externalReference()).isEqualTo("external-reference-123");
        assertThat(response.recoveryCode()).isEqualTo("4821");
        assertThat(response.status()).isEqualTo(PaymentStatusResponse.APROVADO);
        assertThat(response.luckyNumbers()).containsExactly("00042", "00090", "00091");
        assertThat(response.previousLuckyNumbers()).isEmpty();
        assertThat(response.totalLuckyNumbers()).isEqualTo(3);
    }

    private TransactionServiceImpl transactionService() {
        return new TransactionServiceImpl(
                raffleConfigService,
                transactionRepository,
                paymentProviderClient,
                luckyNumberService,
                participantFlagService,
                recoveryCodeService);
    }
}
