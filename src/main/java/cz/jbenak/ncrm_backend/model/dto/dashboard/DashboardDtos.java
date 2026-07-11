package cz.jbenak.ncrm_backend.model.dto.dashboard;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-11
 * Container of DTOs used by the owner dashboard endpoints consumed by the React frontend.
 */
public final class DashboardDtos {

    private DashboardDtos() {
    }

    /**
     * High-level summary of the business for the owner dashboard.
     */
    public record DashboardSummary(
            long totalCustomers,
            long activeCustomers,
            long totalOrders,
            long openOrders,
            BigDecimal totalRevenue,
            long plannedMeetings,
            long completedMeetings,
            long sentCampaigns
    ) {
    }

    /**
     * Revenue and order count aggregated per month (e.g. "2026-07").
     */
    public record OrdersByMonth(String month, long orderCount, BigDecimal revenue) {
    }

    /**
     * Performance of a single sales representative.
     */
    public record SalesByRepresentative(
            UUID salesRepresentativeId,
            String name,
            long orderCount,
            BigDecimal revenue,
            long meetingCount
    ) {
    }

    /**
     * Top customer by revenue.
     */
    public record TopCustomer(UUID customerId, String name, long orderCount, BigDecimal revenue) {
    }

    /**
     * Composite payload with all dashboard widgets for a single frontend call.
     */
    public record OwnerDashboard(
            DashboardSummary summary,
            List<OrdersByMonth> ordersByMonth,
            List<SalesByRepresentative> salesByRepresentative,
            List<TopCustomer> topCustomers
    ) {
    }
}
