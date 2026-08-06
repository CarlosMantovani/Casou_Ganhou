package com.weddingraffle.rifa.service.impl;

import com.weddingraffle.rifa.dto.TransactionCreateRequest;
import com.weddingraffle.rifa.dto.TransactionCreateResponse;
import com.weddingraffle.rifa.dto.TransactionQuoteRequest;
import com.weddingraffle.rifa.dto.TransactionQuoteResponse;
import com.weddingraffle.rifa.dto.TransactionStatusResponse;
import com.weddingraffle.rifa.entity.PaymentMethod;
import com.weddingraffle.rifa.entity.PaymentStatus;
import com.weddingraffle.rifa.entity.Transaction;
import com.weddingraffle.rifa.exception.ResourceNotFoundException;
import com.weddingraffle.rifa.integration.CheckoutPreferenceRequest;
import com.weddingraffle.rifa.integration.CheckoutPreferenceResponse;
import com.weddingraffle.rifa.integration.PaymentProviderClient;
import com.weddingraffle.rifa.integration.PaymentProviderPayment;
import com.weddingraffle.rifa.repository.TransactionRepository;
import com.weddingraffle.rifa.service.LuckyNumberService;
import com.weddingraffle.rifa.service.PaymentApprovedEvent;
import com.weddingraffle.rifa.service.RaffleConfigService;
import com.weddingraffle.rifa.service.TransactionService;
import com.weddingraffle.rifa.util.ParticipantNormalizer;
import java.math.BigDecimal;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class TransactionServiceImpl implements TransactionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionServiceImpl.class);

    private final RaffleConfigService raffleConfigService;
    private final TransactionRepository transactionRepository;
    private final PaymentProviderClient paymentProviderClient;
    private final LuckyNumberService luckyNumberService;
    private final ApplicationEventPublisher applicationEventPublisher;

    public TransactionServiceImpl(
            RaffleConfigService raffleConfigService,
            TransactionRepository transactionRepository,
            PaymentProviderClient paymentProviderClient,
            LuckyNumberService luckyNumberService,
            ApplicationEventPublisher applicationEventPublisher) {
        this.raffleConfigService = raffleConfigService;
        this.transactionRepository = transactionRepository;
        this.paymentProviderClient = paymentProviderClient;
        this.luckyNumberService = luckyNumberService;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public TransactionQuoteResponse quote(TransactionQuoteRequest request) {
        String name = ParticipantNormalizer.normalizeName(request.name());
        String phone = ParticipantNormalizer.normalizePhone(request.phone());
        String email = ParticipantNormalizer.normalizeEmail(request.email());
        BigDecimal unitPrice = raffleConfigService.currentUnitPrice();
        BigDecimal totalAmount = unitPrice.multiply(BigDecimal.valueOf(request.quantity()));
        return new TransactionQuoteResponse(name, phone, email, request.quantity(), unitPrice, totalAmount);
    }

    @Override
    @Transactional
    public TransactionCreateResponse create(TransactionCreateRequest request) {
        String name = ParticipantNormalizer.normalizeName(request.name());
        String phone = ParticipantNormalizer.normalizePhone(request.phone());
        String email = ParticipantNormalizer.normalizeEmail(request.email());
        String externalReference = UUID.randomUUID().toString();
        BigDecimal unitPrice = raffleConfigService.currentUnitPrice();
        BigDecimal totalAmount = unitPrice.multiply(BigDecimal.valueOf(request.quantity()));

        CheckoutPreferenceResponse preference = paymentProviderClient.createPreference(
                new CheckoutPreferenceRequest(name, email, request.quantity(), unitPrice, externalReference));

        Transaction transaction = new Transaction(
                name,
                phone,
                email,
                request.quantity(),
                totalAmount,
                unitPrice,
                PaymentStatus.PENDING,
                PaymentMethod.MERCADO_PAGO,
                externalReference);
        transaction.assignPreference(preference.preferenceId());
        transactionRepository.save(transaction);

        LOGGER.info("Created pending transaction with externalReference={}", externalReference);

        return new TransactionCreateResponse(externalReference, preference.preferenceId(), preference.checkoutUrl());
    }

    @Override
    @Transactional
    public void processPaymentNotification(String paymentId) {
        PaymentProviderPayment payment = paymentProviderClient.getPayment(paymentId);
        Transaction transaction = transactionRepository
                .findByExternalReference(payment.externalReference())
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found."));

        if (transaction.getStatus() == PaymentStatus.APPROVED) {
            LOGGER.info(
                    "Ignored already approved transaction with externalReference={}",
                    transaction.getExternalReference());
            return;
        }

        refreshPaymentStatus(transaction, payment);
        LOGGER.info(
                "Updated transaction externalReference={} to status={}",
                transaction.getExternalReference(),
                transaction.getStatus());
    }

    @Override
    @Transactional
    public TransactionStatusResponse getStatus(String externalReference, String paymentId) {
        Transaction transaction = transactionRepository
                .findByExternalReference(externalReference)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found."));

        if (transaction.getStatus() == PaymentStatus.PENDING) {
            String paymentIdToCheck = StringUtils.hasText(paymentId) ? paymentId : transaction.getMpPaymentId();
            if (StringUtils.hasText(paymentIdToCheck)) {
                refreshPaymentStatus(transaction, paymentProviderClient.getPayment(paymentIdToCheck));
            }
        }

        return new TransactionStatusResponse(
                transaction.getExternalReference(),
                StringUtils.hasText(transaction.getEmail()),
                transaction.getStatus(),
                transaction.getQuantity(),
                transaction.getTotalAmount(),
                luckyNumberService.findNumbers(transaction.getExternalReference()));
    }

    private void refreshPaymentStatus(Transaction transaction, PaymentProviderPayment payment) {
        if (!transaction.getExternalReference().equals(payment.externalReference())) {
            throw new ResourceNotFoundException("Transaction not found.");
        }

        PaymentStatus paymentStatus = toPaymentStatus(payment.status());
        if (paymentStatus == PaymentStatus.APPROVED) {
            luckyNumberService.generateFor(transaction);
        }
        transaction.markPayment(paymentStatus, payment.paymentId());
        transactionRepository.save(transaction);
        publishPaymentApprovedEvent(transaction, paymentStatus);
    }

    private static PaymentStatus toPaymentStatus(String mercadoPagoStatus) {
        return switch (mercadoPagoStatus) {
            case "approved" -> PaymentStatus.APPROVED;
            case "rejected" -> PaymentStatus.REJECTED;
            case "cancelled" -> PaymentStatus.CANCELLED;
            default -> PaymentStatus.PENDING;
        };
    }

    private void publishPaymentApprovedEvent(Transaction transaction, PaymentStatus paymentStatus) {
        if (paymentStatus == PaymentStatus.APPROVED) {
            applicationEventPublisher.publishEvent(new PaymentApprovedEvent(transaction.getExternalReference()));
        }
    }
}
