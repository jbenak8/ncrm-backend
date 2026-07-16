package cz.jbenak.ncrm_backend.services;

import cz.jbenak.ncrm_backend.model.dto.order.OrderRequest;
import cz.jbenak.ncrm_backend.model.entity.company.SalesRepresentativeEntity;
import cz.jbenak.ncrm_backend.model.entity.customer.CustomerEntity;
import cz.jbenak.ncrm_backend.model.entity.order.OrderEntity;
import cz.jbenak.ncrm_backend.model.entity.order.OrderItemEntity;
import cz.jbenak.ncrm_backend.model.entity.store.ItemEntity;
import cz.jbenak.ncrm_backend.model.entity.store.ItemPriceEntity;
import cz.jbenak.ncrm_backend.model.mapper.OrderMapper;
import cz.jbenak.ncrm_backend.repository.ContactPersonRepository;
import cz.jbenak.ncrm_backend.repository.CustomerRepository;
import cz.jbenak.ncrm_backend.repository.ItemRepository;
import cz.jbenak.ncrm_backend.repository.OrderRepository;
import cz.jbenak.ncrm_backend.repository.SalesRepresentativeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-11
 * Unit tests of {@link OrderService}: price snapshotting and total computation.
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private ContactPersonRepository contactPersonRepository;
    @Mock
    private SalesRepresentativeRepository salesRepresentativeRepository;
    @Mock
    private ItemRepository itemRepository;
    @Mock
    private OrderMapper orderMapper;
    @Mock
    private OrderEmailService orderEmailService;

    @InjectMocks
    private OrderService orderService;

    @Test
    void computeTotalSumsLineTotals() {
        OrderEntity order = new OrderEntity();
        order.addItem(item("100.50"));
        order.addItem(item("49.50"));

        assertThat(orderService.computeTotal(order)).isEqualByComparingTo("150.00");
    }

    @Test
    void createSnapshotsUnitPriceAndComputesTotal() {
        UUID customerId = UUID.randomUUID();
        UUID repId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        CustomerEntity customer = new CustomerEntity();
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(salesRepresentativeRepository.findById(repId)).thenReturn(Optional.of(new SalesRepresentativeEntity()));

        ItemEntity item = new ItemEntity();
        item.setCode("IT-1");
        ItemPriceEntity price = new ItemPriceEntity();
        price.setPrice(new BigDecimal("10.00"));
        price.setCurrency("CZK");
        item.setPrice(price);
        when(itemRepository.findById(itemId)).thenReturn(Optional.of(item));
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderRequest request = new OrderRequest(customerId, null, repId, LocalDate.now(), null, null,
                List.of(new OrderRequest.OrderItemRequest(itemId, new BigDecimal("3"))));
        orderService.create(request);

        ArgumentCaptor<OrderEntity> captor = ArgumentCaptor.forClass(OrderEntity.class);
        verify(orderRepository).save(captor.capture());
        OrderEntity saved = captor.getValue();
        assertThat(saved.getItems()).hasSize(1);
        assertThat(saved.getItems().getFirst().getUnitPrice()).isEqualByComparingTo("10.00");
        assertThat(saved.getTotalPrice()).isEqualByComparingTo("30.00");
        assertThat(saved.getCurrency()).isEqualTo("CZK");
        assertThat(saved.getStatus()).isEqualTo(OrderEntity.OrderStatus.NEW);
        assertThat(saved.getOrderNumber()).startsWith("ORD-");
        verify(orderEmailService).sendOrderCreated(saved);
    }

    @Test
    void updateReplacesItemsAndNotifiesCustomer() {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID repId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        OrderEntity order = new OrderEntity();
        order.setOrderNumber("ORD-1");
        order.setStatus(OrderEntity.OrderStatus.NEW);
        order.addItem(item("5.00"));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(new CustomerEntity()));
        when(salesRepresentativeRepository.findById(repId)).thenReturn(Optional.of(new SalesRepresentativeEntity()));

        ItemEntity item = new ItemEntity();
        item.setCode("IT-2");
        ItemPriceEntity price = new ItemPriceEntity();
        price.setPrice(new BigDecimal("20.00"));
        price.setCurrency("CZK");
        item.setPrice(price);
        when(itemRepository.findById(itemId)).thenReturn(Optional.of(item));
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderRequest request = new OrderRequest(customerId, null, repId, LocalDate.now(), null, null,
                List.of(new OrderRequest.OrderItemRequest(itemId, new BigDecimal("2"))));
        orderService.update(orderId, request);

        assertThat(order.getItems()).hasSize(1);
        assertThat(order.getItems().getFirst().getUnitPrice()).isEqualByComparingTo("20.00");
        assertThat(order.getTotalPrice()).isEqualByComparingTo("40.00");
        verify(orderEmailService).sendOrderUpdated(order);
    }

    @Test
    void updateStatusNotifiesCustomerAboutChange() {
        UUID orderId = UUID.randomUUID();
        OrderEntity order = new OrderEntity();
        order.setOrderNumber("ORD-1");
        order.setStatus(OrderEntity.OrderStatus.NEW);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        orderService.updateStatus(orderId, OrderEntity.OrderStatus.CONFIRMED);

        assertThat(order.getStatus()).isEqualTo(OrderEntity.OrderStatus.CONFIRMED);
        verify(orderEmailService).sendOrderStatusChanged(order, OrderEntity.OrderStatus.NEW);
    }

    @Test
    void updateStatusDoesNotNotifyWhenStatusIsUnchanged() {
        UUID orderId = UUID.randomUUID();
        OrderEntity order = new OrderEntity();
        order.setOrderNumber("ORD-1");
        order.setStatus(OrderEntity.OrderStatus.NEW);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        orderService.updateStatus(orderId, OrderEntity.OrderStatus.NEW);

        verify(orderEmailService, never()).sendOrderStatusChanged(any(), any());
    }

    @Test
    void createFailsWhenItemHasNoPrice() {
        UUID customerId = UUID.randomUUID();
        UUID repId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(new CustomerEntity()));
        when(salesRepresentativeRepository.findById(repId)).thenReturn(Optional.of(new SalesRepresentativeEntity()));
        ItemEntity item = new ItemEntity();
        item.setCode("NO-PRICE");
        when(itemRepository.findById(itemId)).thenReturn(Optional.of(item));

        OrderRequest request = new OrderRequest(customerId, null, repId, LocalDate.now(), null, null,
                List.of(new OrderRequest.OrderItemRequest(itemId, BigDecimal.ONE)));

        assertThatThrownBy(() -> orderService.create(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("NO-PRICE");
    }

    private OrderItemEntity item(String totalPrice) {
        OrderItemEntity item = new OrderItemEntity();
        item.setTotalPrice(new BigDecimal(totalPrice));
        return item;
    }
}
