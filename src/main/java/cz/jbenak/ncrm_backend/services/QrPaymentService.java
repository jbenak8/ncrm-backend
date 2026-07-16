package cz.jbenak.ncrm_backend.services;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.EnumMap;
import java.util.Map;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-16
 * Generates the Czech payment QR code (the "QR platba" standard, SPD 1.0) printed on invoices
 * paid by bank transfer. The payment string carries the IBAN, amount, currency, variable symbol
 * and a message for the recipient; banking applications scan it to pre-fill the payment order.
 */
@Slf4j
@Service
public class QrPaymentService {

    private static final int QR_SIZE = 300;

    /**
     * Builds the SPD 1.0 payment string, e.g.
     * {@code SPD*1.0*ACC:CZ6508000000192000145399*AM:1234.50*CC:CZK*X-VS:2026000123*MSG:FAKTURA 2026-000123}.
     */
    public String buildPaymentString(String iban, BigDecimal amount, String currency,
                                     String variableSymbol, String message) {
        StringBuilder spd = new StringBuilder("SPD*1.0*ACC:").append(iban.replace(" ", ""));
        if (amount != null) {
            spd.append("*AM:").append(amount.stripTrailingZeros().toPlainString());
        }
        if (currency != null) {
            spd.append("*CC:").append(currency);
        }
        if (variableSymbol != null) {
            spd.append("*X-VS:").append(variableSymbol);
        }
        if (message != null && !message.isBlank()) {
            spd.append("*MSG:").append(sanitizeMessage(message));
        }
        return spd.toString();
    }

    /**
     * Renders the payment string as a QR code image usable as a JasperReports image parameter.
     */
    public BufferedImage generateQrImage(String paymentString) {
        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.MARGIN, 1);
            BitMatrix matrix = new QRCodeWriter().encode(paymentString, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE, hints);
            return MatrixToImageWriter.toBufferedImage(matrix);
        } catch (Exception e) {
            log.error("Failed to generate payment QR code for '{}'", paymentString, e);
            throw new IllegalStateException("Failed to generate payment QR code: " + e.getMessage(), e);
        }
    }

    /** The SPD standard allows only a restricted character set; diacritics and '*' are removed. */
    private String sanitizeMessage(String message) {
        String withoutDiacritics = Normalizer.normalize(message, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return withoutDiacritics.replace("*", " ").toUpperCase();
    }
}
