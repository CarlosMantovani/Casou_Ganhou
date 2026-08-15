package com.weddingraffle.rifa.service.impl;

import com.weddingraffle.rifa.entity.PaymentStatus;
import com.weddingraffle.rifa.entity.Transaction;
import com.weddingraffle.rifa.exception.InvalidTransactionStateException;
import com.weddingraffle.rifa.exception.ResourceNotFoundException;
import com.weddingraffle.rifa.repository.TransactionRepository;
import com.weddingraffle.rifa.service.LuckyNumberPdfService;
import com.weddingraffle.rifa.service.LuckyNumberService;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LuckyNumberPdfServiceImpl implements LuckyNumberPdfService {

    private final TransactionRepository transactionRepository;
    private final LuckyNumberService luckyNumberService;

    public LuckyNumberPdfServiceImpl(
            TransactionRepository transactionRepository, LuckyNumberService luckyNumberService) {
        this.transactionRepository = transactionRepository;
        this.luckyNumberService = luckyNumberService;
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generate(String externalReference) {
        Transaction transaction = transactionRepository
                .findByExternalReference(externalReference)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found."));

        if (transaction.getStatus() != PaymentStatus.APPROVED) {
            throw new InvalidTransactionStateException("Lucky numbers are available only for approved transactions.");
        }

        List<String> luckyNumbers = luckyNumberService.findNumbers(externalReference);
        if (luckyNumbers.isEmpty()) {
            throw new InvalidTransactionStateException("Approved transaction has no lucky numbers.");
        }

        return toPdf(transaction, luckyNumbers);
    }

    private byte[] toPdf(Transaction transaction, List<String> luckyNumbers) {
        try (PDDocument document = new PDDocument();
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                PDType1Font titleFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                PDType1Font textFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
                PDFont emojiFont = loadEmojiFont(document);

                writeLine(content, titleFont, 20, 72, 720, "Presente Premiado");
                writeLine(
                        content,
                        textFont,
                        13,
                        72,
                        680,
                        "Obrigado pela sua contribuicao, " + transaction.getName() + ".");
                writeParticipantFlag(content, textFont, emojiFont, transaction, 72, 655);
                writeLine(content, textFont, 13, 72, 625, "Seus numeros da sorte sao:");

                int y = 595;
                for (String luckyNumber : luckyNumbers) {
                    writeLine(content, titleFont, 16, 96, y, luckyNumber);
                    y -= 24;
                }

                writeLine(content, textFont, 13, 72, y - 12, "Boa sorte no sorteio!");
            }

            document.save(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to generate lucky numbers PDF.", exception);
        }
    }

    private static void writeLine(PDPageContentStream content, PDFont font, int fontSize, int x, int y, String text)
            throws IOException {
        boolean textBlockStarted = false;
        content.beginText();
        textBlockStarted = true;
        try {
            content.setFont(font, fontSize);
            content.newLineAtOffset(x, y);
            content.showText(text);
        } finally {
            if (textBlockStarted) {
                content.endText();
            }
        }
    }

    private static PDFont loadEmojiFont(PDDocument document) {
        List<Path> candidates = List.of(
                Path.of("C:\\Windows\\Fonts\\seguiemj.ttf"),
                Path.of("/usr/share/fonts/truetype/noto/NotoColorEmoji.ttf"),
                Path.of("/usr/share/fonts/truetype/noto/NotoEmoji-Regular.ttf"));

        for (Path candidate : candidates) {
            if (Files.exists(candidate)) {
                try {
                    return PDType0Font.load(document, candidate.toFile());
                } catch (IOException | RuntimeException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    private static void writeParticipantFlag(
            PDPageContentStream content, PDFont textFont, PDFont emojiFont, Transaction transaction, int x, int y)
            throws IOException {
        if (transaction.getParticipantFlagName() == null) {
            return;
        }

        writeLine(content, textFont, 13, x, y, "Sua bandeira:");
        if (emojiFont == null) {
            writeLine(content, textFont, 13, x + 95, y, transaction.getParticipantFlagName());
            return;
        }

        try {
            if (transaction.getParticipantFlagEmoji() != null) {
                writeLine(content, emojiFont, 14, x + 95, y, transaction.getParticipantFlagEmoji());
                writeLine(content, textFont, 13, x + 125, y, transaction.getParticipantFlagName());
                return;
            }
        } catch (IOException | RuntimeException exception) {
            // Falls back to the flag name when the local PDF font cannot render the emoji glyph.
        }
        writeLine(content, textFont, 13, x + 95, y, transaction.getParticipantFlagName());
    }
}
