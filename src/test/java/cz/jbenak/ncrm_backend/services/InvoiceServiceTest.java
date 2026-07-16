package cz.jbenak.ncrm_backend.services;

import cz.jbenak.ncrm_backend.model.dto.invoice.IssueInvoiceRequest;
import cz.jbenak.ncrm_backend.model.entity.NumberSequenceEntity;
import cz.jbenak.ncrm_backend.model.entity.invoice.InvoiceEntity;
import cz.jbenak.ncrm_backend.model.entity.order.OrderEntity;
import cz.jbenak.ncrm_backend.model.entity.order.OrderItemEntity;
import cz.jbenak.ncrm_backend.model.entity.store.ItemEntity;
import cz.jbenak.ncrm_backend.model.entity.store.ItemPriceEntity;
import cz.jbenak.ncrm_backend.model.mapper.InvoiceMapper;
import cz.jbenak.ncrm_backend.repository.InvoiceRepository;
import cz.jbenak.ncrm_backend.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests of {@link InvoiceService} verifying issuing invoices for completed orders:
 * status and duplicity validations, VAT computation from the item price snapshots,
 * due date resolution by the payment type, the variable symbol derivation and sending
 * the invoice by e-mail with the printable PDF.
 */
@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private InvoiceMapper invoiceMapper;
    @Mock
    private NumberSequenceService numberSequenceService;
    @Mock
    private ReportService reportService;
    @Mock
    private InvoiceEmailService invoiceEmailService;

    @InjectMocks
    private InvoiceService invoiceService;

    private UUID orderId;
    private OrderEntity order;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        order = completedOrder();
        lenient().when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        lenient().when(invoiceRepository.existsByOrderId(orderId)).thenReturn(false);
        lenient().when(invoiceRepository.save(any(InvoiceEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(numberSequenceService.tryNextNumber(NumberSequenceEntity.SequenceType.INVOICE))
                .thenReturn(Optional.of("2026-000042"));
    }

    @Test
    void issueCreatesInvoiceSnapshotWithVat() {
        invoiceService.issue(new IssueInvoiceRequest(orderId, InvoiceEntity.PaymentType.TRANSFER, null, "Poznámka"));

        ArgumentCaptor<InvoiceEntity> captor = ArgumentCaptor.forClass(InvoiceEntity.class);
        verify(invoiceRepository).save(captor.capture());
        InvoiceEntity invoice = captor.getValue();
        assertThat(invoice.getInvoiceNumber()).isEqualTo("2026-000042");
        assertThat(invoice.getVariableSymbol()).isEqualTo("2026000042");
        assertThat(invoice.getCurrency()).isEqualTo("CZK");
        assertThat(invoice.getItems()).hasSize(1);
        assertThat(invoice.getItems().getFirst().getItemName()).isEqualTo("Papír A4");
        assertThat(invoice.getItems().getFirst().getVatRate()).isEqualByComparingTo("21.00");
        assertThat(invoice.getTotalNet()).isEqualByComparingTo("100.00");
        assertThat(invoice.getTotalVat()).isEqualByComparingTo("21.00");
        assertThat(invoice.getTotalGross()).isEqualByComparingTo("121.00");
    }

    @Test
    void issueTransferUsesDefaultDueDays() {
        invoiceService.issue(new IssueInvoiceRequest(orderId, InvoiceEntity.PaymentType.TRANSFER, null, null));

        ArgumentCaptor<InvoiceEntity> captor = ArgumentCaptor.forClass(InvoiceEntity.class);
        verify(invoiceRepository).save(captor.capture());
        InvoiceEntity invoice = captor.getValue();
        assertThat(invoice.getDueDate())
                .isEqualTo(invoice.getIssueDate().plusDays(InvoiceService.DEFAULT_DUE_DAYS));
    }

    @Test
    void issueCashIsDueImmediately() {
        invoiceService.issue(new IssueInvoiceRequest(orderId, InvoiceEntity.PaymentType.CASH, 30, null));

        ArgumentCaptor<InvoiceEntity> captor = ArgumentCaptor.forClass(InvoiceEntity.class);
        verify(invoiceRepository).save(captor.capture());
        InvoiceEntity invoice = captor.getValue();
        assertThat(invoice.getPaymentType()).isEqualTo(InvoiceEntity.PaymentType.CASH);
        assertThat(invoice.getDueDate()).isEqualTo(invoice.getIssueDate());
    }

    @Test
    void issueUsesFallbackNumberWhenSequenceMissing() {
        when(numberSequenceService.tryNextNumber(NumberSequenceEntity.SequenceType.INVOICE))
                .thenReturn(Optional.empty());

        invoiceService.issue(new IssueInvoiceRequest(orderId, InvoiceEntity.PaymentType.TRANSFER, null, null));

        ArgumentCaptor<InvoiceEntity> captor = ArgumentCaptor.forClass(InvoiceEntity.class);
        verify(invoiceRepository).save(captor.capture());
        assertThat(captor.getValue().getInvoiceNumber()).startsWith("FAK-");
    }

    @Test
    void issueRejectsNotCompletedOrder() {
        order.setStatus(OrderEntity.OrderStatus.IN_PROGRESS);

        IssueInvoiceRequest request = new IssueInvoiceRequest(orderId, InvoiceEntity.PaymentType.CASH, null, null);
        assertThatThrownBy(() -> invoiceService.issue(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("COMPLETED");
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void issueRejectsAlreadyInvoicedOrder() {
        when(invoiceRepository.existsByOrderId(orderId)).thenReturn(true);

        IssueInvoiceRequest request = new IssueInvoiceRequest(orderId, InvoiceEntity.PaymentType.CASH, null, null);
        assertThatThrownBy(() -> invoiceService.issue(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already invoiced");
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void issueThrowsWhenOrderMissing() {
        UUID missingId = UUID.randomUUID();
        when(orderRepository.findById(missingId)).thenReturn(Optional.empty());

        IssueInvoiceRequest request = new IssueInvoiceRequest(missingId, InvoiceEntity.PaymentType.CASH, null, null);
        assertThatThrownBy(() -> invoiceService.issue(request))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void sendByEmailAttachesPdf() {
        UUID invoiceId = UUID.randomUUID();
        InvoiceEntity invoice = new InvoiceEntity();
        invoice.setInvoiceNumber("2026-000042");
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        byte[] pdf = {1, 2, 3};
        when(reportService.invoicePrintReport(invoiceId)).thenReturn(pdf);

        invoiceService.sendByEmail(invoiceId);

        verify(invoiceEmailService).sendInvoice(invoice, pdf);
    }

    @Test
    void variableSymbolIsDerivedFromDigits() {
        assertThat(InvoiceService.variableSymbol("2026-000042")).isEqualTo("2026000042");
        assertThat(InvoiceService.variableSymbol("FAK-20260716-ABCD")).isEqualTo("20260716");
        assertThat(InvoiceService.variableSymbol("F-2026-123456789012")).isEqualTo("3456789012");
        assertThat(InvoiceService.variableSymbol("ABC")).isNull();
    }

    private OrderEntity completedOrder() {
        ItemPriceEntity price = new ItemPriceEntity();
        price.setPrice(new BigDecimal("50.00"));
        price.setCurrency("CZK");
        price.setVatRate(new BigDecimal("21.00"));

        ItemEntity item = new ItemEntity();
        item.setCode("IT-1");
        item.setName("Papír A4");
        item.setUnit("ks");
        item.setPrice(price);

        OrderItemEntity line = new OrderItemEntity();
        line.setItem(item);
        line.setQuantity(new BigDecimal("2"));
        line.setUnitPrice(new BigDecimal("50.00"));
        line.setTotalPrice(new BigDecimal("100.00"));

        OrderEntity completedOrder = new OrderEntity();
        completedOrder.setId(orderId);
        completedOrder.setOrderNumber("ORD-1");
        completedOrder.setOrderDate(LocalDate.of(2026, Month.JULY, 1));
        completedOrder.setStatus(OrderEntity.OrderStatus.COMPLETED);
        completedOrder.setCurrency("CZK");
        completedOrder.setTotalPrice(new BigDecimal("100.00"));
        completedOrder.addItem(line);
        return completedOrder;
    }
}
