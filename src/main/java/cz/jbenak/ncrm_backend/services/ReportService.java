package cz.jbenak.ncrm_backend.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-11
 * Reporting service based on JasperReports. Provides PDF reports for all user roles:
 * - owner: overall sales overview (revenue by month),
 * - sales representative: personal performance (orders and meetings),
 * - customer: overview of their own orders.
 * Compiled reports are cached to avoid repeated compilation of the JRXML templates.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final DashboardService dashboardService;
    private final OrderService orderService;

    private static final String REPORT_TITLE_PARAM = "REPORT_TITLE";

    private final Map<String, JasperReport> reportCache = new ConcurrentHashMap<>();

    /**
     * Sales overview report for the owner: revenue and order counts aggregated by month.
     */
    public byte[] ownerSalesReport() {
        List<Map<String, ?>> data = dashboardService.ordersByMonth().stream()
                .<Map<String, ?>>map(row -> Map.of(
                        "month", row.month(),
                        "orderCount", row.orderCount(),
                        "revenue", row.revenue()))
                .toList();
        Map<String, Object> params = new HashMap<>();
        params.put(REPORT_TITLE_PARAM, "Sales overview");
        return exportPdf("reports/owner_sales_overview.jrxml", params, data);
    }

    /**
     * Performance report for a sales representative: orders, revenue and meeting counts.
     */
    public byte[] salesRepresentativePerformanceReport() {
        List<Map<String, ?>> data = dashboardService.salesByRepresentative().stream()
                .<Map<String, ?>>map(row -> Map.of(
                        "name", row.name(),
                        "orderCount", row.orderCount(),
                        "revenue", row.revenue(),
                        "meetingCount", row.meetingCount()))
                .toList();
        Map<String, Object> params = new HashMap<>();
        params.put(REPORT_TITLE_PARAM, "Sales representatives performance");
        return exportPdf("reports/sales_rep_performance.jrxml", params, data);
    }

    /**
     * Orders overview report for a customer: list of their orders with totals.
     */
    public byte[] customerOrdersReport(UUID customerId) {
        List<Map<String, ?>> data = orderService.findByCustomer(customerId).stream()
                .<Map<String, ?>>map(order -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("orderNumber", order.orderNumber());
                    row.put("orderDate", order.orderDate() == null ? null : order.orderDate().toString());
                    row.put("status", order.status() == null ? null : order.status().name());
                    row.put("totalPrice", order.totalPrice());
                    row.put("currency", order.currency());
                    return row;
                })
                .toList();
        Map<String, Object> params = new HashMap<>();
        params.put(REPORT_TITLE_PARAM, "Orders overview");
        return exportPdf("reports/customer_orders.jrxml", params, data);
    }

    private byte[] exportPdf(String templatePath, Map<String, Object> params, List<Map<String, ?>> data) {
        try {
            log.debug("Generating PDF report {} with {} row(s)", templatePath, data.size());
            JasperReport report = reportCache.computeIfAbsent(templatePath, this::compile);
            JasperPrint print = JasperFillManager.fillReport(report, params, new JRMapCollectionDataSource(data));
            byte[] pdf = JasperExportManager.exportReportToPdf(print);
            log.info("Generated PDF report {} ({} bytes)", templatePath, pdf.length);
            return pdf;
        } catch (Exception e) {
            log.error("Failed to generate report {}", templatePath, e);
            throw new IllegalStateException("Failed to generate report " + templatePath + ": " + e.getMessage(), e);
        }
    }

    private JasperReport compile(String templatePath) {
        log.info("Compiling JasperReports template {}", templatePath);
        try (InputStream template = getClass().getClassLoader().getResourceAsStream(templatePath)) {
            if (template == null) {
                throw new IllegalStateException("Report template " + templatePath + " not found on classpath");
            }
            return JasperCompileManager.compileReport(template);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to compile report template " + templatePath, e);
        }
    }
}
