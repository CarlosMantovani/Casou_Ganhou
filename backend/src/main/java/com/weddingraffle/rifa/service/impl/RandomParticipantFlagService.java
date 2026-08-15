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
            new ParticipantFlag("BRAZIL", "Brasil", "🇧🇷"),
            new ParticipantFlag("ARGENTINA", "Argentina", "🇦🇷"),
            new ParticipantFlag("JAPAN", "Japao", "🇯🇵"),
            new ParticipantFlag("ITALY", "Italia", "🇮🇹"),
            new ParticipantFlag("CANADA", "Canada", "🇨🇦"),
            new ParticipantFlag("PORTUGAL", "Portugal", "🇵🇹"),
            new ParticipantFlag("SPAIN", "Espanha", "🇪🇸"),
            new ParticipantFlag("FRANCE", "Franca", "🇫🇷"),
            new ParticipantFlag("GERMANY", "Alemanha", "🇩🇪"),
            new ParticipantFlag("MEXICO", "Mexico", "🇲🇽"),
            new ParticipantFlag("URUGUAY", "Uruguai", "🇺🇾"),
            new ParticipantFlag("CHILE", "Chile", "🇨🇱"));

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
