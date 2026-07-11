package cz.jbenak.ncrm_backend.services;

import cz.jbenak.ncrm_backend.model.dto.dashboard.DashboardDtos;
import cz.jbenak.ncrm_backend.model.dto.order.OrderDto;
import cz.jbenak.ncrm_backend.model.entity.order.OrderEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
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
                new OrderDto(UUID.randomUUID(), "ORD-1", customerId, "ACME", null, null,
                        null, null, LocalDate.of(2026, 7, 1), OrderEntity.OrderStatus.NEW,
                        new BigDecimal("123.45"), "CZK", null, List.of()),
                new OrderDto(UUID.randomUUID(), "ORD-2", customerId, "ACME", null, null,
                        null, null, null, null, new BigDecimal("50"), "CZK", null, List.of())));

        assertIsPdf(reportService.customerOrdersReport(customerId));
    }

    @Test
    void customerOrdersReportWorksWithNoOrders() {
        UUID customerId = UUID.randomUUID();
        when(orderService.findByCustomer(customerId)).thenReturn(List.of());

        assertIsPdf(reportService.customerOrdersReport(customerId));
    }
}
