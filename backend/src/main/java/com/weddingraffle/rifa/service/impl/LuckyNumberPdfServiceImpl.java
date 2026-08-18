package com.weddingraffle.rifa.service.impl;

import com.weddingraffle.rifa.entity.PaymentStatus;
import com.weddingraffle.rifa.entity.Transaction;
import com.weddingraffle.rifa.exception.InvalidTransactionStateException;
import com.weddingraffle.rifa.exception.ResourceNotFoundException;
import com.weddingraffle.rifa.repository.TransactionRepository;
import com.weddingraffle.rifa.service.LuckyNumberPdfService;
import com.weddingraffle.rifa.service.LuckyNumberService;
import java.awt.Color;
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
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LuckyNumberPdfServiceImpl implements LuckyNumberPdfService {

    private static final Color CHARCOAL = new Color(46, 42, 39);
    private static final Color TERRACOTTA = new Color(184, 92, 74);
    private static final Color GOLD = new Color(201, 162, 39);
    private static final Color BLUSH = new Color(243, 225, 220);
    private static final float PAGE_MARGIN = 48;
    private static final float HEADER_HEIGHT = 92;
    private static final float FOOTER_HEIGHT = 44;
    private static final float CONTENT_BOTTOM = FOOTER_HEIGHT + 20;
    private static final int NUMBER_COLUMNS = 4;
    private static final float NUMBER_GAP = 10;
    private static final float NUMBER_CARD_HEIGHT = 34;
    private static final float NUMBER_CARD_RADIUS = 8;

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
            PDFont titleFont = new PDType1Font(Standard14Fonts.FontName.TIMES_BOLD);
            PDFont numberFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDFont textFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDFont emojiFont = loadEmojiFont(document);

            PdfPage currentPage = createPage(document, 1, titleFont, textFont, false);
            float y =
                    writeTransactionDetails(currentPage.content(), titleFont, textFont, emojiFont, transaction, luckyNumbers.size());
            y = writeNumbersSectionHeader(currentPage.content(), titleFont, textFont, y, luckyNumbers.size(), false);
            float numberCardWidth = (PDRectangle.A4.getWidth() - (PAGE_MARGIN * 2) - (NUMBER_GAP * (NUMBER_COLUMNS - 1)))
                    / NUMBER_COLUMNS;

            for (int index = 0; index < luckyNumbers.size(); index++) {
                if (y - NUMBER_CARD_HEIGHT < CONTENT_BOTTOM) {
                    closePage(currentPage, textFont);
                    currentPage = createPage(document, currentPage.number() + 1, titleFont, textFont, true);
                    y = writeNumbersSectionHeader(
                            currentPage.content(),
                            titleFont,
                            textFont,
                            PDRectangle.A4.getHeight() - HEADER_HEIGHT - 34,
                            luckyNumbers.size(),
                            true);
                }

                int column = index % NUMBER_COLUMNS;
                float x = PAGE_MARGIN + (column * (numberCardWidth + NUMBER_GAP));
                drawNumberCard(currentPage.content(), numberFont, x, y, numberCardWidth, luckyNumbers.get(index));

                if (column == NUMBER_COLUMNS - 1 || index == luckyNumbers.size() - 1) {
                    y -= NUMBER_CARD_HEIGHT + NUMBER_GAP;
                }
            }

            closePage(currentPage, textFont);

            document.save(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to generate lucky numbers PDF.", exception);
        }
    }

    private static PdfPage createPage(
            PDDocument document, int pageNumber, PDFont titleFont, PDFont textFont, boolean isContinuation) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        PDPageContentStream content = new PDPageContentStream(document, page);
        float pageHeight = page.getMediaBox().getHeight();

        content.setNonStrokingColor(CHARCOAL);
        content.addRect(0, pageHeight - HEADER_HEIGHT, page.getMediaBox().getWidth(), HEADER_HEIGHT);
        content.fill();

        writeLine(content, titleFont, 24, PAGE_MARGIN, pageHeight - 42, "Presente Premiado", GOLD);
        writeLine(
                content,
                textFont,
                10,
                PAGE_MARGIN,
                pageHeight - 66,
                isContinuation ? "Numeros da sorte - continuacao" : "Numeros da sorte",
                Color.WHITE);
        return new PdfPage(content, pageNumber);
    }

    private static float writeTransactionDetails(
            PDPageContentStream content,
            PDFont titleFont,
            PDFont textFont,
            PDFont emojiFont,
            Transaction transaction,
            int luckyNumberCount)
            throws IOException {
        float y = PDRectangle.A4.getHeight() - HEADER_HEIGHT - 34;
        writeLine(content, titleFont, 19, PAGE_MARGIN, y, "Seus numeros da sorte", CHARCOAL);
        y -= 30;
        y = writeWrappedText(
                content,
                textFont,
                12,
                PAGE_MARGIN,
                y,
                PDRectangle.A4.getWidth() - (PAGE_MARGIN * 2),
                "Obrigado pela sua contribuicao, " + transaction.getName() + ".",
                CHARCOAL);
        y -= 8;

        if (transaction.getParticipantFlagName() != null) {
            writeParticipantFlag(content, textFont, emojiFont, transaction, (int) PAGE_MARGIN, (int) y);
            y -= 26;
        }

        content.setNonStrokingColor(GOLD);
        content.addRect(PAGE_MARGIN, y - 4, 72, 2);
        content.fill();
        y -= 28;

        String summary = luckyNumberCount == 1 ? "1 numero gerado" : luckyNumberCount + " numeros gerados";
        writeLine(content, textFont, 11, PAGE_MARGIN, y, summary, TERRACOTTA);
        return y - 30;
    }

    private static float writeNumbersSectionHeader(
            PDPageContentStream content,
            PDFont titleFont,
            PDFont textFont,
            float y,
            int luckyNumberCount,
            boolean isContinuation)
            throws IOException {
        writeLine(
                content,
                titleFont,
                15,
                PAGE_MARGIN,
                y,
                isContinuation ? "Numeros da sorte - continuacao" : "Numeros gerados",
                CHARCOAL);
        y -= 18;
        writeLine(
                content,
                textFont,
                9,
                PAGE_MARGIN,
                y,
                "Comprovante com " + luckyNumberCount + " numero(s) desta compra.",
                TERRACOTTA);
        return y - 18;
    }

    private static float writeWrappedText(
            PDPageContentStream content,
            PDFont font,
            int fontSize,
            float x,
            float y,
            float maxWidth,
            String text,
            Color color)
            throws IOException {
        StringBuilder line = new StringBuilder();
        for (String word : text.split("\\s+")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (font.getStringWidth(candidate) / 1000 * fontSize > maxWidth && !line.isEmpty()) {
                writeLine(content, font, fontSize, x, y, line.toString(), color);
                y -= fontSize + 4;
                line.setLength(0);
            }
            if (!line.isEmpty()) {
                line.append(' ');
            }
            line.append(word);
        }
        if (!line.isEmpty()) {
            writeLine(content, font, fontSize, x, y, line.toString(), color);
            y -= fontSize + 4;
        }
        return y;
    }

    private static void drawNumberCard(
            PDPageContentStream content, PDFont numberFont, float x, float y, float width, String luckyNumber) throws IOException {
        float cardY = y - NUMBER_CARD_HEIGHT;

        content.setNonStrokingColor(BLUSH);
        addRoundedRectangle(content, x, cardY, width, NUMBER_CARD_HEIGHT, NUMBER_CARD_RADIUS);
        content.fill();

        content.setStrokingColor(TERRACOTTA);
        addRoundedRectangle(content, x, cardY, width, NUMBER_CARD_HEIGHT, NUMBER_CARD_RADIUS);
        content.stroke();

        float textWidth = numberFont.getStringWidth(luckyNumber) / 1000 * 14;
        writeLine(content, numberFont, 14, x + ((width - textWidth) / 2), y - 22, luckyNumber, CHARCOAL);
    }

    private static void addRoundedRectangle(
            PDPageContentStream content, float x, float y, float width, float height, float radius) throws IOException {
        float right = x + width;
        float top = y + height;
        float curve = radius * 0.55228475f;

        content.moveTo(x + radius, y);
        content.lineTo(right - radius, y);
        content.curveTo(right - radius + curve, y, right, y + radius - curve, right, y + radius);
        content.lineTo(right, top - radius);
        content.curveTo(right, top - radius + curve, right - radius + curve, top, right - radius, top);
        content.lineTo(x + radius, top);
        content.curveTo(x + radius - curve, top, x, top - radius + curve, x, top - radius);
        content.lineTo(x, y + radius);
        content.curveTo(x, y + radius - curve, x + radius - curve, y, x + radius, y);
        content.closePath();
    }

    private static void closePage(PdfPage page, PDFont textFont) throws IOException {
        writeLine(
                page.content(),
                textFont,
                9,
                PAGE_MARGIN,
                FOOTER_HEIGHT - 10,
                "Boa sorte no sorteio!",
                TERRACOTTA);
        String pageText = "Pagina " + page.number();
        float pageTextWidth = textFont.getStringWidth(pageText) / 1000 * 9;
        writeLine(
                page.content(),
                textFont,
                9,
                PDRectangle.A4.getWidth() - PAGE_MARGIN - pageTextWidth,
                FOOTER_HEIGHT - 10,
                pageText,
                CHARCOAL);
        page.content().close();
    }

    private static void writeLine(PDPageContentStream content, PDFont font, int fontSize, float x, float y, String text, Color color)
            throws IOException {
        boolean textBlockStarted = false;
        content.beginText();
        textBlockStarted = true;
        try {
            content.setFont(font, fontSize);
            content.setNonStrokingColor(color);
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

        writeLine(content, textFont, 12, x, y, "Sua bandeira:", CHARCOAL);
        if (emojiFont == null) {
            writeLine(content, textFont, 12, x + 95, y, transaction.getParticipantFlagName(), CHARCOAL);
            return;
        }

        try {
            if (transaction.getParticipantFlagEmoji() != null) {
                writeLine(content, emojiFont, 14, x + 95, y, transaction.getParticipantFlagEmoji(), CHARCOAL);
                writeLine(content, textFont, 12, x + 125, y, transaction.getParticipantFlagName(), CHARCOAL);
                return;
            }
        } catch (IOException | RuntimeException exception) {
            // Falls back to the flag name when the local PDF font cannot render the emoji glyph.
        }
        writeLine(content, textFont, 12, x + 95, y, transaction.getParticipantFlagName(), CHARCOAL);
    }

    private record PdfPage(PDPageContentStream content, int number) {}
}
