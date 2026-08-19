package com.weddingraffle.rifa.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.weddingraffle.rifa.entity.ParticipantFlag;
import com.weddingraffle.rifa.entity.PaymentStatus;
import com.weddingraffle.rifa.entity.Transaction;
import com.weddingraffle.rifa.repository.TransactionRepository;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RandomParticipantFlagServiceTests {

    @Mock
    private TransactionRepository transactionRepository;

    @Test
    void reusesExistingFlagForKnownPhone() {
        RandomParticipantFlagService service = new RandomParticipantFlagService(transactionRepository);
        Transaction transaction =
                new Transaction("guest@example.com", 1, new BigDecimal("10.00"), PaymentStatus.PENDING, "external");
        transaction.assignParticipantFlag(new ParticipantFlag("BRAZIL", "Brasil", "🇧🇷"));
        when(transactionRepository.findFirstByPhoneOrderByCreatedAtAsc("11999999999"))
                .thenReturn(Optional.of(transaction));

        ParticipantFlag flag = service.resolveForPhone("11999999999");

        assertThat(flag.code()).isEqualTo("BRAZIL");
        assertThat(flag.name()).isEqualTo("Brasil");
        assertThat(flag.emoji()).isEqualTo("🇧🇷");
    }

    @Test
    void assignsOnlyUnusedFlagForNewPhone() {
        RandomParticipantFlagService service = new RandomParticipantFlagService(transactionRepository);
        when(transactionRepository.findFirstByPhoneOrderByCreatedAtAsc("11999999999"))
                .thenReturn(Optional.empty());
        List<String> flagCodes = flagCodes();
        List<String> usedFlagCodes = new ArrayList<>(flagCodes);
        String expectedFlagCode = usedFlagCodes.removeLast();
        when(transactionRepository.findDistinctParticipantFlagCodes()).thenReturn(usedFlagCodes);

        ParticipantFlag flag = service.resolveForPhone("11999999999");

        assertThat(flag.code()).isEqualTo(expectedFlagCode);
    }

    @Test
    void failsWhenAllFlagsAreAlreadyUsed() {
        RandomParticipantFlagService service = new RandomParticipantFlagService(transactionRepository);
        when(transactionRepository.findFirstByPhoneOrderByCreatedAtAsc("11999999999"))
                .thenReturn(Optional.empty());
        when(transactionRepository.findDistinctParticipantFlagCodes()).thenReturn(flagCodes());

        assertThatThrownBy(() -> service.resolveForPhone("11999999999"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No participant flags available for a new phone.");
    }

    @Test
    void loadsALargeFlagCatalog() {
        assertThat(flagCodes()).hasSizeGreaterThanOrEqualTo(190).doesNotHaveDuplicates();
    }

    private static List<String> flagCodes() {
        try (var input = RandomParticipantFlagServiceTests.class
                        .getClassLoader()
                        .getResourceAsStream("participant-flags.csv");
                var reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            return reader.lines()
                    .filter(line -> !line.isBlank() && !line.startsWith("#"))
                    .map(line -> line.split(";", -1)[0])
                    .toList();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load participant flags test resource.", exception);
        }
    }
}
