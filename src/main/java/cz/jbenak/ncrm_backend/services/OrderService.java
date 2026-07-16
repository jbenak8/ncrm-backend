package cz.jbenak.ncrm_backend.services;

import cz.jbenak.ncrm_backend.model.dto.order.OrderDto;
import cz.jbenak.ncrm_backend.model.dto.order.OrderRequest;
import cz.jbenak.ncrm_backend.model.entity.order.OrderEntity;
import cz.jbenak.ncrm_backend.model.entity.order.OrderItemEntity;
import cz.jbenak.ncrm_backend.model.entity.store.ItemEntity;
import cz.jbenak.ncrm_backend.model.mapper.OrderMapper;
import cz.jbenak.ncrm_backend.repository.ContactPersonRepository;
import cz.jbenak.ncrm_backend.repository.CustomerRepository;
import cz.jbenak.ncrm_backend.repository.ItemRepository;
import cz.jbenak.ncrm_backend.repository.OrderRepository;
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
            "customer.id", "customer.name", "salesRepresentative.id", "salesRepresentative.code");

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final ContactPersonRepository contactPersonRepository;
    private final SalesRepresentativeRepository salesRepresentativeRepository;
    private final ItemRepository itemRepository;
    private final OrderMapper orderMapper;
    private final OrderEmailService orderEmailService;

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
        OrderEntity order = new OrderEntity();
        order.setOrderNumber(generateOrderNumber());
        order.setCustomer(customerRepository.findById(request.customerId())
                .orElseThrow(() -> new NotFoundException("Customer", request.customerId())));
        order.setContactPerson(request.contactPersonId() == null ? null
                : contactPersonRepository.findById(request.contactPersonId())
                .orElseThrow(() -> new NotFoundException("ContactPerson", request.contactPersonId())));
        order.setSalesRepresentative(salesRepresentativeRepository.findById(request.salesRepresentativeId())
                .orElseThrow(() -> new NotFoundException("SalesRepresentative", request.salesRepresentativeId())));
        order.setOrderDate(request.orderDate());
        order.setStatus(OrderEntity.OrderStatus.NEW);
        order.setCurrency(request.currency());
        order.setNote(request.note());
        applyItems(order, request);
        log.info("Created order {} for customer {} with {} item(s), total {} {}",
                order.getOrderNumber(), request.customerId(), order.getItems().size(),
                order.getTotalPrice(), order.getCurrency());
        OrderEntity saved = orderRepository.save(order);
        orderEmailService.sendOrderCreated(saved);
        return orderMapper.toDto(saved);
    }

    /**
     * Updates an existing order from the request. The order items are replaced and their unit
     * prices are snapshotted again from the current item prices. The customer is notified by e-mail.
     */
    public OrderDto update(UUID id, OrderRequest request) {
        OrderEntity order = getOrder(id);
        order.setCustomer(customerRepository.findById(request.customerId())
                .orElseThrow(() -> new NotFoundException("Customer", request.customerId())));
        order.setContactPerson(request.contactPersonId() == null ? null
                : contactPersonRepository.findById(request.contactPersonId())
                .orElseThrow(() -> new NotFoundException("ContactPerson", request.contactPersonId())));
        order.setSalesRepresentative(salesRepresentativeRepository.findById(request.salesRepresentativeId())
                .orElseThrow(() -> new NotFoundException("SalesRepresentative", request.salesRepresentativeId())));
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

    private OrderEntity getOrder(UUID id) {
        return orderRepository.findById(id).orElseThrow(() -> new NotFoundException("Order", id));
    }

    private String generateOrderNumber() {
        return "ORD-" + LocalDate.now(ZoneId.systemDefault()).format(DateTimeFormatter.BASIC_ISO_DATE)
                + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
