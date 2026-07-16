package cz.jbenak.ncrm_backend.controler;

import cz.jbenak.ncrm_backend.model.dto.invoice.InvoiceDto;
import cz.jbenak.ncrm_backend.model.dto.invoice.IssueInvoiceRequest;
import cz.jbenak.ncrm_backend.services.InvoiceService;
import cz.jbenak.ncrm_backend.services.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-16
 * REST API for invoicing of completed orders: issuing an invoice (with the cash or bank transfer
 * payment type), reading issued invoices, printing the invoice as a PDF (Czech VAT invoice layout
 * with a payment QR code) and sending the invoice to the customer by e-mail.
 */
@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final ReportService reportService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'SALES_REPRESENTATIVE')")
    public List<InvoiceDto> findAll() {
        return invoiceService.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'SALES_REPRESENTATIVE', 'CUSTOMER')")
    public InvoiceDto findById(@PathVariable UUID id) {
        return invoiceService.findById(id);
    }

    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'SALES_REPRESENTATIVE', 'CUSTOMER')")
    public InvoiceDto findByOrder(@PathVariable UUID orderId) {
        return invoiceService.findByOrder(orderId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'SALES_REPRESENTATIVE')")
    public InvoiceDto issue(@Valid @RequestBody IssueInvoiceRequest request) {
        return invoiceService.issue(request);
    }

    @GetMapping("/{id}/print")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'SALES_REPRESENTATIVE', 'CUSTOMER')")
    public ResponseEntity<byte[]> print(@PathVariable UUID id) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"faktura.pdf\"")
                .body(reportService.invoicePrintReport(id));
    }

    @PostMapping("/{id}/send")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'SALES_REPRESENTATIVE')")
    public void sendByEmail(@PathVariable UUID id) {
        invoiceService.sendByEmail(id);
    }
}
