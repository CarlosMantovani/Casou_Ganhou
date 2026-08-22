package com.weddingraffle.rifa.service.impl;

import com.weddingraffle.rifa.dto.PaymentStatusResponse;
import com.weddingraffle.rifa.dto.TransactionCreateRequest;
import com.weddingraffle.rifa.dto.TransactionCreateResponse;
import com.weddingraffle.rifa.dto.TransactionQuoteRequest;
import com.weddingraffle.rifa.dto.TransactionQuoteResponse;
import com.weddingraffle.rifa.dto.TransactionRecoveryRequest;
import com.weddingraffle.rifa.dto.TransactionStatusResponse;
import com.weddingraffle.rifa.entity.PaymentStatus;
import com.weddingraffle.rifa.entity.Transaction;
import com.weddingraffle.rifa.exception.InvalidRaffleStateException;
import com.weddingraffle.rifa.exception.ResourceNotFoundException;
import com.weddingraffle.rifa.integration.CheckoutPreferenceResponse;
import com.weddingraffle.rifa.integration.PaymentProviderClient;
import com.weddingraffle.rifa.integration.PaymentProviderPayment;
import com.weddingraffle.rifa.repository.TransactionRepository;
import com.weddingraffle.rifa.service.CapacityAllocationResult;
import com.weddingraffle.rifa.service.CapacityReservationService;
import com.weddingraffle.rifa.service.LuckyNumberService;
import com.weddingraffle.rifa.service.OnlinePurchaseAttempt;
import com.weddingraffle.rifa.service.PurchaseIntentService;
import com.weddingraffle.rifa.service.RaffleConfigService;
import com.weddingraffle.rifa.service.TransactionService;
import com.weddingraffle.rifa.util.ParticipantNormalizer;
import com.weddingraffle.rifa.util.PurchaseRequestHasher;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
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
    private final CapacityReservationService capacityReservationService;
    private final PurchaseIntentService purchaseIntentService;
    private final PurchaseRequestHasher purchaseRequestHasher;

    public TransactionServiceImpl(
            RaffleConfigService raffleConfigService,
            TransactionRepository transactionRepository,
            PaymentProviderClient paymentProviderClient,
            LuckyNumberService luckyNumberService,
            CapacityReservationService capacityReservationService,
            PurchaseIntentService purchaseIntentService,
            PurchaseRequestHasher purchaseRequestHasher) {
        this.raffleConfigService = raffleConfigService;
        this.transactionRepository = transactionRepository;
        this.paymentProviderClient = paymentProviderClient;
        this.luckyNumberService = luckyNumberService;
        this.capacityReservationService = capacityReservationService;
        this.purchaseIntentService = purchaseIntentService;
        this.purchaseRequestHasher = purchaseRequestHasher;
    }

    @Override
    public TransactionQuoteResponse quote(TransactionQuoteRequest request) {
        ensureDrawIsOpen();
        String name = ParticipantNormalizer.normalizeName(request.name());
        String phone = ParticipantNormalizer.normalizePhone(request.phone());
        BigDecimal unitPrice = raffleConfigService.getCurrentUnitPrice();
        BigDecimal totalAmount = unitPrice.multiply(BigDecimal.valueOf(request.quantity()));
        return new TransactionQuoteResponse(name, phone, request.quantity(), unitPrice, totalAmount);
    }

    @Override
    public TransactionCreateResponse create(String idempotencyKey, TransactionCreateRequest request) {
        String normalizedIdempotencyKey = purchaseRequestHasher.normalizeIdempotencyKey(idempotencyKey);
        String name = ParticipantNormalizer.normalizeName(request.name());
        String phone = ParticipantNormalizer.normalizePhone(request.phone());
        String requestHash = purchaseRequestHasher.online(name, phone, request.quantity());

        OnlinePurchaseAttempt attempt = purchaseIntentService
                .findOnline(normalizedIdempotencyKey, requestHash)
                .orElseGet(() ->
                        prepareOnlineAttempt(normalizedIdempotencyKey, requestHash, name, phone, request.quantity()));
        if (attempt.isCompleted()) {
            return attempt.completedResponse();
        }

        CheckoutPreferenceResponse preference =
                paymentProviderClient.createPreference(attempt.checkoutPreferenceRequest(), normalizedIdempotencyKey);
        TransactionCreateResponse response =
                purchaseIntentService.completeOnline(normalizedIdempotencyKey, requestHash, preference);
        LOGGER.info("Completed pending transaction checkout with externalReference={}", response.externalReference());
        return response;
    }

    private OnlinePurchaseAttempt prepareOnlineAttempt(
            String idempotencyKey, String requestHash, String name, String phone, int quantity) {
        ensureDrawIsOpen();
        BigDecimal unitPrice = raffleConfigService.getCurrentUnitPrice();
        BigDecimal totalAmount = unitPrice.multiply(BigDecimal.valueOf(quantity));
        try {
            return purchaseIntentService.prepareOnline(
                    idempotencyKey, requestHash, name, phone, quantity, unitPrice, totalAmount);
        } catch (DataIntegrityViolationException exception) {
            return purchaseIntentService.findOnline(idempotencyKey, requestHash).orElseThrow(() -> exception);
        }
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
            allocateLuckyNumbersOrStartReview(transaction);
        }
        transaction.markPayment(paymentStatus, payment.paymentId());
        transactionRepository.save(transaction);
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
            refreshPendingTransaction(transaction);
        }

        return toStatusResponse(transaction);
    }

    @Override
    @Transactional
    public TransactionStatusResponse recover(TransactionRecoveryRequest request) {
        String phone = ParticipantNormalizer.normalizePhone(request.phone());
        List<Transaction> transactions =
                transactionRepository.findByPhoneAndRecoveryCodeOrderByCreatedAtDesc(phone, request.recoveryCode());
        if (transactions.isEmpty()) {
            throw new ResourceNotFoundException("Transaction not found.");
        }

        for (Transaction transaction : transactions) {
            if (transaction.getStatus() == PaymentStatus.PENDING && transaction.getMpPaymentId() != null) {
                refreshPendingTransaction(transaction);
            }
        }

        return transactions.stream()
                .filter(transaction -> transaction.getStatus() == PaymentStatus.APPROVED)
                .findFirst()
                .map(this::toRecoveryResponse)
                .orElseGet(() -> toStatusResponse(transactions.getFirst()));
    }

    private void refreshPendingTransaction(Transaction transaction) {
        PaymentProviderPayment payment = paymentProviderClient.getPayment(transaction.getMpPaymentId());
        PaymentStatus paymentStatus = toPaymentStatus(payment.status());
        PaymentStatus currentStatus = transaction.getStatus();
        if (paymentStatus == PaymentStatus.APPROVED && currentStatus != PaymentStatus.APPROVED) {
            allocateLuckyNumbersOrStartReview(transaction);
        }
        transaction.markPayment(paymentStatus, payment.paymentId());
        transactionRepository.save(transaction);
    }

    private TransactionStatusResponse toStatusResponse(Transaction transaction) {
        List<String> luckyNumbers = luckyNumberService.findNumbers(transaction.getExternalReference());
        List<String> previousLuckyNumbers = transaction.getStatus() == PaymentStatus.APPROVED
                ? luckyNumberService.findPreviousApprovedNumbers(
                        transaction.getPhone(), transaction.getExternalReference())
                : List.of();
        return new TransactionStatusResponse(
                transaction.getExternalReference(),
                transaction.getRecoveryCode(),
                PaymentStatusResponse.from(transaction.getStatus()),
                transaction.getQuantity(),
                transaction.getTotalAmount(),
                transaction.getParticipantFlagName(),
                transaction.getParticipantFlagEmoji(),
                luckyNumbers,
                previousLuckyNumbers,
                luckyNumbers.size() + previousLuckyNumbers.size());
    }

    private void allocateLuckyNumbersOrStartReview(Transaction transaction) {
        if (transaction.getCapacityReviewStatus() != null) {
            return;
        }
        CapacityAllocationResult allocation =
                capacityReservationService.allocate(transaction.getExternalReference(), transaction.getQuantity());
        if (allocation == CapacityAllocationResult.INSUFFICIENT_CAPACITY) {
            transaction.markCapacityReviewPending();
            return;
        }
        if (allocation == CapacityAllocationResult.ALLOCATED) {
            luckyNumberService.generateFor(transaction);
        }
    }

    private TransactionStatusResponse toRecoveryResponse(Transaction transaction) {
        List<String> luckyNumbers = luckyNumberService.findApprovedNumbersByPhone(transaction.getPhone());
        return new TransactionStatusResponse(
                transaction.getExternalReference(),
                transaction.getRecoveryCode(),
                PaymentStatusResponse.from(PaymentStatus.APPROVED),
                luckyNumbers.size(),
                BigDecimal.ZERO,
                transaction.getParticipantFlagName(),
                transaction.getParticipantFlagEmoji(),
                luckyNumbers,
                List.of(),
                luckyNumbers.size());
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

    private void ensureDrawIsOpen() {
        if (raffleConfigService.isDrawClosed()) {
            throw new InvalidRaffleStateException("Draw is closed. No more numbers can be purchased.");
        }
    }
}
