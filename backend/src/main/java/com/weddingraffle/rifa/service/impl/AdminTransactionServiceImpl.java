package com.weddingraffle.rifa.service.impl;

import com.weddingraffle.rifa.dto.AdminTransactionResponse;
import com.weddingraffle.rifa.dto.AdminTransactionSummaryResponse;
import com.weddingraffle.rifa.dto.CapacityReviewDecision;
import com.weddingraffle.rifa.dto.CashTransactionCreateRequest;
import com.weddingraffle.rifa.dto.CashTransactionCreateResponse;
import com.weddingraffle.rifa.dto.PaymentStatusResponse;
import com.weddingraffle.rifa.entity.CapacityReviewStatus;
import com.weddingraffle.rifa.entity.LuckyNumber;
import com.weddingraffle.rifa.entity.PaymentMethod;
import com.weddingraffle.rifa.entity.PaymentStatus;
import com.weddingraffle.rifa.entity.Transaction;
import com.weddingraffle.rifa.exception.InvalidRaffleStateException;
import com.weddingraffle.rifa.exception.InvalidTransactionStateException;
import com.weddingraffle.rifa.exception.ResourceNotFoundException;
import com.weddingraffle.rifa.repository.AdminTransactionSummaryProjection;
import com.weddingraffle.rifa.repository.LuckyNumberRepository;
import com.weddingraffle.rifa.repository.TransactionRepository;
import com.weddingraffle.rifa.service.AdminTransactionService;
import com.weddingraffle.rifa.service.CapacityAllocationResult;
import com.weddingraffle.rifa.service.CapacityReservationService;
import com.weddingraffle.rifa.service.LuckyNumberService;
import com.weddingraffle.rifa.service.ParticipantFlagService;
import com.weddingraffle.rifa.service.RaffleConfigService;
import com.weddingraffle.rifa.service.RecoveryCodeService;
import com.weddingraffle.rifa.util.ParticipantNormalizer;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AdminTransactionServiceImpl implements AdminTransactionService {

    private final RaffleConfigService raffleConfigService;
    private final TransactionRepository transactionRepository;
    private final LuckyNumberRepository luckyNumberRepository;
    private final LuckyNumberService luckyNumberService;
    private final ParticipantFlagService participantFlagService;
    private final RecoveryCodeService recoveryCodeService;
    private final CapacityReservationService capacityReservationService;

    public AdminTransactionServiceImpl(
            RaffleConfigService raffleConfigService,
            TransactionRepository transactionRepository,
            LuckyNumberRepository luckyNumberRepository,
            LuckyNumberService luckyNumberService,
            ParticipantFlagService participantFlagService,
            RecoveryCodeService recoveryCodeService,
            CapacityReservationService capacityReservationService) {
        this.raffleConfigService = raffleConfigService;
        this.transactionRepository = transactionRepository;
        this.luckyNumberRepository = luckyNumberRepository;
        this.luckyNumberService = luckyNumberService;
        this.participantFlagService = participantFlagService;
        this.recoveryCodeService = recoveryCodeService;
        this.capacityReservationService = capacityReservationService;
    }

    @Override
    @Transactional(readOnly = true)
    public AdminTransactionSummaryResponse getSummary() {
        AdminTransactionSummaryProjection summary = transactionRepository.getAdminSummary();
        return new AdminTransactionSummaryResponse(
                summary.getTotalTransactions(), summary.getApprovedLuckyNumbers(), summary.getApprovedRevenue());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminTransactionResponse> list(String query, Pageable pageable) {
        Page<Transaction> transactions = StringUtils.hasText(query)
                ? transactionRepository.findByNameOrPhone(query.trim(), normalizePhoneSearch(query), pageable)
                : transactionRepository.findAll(pageable);
        Map<Transaction, List<String>> numbersByTransaction = numbersByTransaction(transactions.getContent());
        return transactions.map(transaction -> toResponse(transaction, numbersByTransaction));
    }

    @Override
    @Transactional
    public CashTransactionCreateResponse createCashTransaction(CashTransactionCreateRequest request) {
        ensureDrawIsOpen();
        String name = ParticipantNormalizer.normalizeName(request.name());
        String phone = ParticipantNormalizer.normalizePhone(request.phone());
        String email = ParticipantNormalizer.normalizeEmail(request.email());
        BigDecimal unitPrice = raffleConfigService.getCurrentUnitPrice();
        BigDecimal totalAmount = unitPrice.multiply(BigDecimal.valueOf(request.quantity()));
        String externalReference = UUID.randomUUID().toString();

        capacityReservationService.reserve(externalReference, request.quantity());

        Transaction transaction = new Transaction(
                name,
                phone,
                email,
                request.quantity(),
                unitPrice,
                totalAmount,
                PaymentStatus.APPROVED,
                PaymentMethod.CASH,
                externalReference);
        transaction.assignParticipantFlag(participantFlagService.resolveForPhone(phone));
        transaction.assignRecoveryCode(recoveryCodeService.resolveForPhone(phone));
        transactionRepository.save(transaction);
        CapacityAllocationResult allocation =
                capacityReservationService.allocate(externalReference, request.quantity());
        if (allocation != CapacityAllocationResult.ALLOCATED) {
            throw new IllegalStateException("Cash transaction capacity was not allocated.");
        }
        List<String> luckyNumbers = luckyNumberService.generateFor(transaction).stream()
                .map(LuckyNumber::getNumber)
                .sorted()
                .toList();
        List<String> previousLuckyNumbers = luckyNumberService.findPreviousApprovedNumbers(
                transaction.getPhone(), transaction.getExternalReference());

        return new CashTransactionCreateResponse(
                transaction.getExternalReference(),
                transaction.getRecoveryCode(),
                transaction.getName(),
                transaction.getPhone(),
                transaction.getEmail(),
                transaction.getPaymentMethod(),
                PaymentStatusResponse.from(transaction.getStatus()),
                transaction.getQuantity(),
                transaction.getTotalAmount(),
                transaction.getParticipantFlagName(),
                transaction.getParticipantFlagEmoji(),
                luckyNumbers,
                previousLuckyNumbers,
                luckyNumbers.size() + previousLuckyNumbers.size());
    }

    @Override
    @Transactional
    public void deleteCashTransaction(String externalReference) {
        Transaction transaction = transactionRepository
                .findByExternalReference(externalReference)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found."));

        if (transaction.getPaymentMethod() != PaymentMethod.CASH) {
            throw new InvalidTransactionStateException("Only cash transactions can be deleted.");
        }

        luckyNumberRepository.deleteByTransaction(transaction);
        capacityReservationService.releaseAllocation(externalReference);
        transactionRepository.delete(transaction);
    }

    @Override
    @Transactional
    public void resolveCapacityReview(String externalReference, CapacityReviewDecision decision) {
        Transaction transaction = transactionRepository
                .findByExternalReference(externalReference)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found."));
        if (transaction.getPaymentMethod() != PaymentMethod.MERCADO_PAGO
                || transaction.getStatus() != PaymentStatus.APPROVED
                || transaction.getCapacityReviewStatus() != CapacityReviewStatus.PENDING) {
            throw new InvalidTransactionStateException("Transaction is not pending capacity review.");
        }

        CapacityReviewStatus resolution =
                switch (decision) {
                    case REFUND_COMPLETED -> CapacityReviewStatus.REFUND_COMPLETED;
                    case CONTRIBUTION_WITHOUT_NUMBERS -> CapacityReviewStatus.CONTRIBUTION_WITHOUT_NUMBERS;
                };
        transaction.completeCapacityReview(resolution);
        transactionRepository.save(transaction);
    }

    private Map<Transaction, List<String>> numbersByTransaction(List<Transaction> transactions) {
        if (transactions.isEmpty()) {
            return Collections.emptyMap();
        }
        return luckyNumberRepository.findByTransactionInOrderByNumberAsc(transactions).stream()
                .collect(Collectors.groupingBy(
                        LuckyNumber::getTransaction, Collectors.mapping(LuckyNumber::getNumber, Collectors.toList())));
    }

    private void ensureDrawIsOpen() {
        if (raffleConfigService.isDrawClosed()) {
            throw new InvalidRaffleStateException("Draw is closed. No more numbers can be purchased.");
        }
    }

    private static String normalizePhoneSearch(String query) {
        return query.replaceAll("\\D", "");
    }

    private static AdminTransactionResponse toResponse(
            Transaction transaction, Map<Transaction, List<String>> numbersByTransaction) {
        return new AdminTransactionResponse(
                transaction.getExternalReference(),
                transaction.getCreatedAt(),
                transaction.getName(),
                transaction.getPhone(),
                transaction.getEmail(),
                transaction.getPaymentMethod(),
                transaction.getCapacityReviewStatus(),
                transaction.getQuantity(),
                transaction.getTotalAmount(),
                PaymentStatusResponse.from(transaction.getStatus()),
                numbersByTransaction.getOrDefault(transaction, List.of()));
    }
}
