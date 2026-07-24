package cz.jbenak.ncrm_backend.services;

import cz.jbenak.ncrm_backend.model.entity.AddressEntity;
import cz.jbenak.ncrm_backend.model.entity.CountryEntity;
import cz.jbenak.ncrm_backend.model.entity.company.CompanyEntity;
import cz.jbenak.ncrm_backend.model.entity.customer.CustomerEntity;
import cz.jbenak.ncrm_backend.model.entity.invoice.InvoiceEntity;
import cz.jbenak.ncrm_backend.model.entity.invoice.InvoiceItemEntity;
import cz.jbenak.ncrm_backend.model.entity.order.OrderEntity;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.Month;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests of {@link InvoiceIsdocService}: the generated XML is well-formed, uses the
 * ISDOC namespace and contains the invoice header, supplier/customer parties, lines,
 * the VAT recapitulation, totals and payment details.
 */
class InvoiceIsdocServiceTest {

    private final InvoiceIsdocService invoiceIsdocService = new InvoiceIsdocService();

    @Test
    void generateProducesWellFormedIsdocXml() throws Exception {
        byte[] isdoc = invoiceIsdocService.generate(invoice(InvoiceEntity.PaymentType.TRANSFER));

        var factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        Document doc = factory.newDocumentBuilder().parse(new ByteArrayInputStream(isdoc));
        assertThat(doc.getDocumentElement().getLocalName()).isEqualTo("Invoice");
        assertThat(doc.getDocumentElement().getNamespaceURI()).isEqualTo("http://isdoc.cz/namespace/2013");
        assertThat(doc.getDocumentElement().getAttribute("version")).isEqualTo("6.0.1");
    }

    @Test
    void generateContainsHeaderPartiesLinesAndTotals() {
        String xml = new String(invoiceIsdocService.generate(invoice(InvoiceEntity.PaymentType.TRANSFER)),
                StandardCharsets.UTF_8);

        assertThat(xml)
                .contains("<ID>2026-000042</ID>")
                .contains("<IssueDate>2026-07-16</IssueDate>")
                .contains("<LocalCurrencyCode>CZK</LocalCurrencyCode>")
                .contains("Dodavatel s.r.o.")
                .contains("<CompanyID>CZ12345678</CompanyID>")
                .contains("Acme a.s.")
                .contains("Papír A4")
                .contains("<LineExtensionAmount>100.00</LineExtensionAmount>")
                .contains("<TaxableAmount>100.00</TaxableAmount>")
                .contains("<Percent>21.00</Percent>")
                .contains("<PayableAmount>121.00</PayableAmount>");
    }

    @Test
    void generateContainsBankDetailsForTransfer() {
        String xml = new String(invoiceIsdocService.generate(invoice(InvoiceEntity.PaymentType.TRANSFER)),
                StandardCharsets.UTF_8);

        assertThat(xml)
                .contains("<PaymentMeansCode>42</PaymentMeansCode>")
                .contains("<ID>123456789</ID>")
                .contains("<BankCode>0100</BankCode>")
                .contains("<VariableSymbol>2026000042</VariableSymbol>")
                .contains("<PaymentDueDate>2026-08-01</PaymentDueDate>");
    }

    @Test
    void generateForCashOmitsBankDetails() {
        String xml = new String(invoiceIsdocService.generate(invoice(InvoiceEntity.PaymentType.CASH)),
                StandardCharsets.UTF_8);

        assertThat(xml)
                .contains("<PaymentMeansCode>10</PaymentMeansCode>")
                .doesNotContain("<BankCode>")
                .doesNotContain("<VariableSymbol>");
    }

    @Test
    void generateHandlesInvoiceWithoutOrder() {
        InvoiceEntity invoice = invoice(InvoiceEntity.PaymentType.TRANSFER);
        invoice.setOrder(null);

        byte[] isdoc = invoiceIsdocService.generate(invoice);

        assertThat(new String(isdoc, StandardCharsets.UTF_8)).contains("<ID>2026-000042</ID>");
    }

    private InvoiceEntity invoice(InvoiceEntity.PaymentType paymentType) {
        CountryEntity country = new CountryEntity();
        country.setIsoCode("CZ");
        country.setName("Česká republika");

        AddressEntity supplierAddress = new AddressEntity();
        supplierAddress.setStreet("Dlouhá");
        supplierAddress.setHouseNumber("12");
        supplierAddress.setCity("Praha");
        supplierAddress.setZipCode("11000");
        supplierAddress.setCountry(country);

        CompanyEntity company = new CompanyEntity();
        company.setName("Dodavatel s.r.o.");
        company.setRegistrationId("12345678");
        company.setVatId("CZ12345678");
        company.setAddress(supplierAddress);
        company.setBankAccount("123456789/0100");
        company.setBankName("Komerční banka");
        company.setIban("CZ6501000000000123456789");
        company.setBic("KOMBCZPP");

        CustomerEntity customer = new CustomerEntity();
        customer.setName("Acme a.s.");
        customer.setRegistrationId("87654321");
        customer.setVatId("CZ87654321");

        OrderEntity order = new OrderEntity();
        order.setOrderNumber("ORD-1");
        order.setCompany(company);
        order.setCustomer(customer);

        InvoiceItemEntity item = new InvoiceItemEntity();
        item.setItemCode("IT-1");
        item.setItemName("Papír A4");
        item.setQuantity(new BigDecimal("2"));
        item.setUnit("ks");
        item.setUnitPrice(new BigDecimal("50.00"));
        item.setVatRate(new BigDecimal("21.00"));
        item.setTotalNet(new BigDecimal("100.00"));
        item.setTotalVat(new BigDecimal("21.00"));
        item.setTotalGross(new BigDecimal("121.00"));

        InvoiceEntity invoice = new InvoiceEntity();
        invoice.setId(UUID.randomUUID());
        invoice.setInvoiceNumber("2026-000042");
        invoice.setOrder(order);
        invoice.setPaymentType(paymentType);
        invoice.setIssueDate(LocalDate.of(2026, Month.JULY, 16));
        invoice.setTaxDate(LocalDate.of(2026, Month.JULY, 16));
        invoice.setDueDate(LocalDate.of(2026, Month.AUGUST, 1));
        invoice.setVariableSymbol("2026000042");
        invoice.setTotalNet(new BigDecimal("100.00"));
        invoice.setTotalVat(new BigDecimal("21.00"));
        invoice.setTotalGross(new BigDecimal("121.00"));
        invoice.setCurrency("CZK");
        invoice.addItem(item);
        return invoice;
    }
}
