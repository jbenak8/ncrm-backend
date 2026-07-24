package cz.jbenak.ncrm_backend.services;

import cz.jbenak.ncrm_backend.model.dto.dashboard.DashboardDtos;
import cz.jbenak.ncrm_backend.model.entity.company.SalesRepresentativeEntity;
import cz.jbenak.ncrm_backend.model.entity.customer.MeetingEntity;
import cz.jbenak.ncrm_backend.model.entity.marketing.CampaignEntity;
import cz.jbenak.ncrm_backend.model.entity.security.UserEntity;
import cz.jbenak.ncrm_backend.repository.CampaignRepository;
import cz.jbenak.ncrm_backend.repository.CustomerRepository;
import cz.jbenak.ncrm_backend.repository.MeetingRepository;
import cz.jbenak.ncrm_backend.repository.OrderRepository;
import cz.jbenak.ncrm_backend.repository.SalesRepresentativeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

/**
 * Unit tests of {@link DashboardService} covering the KPI summary and aggregation mappings
 * used by the owner dashboards.
 */
@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private MeetingRepository meetingRepository;
    @Mock
    private CampaignRepository campaignRepository;
    @Mock
    private SalesRepresentativeRepository salesRepresentativeRepository;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    void summaryAggregatesAllCounters() {
        when(customerRepository.count()).thenReturn(20L);
        when(customerRepository.countByActiveTrue()).thenReturn(18L);
        when(orderRepository.count()).thenReturn(50L);
        when(orderRepository.countByStatusIn(any())).thenReturn(5L);
        when(orderRepository.sumTotalRevenue()).thenReturn(new BigDecimal("125000.50"));
        when(meetingRepository.countByStatus(MeetingEntity.MeetingStatus.PLANNED)).thenReturn(3L);
        when(meetingRepository.countByStatus(MeetingEntity.MeetingStatus.COMPLETED)).thenReturn(12L);
        when(campaignRepository.countByStatus(CampaignEntity.CampaignStatus.SENT)).thenReturn(2L);

        DashboardDtos.DashboardSummary summary = dashboardService.summary();

        assertThat(summary.totalCustomers()).isEqualTo(20L);
        assertThat(summary.activeCustomers()).isEqualTo(18L);
        assertThat(summary.totalOrders()).isEqualTo(50L);
        assertThat(summary.openOrders()).isEqualTo(5L);
        assertThat(summary.totalRevenue()).isEqualByComparingTo("125000.50");
        assertThat(summary.plannedMeetings()).isEqualTo(3L);
        assertThat(summary.completedMeetings()).isEqualTo(12L);
        assertThat(summary.sentCampaigns()).isEqualTo(2L);
    }

    @Test
    void ordersByMonthMapsAggregationRows() {
        when(orderRepository.aggregateOrdersByMonth()).thenReturn(List.<Object[]>of(
                new Object[]{"2026-06", 4L, new BigDecimal("1000")},
                new Object[]{"2026-07", 2L, new BigDecimal("500")}));

        List<DashboardDtos.OrdersByMonth> rows = dashboardService.ordersByMonth();

        assertThat(rows).hasSize(2);
        assertThat(rows.getFirst().month()).isEqualTo("2026-06");
        assertThat(rows.getFirst().orderCount()).isEqualTo(4L);
        assertThat(rows.getFirst().revenue()).isEqualByComparingTo("1000");
    }

    @Test
    void salesByRepresentativeResolvesRepresentativeName() {
        UUID repId = UUID.randomUUID();
        UserEntity user = new UserEntity();
        user.setFirstName("Jan");
        user.setLastName("Novák");
        SalesRepresentativeEntity rep = new SalesRepresentativeEntity();
        rep.setUser(user);

        when(orderRepository.aggregateOrdersBySalesRepresentative()).thenReturn(List.<Object[]>of(
                new Object[]{repId, 7L, new BigDecimal("7000")}));
        when(salesRepresentativeRepository.findById(repId)).thenReturn(Optional.of(rep));
        when(meetingRepository.countBySalesRepresentativeId(repId)).thenReturn(4L);

        List<DashboardDtos.SalesByRepresentative> rows = dashboardService.salesByRepresentative();

        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst().name()).isEqualTo("Jan Novák");
        assertThat(rows.getFirst().orderCount()).isEqualTo(7L);
        assertThat(rows.getFirst().meetingCount()).isEqualTo(4L);
    }

    @Test
    void salesByRepresentativeFallsBackToUnknownName() {
        UUID repId = UUID.randomUUID();
        when(orderRepository.aggregateOrdersBySalesRepresentative()).thenReturn(List.<Object[]>of(
                new Object[]{repId, 1L, BigDecimal.ONE}));
        when(salesRepresentativeRepository.findById(repId)).thenReturn(Optional.empty());
        when(meetingRepository.countBySalesRepresentativeId(repId)).thenReturn(0L);

        List<DashboardDtos.SalesByRepresentative> rows = dashboardService.salesByRepresentative();

        assertThat(rows.getFirst().name()).isEqualTo("Unknown");
    }

    @Test
    void salesByRepresentativeSkipsOrdersWithoutRepresentative() {
        UUID repId = UUID.randomUUID();
        UserEntity user = new UserEntity();
        user.setFirstName("Jan");
        user.setLastName("Novák");
        SalesRepresentativeEntity rep = new SalesRepresentativeEntity();
        rep.setUser(user);

        // Orders placed directly by customers are aggregated with a null representative id
        // and must not break the dashboard (previously caused "The given id must not be null").
        when(orderRepository.aggregateOrdersBySalesRepresentative()).thenReturn(List.<Object[]>of(
                new Object[]{null, 3L, new BigDecimal("3000")},
                new Object[]{repId, 7L, new BigDecimal("7000")}));
        when(salesRepresentativeRepository.findById(repId)).thenReturn(Optional.of(rep));
        when(meetingRepository.countBySalesRepresentativeId(repId)).thenReturn(4L);

        List<DashboardDtos.SalesByRepresentative> rows = dashboardService.salesByRepresentative();

        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst().salesRepresentativeId()).isEqualTo(repId);
    }

    @Test
    void topCustomersMapsAggregationRows() {
        UUID customerId = UUID.randomUUID();
        when(orderRepository.aggregateTopCustomers(10)).thenReturn(List.<Object[]>of(
                new Object[]{customerId, "ACME", 9L, new BigDecimal("9999")}));

        List<DashboardDtos.TopCustomer> rows = dashboardService.topCustomers(10);

        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst().customerId()).isEqualTo(customerId);
        assertThat(rows.getFirst().name()).isEqualTo("ACME");
    }

    @Test
    void ownerDashboardCombinesAllWidgets() {
        when(orderRepository.aggregateOrdersByMonth()).thenReturn(List.of());
        when(orderRepository.aggregateOrdersBySalesRepresentative()).thenReturn(List.of());
        when(orderRepository.aggregateTopCustomers(anyInt())).thenReturn(List.of());
        when(orderRepository.sumTotalRevenue()).thenReturn(BigDecimal.ZERO);

        DashboardDtos.OwnerDashboard dashboard = dashboardService.ownerDashboard();

        assertThat(dashboard.summary()).isNotNull();
        assertThat(dashboard.ordersByMonth()).isEmpty();
        assertThat(dashboard.salesByRepresentative()).isEmpty();
        assertThat(dashboard.topCustomers()).isEmpty();
    }
}
