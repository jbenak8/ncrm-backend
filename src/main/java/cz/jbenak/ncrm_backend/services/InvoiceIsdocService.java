package cz.jbenak.ncrm_backend.services;

import cz.jbenak.ncrm_backend.model.entity.AddressEntity;
import cz.jbenak.ncrm_backend.model.entity.company.CompanyEntity;
import cz.jbenak.ncrm_backend.model.entity.customer.CustomerEntity;
import cz.jbenak.ncrm_backend.model.entity.invoice.InvoiceEntity;
import cz.jbenak.ncrm_backend.model.entity.invoice.InvoiceItemEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-23
 * Generates the machine-readable ISDOC (Information System Document, version 6.0.1) XML
 * representation of an issued invoice. The ISDOC file is attached to the invoice e-mail
 * together with the printable PDF so that the customer can import the invoice into their
 * accounting software. The supplier is taken from the order's own company and the customer
 * from the order's customer; all amounts are the persisted invoice snapshots.
 */
@Slf4j
@Service
public class InvoiceIsdocService {

    private static final String ISDOC_NAMESPACE = "http://isdoc.cz/namespace/2013";
    private static final String ISDOC_VERSION = "6.0.1";
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    /** Generates the ISDOC XML of the invoice as a UTF-8 encoded byte array. */
    public byte[] generate(InvoiceEntity invoice) {
        try {
            var factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            Document doc = factory.newDocumentBuilder().newDocument();

            Element root = doc.createElementNS(ISDOC_NAMESPACE, "Invoice");
            root.setAttribute("version", ISDOC_VERSION);
            doc.appendChild(root);

            appendHeader(doc, root, invoice);
            appendSupplier(doc, root, invoice);
            appendCustomer(doc, root, invoice);
            appendLines(doc, root, invoice);
            appendTaxTotal(doc, root, invoice);
            appendMonetaryTotal(doc, root, invoice);
            appendPaymentMeans(doc, root, invoice);

            var transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, StandardCharsets.UTF_8.name());
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            var out = new ByteArrayOutputStream();
            transformer.transform(new DOMSource(doc), new StreamResult(out));
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate ISDOC for invoice {}", invoice.getInvoiceNumber(), e);
            throw new IllegalStateException("Failed to generate ISDOC for invoice "
                    + invoice.getInvoiceNumber() + ": " + e.getMessage(), e);
        }
    }

    /** Header of the document: type, identifiers, dates, currency and optional note. */
    private void appendHeader(Document doc, Element root, InvoiceEntity invoice) {
        // DocumentType 1 = standard invoice (tax document).
        text(doc, root, "DocumentType", "1");
        text(doc, root, "ID", invoice.getInvoiceNumber());
        text(doc, root, "UUID", invoice.getId() != null
                ? invoice.getId().toString()
                : UUID.randomUUID().toString());
        date(doc, root, "IssueDate", invoice.getIssueDate());
        date(doc, root, "TaxPointDate", invoice.getTaxDate());
        text(doc, root, "VATApplicable", Boolean.toString(invoice.getTotalVat() != null
                && invoice.getTotalVat().signum() != 0));
        text(doc, root, "ElectronicPossibilityAgreementReference", "");
        if (isNotBlank(invoice.getNote())) {
            text(doc, root, "Note", invoice.getNote());
        }
        text(doc, root, "LocalCurrencyCode", invoice.getCurrency() != null ? invoice.getCurrency() : "CZK");
        text(doc, root, "CurrRate", "1");
        text(doc, root, "RefCurrRate", "1");
    }

    /** Supplier party taken from the own company that issued the order. */
    private void appendSupplier(Document doc, Element root, InvoiceEntity invoice) {
        Element supplier = child(doc, root, "AccountingSupplierParty");
        CompanyEntity company = invoice.getOrder() != null ? invoice.getOrder().getCompany() : null;
        if (company == null) {
            appendParty(doc, supplier, null, null, null, null);
            return;
        }
        String name = isNotBlank(company.getNameSecondLine())
                ? company.getName() + " " + company.getNameSecondLine()
                : company.getName();
        appendParty(doc, supplier, company.getRegistrationId(), name, company.getAddress(), company.getVatId());
    }

    /** Customer party taken from the order's customer. */
    private void appendCustomer(Document doc, Element root, InvoiceEntity invoice) {
        Element customerParty = child(doc, root, "AccountingCustomerParty");
        CustomerEntity customer = invoice.getOrder() != null ? invoice.getOrder().getCustomer() : null;
        if (customer == null) {
            appendParty(doc, customerParty, null, null, null, null);
            return;
        }
        appendParty(doc, customerParty, customer.getRegistrationId(), customer.getName(),
                customer.getHeadquartersAddress(), customer.getVatId());
    }

    /** Common ISDOC party structure: identification, name, postal address and VAT scheme. */
    private void appendParty(Document doc, Element parent, String registrationId, String name,
                             AddressEntity address, String vatId) {
        Element party = child(doc, parent, "Party");
        Element identification = child(doc, party, "PartyIdentification");
        text(doc, identification, "ID", registrationId != null ? registrationId : "");
        Element partyName = child(doc, party, "PartyName");
        text(doc, partyName, "Name", name != null ? name : "");
        Element postalAddress = child(doc, party, "PostalAddress");
        text(doc, postalAddress, "StreetName", address != null && address.getStreet() != null
                ? address.getStreet() : "");
        text(doc, postalAddress, "BuildingNumber", address != null ? buildingNumber(address) : "");
        text(doc, postalAddress, "CityName", address != null && address.getCity() != null
                ? address.getCity() : "");
        text(doc, postalAddress, "PostalZone", address != null && address.getZipCode() != null
                ? address.getZipCode() : "");
        Element country = child(doc, postalAddress, "Country");
        String countryCode = address != null && address.getCountry() != null
                && isNotBlank(address.getCountry().getIsoCode()) ? address.getCountry().getIsoCode() : "CZ";
        String countryName = address != null && address.getCountry() != null
                && isNotBlank(address.getCountry().getName()) ? address.getCountry().getName() : "Česká republika";
        text(doc, country, "IdentificationCode", countryCode);
        text(doc, country, "Name", countryName);
        if (isNotBlank(vatId)) {
            Element taxScheme = child(doc, party, "PartyTaxScheme");
            text(doc, taxScheme, "CompanyID", vatId);
            text(doc, taxScheme, "TaxScheme", "VAT");
        }
    }

    /** Invoice lines with net/gross amounts, unit prices and the VAT category. */
    private void appendLines(Document doc, Element root, InvoiceEntity invoice) {
        Element lines = child(doc, root, "InvoiceLines");
        int lineNumber = 1;
        for (InvoiceItemEntity item : invoice.getItems()) {
            Element line = child(doc, lines, "InvoiceLine");
            text(doc, line, "ID", String.valueOf(lineNumber++));
            Element quantity = text(doc, line, "InvoicedQuantity", amount(item.getQuantity()));
            if (isNotBlank(item.getUnit())) {
                quantity.setAttribute("unitCode", item.getUnit());
            }
            text(doc, line, "LineExtensionAmount", amount(item.getTotalNet()));
            text(doc, line, "LineExtensionAmountTaxInclusive", amount(item.getTotalGross()));
            text(doc, line, "LineExtensionTaxAmount", amount(item.getTotalVat()));
            text(doc, line, "UnitPrice", amount(item.getUnitPrice()));
            text(doc, line, "UnitPriceTaxInclusive", amount(unitPriceTaxInclusive(item)));
            Element taxCategory = child(doc, line, "ClassifiedTaxCategory");
            text(doc, taxCategory, "Percent", amount(item.getVatRate()));
            // VATCalculationMethod 0 = VAT is calculated from the net amount ("zdola").
            text(doc, taxCategory, "VATCalculationMethod", "0");
            Element itemElement = child(doc, line, "Item");
            text(doc, itemElement, "Description", item.getItemName() != null ? item.getItemName() : "");
            if (isNotBlank(item.getItemCode())) {
                Element sellerId = child(doc, itemElement, "SellersItemIdentification");
                text(doc, sellerId, "ID", item.getItemCode());
            }
        }
    }

    /** Recapitulation of the VAT grouped by the rate applied on the lines. */
    private void appendTaxTotal(Document doc, Element root, InvoiceEntity invoice) {
        Element taxTotal = child(doc, root, "TaxTotal");
        Map<BigDecimal, BigDecimal[]> byRate = new LinkedHashMap<>();
        for (InvoiceItemEntity item : invoice.getItems()) {
            BigDecimal rate = item.getVatRate() == null
                    ? BigDecimal.ZERO
                    : item.getVatRate().setScale(2, RoundingMode.HALF_UP);
            BigDecimal[] sums = byRate.computeIfAbsent(rate, r -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            sums[0] = sums[0].add(zeroIfNull(item.getTotalNet()));
            sums[1] = sums[1].add(zeroIfNull(item.getTotalVat()));
        }
        for (Map.Entry<BigDecimal, BigDecimal[]> entry : byRate.entrySet()) {
            Element subTotal = child(doc, taxTotal, "TaxSubTotal");
            BigDecimal net = entry.getValue()[0];
            BigDecimal vat = entry.getValue()[1];
            text(doc, subTotal, "TaxableAmount", amount(net));
            text(doc, subTotal, "TaxAmount", amount(vat));
            text(doc, subTotal, "TaxInclusiveAmount", amount(net.add(vat)));
            text(doc, subTotal, "AlreadyClaimedTaxableAmount", "0");
            text(doc, subTotal, "AlreadyClaimedTaxAmount", "0");
            text(doc, subTotal, "AlreadyClaimedTaxInclusiveAmount", "0");
            text(doc, subTotal, "DifferenceTaxableAmount", amount(net));
            text(doc, subTotal, "DifferenceTaxAmount", amount(vat));
            text(doc, subTotal, "DifferenceTaxInclusiveAmount", amount(net.add(vat)));
            Element category = child(doc, subTotal, "TaxCategory");
            text(doc, category, "Percent", amount(entry.getKey()));
        }
        text(doc, taxTotal, "TaxAmount", amount(invoice.getTotalVat()));
    }

    /** Legal monetary totals of the whole invoice. */
    private void appendMonetaryTotal(Document doc, Element root, InvoiceEntity invoice) {
        Element total = child(doc, root, "LegalMonetaryTotal");
        text(doc, total, "TaxExclusiveAmount", amount(invoice.getTotalNet()));
        text(doc, total, "TaxInclusiveAmount", amount(invoice.getTotalGross()));
        text(doc, total, "AlreadyClaimedTaxExclusiveAmount", "0");
        text(doc, total, "AlreadyClaimedTaxInclusiveAmount", "0");
        text(doc, total, "DifferenceTaxExclusiveAmount", amount(invoice.getTotalNet()));
        text(doc, total, "DifferenceTaxInclusiveAmount", amount(invoice.getTotalGross()));
        text(doc, total, "PayableRoundingAmount", "0");
        text(doc, total, "PaidDepositsAmount", "0");
        text(doc, total, "PayableAmount", amount(invoice.getTotalGross()));
    }

    /** Payment details: cash or bank transfer with account, symbols and due date. */
    private void appendPaymentMeans(Document doc, Element root, InvoiceEntity invoice) {
        Element paymentMeans = child(doc, root, "PaymentMeans");
        Element payment = child(doc, paymentMeans, "Payment");
        text(doc, payment, "PaidAmount", amount(invoice.getTotalGross()));
        // PaymentMeansCode 10 = in cash, 42 = payment to a bank account.
        boolean cash = invoice.getPaymentType() == InvoiceEntity.PaymentType.CASH;
        text(doc, payment, "PaymentMeansCode", cash ? "10" : "42");
        Element details = child(doc, payment, "Details");
        if (cash) {
            // Cash branch of DetailsType requires DocumentID and IssueDate.
            text(doc, details, "DocumentID", invoice.getInvoiceNumber());
            date(doc, details, "IssueDate", invoice.getIssueDate());
        } else {
            // Money transfer branch requires PaymentDueDate and the whole BankAccount group.
            date(doc, details, "PaymentDueDate",
                    invoice.getDueDate() != null ? invoice.getDueDate() : invoice.getIssueDate());
            CompanyEntity company = invoice.getOrder() != null ? invoice.getOrder().getCompany() : null;
            String accountNumber = "";
            String bankCode = "";
            if (company != null && isNotBlank(company.getBankAccount())) {
                String account = company.getBankAccount();
                int slash = account.indexOf('/');
                accountNumber = slash > 0 ? account.substring(0, slash) : account;
                bankCode = slash > 0 ? account.substring(slash + 1) : "";
            }
            text(doc, details, "ID", accountNumber);
            text(doc, details, "BankCode", bankCode);
            text(doc, details, "Name", company != null ? company.getBankName() : "");
            text(doc, details, "IBAN", company != null ? company.getIban() : "");
            text(doc, details, "BIC", company != null ? company.getBic() : "");
            if (isNotBlank(invoice.getVariableSymbol())) {
                text(doc, details, "VariableSymbol", invoice.getVariableSymbol());
            }
        }
    }

    /** Gross unit price derived from the net unit price and the line VAT rate. */
    private BigDecimal unitPriceTaxInclusive(InvoiceItemEntity item) {
        if (item.getUnitPrice() == null) {
            return null;
        }
        if (item.getVatRate() == null) {
            return item.getUnitPrice();
        }
        BigDecimal coefficient = BigDecimal.ONE.add(
                item.getVatRate().divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
        return item.getUnitPrice().multiply(coefficient).setScale(2, RoundingMode.HALF_UP);
    }

    private String buildingNumber(AddressEntity address) {
        if (isNotBlank(address.getHouseNumber())) {
            return address.getHouseNumber();
        }
        return address.getStreetNumber() != null ? address.getStreetNumber() : "";
    }

    private Element child(Document doc, Element parent, String name) {
        Element element = doc.createElementNS(ISDOC_NAMESPACE, name);
        parent.appendChild(element);
        return element;
    }

    private Element text(Document doc, Element parent, String name, String value) {
        Element element = child(doc, parent, name);
        element.setTextContent(value != null ? value : "");
        return element;
    }

    private void date(Document doc, Element parent, String name, LocalDate value) {
        text(doc, parent, name, value != null ? value.format(ISO_DATE) : "");
    }

    private String amount(BigDecimal value) {
        return zeroIfNull(value).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }
}
