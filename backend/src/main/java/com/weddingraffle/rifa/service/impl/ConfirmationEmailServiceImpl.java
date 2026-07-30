package com.weddingraffle.rifa.service.impl;

import com.weddingraffle.rifa.config.AppProperties;
import com.weddingraffle.rifa.entity.PaymentStatus;
import com.weddingraffle.rifa.entity.Transaction;
import com.weddingraffle.rifa.repository.TransactionRepository;
import com.weddingraffle.rifa.service.ConfirmationEmailService;
import com.weddingraffle.rifa.service.LuckyNumberService;
import java.time.OffsetDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ConfirmationEmailServiceImpl implements ConfirmationEmailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfirmationEmailServiceImpl.class);
    private static final String SUBJECT = "Seus números da sorte";

    private final AppProperties appProperties;
    private final TransactionRepository transactionRepository;
    private final LuckyNumberService luckyNumberService;
    private final JavaMailSender javaMailSender;

    public ConfirmationEmailServiceImpl(
            AppProperties appProperties,
            TransactionRepository transactionRepository,
            LuckyNumberService luckyNumberService,
            JavaMailSender javaMailSender) {
        this.appProperties = appProperties;
        this.transactionRepository = transactionRepository;
        this.luckyNumberService = luckyNumberService;
        this.javaMailSender = javaMailSender;
    }

    @Override
    @Transactional
    public void sendForApprovedTransaction(String externalReference) {
        Transaction transaction = transactionRepository
                .findByExternalReference(externalReference)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found."));

        if (transaction.getStatus() != PaymentStatus.APPROVED || transaction.getConfirmationEmailSentAt() != null) {
            return;
        }

        if (!StringUtils.hasText(transaction.getEmail())) {
            LOGGER.info(
                    "Skipped confirmation email for externalReference={} because email is empty.", externalReference);
            return;
        }

        List<String> luckyNumbers = luckyNumberService.findNumbers(externalReference);
        if (luckyNumbers.isEmpty()) {
            markFailure(transaction, "Approved transaction has no lucky numbers.");
            return;
        }

        try {
            javaMailSender.send(message(transaction, luckyNumbers));
            transaction.markConfirmationEmailSent(OffsetDateTime.now());
            transactionRepository.save(transaction);
            LOGGER.info("Sent confirmation email for externalReference={}", externalReference);
        } catch (MailException exception) {
            markFailure(transaction, exception.getMessage());
            LOGGER.error("Failed to send confirmation email for externalReference={}", externalReference, exception);
        }
    }

    private void markFailure(Transaction transaction, String error) {
        transaction.markConfirmationEmailFailed(OffsetDateTime.now(), truncate(error));
        transactionRepository.save(transaction);
    }

    private SimpleMailMessage message(Transaction transaction, List<String> luckyNumbers) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(appProperties.mail().from());
        message.setTo(transaction.getEmail());
        message.setSubject(SUBJECT);
        message.setText(body(luckyNumbers));
        return message;
    }

    private static String body(List<String> luckyNumbers) {
        return """
                Olá!

                Obrigado pela contribuição para o nosso presente de casamento.
                Seu pagamento foi aprovado e seus números da sorte são:

                %s

                Boa sorte no sorteio!
                """
                .formatted(String.join(System.lineSeparator(), luckyNumbers));
    }

    private static String truncate(String value) {
        if (value == null) {
            return "Unknown mail error.";
        }
        return value.length() <= 500 ? value : value.substring(0, 500);
    }
}
