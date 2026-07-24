package cz.jbenak.ncrm_backend.services;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests of {@link QrPaymentService} verifying the SPD 1.0 payment string format
 * and the QR code image rendering.
 */
class QrPaymentServiceTest {

    private final QrPaymentService qrPaymentService = new QrPaymentService();

    @Test
    void buildsFullPaymentString() {
        String spd = qrPaymentService.buildPaymentString("CZ65 0800 0000 1920 0014 5399",
                new BigDecimal("1234.50"), "CZK", "2026000042", "Faktura č. 2026-000042");

        assertThat(spd).isEqualTo(
                "SPD*1.0*ACC:CZ6508000000192000145399*AM:1234.5*CC:CZK*X-VS:2026000042*MSG:FAKTURA C. 2026-000042");
    }

    @Test
    void buildsMinimalPaymentString() {
        String spd = qrPaymentService.buildPaymentString("CZ6508000000192000145399", null, null, null, null);

        assertThat(spd).isEqualTo("SPD*1.0*ACC:CZ6508000000192000145399");
    }

    @Test
    void generatesQrImage() {
        String spd = qrPaymentService.buildPaymentString("CZ6508000000192000145399",
                new BigDecimal("121.00"), "CZK", "2026000042", "FAKTURA 2026-000042");

        BufferedImage image = qrPaymentService.generateQrImage(spd);

        assertThat(image).isNotNull();
        assertThat(image.getWidth()).isGreaterThan(0);
        assertThat(image.getHeight()).isGreaterThan(0);
    }
}
