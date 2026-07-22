package cz.jbenak.ncrm_backend.services;

import cz.jbenak.ncrm_backend.model.dto.quotation.QuotationDto;
import cz.jbenak.ncrm_backend.model.dto.quotation.QuotationRequest;
import cz.jbenak.ncrm_backend.model.entity.NumberSequenceEntity;
import cz.jbenak.ncrm_backend.model.entity.company.CompanyEntity;
import cz.jbenak.ncrm_backend.model.entity.order.OrderEntity;
import cz.jbenak.ncrm_backend.model.entity.quotation.QuotationEntity;
import cz.jbenak.ncrm_backend.model.entity.quotation.QuotationItemEntity;
import cz.jbenak.ncrm_backend.model.entity.store.ItemEntity;
import cz.jbenak.ncrm_backend.model.mapper.QuotationMapper;
import cz.jbenak.ncrm_backend.repository.CompanyRepository;
import cz.jbenak.ncrm_backend.repository.ContactPersonRepository;
import cz.jbenak.ncrm_backend.repository.CustomerRepository;
import cz.jbenak.ncrm_backend.repository.ItemRepository;
import cz.jbenak.ncrm_backend.repository.QuotationRepository;
import cz.jbenak.ncrm_backend.repository.SalesRepresentativeRepository;
import cz.jbenak.ncrm_backend.search.SearchSpecificationBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-22
 * Service for realization of price quotations. Quotations use their own number sequence and follow
 * the same mechanism as orders, except that the unit price or the line total of each item may be
 * manually adjusted. A quotation can be sent to the customer by e-mail and converted into an order.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class QuotationService {

    /** Attribute paths of the quotation entity that can be used by the generic search API. */
    private static final Set<String> SEARCHABLE_FIELDS = Set.of(
            "quotationNumber", "quotationDate", "validUntil", "status", "totalPrice", "currency", "note",
            "customer.id", "customer.name", "company.id", "company.name",
            "salesRepresentative.id", "salesRepresentative.code");

    private final QuotationRepository quotationRepository;
    private final CustomerRepository customerRepository;
    private final CompanyRepository companyRepository;
    private final ContactPersonRepository contactPersonRepository;
    private final SalesRepresentativeRepository salesRepresentativeRepository;
    private final ItemRepository itemRepository;
    private final QuotationMapper quotationMapper;
    private final QuotationEmailService quotationEmailService;
    private final NumberSequenceService numberSequenceService;
    private final OrderService orderService;

    @Transactional(readOnly = true)
    public List<QuotationDto> findAll() {
        return quotationMapper.toDtoList(quotationRepository.findAll());
    }

    /**
     * Generic search over quotations. Filters are raw {@code field:operator:value} expressions,
     * combined with a logical AND; see {@link SearchSpecificationBuilder}.
     */
    @Transactional(readOnly = true)
    public Page<QuotationDto> search(List<String> filters, Pageable pageable) {
        log.debug("Searching quotations with filters {}", filters);
        return quotationRepository.findAll(SearchSpecificationBuilder.build(filters, SEARCHABLE_FIELDS), pageable)
                .map(quotationMapper::toDto);
    }

    @Transactional(readOnly = true)
    public QuotationDto findById(UUID id) {
        return quotationMapper.toDto(getQuotation(id));
    }

    @Transactional(readOnly = true)
    public List<QuotationDto> findByCustomer(UUID customerId) {
        return quotationMapper.toDtoList(quotationRepository.findAllByCustomerId(customerId));
    }

    @Transactional(readOnly = true)
    public List<QuotationDto> findBySalesRepresentative(UUID salesRepresentativeId) {
        return quotationMapper.toDtoList(quotationRepository.findAllBySalesRepresentativeId(salesRepresentativeId));
    }

    public QuotationDto create(QuotationRequest request) {
        QuotationEntity quotation = new QuotationEntity();
        quotation.setQuotationNumber(generateQuotationNumber());
        quotation.setCustomer(customerRepository.findById(request.customerId())
                .orElseThrow(() -> new NotFoundException("Customer", request.customerId())));
        quotation.setCompany(resolveCompany(request.companyId()));
        quotation.setContactPerson(request.contactPersonId() == null ? null
                : contactPersonRepository.findById(request.contactPersonId())
                .orElseThrow(() -> new NotFoundException("ContactPerson", request.contactPersonId())));
        quotation.setSalesRepresentative(salesRepresentativeRepository.findById(request.salesRepresentativeId())
                .orElseThrow(() -> new NotFoundException("SalesRepresentative", request.salesRepresentativeId())));
        quotation.setQuotationDate(request.quotationDate());
        quotation.setValidUntil(request.validUntil());
        quotation.setStatus(QuotationEntity.QuotationStatus.NEW);
        quotation.setCurrency(request.currency());
        quotation.setNote(request.note());
        applyItems(quotation, request);
        log.info("Created quotation {} for customer {} with {} item(s), total {} {}",
                quotation.getQuotationNumber(), request.customerId(), quotation.getItems().size(),
                quotation.getTotalPrice(), quotation.getCurrency());
        QuotationEntity saved = quotationRepository.save(quotation);
        return quotationMapper.toDto(saved);
    }

    /**
     * Updates an existing quotation from the request. The quotation items are replaced; their unit
     * prices are snapshotted again from the current item prices unless overridden by the request.
     * A quotation that is in progress (an order has been created from it) cannot be updated.
     */
    public QuotationDto update(UUID id, QuotationRequest request) {
        QuotationEntity quotation = getQuotation(id);
        requireEditable(quotation);
        quotation.setCustomer(customerRepository.findById(request.customerId())
                .orElseThrow(() -> new NotFoundException("Customer", request.customerId())));
        if (request.companyId() != null) {
            quotation.setCompany(resolveCompany(request.companyId()));
        }
        quotation.setContactPerson(request.contactPersonId() == null ? null
                : contactPersonRepository.findById(request.contactPersonId())
                .orElseThrow(() -> new NotFoundException("ContactPerson", request.contactPersonId())));
        quotation.setSalesRepresentative(salesRepresentativeRepository.findById(request.salesRepresentativeId())
                .orElseThrow(() -> new NotFoundException("SalesRepresentative", request.salesRepresentativeId())));
        quotation.setQuotationDate(request.quotationDate());
        quotation.setValidUntil(request.validUntil());
        quotation.setCurrency(request.currency());
        quotation.setNote(request.note());
        quotation.getItems().clear();
        applyItems(quotation, request);
        log.info("Updated quotation {} with {} item(s), total {} {}",
                quotation.getQuotationNumber(), quotation.getItems().size(),
                quotation.getTotalPrice(), quotation.getCurrency());
        return quotationMapper.toDto(quotationRepository.save(quotation));
    }

    public QuotationDto updateStatus(UUID id, QuotationEntity.QuotationStatus status) {
        QuotationEntity quotation = getQuotation(id);
        requireEditable(quotation);
        log.info("Changing status of quotation {} from {} to {}",
                quotation.getQuotationNumber(), quotation.getStatus(), status);
        quotation.setStatus(status);
        return quotationMapper.toDto(quotationRepository.save(quotation));
    }

    /**
     * Sends the quotation to the customer by e-mail and marks it as {@code SENT} when it is still new.
     */
    public QuotationDto sendToCustomer(UUID id) {
        QuotationEntity quotation = getQuotation(id);
        requireEditable(quotation);
        quotationEmailService.sendQuotation(quotation);
        if (quotation.getStatus() == QuotationEntity.QuotationStatus.NEW) {
            quotation.setStatus(QuotationEntity.QuotationStatus.SENT);
        }
        return quotationMapper.toDto(quotationRepository.save(quotation));
    }

    /**
     * Creates a customer order from the quotation. The order takes over the quoted (possibly manually
     * adjusted) prices and the quotation is marked as {@code IN_PROGRESS} ("V realizaci"), after which
     * it can no longer be modified. A quotation can be converted only once and a cancelled or rejected
     * quotation cannot be converted at all.
     */
    public QuotationDto createOrder(UUID id) {
        QuotationEntity quotation = getQuotation(id);
        if (quotation.getOrder() != null || quotation.getStatus() == QuotationEntity.QuotationStatus.IN_PROGRESS) {
            throw new IllegalStateException("Quotation " + quotation.getQuotationNumber()
                    + " has already been converted to an order");
        }
        if (quotation.getStatus() == QuotationEntity.QuotationStatus.REJECTED
                || quotation.getStatus() == QuotationEntity.QuotationStatus.CANCELLED) {
            throw new IllegalStateException("Quotation " + quotation.getQuotationNumber()
                    + " is " + quotation.getStatus() + " and cannot be converted to an order");
        }
        OrderEntity order = orderService.createFromQuotation(quotation);
        quotation.setOrder(order);
        quotation.setStatus(QuotationEntity.QuotationStatus.IN_PROGRESS);
        log.info("Quotation {} converted to order {}", quotation.getQuotationNumber(), order.getOrderNumber());
        return quotationMapper.toDto(quotationRepository.save(quotation));
    }

    /**
     * Builds the quotation lines from the request. The unit price defaults to a snapshot of the
     * current item price and may be overridden per line; the line total defaults to
     * quantity × unit price and may be overridden as well. Finally the quotation total is computed.
     */
    private void applyItems(QuotationEntity quotation, QuotationRequest request) {
        for (QuotationRequest.QuotationItemRequest itemRequest : request.items()) {
            ItemEntity item = itemRepository.findById(itemRequest.itemId())
                    .orElseThrow(() -> new NotFoundException("Item", itemRequest.itemId()));
            BigDecimal unitPrice = itemRequest.unitPrice();
            if (unitPrice == null) {
                if (item.getPrice() == null) {
                    throw new IllegalStateException("Item " + item.getCode()
                            + " has no price defined and no unit price was given");
                }
                unitPrice = item.getPrice().getPrice();
            }
            QuotationItemEntity quotationItem = new QuotationItemEntity();
            quotationItem.setItem(item);
            quotationItem.setQuantity(itemRequest.quantity());
            quotationItem.setUnitPrice(unitPrice);
            quotationItem.setTotalPrice(itemRequest.totalPrice() != null
                    ? itemRequest.totalPrice()
                    : unitPrice.multiply(itemRequest.quantity()));
            if (quotation.getCurrency() == null && item.getPrice() != null) {
                quotation.setCurrency(item.getPrice().getCurrency());
            }
            quotation.addItem(quotationItem);
        }
        quotation.setTotalPrice(computeTotal(quotation));
    }

    /**
     * Computes the quotation total as a sum of the line totals.
     */
    BigDecimal computeTotal(QuotationEntity quotation) {
        return quotation.getItems().stream()
                .map(QuotationItemEntity::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Resolves the own company issuing the quotation: the explicitly requested one when given,
     * otherwise the default company (may be null when no default company is defined).
     */
    private CompanyEntity resolveCompany(UUID companyId) {
        if (companyId != null) {
            return companyRepository.findById(companyId)
                    .orElseThrow(() -> new NotFoundException("Company", companyId));
        }
        return companyRepository.findByDefaultCompanyTrueAndDeletedFalse().orElse(null);
    }

    /**
     * Ensures the quotation can still be modified. Once an order has been created from the quotation,
     * it is in progress ("V realizaci") and must not be changed any more.
     */
    private void requireEditable(QuotationEntity quotation) {
        if (quotation.getStatus() == QuotationEntity.QuotationStatus.IN_PROGRESS) {
            throw new IllegalStateException("Quotation " + quotation.getQuotationNumber()
                    + " is in progress (an order has been created from it) and cannot be modified");
        }
    }

    private QuotationEntity getQuotation(UUID id) {
        return quotationRepository.findById(id).orElseThrow(() -> new NotFoundException("Quotation", id));
    }

    /**
     * Generates the quotation number from the configured {@code QUOTATION} number sequence. When no
     * sequence is defined by the administrator, a legacy fallback number is generated instead.
     */
    private String generateQuotationNumber() {
        return numberSequenceService.tryNextNumber(NumberSequenceEntity.SequenceType.QUOTATION)
                .orElseGet(() -> "QUO-" + LocalDate.now(ZoneId.systemDefault()).format(DateTimeFormatter.BASIC_ISO_DATE)
                        + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }
}
