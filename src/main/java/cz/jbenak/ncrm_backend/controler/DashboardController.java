package cz.jbenak.ncrm_backend.controler;

import cz.jbenak.ncrm_backend.model.dto.dashboard.DashboardDtos;
import cz.jbenak.ncrm_backend.services.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-11
 * REST API providing the owner with data for relevant dashboards in the React frontend
 * (summary KPIs, revenue by month, performance of sales representatives, top customers).
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('OWNER')")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public DashboardDtos.OwnerDashboard ownerDashboard() {
        return dashboardService.ownerDashboard();
    }

    @GetMapping("/summary")
    public DashboardDtos.DashboardSummary summary() {
        return dashboardService.summary();
    }

    @GetMapping("/orders-by-month")
    public List<DashboardDtos.OrdersByMonth> ordersByMonth() {
        return dashboardService.ordersByMonth();
    }

    @GetMapping("/sales-by-representative")
    @PreAuthorize("hasAnyRole('OWNER', 'SALES_REPRESENTATIVE')")
    public List<DashboardDtos.SalesByRepresentative> salesByRepresentative() {
        return dashboardService.salesByRepresentative();
    }

    @GetMapping("/top-customers")
    public List<DashboardDtos.TopCustomer> topCustomers() {
        return dashboardService.topCustomers(10);
    }
}
