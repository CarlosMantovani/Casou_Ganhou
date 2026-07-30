package com.weddingraffle.rifa.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.weddingraffle.rifa.config.AppProperties;
import com.weddingraffle.rifa.entity.PaymentStatus;
import com.weddingraffle.rifa.entity.Transaction;
import com.weddingraffle.rifa.repository.TransactionRepository;
import com.weddingraffle.rifa.service.LuckyNumberService;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailParseException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(MockitoExtension.class)
class ConfirmationEmailServiceImplTests {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private LuckyNumberService luckyNumberService;

    @Mock
    private JavaMailSender javaMailSender;

    @Test
    void sendsConfirmationEmailForApprovedTransactionWithLuckyNumbers() {
        ConfirmationEmailServiceImpl service = new ConfirmationEmailServiceImpl(
                appProperties(), transactionRepository, luckyNumberService, javaMailSender);
        Transaction transaction = approvedTransaction();
        when(transactionRepository.findByExternalReference("external-reference-123"))
                .thenReturn(Optional.of(transaction));
        when(luckyNumberService.findNumbers("external-reference-123")).thenReturn(List.of("00001", "00002"));

        service.sendForApprovedTransaction("external-reference-123");

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender).send(messageCaptor.capture());
        assertThat(messageCaptor.getValue().getFrom()).isEqualTo("no-reply@example.com");
        assertThat(messageCaptor.getValue().getTo()).containsExactly("guest@example.com");
        assertThat(messageCaptor.getValue().getSubject()).isEqualTo("Seus números da sorte");
        assertThat(messageCaptor.getValue().getText()).contains("00001", "00002", "Boa sorte no sorteio!");
        assertThat(transaction.getConfirmationEmailSentAt()).isNotNull();
        assertThat(transaction.getConfirmationEmailFailedAt()).isNull();
        assertThat(transaction.getConfirmationEmailLastError()).isNull();
        verify(transactionRepository).save(transaction);
    }

    @Test
    void doesNotSendWhenConfirmationEmailWasAlreadySent() {
        ConfirmationEmailServiceImpl service = new ConfirmationEmailServiceImpl(
                appProperties(), transactionRepository, luckyNumberService, javaMailSender);
        Transaction transaction = approvedTransaction();
        transaction.markConfirmationEmailSent(OffsetDateTime.now());
        when(transactionRepository.findByExternalReference("external-reference-123"))
                .thenReturn(Optional.of(transaction));

        service.sendForApprovedTransaction("external-reference-123");

        verify(javaMailSender, never()).send(any(SimpleMailMessage.class));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void doesNotSendWhenTransactionIsNotApproved() {
        ConfirmationEmailServiceImpl service = new ConfirmationEmailServiceImpl(
                appProperties(), transactionRepository, luckyNumberService, javaMailSender);
        Transaction transaction = new Transaction(
                "guest@example.com", 2, new BigDecimal("20.00"), PaymentStatus.PENDING, "external-reference-123");
        when(transactionRepository.findByExternalReference("external-reference-123"))
                .thenReturn(Optional.of(transaction));

        service.sendForApprovedTransaction("external-reference-123");

        verify(javaMailSender, never()).send(any(SimpleMailMessage.class));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void recordsFailureWhenApprovedTransactionHasNoLuckyNumbers() {
        ConfirmationEmailServiceImpl service = new ConfirmationEmailServiceImpl(
                appProperties(), transactionRepository, luckyNumberService, javaMailSender);
        Transaction transaction = approvedTransaction();
        when(transactionRepository.findByExternalReference("external-reference-123"))
                .thenReturn(Optional.of(transaction));
        when(luckyNumberService.findNumbers("external-reference-123")).thenReturn(List.of());

        service.sendForApprovedTransaction("external-reference-123");

        verify(javaMailSender, never()).send(any(SimpleMailMessage.class));
        assertThat(transaction.getConfirmationEmailSentAt()).isNull();
        assertThat(transaction.getConfirmationEmailFailedAt()).isNotNull();
        assertThat(transaction.getConfirmationEmailLastError()).isEqualTo("Approved transaction has no lucky numbers.");
        verify(transactionRepository).save(transaction);
    }

    @Test
    void recordsFailureWithoutThrowingWhenMailSenderFails() {
        ConfirmationEmailServiceImpl service = new ConfirmationEmailServiceImpl(
                appProperties(), transactionRepository, luckyNumberService, javaMailSender);
        Transaction transaction = approvedTransaction();
        when(transactionRepository.findByExternalReference("external-reference-123"))
                .thenReturn(Optional.of(transaction));
        when(luckyNumberService.findNumbers("external-reference-123")).thenReturn(List.of("00001"));
        org.mockito.Mockito.doThrow(new MailParseException("smtp down"))
                .when(javaMailSender)
                .send(any(SimpleMailMessage.class));

        service.sendForApprovedTransaction("external-reference-123");

        assertThat(transaction.getConfirmationEmailSentAt()).isNull();
        assertThat(transaction.getConfirmationEmailFailedAt()).isNotNull();
        assertThat(transaction.getConfirmationEmailLastError()).contains("smtp down");
        verify(transactionRepository).save(transaction);
    }

    private static Transaction approvedTransaction() {
        return new Transaction(
                "guest@example.com", 2, new BigDecimal("20.00"), PaymentStatus.APPROVED, "external-reference-123");
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
