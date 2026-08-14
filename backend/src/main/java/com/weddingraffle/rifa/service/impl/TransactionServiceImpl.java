package com.weddingraffle.rifa.service.impl;

import com.weddingraffle.rifa.config.AppProperties;
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
import com.weddingraffle.rifa.service.TransactionService;
import com.weddingraffle.rifa.util.ParticipantNormalizer;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.Objects;
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

    private final AppProperties appProperties;
    private final TransactionRepository transactionRepository;
    private final PaymentProviderClient paymentProviderClient;
    private final LuckyNumberService luckyNumberService;
    private final ApplicationEventPublisher applicationEventPublisher;

    public TransactionServiceImpl(
            AppProperties appProperties,
            TransactionRepository transactionRepository,
            PaymentProviderClient paymentProviderClient,
            LuckyNumberService luckyNumberService,
            ApplicationEventPublisher applicationEventPublisher) {
        this.appProperties = appProperties;
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
        BigDecimal unitPrice = appProperties.raffle().unitPrice();
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
        BigDecimal unitPrice = appProperties.raffle().unitPrice();
        BigDecimal totalAmount = unitPrice.multiply(BigDecimal.valueOf(request.quantity()));

        CheckoutPreferenceResponse preference = paymentProviderClient.createPreference(
                new CheckoutPreferenceRequest(name, email, request.quantity(), unitPrice, externalReference));

        Transaction transaction = new Transaction(
                name,
                phone,
                email,
                request.quantity(),
                unitPrice,
                totalAmount,
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

        PaymentStatus currentStatus = transaction.getStatus();
        PaymentStatus paymentStatus = toPaymentStatus(payment.status());
        boolean paymentIdChanged = !Objects.equals(transaction.getMpPaymentId(), payment.paymentId());

        if (currentStatus == paymentStatus && !paymentIdChanged) {
            LOGGER.info(
                    "Ignored unchanged payment notification for externalReference={} status={}",
                    transaction.getExternalReference(),
                    currentStatus);
            return;
        }

        if (paymentStatus == PaymentStatus.APPROVED && currentStatus != PaymentStatus.APPROVED) {
            luckyNumberService.generateFor(transaction);
        }
        transaction.markPayment(paymentStatus, payment.paymentId());
        transactionRepository.save(transaction);
        publishPaymentApprovedEvent(currentStatus, transaction, paymentStatus);
        LOGGER.info(
                "Updated transaction externalReference={} to status={}",
                transaction.getExternalReference(),
                transaction.getStatus());
    }

    @Override
    @Transactional
    public TransactionStatusResponse getStatus(String externalReference) {
        Transaction transaction = transactionRepository
                .findByExternalReference(externalReference)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found."));

        if (transaction.getStatus() == PaymentStatus.PENDING && transaction.getMpPaymentId() != null) {
            PaymentProviderPayment payment = paymentProviderClient.getPayment(transaction.getMpPaymentId());
            PaymentStatus paymentStatus = toPaymentStatus(payment.status());
            PaymentStatus currentStatus = transaction.getStatus();
            if (paymentStatus == PaymentStatus.APPROVED && currentStatus != PaymentStatus.APPROVED) {
                luckyNumberService.generateFor(transaction);
            }
            transaction.markPayment(paymentStatus, payment.paymentId());
            transactionRepository.save(transaction);
            publishPaymentApprovedEvent(currentStatus, transaction, paymentStatus);
        }

        return new TransactionStatusResponse(
                transaction.getExternalReference(),
                StringUtils.hasText(transaction.getEmail()),
                transaction.getStatus(),
                transaction.getQuantity(),
                transaction.getTotalAmount(),
                luckyNumberService.findNumbers(transaction.getExternalReference()));
    }

    private static PaymentStatus toPaymentStatus(String mercadoPagoStatus) {
        if (!StringUtils.hasText(mercadoPagoStatus)) {
            return PaymentStatus.PENDING;
        }
        return switch (mercadoPagoStatus.toLowerCase(Locale.ROOT)) {
            case "approved" -> PaymentStatus.APPROVED;
            case "rejected" -> PaymentStatus.REJECTED;
            case "cancelled", "canceled" -> PaymentStatus.CANCELLED;
            case "refunded" -> PaymentStatus.REFUNDED;
            case "charged_back" -> PaymentStatus.CHARGED_BACK;
            case "in_mediation" -> PaymentStatus.IN_MEDIATION;
            default -> PaymentStatus.PENDING;
        };
    }

    private void publishPaymentApprovedEvent(
            PaymentStatus previousStatus, Transaction transaction, PaymentStatus paymentStatus) {
        if (paymentStatus == PaymentStatus.APPROVED && previousStatus != PaymentStatus.APPROVED) {
            applicationEventPublisher.publishEvent(new PaymentApprovedEvent(transaction.getExternalReference()));
        }
    }
}
