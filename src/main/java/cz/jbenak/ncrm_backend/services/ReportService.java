package cz.jbenak.ncrm_backend.services;

import cz.jbenak.ncrm_backend.model.entity.AddressEntity;
import cz.jbenak.ncrm_backend.model.entity.company.CompanyEntity;
import cz.jbenak.ncrm_backend.model.entity.customer.CustomerEntity;
import cz.jbenak.ncrm_backend.model.entity.invoice.InvoiceEntity;
import cz.jbenak.ncrm_backend.model.entity.invoice.InvoiceItemEntity;
import cz.jbenak.ncrm_backend.model.entity.order.OrderEntity;
import cz.jbenak.ncrm_backend.repository.CompanyRepository;
import cz.jbenak.ncrm_backend.repository.InvoiceRepository;
import cz.jbenak.ncrm_backend.repository.OrderRepository;
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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
 * - customer: overview of their own orders,
 * - anyone working with orders: a printable document of a single order (Czech order form layout),
 * - invoicing: a printable invoice of an issued invoice (Czech VAT invoice layout with a payment QR code).
 * Compiled reports are cached to avoid repeated compilation of the JRXML templates.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final DashboardService dashboardService;
    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final CompanyRepository companyRepository;
    private final InvoiceRepository invoiceRepository;
    private final QrPaymentService qrPaymentService;

    private static final String REPORT_TITLE_PARAM = "REPORT_TITLE";

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("d.M.yyyy");

    /** Czech labels of the order statuses shown on the printed order. */
    private static final Map<OrderEntity.OrderStatus, String> STATUS_LABELS = Map.of(
            OrderEntity.OrderStatus.NEW, "Nová",
            OrderEntity.OrderStatus.CONFIRMED, "Potvrzená",
            OrderEntity.OrderStatus.IN_PROGRESS, "V realizaci",
            OrderEntity.OrderStatus.COMPLETED, "Dokončená",
            OrderEntity.OrderStatus.CANCELLED, "Stornovaná");

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
                    row.put("currency", currencySymbol(order.currency()));
                    return row;
                })
                .toList();
        Map<String, Object> params = new HashMap<>();
        params.put(REPORT_TITLE_PARAM, "Orders overview");
        return exportPdf("reports/customer_orders.jrxml", params, data);
    }

    /**
     * Printable document of a single order: supplier (the default own company), customer,
     * item table and totals. The layout follows the common Czech order form.
     */
    public byte[] orderPrintReport(UUID orderId) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order", orderId));
        List<Map<String, ?>> data = order.getItems().stream()
                .<Map<String, ?>>map(item -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("itemCode", item.getItem() == null ? null : item.getItem().getCode());
                    row.put("itemName", item.getItem() == null ? null : item.getItem().getName());
                    row.put("quantity", item.getQuantity());
                    row.put("unit", item.getItem() == null ? null : item.getItem().getUnit());
                    row.put("unitPrice", item.getUnitPrice());
                    row.put("totalPrice", item.getTotalPrice());
                    return row;
                })
                .toList();
        Map<String, Object> params = new HashMap<>();
        params.put("ORDER_NUMBER", order.getOrderNumber());
        params.put("ORDER_DATE", order.getOrderDate() == null ? null : order.getOrderDate().format(DATE_FORMAT));
        params.put("STATUS", order.getStatus() == null ? null
                : STATUS_LABELS.getOrDefault(order.getStatus(), order.getStatus().name()));
        params.put("CURRENCY", currencySymbol(order.getCurrency()));
        params.put("NOTE", order.getNote());
        params.put("TOTAL_PRICE", order.getTotalPrice());
        params.put("SALES_REPRESENTATIVE", resolveSalesRepresentativeName(order));
        fillSupplierParams(params);
        fillCustomerParams(params, order);
        return exportPdf("reports/order_print.jrxml", params, data);
    }

    /**
     * Printable invoice document: supplier (the default own company), customer, payment data
     * (payment type, due date, variable symbol, bank account), item table with VAT and totals.
     * The layout follows the common Czech VAT invoice (fakturyweb.cz sample no. 3). For invoices
     * paid by bank transfer a payment QR code (SPD, "QR platba") is printed when the supplier
     * has an IBAN defined.
     */
    public byte[] invoicePrintReport(UUID invoiceId) {
        InvoiceEntity invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new NotFoundException("Invoice", invoiceId));
        List<Map<String, ?>> data = invoice.getItems().stream()
                .<Map<String, ?>>map(item -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("itemCode", item.getItemCode());
                    row.put("itemName", item.getItemName());
                    row.put("quantity", item.getQuantity());
                    row.put("unit", item.getUnit());
                    row.put("unitPrice", item.getUnitPrice());
                    row.put("vatRate", item.getVatRate());
                    row.put("totalNet", item.getTotalNet());
                    row.put("totalVat", item.getTotalVat());
                    row.put("totalGross", item.getTotalGross());
                    return row;
                })
                .toList();
        Map<String, Object> params = new HashMap<>();
        params.put("INVOICE_NUMBER", invoice.getInvoiceNumber());
        params.put("ORDER_NUMBER", invoice.getOrder() == null ? null : invoice.getOrder().getOrderNumber());
        params.put("ISSUE_DATE", invoice.getIssueDate() == null ? null : invoice.getIssueDate().format(DATE_FORMAT));
        params.put("TAX_DATE", invoice.getTaxDate() == null ? null : invoice.getTaxDate().format(DATE_FORMAT));
        params.put("DUE_DATE", invoice.getDueDate() == null ? null : invoice.getDueDate().format(DATE_FORMAT));
        params.put("PAYMENT_TYPE", invoice.getPaymentType() == InvoiceEntity.PaymentType.CASH
                ? "Hotově" : "Převodem");
        params.put("VARIABLE_SYMBOL",
                invoice.getPaymentType() == InvoiceEntity.PaymentType.TRANSFER ? invoice.getVariableSymbol() : null);
        params.put("CURRENCY", currencySymbol(invoice.getCurrency()));
        params.put("NOTE", invoice.getNote());
        params.put("TOTAL_NET", invoice.getTotalNet());
        params.put("TOTAL_VAT", invoice.getTotalVat());
        params.put("TOTAL_GROSS", invoice.getTotalGross());
        fillVatRecapParams(params, invoice);
        CompanyEntity company = companyRepository.findByDefaultCompanyTrueAndDeletedFalse().orElse(null);
        fillSupplierParams(params);
        params.put("SUPPLIER_STAMP", company == null ? null : readSupplierStamp(company));
        fillCustomerParams(params, invoice.getOrder());
        fillPaymentQrParams(params, invoice, company);
        return exportPdf("reports/invoice_print.jrxml", params, data);
    }

    /** VAT recapitulation: net, VAT and gross amounts aggregated by the VAT rate. */
    private void fillVatRecapParams(Map<String, Object> params, InvoiceEntity invoice) {
        StringBuilder recap = new StringBuilder();
        invoice.getItems().stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        item -> item.getVatRate() == null ? java.math.BigDecimal.ZERO : item.getVatRate().stripTrailingZeros(),
                        java.util.TreeMap::new,
                        java.util.stream.Collectors.toList()))
                .forEach((rate, items) -> {
                    java.math.BigDecimal net = items.stream().map(InvoiceItemEntity::getTotalNet)
                            .filter(java.util.Objects::nonNull).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
                    java.math.BigDecimal vat = items.stream().map(InvoiceItemEntity::getTotalVat)
                            .filter(java.util.Objects::nonNull).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
                    java.text.DecimalFormat money = new java.text.DecimalFormat("#,##0.00");
                    recap.append("Sazba ").append(rate.toPlainString()).append(" %: základ ")
                            .append(money.format(net)).append(", DPH ").append(money.format(vat))
                            .append(", celkem ").append(money.format(net.add(vat))).append('\n');
                });
        params.put("VAT_RECAP", recap.isEmpty() ? null : recap.toString());
    }

    /** Payment QR code (SPD) is printed only on transfer invoices when the supplier has an IBAN. */
    private void fillPaymentQrParams(Map<String, Object> params, InvoiceEntity invoice, CompanyEntity company) {
        if (invoice.getPaymentType() != InvoiceEntity.PaymentType.TRANSFER
                || company == null || !isNotBlank(company.getIban())) {
            return;
        }
        String paymentString = qrPaymentService.buildPaymentString(company.getIban(), invoice.getTotalGross(),
                invoice.getCurrency(), invoice.getVariableSymbol(), "FAKTURA " + invoice.getInvoiceNumber());
        params.put("QR_IMAGE", qrPaymentService.generateQrImage(paymentString));
    }

    /** Supplier of the order document is the default own company (when defined). */
    private void fillSupplierParams(Map<String, Object> params) {
        CompanyEntity company = companyRepository.findByDefaultCompanyTrueAndDeletedFalse().orElse(null);
        if (company == null) {
            return;
        }
        params.put("SUPPLIER_LOGO", readSupplierLogo(company));
        params.put("SUPPLIER_NAME", company.getNameSecondLine() == null
                ? company.getName() : company.getName() + " " + company.getNameSecondLine());
        params.put("SUPPLIER_ADDRESS", formatAddress(company.getAddress()));
        params.put("SUPPLIER_REG_ID", company.getRegistrationId());
        params.put("SUPPLIER_VAT_ID", company.getVatId());
        params.put("SUPPLIER_CONTACT", joinNonBlank(" | ", company.getEmail(), company.getPhone()));
        params.put("SUPPLIER_BANK", company.getBankAccount());
        params.put("SUPPLIER_IBAN", company.getIban());
    }

    /** Company logo stored in the database decoded to an AWT image (or {@code null} when missing/unreadable). */
    private java.awt.Image readSupplierLogo(CompanyEntity company) {
        if (company.getLogo() == null || company.getLogo().length == 0) {
            return null;
        }
        try {
            return javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(company.getLogo()));
        } catch (Exception e) {
            log.warn("Could not read logo of company {} for the report", company.getId(), e);
            return null;
        }
    }

    /** Company stamp stored in the database decoded to an AWT image (or {@code null} when missing/unreadable). */
    private java.awt.Image readSupplierStamp(CompanyEntity company) {
        if (company.getStamp() == null || company.getStamp().length == 0) {
            return null;
        }
        try {
            return javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(company.getStamp()));
        } catch (Exception e) {
            log.warn("Could not read stamp of company {} for the report", company.getId(), e);
            return null;
        }
    }

    private void fillCustomerParams(Map<String, Object> params, OrderEntity order) {
        CustomerEntity customer = order.getCustomer();
        if (customer == null) {
            return;
        }
        params.put("CUSTOMER_NAME", customer.getName());
        params.put("CUSTOMER_ADDRESS", formatAddress(customer.getHeadquartersAddress()));
        params.put("CUSTOMER_REG_ID", customer.getRegistrationId());
        params.put("CUSTOMER_VAT_ID", customer.getVatId());
        String contact = order.getContactPerson() == null ? null
                : joinNonBlank(" | ",
                joinNonBlank(" ", order.getContactPerson().getFirstName(), order.getContactPerson().getLastName()),
                order.getContactPerson().getEmail(), order.getContactPerson().getPhone());
        params.put("CUSTOMER_CONTACT", contact);
    }

    private String resolveSalesRepresentativeName(OrderEntity order) {
        var representative = order.getSalesRepresentative();
        if (representative == null) {
            return null;
        }
        String name = representative.getUser() == null ? null
                : joinNonBlank(" ", representative.getUser().getFirstName(), representative.getUser().getLastName());
        return name == null ? representative.getCode() : name;
    }

    /**
     * Formats the address as "Street houseNumber/streetNumber, zip city" where the house number
     * is the Czech "číslo popisné" and the street number the "číslo orientační" (when present).
     */
    private String formatAddress(AddressEntity address) {
        if (address == null) {
            return null;
        }
        String numbers = isNotBlank(address.getHouseNumber()) && isNotBlank(address.getStreetNumber())
                ? address.getHouseNumber() + "/" + address.getStreetNumber()
                : isNotBlank(address.getHouseNumber()) ? address.getHouseNumber() : address.getStreetNumber();
        String street = joinNonBlank(" ", address.getStreet(), numbers);
        return joinNonBlank(", ", street, joinNonBlank(" ", address.getZipCode(), address.getCity()));
    }

    /** Joins the non-blank values with the given separator; returns {@code null} when nothing remains. */
    private String joinNonBlank(String separator, String... values) {
        List<String> parts = new ArrayList<>();
        for (String value : values) {
            if (isNotBlank(value)) {
                parts.add(value);
            }
        }
        return parts.isEmpty() ? null : String.join(separator, parts);
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * Converts the ISO 4217 currency code to the local currency symbol shown on the printed
     * documents (e.g. "CZK" -> "Kč", "EUR" -> "€"). Falls back to the original code when
     * the symbol is unknown.
     */
    private String currencySymbol(String currencyCode) {
        if (!isNotBlank(currencyCode)) {
            return currencyCode;
        }
        try {
            return java.util.Currency.getInstance(currencyCode.trim().toUpperCase(java.util.Locale.ROOT))
                    .getSymbol(new java.util.Locale("cs", "CZ"));
        } catch (IllegalArgumentException e) {
            log.warn("Unknown currency code '{}', using it as-is on the report", currencyCode);
            return currencyCode;
        }
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
