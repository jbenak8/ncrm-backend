package cz.jbenak.ncrm_backend.controler;

import cz.jbenak.ncrm_backend.services.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-11
 * REST API providing PDF reports (JasperReports) for all user roles:
 * owner (sales overview), sales representative (performance) and customer (own orders).
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/owner/sales-overview")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<byte[]> ownerSalesReport() {
        return pdf("sales-overview.pdf", reportService.ownerSalesReport());
    }

    @GetMapping("/representative/performance")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'SALES_REPRESENTATIVE')")
    public ResponseEntity<byte[]> salesRepresentativePerformanceReport() {
        return pdf("performance.pdf", reportService.salesRepresentativePerformanceReport());
    }

    @GetMapping("/customer/{customerId}/orders")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'SALES_REPRESENTATIVE', 'CUSTOMER')")
    public ResponseEntity<byte[]> customerOrdersReport(@PathVariable UUID customerId) {
        return pdf("orders.pdf", reportService.customerOrdersReport(customerId));
    }

    @GetMapping("/order/{orderId}/print")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'SALES_REPRESENTATIVE', 'CUSTOMER')")
    public ResponseEntity<byte[]> orderPrintReport(@PathVariable UUID orderId) {
        return pdf("objednavka.pdf", reportService.orderPrintReport(orderId));
    }

    private ResponseEntity<byte[]> pdf(String filename, byte[] content) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(content);
    }
}
