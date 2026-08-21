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
    void generatesPdfForApprovedTransactionWithLuckyNumbers() throws IOException {
        LuckyNumberPdfServiceImpl service = new LuckyNumberPdfServiceImpl(transactionRepository, luckyNumberService);
        Transaction transaction = new Transaction(
                "Maria Convidada",
                "11999999999",
                "guest@example.com",
                2,
                new BigDecimal("20.00"),
                PaymentStatus.APPROVED,
                com.weddingraffle.rifa.entity.PaymentMethod.CASH,
                "external");
        transaction.assignParticipantFlag(new ParticipantFlag("UY", "Uruguai", "🇺🇾"));
        transaction.assignRecoveryCode("4821");
        when(transactionRepository.findByExternalReference("external")).thenReturn(Optional.of(transaction));
        when(luckyNumberService.findNumbers("external")).thenReturn(List.of("00001", "00002"));
        when(luckyNumberService.findPreviousApprovedNumbers("11999999999", "external"))
                .thenReturn(List.of());

        byte[] pdf = service.generate("external");

        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
        try (PDDocument document = Loader.loadPDF(pdf)) {
            String text = new PDFTextStripper().getText(document);

            assertThat(text)
                    .contains(
                            "Paula e José Carlos",
                            "Presente Premiado",
                            "Obrigado pela sua contribuição, Maria Convidada.",
                            "Sua bandeira: Uruguai",
                            "Código de consulta: 4821",
                            "Use este código com o telefone para consultar seus números na tela inicial.",
                            "00001",
                            "00002");
            assertThat(text).doesNotContain("Sua bandeira: UY");
        }
    }

    @Test
    void separatesPreviousAndCurrentLuckyNumbersForRepeatBuyer() throws IOException {
        LuckyNumberPdfServiceImpl service = new LuckyNumberPdfServiceImpl(transactionRepository, luckyNumberService);
        Transaction transaction = new Transaction(
                "Maria Convidada",
                "11999999999",
                "guest@example.com",
                2,
                new BigDecimal("20.00"),
                PaymentStatus.APPROVED,
                com.weddingraffle.rifa.entity.PaymentMethod.CASH,
                "external");
        transaction.assignRecoveryCode("4821");
        when(transactionRepository.findByExternalReference("external")).thenReturn(Optional.of(transaction));
        when(luckyNumberService.findNumbers("external")).thenReturn(List.of("00003", "00004"));
        when(luckyNumberService.findPreviousApprovedNumbers("11999999999", "external"))
                .thenReturn(List.of("00001", "00002"));

        byte[] pdf = service.generate("external");

        try (PDDocument document = Loader.loadPDF(pdf)) {
            String text = new PDFTextStripper().getText(document);

            assertThat(text)
                    .contains(
                            "Números adquiridos anteriormente: 2",
                            "Números adquiridos agora: 2",
                            "Total de números com esta compra: 4",
                            "Código de consulta: 4821",
                            "Números adquiridos anteriormente",
                            "Números adquiridos agora",
                            "00001",
                            "00002",
                            "00003",
                            "00004");
            assertThat(text)
                    .containsSubsequence(
                            "Números adquiridos agora", "00003", "Números adquiridos anteriormente", "00001");
            assertThat(text)
                    .doesNotContain(
                            "Esta compra gerou",
                            "Você já tinha",
                            "Números adquiridos agora - continuação",
                            "Números adquiridos anteriormente - continuação");
        }
    }

    @Test
    void generatesParticipantPdfWithAllApprovedLuckyNumbersInSingleSection() throws IOException {
        LuckyNumberPdfServiceImpl service = new LuckyNumberPdfServiceImpl(transactionRepository, luckyNumberService);
        Transaction transaction = new Transaction(
                "Maria Convidada",
                "11999999999",
                "guest@example.com",
                2,
                new BigDecimal("20.00"),
                PaymentStatus.APPROVED,
                com.weddingraffle.rifa.entity.PaymentMethod.CASH,
                "external");
        transaction.assignRecoveryCode("4821");
        when(transactionRepository.findByExternalReference("external")).thenReturn(Optional.of(transaction));
        when(luckyNumberService.findApprovedNumbersByPhone("11999999999"))
                .thenReturn(List.of("00001", "00002", "00003", "00004"));

        byte[] pdf = service.generateForParticipant("external");

        try (PDDocument document = Loader.loadPDF(pdf)) {
            String text = new PDFTextStripper().getText(document);

            assertThat(text)
                    .contains(
                            "Obrigado pela sua contribuição, Maria Convidada.",
                            "Código de consulta: 4821",
                            "4 números gerados",
                            "Todos os números",
                            "00001",
                            "00002",
                            "00003",
                            "00004");
            assertThat(text).doesNotContain("Números adquiridos agora", "Números adquiridos anteriormente");
        }
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
        when(luckyNumberService.findPreviousApprovedNumbers("0000000000", "external"))
                .thenReturn(List.of());

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

    @Test
    void failsWhenParticipantHasNoApprovedLuckyNumbers() {
        LuckyNumberPdfServiceImpl service = new LuckyNumberPdfServiceImpl(transactionRepository, luckyNumberService);
        Transaction transaction =
                new Transaction("guest@example.com", 2, new BigDecimal("20.00"), PaymentStatus.PENDING, "external");
        when(transactionRepository.findByExternalReference("external")).thenReturn(Optional.of(transaction));
        when(luckyNumberService.findApprovedNumbersByPhone("0000000000")).thenReturn(List.of());

        assertThatThrownBy(() -> service.generateForParticipant("external"))
                .isInstanceOf(InvalidTransactionStateException.class);
    }
}
