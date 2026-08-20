package com.weddingraffle.rifa.service.impl;

import com.weddingraffle.rifa.config.AppProperties;
import com.weddingraffle.rifa.entity.LuckyNumber;
import com.weddingraffle.rifa.entity.PaymentStatus;
import com.weddingraffle.rifa.entity.Transaction;
import com.weddingraffle.rifa.repository.LuckyNumberRepository;
import com.weddingraffle.rifa.service.LuckyNumberCandidateGenerator;
import com.weddingraffle.rifa.service.LuckyNumberService;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class LuckyNumberServiceImpl implements LuckyNumberService {

    private final AppProperties appProperties;
    private final LuckyNumberRepository luckyNumberRepository;
    private final LuckyNumberCandidateGenerator candidateGenerator;

    public LuckyNumberServiceImpl(
            AppProperties appProperties,
            LuckyNumberRepository luckyNumberRepository,
            LuckyNumberCandidateGenerator candidateGenerator) {
        this.appProperties = appProperties;
        this.luckyNumberRepository = luckyNumberRepository;
        this.candidateGenerator = candidateGenerator;
    }

    @Override
    public List<LuckyNumber> generateFor(Transaction transaction) {
        if (luckyNumberRepository.existsByTransaction(transaction)) {
            return luckyNumberRepository.findByTransactionOrderByNumberAsc(transaction);
        }

        NumberRange range = numberRange();
        List<LuckyNumber> luckyNumbers = new ArrayList<>();
        for (int index = 0; index < transaction.getQuantity(); index++) {
            String number = nextAvailableNumber(range, luckyNumbers);
            luckyNumbers.add(new LuckyNumber(number, transaction.getEmail(), transaction));
        }
        return luckyNumberRepository.saveAll(luckyNumbers);
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
