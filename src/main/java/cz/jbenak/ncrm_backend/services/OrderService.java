package cz.jbenak.ncrm_backend.services;

import cz.jbenak.ncrm_backend.model.dto.order.OrderDto;
import cz.jbenak.ncrm_backend.model.dto.order.OrderRequest;
import cz.jbenak.ncrm_backend.model.entity.NumberSequenceEntity;
import cz.jbenak.ncrm_backend.model.entity.company.CompanyEntity;
import cz.jbenak.ncrm_backend.model.entity.order.OrderEntity;
import cz.jbenak.ncrm_backend.model.entity.order.OrderItemEntity;
import cz.jbenak.ncrm_backend.model.entity.quotation.QuotationEntity;
import cz.jbenak.ncrm_backend.model.entity.quotation.QuotationItemEntity;
import cz.jbenak.ncrm_backend.model.entity.security.UserEntity;
import cz.jbenak.ncrm_backend.model.entity.store.ItemEntity;
import cz.jbenak.ncrm_backend.model.mapper.OrderMapper;
import cz.jbenak.ncrm_backend.repository.CompanyRepository;
import cz.jbenak.ncrm_backend.repository.ContactPersonRepository;
import cz.jbenak.ncrm_backend.repository.CustomerRepository;
import cz.jbenak.ncrm_backend.repository.ItemRepository;
import cz.jbenak.ncrm_backend.repository.OrderRepository;
import cz.jbenak.ncrm_backend.repository.SalesRepresentativeRepository;
import cz.jbenak.ncrm_backend.repository.UserRepository;
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
 * @since 2026-07-11
 * Service for realization of customer orders. Unit prices are snapshotted from the current item price
 * at the time of ordering and the order total is computed server-side. The customer is notified
 * by e-mail about a newly created order, an updated order and a change of the order status.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    /** Attribute paths of the order entity that can be used by the generic search API. */
    private static final Set<String> SEARCHABLE_FIELDS = Set.of(
            "orderNumber", "orderDate", "status", "totalPrice", "currency", "note",
            "customer.id", "customer.name", "company.id", "company.name",
            "salesRepresentative", "salesRepresentative.id", "salesRepresentative.code");

    /** Name of the role marking customer user accounts. */
    private static final String CUSTOMER_ROLE = "CUSTOMER";

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final CompanyRepository companyRepository;
    private final ContactPersonRepository contactPersonRepository;
    private final SalesRepresentativeRepository salesRepresentativeRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final OrderMapper orderMapper;
    private final OrderEmailService orderEmailService;
    private final NumberSequenceService numberSequenceService;

    @Transactional(readOnly = true)
    public List<OrderDto> findAll() {
        return orderMapper.toDtoList(orderRepository.findAll());
    }

    /**
     * Generic search over orders. Filters are raw {@code field:operator:value} expressions,
     * combined with a logical AND; see {@link SearchSpecificationBuilder}.
     */
    @Transactional(readOnly = true)
    public Page<OrderDto> search(List<String> filters, Pageable pageable) {
        log.debug("Searching orders with filters {}", filters);
        return orderRepository.findAll(SearchSpecificationBuilder.build(filters, SEARCHABLE_FIELDS), pageable)
                .map(orderMapper::toDto);
    }

    @Transactional(readOnly = true)
    public OrderDto findById(UUID id) {
        return orderMapper.toDto(getOrder(id));
    }

    @Transactional(readOnly = true)
    public List<OrderDto> findByCustomer(UUID customerId) {
        return orderMapper.toDtoList(orderRepository.findAllByCustomerId(customerId));
    }

    @Transactional(readOnly = true)
    public List<OrderDto> findBySalesRepresentative(UUID salesRepresentativeId) {
        return orderMapper.toDtoList(orderRepository.findAllBySalesRepresentativeId(salesRepresentativeId));
    }

    public OrderDto create(OrderRequest request) {
        return create(request, null);
    }

    /**
     * Creates a new order on behalf of the given authenticated user. For back-office users the
     * sales representative is mandatory. When the user has the {@code CUSTOMER} role, the order
     * is created without a sales representative for the customer linked to the user account and
     * a notification is additionally sent to the e-mail of the company the order belongs to.
     */
    public OrderDto create(OrderRequest request, String username) {
        UserEntity customerUser = findCustomerUser(username);
        OrderEntity order = new OrderEntity();
        order.setOrderNumber(generateOrderNumber());
        if (customerUser != null) {
            if (customerUser.getCustomer() == null) {
                throw new IllegalStateException("User " + username + " is not linked to any customer");
            }
            order.setCustomer(customerUser.getCustomer());
            order.setCompany(resolveCustomerCompany(customerUser, request.companyId()));
        } else {
            if (request.salesRepresentativeId() == null) {
                throw new IllegalStateException("Sales representative is mandatory for back-office orders");
            }
            order.setCustomer(customerRepository.findById(request.customerId())
                    .orElseThrow(() -> new NotFoundException("Customer", request.customerId())));
            order.setCompany(resolveCompany(request.companyId()));
            order.setSalesRepresentative(salesRepresentativeRepository.findById(request.salesRepresentativeId())
                    .orElseThrow(() -> new NotFoundException("SalesRepresentative", request.salesRepresentativeId())));
        }
        order.setContactPerson(request.contactPersonId() == null ? null
                : contactPersonRepository.findById(request.contactPersonId())
                .orElseThrow(() -> new NotFoundException("ContactPerson", request.contactPersonId())));
        order.setOrderDate(request.orderDate());
        order.setStatus(OrderEntity.OrderStatus.NEW);
        order.setCurrency(request.currency());
        order.setNote(request.note());
        applyItems(order, request);
        log.info("Created order {} for customer {} with {} item(s), total {} {}",
                order.getOrderNumber(), order.getCustomer().getId(), order.getItems().size(),
                order.getTotalPrice(), order.getCurrency());
        OrderEntity saved = orderRepository.save(order);
        orderEmailService.sendOrderCreated(saved);
        if (customerUser != null) {
            orderEmailService.sendCustomerOrderReceived(saved);
        }
        return orderMapper.toDto(saved);
    }

    /** Orders created directly by customer users (no sales representative), shown on the dashboard. */
    @Transactional(readOnly = true)
    public List<OrderDto> findCustomerOrders() {
        return orderMapper.toDtoList(orderRepository.findAllBySalesRepresentativeIsNullOrderByOrderDateDesc());
    }

    /**
     * Creates a new order from an accepted price quotation. Unlike {@link #create(OrderRequest)},
     * the unit prices are not snapshotted from the current item prices but copied from the
     * quotation lines, so the customer gets exactly the quoted prices. The customer is notified by e-mail.
     */
    public OrderEntity createFromQuotation(QuotationEntity quotation) {
        OrderEntity order = new OrderEntity();
        order.setOrderNumber(generateOrderNumber());
        order.setCustomer(quotation.getCustomer());
        order.setCompany(quotation.getCompany());
        order.setContactPerson(quotation.getContactPerson());
        order.setSalesRepresentative(quotation.getSalesRepresentative());
        order.setOrderDate(LocalDate.now(ZoneId.systemDefault()));
        order.setStatus(OrderEntity.OrderStatus.NEW);
        order.setCurrency(quotation.getCurrency());
        order.setNote(quotation.getNote());
        for (QuotationItemEntity quotationItem : quotation.getItems()) {
            OrderItemEntity orderItem = new OrderItemEntity();
            orderItem.setItem(quotationItem.getItem());
            orderItem.setQuantity(quotationItem.getQuantity());
            orderItem.setUnitPrice(quotationItem.getUnitPrice());
            orderItem.setTotalPrice(quotationItem.getTotalPrice());
            order.addItem(orderItem);
        }
        order.setTotalPrice(computeTotal(order));
        log.info("Created order {} from quotation {} with {} item(s), total {} {}",
                order.getOrderNumber(), quotation.getQuotationNumber(), order.getItems().size(),
                order.getTotalPrice(), order.getCurrency());
        OrderEntity saved = orderRepository.save(order);
        orderEmailService.sendOrderCreated(saved);
        return saved;
    }

    /**
     * Updates an existing order from the request. The order items are replaced and their unit
     * prices are snapshotted again from the current item prices. The customer is notified by e-mail.
     */
    public OrderDto update(UUID id, OrderRequest request) {
        OrderEntity order = getOrder(id);
        order.setCustomer(customerRepository.findById(request.customerId())
                .orElseThrow(() -> new NotFoundException("Customer", request.customerId())));
        if (request.companyId() != null) {
            order.setCompany(resolveCompany(request.companyId()));
        }
        order.setContactPerson(request.contactPersonId() == null ? null
                : contactPersonRepository.findById(request.contactPersonId())
                .orElseThrow(() -> new NotFoundException("ContactPerson", request.contactPersonId())));
        if (request.salesRepresentativeId() != null) {
            order.setSalesRepresentative(salesRepresentativeRepository.findById(request.salesRepresentativeId())
                    .orElseThrow(() -> new NotFoundException("SalesRepresentative", request.salesRepresentativeId())));
        }
        order.setOrderDate(request.orderDate());
        order.setCurrency(request.currency());
        order.setNote(request.note());
        order.getItems().clear();
        applyItems(order, request);
        log.info("Updated order {} with {} item(s), total {} {}",
                order.getOrderNumber(), order.getItems().size(), order.getTotalPrice(), order.getCurrency());
        OrderEntity saved = orderRepository.save(order);
        orderEmailService.sendOrderUpdated(saved);
        return orderMapper.toDto(saved);
    }

    public OrderDto updateStatus(UUID id, OrderEntity.OrderStatus status) {
        OrderEntity order = getOrder(id);
        OrderEntity.OrderStatus previousStatus = order.getStatus();
        log.info("Changing status of order {} from {} to {}", order.getOrderNumber(), previousStatus, status);
        order.setStatus(status);
        OrderEntity saved = orderRepository.save(order);
        if (previousStatus != status) {
            orderEmailService.sendOrderStatusChanged(saved, previousStatus);
        }
        return orderMapper.toDto(saved);
    }

    /** Builds the order lines from the request, snapshots the unit prices and computes the total. */
    private void applyItems(OrderEntity order, OrderRequest request) {
        for (OrderRequest.OrderItemRequest itemRequest : request.items()) {
            ItemEntity item = itemRepository.findById(itemRequest.itemId())
                    .orElseThrow(() -> new NotFoundException("Item", itemRequest.itemId()));
            if (item.getPrice() == null) {
                throw new IllegalStateException("Item " + item.getCode() + " has no price defined");
            }
            OrderItemEntity orderItem = new OrderItemEntity();
            orderItem.setItem(item);
            orderItem.setQuantity(itemRequest.quantity());
            orderItem.setUnitPrice(item.getPrice().getPrice());
            orderItem.setTotalPrice(item.getPrice().getPrice().multiply(itemRequest.quantity()));
            if (order.getCurrency() == null) {
                order.setCurrency(item.getPrice().getCurrency());
            }
            order.addItem(orderItem);
        }
        order.setTotalPrice(computeTotal(order));
    }

    /**
     * Computes the order total as a sum of the line totals.
     */
    BigDecimal computeTotal(OrderEntity order) {
        return order.getItems().stream()
                .map(OrderItemEntity::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Resolves the own company issuing the order: the explicitly requested one when given,
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
     * Returns the user when they exist and have the {@code CUSTOMER} role, {@code null} otherwise.
     */
    private UserEntity findCustomerUser(String username) {
        if (username == null) {
            return null;
        }
        return userRepository.findByUsername(username)
                .filter(user -> user.getRoles().stream().anyMatch(role -> CUSTOMER_ROLE.equals(role.getName())))
                .orElse(null);
    }

    /**
     * Resolves the own company for an order placed by a customer user: the explicitly requested
     * company when it is one of the companies assigned to the user, otherwise the (single)
     * assigned company, with the default company as a fallback.
     */
    private CompanyEntity resolveCustomerCompany(UserEntity customerUser, UUID companyId) {
        if (companyId != null) {
            return customerUser.getCompanies().stream()
                    .filter(company -> company.getId().equals(companyId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "Company " + companyId + " is not assigned to user " + customerUser.getUsername()));
        }
        return customerUser.getCompanies().stream().findFirst()
                .orElseGet(() -> resolveCompany(null));
    }

    private OrderEntity getOrder(UUID id) {
        return orderRepository.findById(id).orElseThrow(() -> new NotFoundException("Order", id));
    }

    /**
     * Generates the order number from the configured {@code ORDER} number sequence. When no
     * sequence is defined by the administrator, a legacy fallback number is generated instead.
     */
    private String generateOrderNumber() {
        return numberSequenceService.tryNextNumber(NumberSequenceEntity.SequenceType.ORDER)
                .orElseGet(() -> "ORD-" + LocalDate.now(ZoneId.systemDefault()).format(DateTimeFormatter.BASIC_ISO_DATE)
                        + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }
}
