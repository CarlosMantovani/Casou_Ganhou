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
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LuckyNumberPdfServiceImpl implements LuckyNumberPdfService {

    private static final String WEDDING_COUPLE_TITLE = "Paula e José Carlos";
    private static final String RAFFLE_TITLE = "Presente Premiado";
    private static final Color IVORY = new Color(247, 241, 230);
    private static final Color IVORY_DEEP = new Color(240, 232, 216);
    private static final Color CHARCOAL = new Color(43, 36, 25);
    private static final Color WARM_GRAY = new Color(91, 81, 64);
    private static final Color GREEN = new Color(36, 64, 46);
    private static final Color GREEN_DEEP = new Color(21, 42, 29);
    private static final Color WINE = new Color(122, 46, 51);
    private static final Color GOLD = new Color(184, 147, 90);
    private static final float PAGE_MARGIN = 48;
    private static final float HEADER_HEIGHT = 112;
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
        List<String> previousLuckyNumbers = luckyNumberService.findPreviousApprovedNumbers(
                transaction.getPhone(), transaction.getExternalReference());

        return toPdf(transaction, luckyNumbers, previousLuckyNumbers);
    }

    private byte[] toPdf(Transaction transaction, List<String> luckyNumbers, List<String> previousLuckyNumbers) {
        try (PDDocument document = new PDDocument();
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDFont titleFont = new PDType1Font(Standard14Fonts.FontName.TIMES_BOLD);
            PDFont numberFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDFont textFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            PdfPage currentPage = createPage(document, 1, titleFont, textFont, false);
            float numberCardWidth =
                    (PDRectangle.A4.getWidth() - (PAGE_MARGIN * 2) - (NUMBER_GAP * (NUMBER_COLUMNS - 1)))
                            / NUMBER_COLUMNS;
            float y = writeTransactionDetails(
                    currentPage.content(),
                    titleFont,
                    textFont,
                    transaction,
                    luckyNumbers.size(),
                    previousLuckyNumbers.size());

            if (!previousLuckyNumbers.isEmpty()) {
                PageCursor cursor = writeNumberSection(
                        document,
                        currentPage,
                        titleFont,
                        textFont,
                        numberFont,
                        y,
                        "Números adquiridos agora",
                        luckyNumbers,
                        numberCardWidth);
                currentPage = cursor.page();
                y = cursor.y() - 14;
                cursor = writeNumberSection(
                        document,
                        currentPage,
                        titleFont,
                        textFont,
                        numberFont,
                        y,
                        "Números adquiridos anteriormente",
                        previousLuckyNumbers,
                        numberCardWidth);
                currentPage = cursor.page();
            } else {
                PageCursor cursor = writeNumberSection(
                        document,
                        currentPage,
                        titleFont,
                        textFont,
                        numberFont,
                        y,
                        "Números gerados",
                        luckyNumbers,
                        numberCardWidth);
                currentPage = cursor.page();
            }

            closePage(currentPage, textFont);

            document.save(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to generate lucky numbers PDF.", exception);
        }
    }

    private static PdfPage createPage(
            PDDocument document, int pageNumber, PDFont titleFont, PDFont textFont, boolean isContinuation)
            throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        PDPageContentStream content = new PDPageContentStream(document, page);
        float pageHeight = page.getMediaBox().getHeight();

        content.setNonStrokingColor(IVORY);
        content.addRect(0, 0, page.getMediaBox().getWidth(), pageHeight);
        content.fill();

        content.setNonStrokingColor(GREEN);
        content.addRect(0, pageHeight - HEADER_HEIGHT, page.getMediaBox().getWidth(), HEADER_HEIGHT);
        content.fill();

        content.setStrokingColor(GOLD);
        content.setLineWidth(1.2f);
        content.moveTo(PAGE_MARGIN, pageHeight - HEADER_HEIGHT + 18);
        content.lineTo(page.getMediaBox().getWidth() - PAGE_MARGIN, pageHeight - HEADER_HEIGHT + 18);
        content.stroke();

        writeLine(content, titleFont, 26, PAGE_MARGIN, pageHeight - 42, WEDDING_COUPLE_TITLE, GOLD);
        writeLine(content, titleFont, 18, PAGE_MARGIN, pageHeight - 68, RAFFLE_TITLE, Color.WHITE);
        writeLine(
                content,
                textFont,
                10,
                PAGE_MARGIN,
                pageHeight - 92,
                isContinuation ? "Números da sorte - continuação" : "Números da sorte",
                IVORY_DEEP);
        return new PdfPage(content, pageNumber);
    }

    private static float writeTransactionDetails(
            PDPageContentStream content,
            PDFont titleFont,
            PDFont textFont,
            Transaction transaction,
            int currentLuckyNumberCount,
            int previousLuckyNumberCount)
            throws IOException {
        float y = PDRectangle.A4.getHeight() - HEADER_HEIGHT - 34;
        writeLine(content, titleFont, 19, PAGE_MARGIN, y, "Seus números da sorte", CHARCOAL);
        y -= 30;
        y = writeWrappedText(
                content,
                textFont,
                12,
                PAGE_MARGIN,
                y,
                PDRectangle.A4.getWidth() - (PAGE_MARGIN * 2),
                "Obrigado pela sua contribuição, " + transaction.getName() + ".",
                CHARCOAL);
        y -= 8;

        if (transaction.getParticipantFlagName() != null) {
            writeParticipantFlag(content, textFont, transaction, (int) PAGE_MARGIN, (int) y);
            y -= 26;
        }

        content.setNonStrokingColor(GOLD);
        content.addRect(PAGE_MARGIN, y - 4, 72, 2);
        content.fill();
        y -= 28;

        if (previousLuckyNumberCount > 0) {
            writeLine(
                    content,
                    textFont,
                    11,
                    PAGE_MARGIN,
                    y,
                    "Números adquiridos anteriormente: " + previousLuckyNumberCount,
                    WARM_GRAY);
            y -= 18;
            writeLine(
                    content,
                    textFont,
                    11,
                    PAGE_MARGIN,
                    y,
                    "Números adquiridos agora: " + currentLuckyNumberCount,
                    WINE);
            y -= 18;
            writeLine(
                    content,
                    textFont,
                    11,
                    PAGE_MARGIN,
                    y,
                    "Total de números com esta compra: " + (previousLuckyNumberCount + currentLuckyNumberCount),
                    GREEN);
            return y - 30;
        }

        writeLine(content, textFont, 11, PAGE_MARGIN, y, formatGeneratedNumberCount(currentLuckyNumberCount), WINE);
        return y - 30;
    }

    private static PageCursor writeNumberSection(
            PDDocument document,
            PdfPage currentPage,
            PDFont titleFont,
            PDFont textFont,
            PDFont numberFont,
            float y,
            String title,
            List<String> luckyNumbers,
            float numberCardWidth)
            throws IOException {
        if (y - 30 < CONTENT_BOTTOM) {
            closePage(currentPage, textFont);
            currentPage = createPage(document, currentPage.number() + 1, titleFont, textFont, true);
            y = PDRectangle.A4.getHeight() - HEADER_HEIGHT - 34;
        }

        y = writeNumbersSectionHeader(currentPage.content(), titleFont, y, title);

        for (int index = 0; index < luckyNumbers.size(); index++) {
            if (y - NUMBER_CARD_HEIGHT < CONTENT_BOTTOM) {
                closePage(currentPage, textFont);
                currentPage = createPage(document, currentPage.number() + 1, titleFont, textFont, true);
                y = writeNumbersSectionHeader(
                        currentPage.content(), titleFont, PDRectangle.A4.getHeight() - HEADER_HEIGHT - 34, title);
            }

            int column = index % NUMBER_COLUMNS;
            float x = PAGE_MARGIN + (column * (numberCardWidth + NUMBER_GAP));
            drawNumberCard(currentPage.content(), numberFont, x, y, numberCardWidth, luckyNumbers.get(index));

            if (column == NUMBER_COLUMNS - 1 || index == luckyNumbers.size() - 1) {
                y -= NUMBER_CARD_HEIGHT + NUMBER_GAP;
            }
        }

        return new PageCursor(currentPage, y);
    }

    private static float writeNumbersSectionHeader(PDPageContentStream content, PDFont titleFont, float y, String title)
            throws IOException {
        writeLine(content, titleFont, 15, PAGE_MARGIN, y, title, CHARCOAL);
        return y - 24;
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
            PDPageContentStream content, PDFont numberFont, float x, float y, float width, String luckyNumber)
            throws IOException {
        float cardY = y - NUMBER_CARD_HEIGHT;

        content.setNonStrokingColor(IVORY_DEEP);
        addRoundedRectangle(content, x, cardY, width, NUMBER_CARD_HEIGHT, NUMBER_CARD_RADIUS);
        content.fill();

        content.setStrokingColor(GOLD);
        addRoundedRectangle(content, x, cardY, width, NUMBER_CARD_HEIGHT, NUMBER_CARD_RADIUS);
        content.stroke();

        float textWidth = numberFont.getStringWidth(luckyNumber) / 1000 * 14;
        writeLine(content, numberFont, 14, x + ((width - textWidth) / 2), y - 22, luckyNumber, GREEN_DEEP);
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
        writeLine(page.content(), textFont, 9, PAGE_MARGIN, FOOTER_HEIGHT - 10, "Boa sorte no sorteio!", WINE);
        String pageText = "Página " + page.number();
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

    private static void writeLine(
            PDPageContentStream content, PDFont font, int fontSize, float x, float y, String text, Color color)
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

    private static void writeParticipantFlag(
            PDPageContentStream content, PDFont textFont, Transaction transaction, int x, int y) throws IOException {
        if (transaction.getParticipantFlagName() == null) {
            return;
        }

        writeLine(content, textFont, 12, x, y, "Sua bandeira: " + transaction.getParticipantFlagName(), CHARCOAL);
    }

    private static String formatNumberCount(int count) {
        return count == 1 ? "1 número" : count + " números";
    }

    private static String formatGeneratedNumberCount(int count) {
        return count == 1 ? "1 número gerado" : count + " números gerados";
    }

    private record PdfPage(PDPageContentStream content, int number) {}

    private record PageCursor(PdfPage page, float y) {}
}
