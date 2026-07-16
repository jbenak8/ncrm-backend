package cz.jbenak.ncrm_backend.services;

import cz.jbenak.ncrm_backend.model.dto.invoice.InvoiceDto;
import cz.jbenak.ncrm_backend.model.dto.invoice.IssueInvoiceRequest;
import cz.jbenak.ncrm_backend.model.entity.NumberSequenceEntity;
import cz.jbenak.ncrm_backend.model.entity.invoice.InvoiceEntity;
import cz.jbenak.ncrm_backend.model.entity.invoice.InvoiceItemEntity;
import cz.jbenak.ncrm_backend.model.entity.order.OrderEntity;
import cz.jbenak.ncrm_backend.model.entity.order.OrderItemEntity;
import cz.jbenak.ncrm_backend.model.mapper.InvoiceMapper;
import cz.jbenak.ncrm_backend.repository.InvoiceRepository;
import cz.jbenak.ncrm_backend.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-16
 * Service for issuing invoices for completed customer orders. The invoice is an immutable
 * snapshot of the order: item names, net prices and VAT rates (taken from the current item
 * prices) are copied into the invoice lines and the VAT amounts and totals are computed
 * server-side. The invoice number is drawn from the {@code INVOICE} number sequence with
 * a legacy fallback, and the variable symbol is derived from the digits of the invoice number.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class InvoiceService {

    /** Default invoice due period in days when the request does not specify one. */
    static final int DEFAULT_DUE_DAYS = 14;

    private final InvoiceRepository invoiceRepository;
    private final OrderRepository orderRepository;
    private final InvoiceMapper invoiceMapper;
    private final NumberSequenceService numberSequenceService;
    private final ReportService reportService;
    private final InvoiceEmailService invoiceEmailService;

    @Transactional(readOnly = true)
    public List<InvoiceDto> findAll() {
        return invoiceMapper.toDtoList(invoiceRepository.findAll());
    }

    @Transactional(readOnly = true)
    public InvoiceDto findById(UUID id) {
        return invoiceMapper.toDto(getInvoice(id));
    }

    @Transactional(readOnly = true)
    public InvoiceDto findByOrder(UUID orderId) {
        return invoiceMapper.toDto(invoiceRepository.findByOrderId(orderId)
                .orElseThrow(() -> new NotFoundException("Invoice for order " + orderId + " not found")));
    }

    /**
     * Issues an invoice for a completed order. The order must be in the {@code COMPLETED} status
     * and must not be invoiced yet. All item values are snapshotted from the order lines and the
     * current item catalogue (VAT rates), so the invoice never changes retroactively.
     */
    public InvoiceDto issue(IssueInvoiceRequest request) {
        OrderEntity order = orderRepository.findById(request.orderId())
                .orElseThrow(() -> new NotFoundException("Order", request.orderId()));
        if (order.getStatus() != OrderEntity.OrderStatus.COMPLETED) {
            throw new IllegalStateException("Invoice can only be issued for a COMPLETED order, "
                    + "order " + order.getOrderNumber() + " is " + order.getStatus());
        }
        if (invoiceRepository.existsByOrderId(order.getId())) {
            throw new IllegalStateException("Order " + order.getOrderNumber() + " is already invoiced");
        }
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        InvoiceEntity invoice = new InvoiceEntity();
        invoice.setInvoiceNumber(generateInvoiceNumber());
        invoice.setOrder(order);
        invoice.setPaymentType(request.paymentType());
        invoice.setIssueDate(today);
        invoice.setTaxDate(today);
        int dueDays = request.dueDays() == null ? DEFAULT_DUE_DAYS : request.dueDays();
        invoice.setDueDate(request.paymentType() == InvoiceEntity.PaymentType.CASH
                ? today
                : today.plusDays(dueDays));
        invoice.setVariableSymbol(variableSymbol(invoice.getInvoiceNumber()));
        invoice.setCurrency(order.getCurrency());
        invoice.setNote(request.note());
        applyItems(invoice, order);
        InvoiceEntity saved = invoiceRepository.save(invoice);
        log.info("Issued invoice {} for order {} ({} item(s), total {} {}, payment {})",
                saved.getInvoiceNumber(), order.getOrderNumber(), saved.getItems().size(),
                saved.getTotalGross(), saved.getCurrency(), saved.getPaymentType());
        return invoiceMapper.toDto(saved);
    }

    /** Builds the invoice lines as snapshots of the order lines and computes the VAT and totals. */
    private void applyItems(InvoiceEntity invoice, OrderEntity order) {
        BigDecimal totalNet = BigDecimal.ZERO;
        BigDecimal totalVat = BigDecimal.ZERO;
        for (OrderItemEntity orderItem : order.getItems()) {
            InvoiceItemEntity line = new InvoiceItemEntity();
            if (orderItem.getItem() != null) {
                line.setItemCode(orderItem.getItem().getCode());
                line.setItemName(orderItem.getItem().getName());
                line.setUnit(orderItem.getItem().getUnit());
                if (orderItem.getItem().getPrice() != null) {
                    line.setVatRate(orderItem.getItem().getPrice().getVatRate());
                }
            }
            line.setQuantity(orderItem.getQuantity());
            line.setUnitPrice(orderItem.getUnitPrice());
            BigDecimal net = orderItem.getTotalPrice() == null
                    ? orderItem.getUnitPrice().multiply(orderItem.getQuantity())
                    : orderItem.getTotalPrice();
            BigDecimal vat = line.getVatRate() == null
                    ? BigDecimal.ZERO
                    : net.multiply(line.getVatRate())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            line.setTotalNet(net);
            line.setTotalVat(vat);
            line.setTotalGross(net.add(vat));
            invoice.addItem(line);
            totalNet = totalNet.add(net);
            totalVat = totalVat.add(vat);
        }
        invoice.setTotalNet(totalNet);
        invoice.setTotalVat(totalVat);
        invoice.setTotalGross(totalNet.add(totalVat));
    }

    /**
     * Sends the invoice to the customer by e-mail with the printable PDF attached.
     */
    public void sendByEmail(UUID id) {
        InvoiceEntity invoice = getInvoice(id);
        byte[] pdf = reportService.invoicePrintReport(id);
        invoiceEmailService.sendInvoice(invoice, pdf);
    }

    InvoiceEntity getInvoice(UUID id) {
        return invoiceRepository.findById(id).orElseThrow(() -> new NotFoundException("Invoice", id));
    }

    /**
     * Generates the invoice number from the configured {@code INVOICE} number sequence. When no
     * sequence is defined by the administrator, a legacy fallback number is generated instead.
     */
    private String generateInvoiceNumber() {
        return numberSequenceService.tryNextNumber(NumberSequenceEntity.SequenceType.INVOICE)
                .orElseGet(() -> "FAK-" + LocalDate.now(ZoneId.systemDefault()).format(DateTimeFormatter.BASIC_ISO_DATE)
                        + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }

    /**
     * Derives the variable symbol from the invoice number: all digits joined together,
     * truncated to the last 10 digits (the maximum length of a variable symbol).
     */
    static String variableSymbol(String invoiceNumber) {
        String digits = invoiceNumber.replaceAll("\\D", "");
        if (digits.isEmpty()) {
            return null;
        }
        return digits.length() <= 10 ? digits : digits.substring(digits.length() - 10);
    }
}
