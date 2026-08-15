package com.weddingraffle.rifa.service.impl;

import com.weddingraffle.rifa.entity.ParticipantFlag;
import com.weddingraffle.rifa.repository.TransactionRepository;
import com.weddingraffle.rifa.service.ParticipantFlagService;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;

@Service
public class RandomParticipantFlagService implements ParticipantFlagService {

    private static final List<ParticipantFlag> FLAGS = List.of(
            new ParticipantFlag("BRAZIL", "Brasil", "BR"),
            new ParticipantFlag("ARGENTINA", "Argentina", "AR"),
            new ParticipantFlag("JAPAN", "Japao", "JP"),
            new ParticipantFlag("ITALY", "Italia", "IT"),
            new ParticipantFlag("CANADA", "Canada", "CA"),
            new ParticipantFlag("PORTUGAL", "Portugal", "PT"),
            new ParticipantFlag("SPAIN", "Espanha", "ES"),
            new ParticipantFlag("FRANCE", "Franca", "FR"),
            new ParticipantFlag("GERMANY", "Alemanha", "DE"),
            new ParticipantFlag("MEXICO", "Mexico", "MX"),
            new ParticipantFlag("URUGUAY", "Uruguai", "UY"),
            new ParticipantFlag("CHILE", "Chile", "CL"));

    private final TransactionRepository transactionRepository;

    public RandomParticipantFlagService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Override
    public ParticipantFlag resolveForPhone(String phone) {
        return transactionRepository
                .findFirstByPhoneOrderByCreatedAtAsc(phone)
                .map(transaction -> new ParticipantFlag(
                        transaction.getParticipantFlagCode(),
                        transaction.getParticipantFlagName(),
                        transaction.getParticipantFlagEmoji()))
                .orElseGet(() -> FLAGS.get(ThreadLocalRandom.current().nextInt(FLAGS.size())));
    }
}
