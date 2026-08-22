package com.weddingraffle.rifa.service.impl;

import com.weddingraffle.rifa.config.AppProperties;
import com.weddingraffle.rifa.entity.LuckyNumber;
import com.weddingraffle.rifa.entity.PaymentStatus;
import com.weddingraffle.rifa.entity.Transaction;
import com.weddingraffle.rifa.repository.LuckyNumberRepository;
import com.weddingraffle.rifa.service.LuckyNumberCandidateGenerator;
import com.weddingraffle.rifa.service.LuckyNumberService;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class LuckyNumberServiceImpl implements LuckyNumberService {

    private final AppProperties appProperties;
    private final LuckyNumberRepository luckyNumberRepository;
    private final LuckyNumberCandidateGenerator candidateGenerator;
    private final Clock clock;

    public LuckyNumberServiceImpl(
            AppProperties appProperties,
            LuckyNumberRepository luckyNumberRepository,
            LuckyNumberCandidateGenerator candidateGenerator,
            Clock clock) {
        this.appProperties = appProperties;
        this.luckyNumberRepository = luckyNumberRepository;
        this.candidateGenerator = candidateGenerator;
        this.clock = clock;
    }

    @Override
    public List<LuckyNumber> generateFor(Transaction transaction) {
        if (transaction.hasCompletedLuckyNumberBatch()) {
            return completedBatch(transaction);
        }
        if (luckyNumberRepository.existsByTransaction(transaction)) {
            throw new IllegalStateException("Lucky numbers exist without a completed batch marker.");
        }

        NumberRange range = numberRange();
        List<LuckyNumber> luckyNumbers = new ArrayList<>();
        for (int index = 1; index <= transaction.getQuantity(); index++) {
            String number = nextAvailableNumber(range, luckyNumbers);
            luckyNumbers.add(new LuckyNumber(number, transaction.getEmail(), transaction, index));
        }
        List<LuckyNumber> persisted = luckyNumberRepository.saveAllAndFlush(luckyNumbers);
        ensureExactBatch(transaction, persisted);
        transaction.markLuckyNumberBatchCompleted(OffsetDateTime.now(clock));
        return persisted;
    }

    @Override
    public List<String> findNumbers(String externalReference) {
        return luckyNumberRepository.findNumbersByTransactionExternalReference(externalReference);
    }

    @Override
    public List<String> findApprovedNumbersByPhone(String phone) {
        return luckyNumberRepository.findNumbersByPhoneAndStatus(phone, PaymentStatus.APPROVED);
    }

    @Override
    public List<String> findPreviousApprovedNumbers(String phone, String externalReference) {
        return luckyNumberRepository.findNumbersByPhoneAndStatusExcludingExternalReference(
                phone, PaymentStatus.APPROVED, externalReference);
    }

    private String nextAvailableNumber(NumberRange range, List<LuckyNumber> pendingNumbers) {
        for (int attempts = 0; attempts <= range.capacity(); attempts++) {
            String candidate = range.format(candidateGenerator.nextInt(range.min(), range.max()));
            if (!contains(pendingNumbers, candidate) && !luckyNumberRepository.existsByNumber(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("No lucky numbers available.");
    }

    private NumberRange numberRange() {
        String minValue = appProperties.raffle().numberMin();
        String maxValue = appProperties.raffle().numberMax();
        int min = Integer.parseInt(minValue);
        int max = Integer.parseInt(maxValue);
        if (min > max) {
            throw new IllegalStateException("Invalid lucky number range.");
        }
        return new NumberRange(min, max, Math.max(minValue.length(), maxValue.length()));
    }

    private List<LuckyNumber> completedBatch(Transaction transaction) {
        List<LuckyNumber> luckyNumbers = luckyNumberRepository.findByTransactionOrderByNumberAsc(transaction);
        ensureExactBatch(transaction, luckyNumbers);
        return luckyNumbers;
    }

    private static void ensureExactBatch(Transaction transaction, List<LuckyNumber> luckyNumbers) {
        Set<Integer> indexes = new HashSet<>();
        for (LuckyNumber luckyNumber : luckyNumbers) {
            indexes.add(luckyNumber.getAllocationIndex());
        }
        if (luckyNumbers.size() != transaction.getQuantity() || indexes.size() != transaction.getQuantity()) {
            throw new IllegalStateException("Lucky-number batch does not match transaction quantity.");
        }
        for (int expectedIndex = 1; expectedIndex <= transaction.getQuantity(); expectedIndex++) {
            if (!indexes.contains(expectedIndex)) {
                throw new IllegalStateException("Lucky-number batch does not match transaction quantity.");
            }
        }
    }

    private static boolean contains(List<LuckyNumber> luckyNumbers, String number) {
        return luckyNumbers.stream()
                .anyMatch(luckyNumber -> luckyNumber.getNumber().equals(number));
    }

    private record NumberRange(int min, int max, int width) {

        long capacity() {
            return (long) max - min + 1;
        }

        String format(int number) {
            return String.format("%0" + width + "d", number);
        }
    }
}
