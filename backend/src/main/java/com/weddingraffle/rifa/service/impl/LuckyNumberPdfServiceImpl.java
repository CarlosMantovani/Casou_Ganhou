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
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
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

                writeLine(content, titleFont, 20, 72, 720, "Presente Premiado");
                writeLine(
                        content,
                        textFont,
                        13,
                        72,
                        680,
                        "Obrigado pela sua contribuicao, " + transaction.getName() + ".");
                writeLine(content, textFont, 13, 72, 655, "Seus numeros da sorte sao:");

                int y = 625;
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

    private static void writeLine(
            PDPageContentStream content, PDType1Font font, int fontSize, int x, int y, String text) throws IOException {
        content.beginText();
        content.setFont(font, fontSize);
        content.newLineAtOffset(x, y);
        content.showText(text);
        content.endText();
    }
}
