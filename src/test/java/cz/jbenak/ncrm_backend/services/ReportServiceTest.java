package cz.jbenak.ncrm_backend.services;

import cz.jbenak.ncrm_backend.model.dto.dashboard.DashboardDtos;
import cz.jbenak.ncrm_backend.model.dto.order.OrderDto;
import cz.jbenak.ncrm_backend.model.entity.company.CompanyEntity;
import cz.jbenak.ncrm_backend.model.entity.customer.CustomerEntity;
import cz.jbenak.ncrm_backend.model.entity.invoice.InvoiceEntity;
import cz.jbenak.ncrm_backend.model.entity.invoice.InvoiceItemEntity;
import cz.jbenak.ncrm_backend.model.entity.order.OrderEntity;
import cz.jbenak.ncrm_backend.model.entity.order.OrderItemEntity;
import cz.jbenak.ncrm_backend.model.entity.store.ItemEntity;
import cz.jbenak.ncrm_backend.repository.CompanyRepository;
import cz.jbenak.ncrm_backend.repository.InvoiceRepository;
import cz.jbenak.ncrm_backend.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Unit tests of {@link ReportService} verifying that the JasperReports JRXML templates compile
 * and produce valid PDF documents for all three user roles.
 */
@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private DashboardService dashboardService;
    @Mock
    private OrderService orderService;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private CompanyRepository companyRepository;
    @Mock
    private InvoiceRepository invoiceRepository;
    @Spy
    private QrPaymentService qrPaymentService = new QrPaymentService();

    @InjectMocks
    private ReportService reportService;

    private static void assertIsPdf(byte[] content) {
        assertThat(content).isNotEmpty();
        assertThat(new String(content, 0, 4)).isEqualTo("%PDF");
    }

    @Test
    void ownerSalesReportProducesPdf() {
        when(dashboardService.ordersByMonth()).thenReturn(List.of(
                new DashboardDtos.OrdersByMonth("2026-06", 4L, new BigDecimal("1000")),
                new DashboardDtos.OrdersByMonth("2026-07", 2L, new BigDecimal("500"))));

        assertIsPdf(reportService.ownerSalesReport());
    }

    @Test
    void salesRepresentativePerformanceReportProducesPdf() {
        when(dashboardService.salesByRepresentative()).thenReturn(List.of(
                new DashboardDtos.SalesByRepresentative(UUID.randomUUID(), "Jan Novák", 7L,
                        new BigDecimal("7000"), 4L)));

        assertIsPdf(reportService.salesRepresentativePerformanceReport());
    }

    @Test
    void customerOrdersReportProducesPdf() {
        UUID customerId = UUID.randomUUID();
        when(orderService.findByCustomer(customerId)).thenReturn(List.of(
                new OrderDto(UUID.randomUUID(), "ORD-1", customerId, "ACME", null, null, null, null,
                        null, null, LocalDate.of(2026, Month.JULY, 1), OrderEntity.OrderStatus.NEW,
                        new BigDecimal("123.45"), "CZK", null, List.of()),
                new OrderDto(UUID.randomUUID(), "ORD-2", customerId, "ACME", null, null, null, null,
                        null, null, null, null, new BigDecimal("50"), "CZK", null, List.of())));

        assertIsPdf(reportService.customerOrdersReport(customerId));
    }

    @Test
    void customerOrdersReportWorksWithNoOrders() {
        UUID customerId = UUID.randomUUID();
        when(orderService.findByCustomer(customerId)).thenReturn(List.of());

        assertIsPdf(reportService.customerOrdersReport(customerId));
    }

    @Test
    void orderPrintReportProducesPdf() {
        UUID orderId = UUID.randomUUID();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order()));
        when(companyRepository.findByDefaultCompanyTrueAndDeletedFalse()).thenReturn(Optional.of(company()));

        assertIsPdf(reportService.orderPrintReport(orderId));
    }

    @Test
    void orderPrintReportWorksWithoutDefaultCompany() {
        UUID orderId = UUID.randomUUID();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order()));
        when(companyRepository.findByDefaultCompanyTrueAndDeletedFalse()).thenReturn(Optional.empty());

        assertIsPdf(reportService.orderPrintReport(orderId));
    }

    @Test
    void orderPrintReportThrowsWhenOrderMissing() {
        UUID orderId = UUID.randomUUID();
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reportService.orderPrintReport(orderId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void invoicePrintReportProducesPdfWithQrForTransfer() {
        UUID invoiceId = UUID.randomUUID();
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice(InvoiceEntity.PaymentType.TRANSFER)));
        when(companyRepository.findByDefaultCompanyTrueAndDeletedFalse()).thenReturn(Optional.of(company()));

        assertIsPdf(reportService.invoicePrintReport(invoiceId));
    }

    @Test
    void invoicePrintReportProducesPdfForCashWithoutQr() {
        UUID invoiceId = UUID.randomUUID();
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice(InvoiceEntity.PaymentType.CASH)));
        when(companyRepository.findByDefaultCompanyTrueAndDeletedFalse()).thenReturn(Optional.of(company()));

        assertIsPdf(reportService.invoicePrintReport(invoiceId));
    }

    @Test
    void invoicePrintReportProducesPdfWithCompanyLogo() throws Exception {
        UUID invoiceId = UUID.randomUUID();
        CompanyEntity company = company();
        company.setLogo(pngLogo());
        company.setLogoContentType("image/png");
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice(InvoiceEntity.PaymentType.TRANSFER)));
        when(companyRepository.findByDefaultCompanyTrueAndDeletedFalse()).thenReturn(Optional.of(company));

        assertIsPdf(reportService.invoicePrintReport(invoiceId));
    }

    @Test
    void invoicePrintReportProducesPdfWhenLogoIsUnreadable() {
        UUID invoiceId = UUID.randomUUID();
        CompanyEntity company = company();
        company.setLogo(new byte[]{1, 2, 3});
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice(InvoiceEntity.PaymentType.TRANSFER)));
        when(companyRepository.findByDefaultCompanyTrueAndDeletedFalse()).thenReturn(Optional.of(company));

        assertIsPdf(reportService.invoicePrintReport(invoiceId));
    }

    private static byte[] pngLogo() throws Exception {
        java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(40, 20, java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    @Test
    void invoicePrintReportThrowsWhenInvoiceMissing() {
        UUID invoiceId = UUID.randomUUID();
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reportService.invoicePrintReport(invoiceId))
                .isInstanceOf(NotFoundException.class);
    }

    private InvoiceEntity invoice(InvoiceEntity.PaymentType paymentType) {
        InvoiceItemEntity line = new InvoiceItemEntity();
        line.setItemCode("IT-1");
        line.setItemName("Papír A4");
        line.setUnit("ks");
        line.setQuantity(new BigDecimal("2"));
        line.setUnitPrice(new BigDecimal("50.00"));
        line.setVatRate(new BigDecimal("21.00"));
        line.setTotalNet(new BigDecimal("100.00"));
        line.setTotalVat(new BigDecimal("21.00"));
        line.setTotalGross(new BigDecimal("121.00"));

        InvoiceEntity invoice = new InvoiceEntity();
        invoice.setInvoiceNumber("2026-000001");
        invoice.setOrder(order());
        invoice.setPaymentType(paymentType);
        invoice.setIssueDate(LocalDate.of(2026, Month.JULY, 16));
        invoice.setTaxDate(LocalDate.of(2026, Month.JULY, 16));
        invoice.setDueDate(LocalDate.of(2026, Month.JULY, 30));
        invoice.setVariableSymbol("2026000001");
        invoice.setTotalNet(new BigDecimal("100.00"));
        invoice.setTotalVat(new BigDecimal("21.00"));
        invoice.setTotalGross(new BigDecimal("121.00"));
        invoice.setCurrency("CZK");
        invoice.addItem(line);
        return invoice;
    }

    private OrderEntity order() {
        CustomerEntity customer = new CustomerEntity();
        customer.setName("ACME s.r.o.");
        customer.setRegistrationId("12345678");

        ItemEntity item = new ItemEntity();
        item.setCode("IT-1");
        item.setName("Papír A4");
        item.setUnit("ks");

        OrderItemEntity line = new OrderItemEntity();
        line.setItem(item);
        line.setQuantity(new BigDecimal("2"));
        line.setUnitPrice(new BigDecimal("50.00"));
        line.setTotalPrice(new BigDecimal("100.00"));

        OrderEntity order = new OrderEntity();
        order.setOrderNumber("ORD-1");
        order.setCustomer(customer);
        order.setOrderDate(LocalDate.of(2026, Month.JULY, 16));
        order.setStatus(OrderEntity.OrderStatus.NEW);
        order.setCurrency("CZK");
        order.setTotalPrice(new BigDecimal("100.00"));
        order.setNote("Dodání do 14 dnů");
        order.addItem(line);
        return order;
    }

    private CompanyEntity company() {
        CompanyEntity company = new CompanyEntity();
        company.setName("nCRM s.r.o.");
        company.setRegistrationId("87654321");
        company.setVatId("CZ87654321");
        company.setEmail("info@ncrm.cz");
        company.setBankAccount("123456789/0100");
        company.setIban("CZ6508000000192000145399");
        return company;
    }
}
