package com.weddingraffle.rifa.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.weddingraffle.rifa.entity.ParticipantFlag;
import com.weddingraffle.rifa.entity.PaymentStatus;
import com.weddingraffle.rifa.entity.Transaction;
import com.weddingraffle.rifa.exception.InvalidTransactionStateException;
import com.weddingraffle.rifa.repository.TransactionRepository;
import com.weddingraffle.rifa.service.LuckyNumberService;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LuckyNumberPdfServiceImplTests {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private LuckyNumberService luckyNumberService;

    @Test
    void generatesPdfForApprovedTransactionWithLuckyNumbers() {
        LuckyNumberPdfServiceImpl service = new LuckyNumberPdfServiceImpl(transactionRepository, luckyNumberService);
        Transaction transaction =
                new Transaction("guest@example.com", 2, new BigDecimal("20.00"), PaymentStatus.APPROVED, "external");
        transaction.assignParticipantFlag(new ParticipantFlag("BRAZIL", "Brasil", "🇧🇷"));
        when(transactionRepository.findByExternalReference("external")).thenReturn(Optional.of(transaction));
        when(luckyNumberService.findNumbers("external")).thenReturn(List.of("00001", "00002"));

        byte[] pdf = service.generate("external");

        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
    }

    @Test
    void generatesMultiplePagesWithoutOmittingLuckyNumbers() throws IOException {
        LuckyNumberPdfServiceImpl service = new LuckyNumberPdfServiceImpl(transactionRepository, luckyNumberService);
        Transaction transaction = new Transaction(
                "guest@example.com", 200, new BigDecimal("2000.00"), PaymentStatus.APPROVED, "external");
        List<String> luckyNumbers = java.util.stream.IntStream.rangeClosed(1, 200)
                .mapToObj(number -> String.format("%05d", number))
                .toList();
        when(transactionRepository.findByExternalReference("external")).thenReturn(Optional.of(transaction));
        when(luckyNumberService.findNumbers("external")).thenReturn(luckyNumbers);

        byte[] pdf = service.generate("external");

        try (PDDocument document = Loader.loadPDF(pdf)) {
            String text = new PDFTextStripper().getText(document);

            assertThat(document.getNumberOfPages()).isGreaterThan(1);
            assertThat(text).contains("200 números gerados", "Números da sorte - continuação");
            assertThat(text).containsSubsequence(luckyNumbers);
        }
    }

    @Test
    void failsWhenTransactionIsNotApproved() {
        LuckyNumberPdfServiceImpl service = new LuckyNumberPdfServiceImpl(transactionRepository, luckyNumberService);
        Transaction transaction =
                new Transaction("guest@example.com", 2, new BigDecimal("20.00"), PaymentStatus.PENDING, "external");
        when(transactionRepository.findByExternalReference("external")).thenReturn(Optional.of(transaction));

        assertThatThrownBy(() -> service.generate("external")).isInstanceOf(InvalidTransactionStateException.class);
    }
}
