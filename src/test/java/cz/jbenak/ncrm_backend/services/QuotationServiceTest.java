package cz.jbenak.ncrm_backend.services;

import cz.jbenak.ncrm_backend.model.dto.quotation.QuotationRequest;
import cz.jbenak.ncrm_backend.model.entity.company.SalesRepresentativeEntity;
import cz.jbenak.ncrm_backend.model.entity.customer.CustomerEntity;
import cz.jbenak.ncrm_backend.model.entity.order.OrderEntity;
import cz.jbenak.ncrm_backend.model.entity.quotation.QuotationEntity;
import cz.jbenak.ncrm_backend.model.entity.quotation.QuotationItemEntity;
import cz.jbenak.ncrm_backend.model.entity.store.ItemEntity;
import cz.jbenak.ncrm_backend.model.entity.store.ItemPriceEntity;
import cz.jbenak.ncrm_backend.model.mapper.QuotationMapper;
import cz.jbenak.ncrm_backend.repository.CompanyRepository;
import cz.jbenak.ncrm_backend.repository.ContactPersonRepository;
import cz.jbenak.ncrm_backend.repository.CustomerRepository;
import cz.jbenak.ncrm_backend.repository.ItemRepository;
import cz.jbenak.ncrm_backend.repository.QuotationRepository;
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
 * @since 2026-07-22
 * Unit tests of {@link QuotationService}: price snapshotting, manual price overrides,
 * sending the quotation by e-mail and conversion into an order.
 */
@ExtendWith(MockitoExtension.class)
class QuotationServiceTest {

    @Mock
    private QuotationRepository quotationRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private CompanyRepository companyRepository;
    @Mock
    private ContactPersonRepository contactPersonRepository;
    @Mock
    private SalesRepresentativeRepository salesRepresentativeRepository;
    @Mock
    private ItemRepository itemRepository;
    @Mock
    private QuotationMapper quotationMapper;
    @Mock
    private QuotationEmailService quotationEmailService;
    @Mock
    private NumberSequenceService numberSequenceService;
    @Mock
    private OrderService orderService;

    @InjectMocks
    private QuotationService quotationService;

    @Test
    void computeTotalSumsLineTotals() {
        QuotationEntity quotation = new QuotationEntity();
        quotation.addItem(item("100.50"));
        quotation.addItem(item("49.50"));

        assertThat(quotationService.computeTotal(quotation)).isEqualByComparingTo("150.00");
    }

    @Test
    void createSnapshotsUnitPriceAndComputesTotal() {
        UUID customerId = UUID.randomUUID();
        UUID repId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(new CustomerEntity()));
        when(salesRepresentativeRepository.findById(repId)).thenReturn(Optional.of(new SalesRepresentativeEntity()));
        when(itemRepository.findById(itemId)).thenReturn(Optional.of(pricedItem("IT-1", "10.00")));
        when(quotationRepository.save(any(QuotationEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        QuotationRequest request = new QuotationRequest(customerId, null, null, repId, LocalDate.now(), null, null, null,
                List.of(new QuotationRequest.QuotationItemRequest(itemId, new BigDecimal("3"), null, null)));
        quotationService.create(request);

        ArgumentCaptor<QuotationEntity> captor = ArgumentCaptor.forClass(QuotationEntity.class);
        verify(quotationRepository).save(captor.capture());
        QuotationEntity saved = captor.getValue();
        assertThat(saved.getItems()).hasSize(1);
        assertThat(saved.getItems().getFirst().getUnitPrice()).isEqualByComparingTo("10.00");
        assertThat(saved.getTotalPrice()).isEqualByComparingTo("30.00");
        assertThat(saved.getCurrency()).isEqualTo("CZK");
        assertThat(saved.getStatus()).isEqualTo(QuotationEntity.QuotationStatus.NEW);
        assertThat(saved.getQuotationNumber()).startsWith("QUO-");
    }

    @Test
    void createAppliesManualUnitPriceOverride() {
        UUID customerId = UUID.randomUUID();
        UUID repId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(new CustomerEntity()));
        when(salesRepresentativeRepository.findById(repId)).thenReturn(Optional.of(new SalesRepresentativeEntity()));
        when(itemRepository.findById(itemId)).thenReturn(Optional.of(pricedItem("IT-1", "10.00")));
        when(quotationRepository.save(any(QuotationEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        QuotationRequest request = new QuotationRequest(customerId, null, null, repId, LocalDate.now(), null, null, null,
                List.of(new QuotationRequest.QuotationItemRequest(itemId, new BigDecimal("2"), new BigDecimal("8.50"), null)));
        quotationService.create(request);

        ArgumentCaptor<QuotationEntity> captor = ArgumentCaptor.forClass(QuotationEntity.class);
        verify(quotationRepository).save(captor.capture());
        QuotationEntity saved = captor.getValue();
        assertThat(saved.getItems().getFirst().getUnitPrice()).isEqualByComparingTo("8.50");
        assertThat(saved.getItems().getFirst().getTotalPrice()).isEqualByComparingTo("17.00");
        assertThat(saved.getTotalPrice()).isEqualByComparingTo("17.00");
    }

    @Test
    void createAppliesManualTotalPriceOverride() {
        UUID customerId = UUID.randomUUID();
        UUID repId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(new CustomerEntity()));
        when(salesRepresentativeRepository.findById(repId)).thenReturn(Optional.of(new SalesRepresentativeEntity()));
        when(itemRepository.findById(itemId)).thenReturn(Optional.of(pricedItem("IT-1", "10.00")));
        when(quotationRepository.save(any(QuotationEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        QuotationRequest request = new QuotationRequest(customerId, null, null, repId, LocalDate.now(), null, null, null,
                List.of(new QuotationRequest.QuotationItemRequest(itemId, new BigDecimal("3"), null, new BigDecimal("25.00"))));
        quotationService.create(request);

        ArgumentCaptor<QuotationEntity> captor = ArgumentCaptor.forClass(QuotationEntity.class);
        verify(quotationRepository).save(captor.capture());
        QuotationEntity saved = captor.getValue();
        assertThat(saved.getItems().getFirst().getUnitPrice()).isEqualByComparingTo("10.00");
        assertThat(saved.getItems().getFirst().getTotalPrice()).isEqualByComparingTo("25.00");
        assertThat(saved.getTotalPrice()).isEqualByComparingTo("25.00");
    }

    @Test
    void createFailsWhenItemHasNoPriceAndNoOverride() {
        UUID customerId = UUID.randomUUID();
        UUID repId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(new CustomerEntity()));
        when(salesRepresentativeRepository.findById(repId)).thenReturn(Optional.of(new SalesRepresentativeEntity()));
        ItemEntity item = new ItemEntity();
        item.setCode("NO-PRICE");
        when(itemRepository.findById(itemId)).thenReturn(Optional.of(item));

        QuotationRequest request = new QuotationRequest(customerId, null, null, repId, LocalDate.now(), null, null, null,
                List.of(new QuotationRequest.QuotationItemRequest(itemId, BigDecimal.ONE, null, null)));

        assertThatThrownBy(() -> quotationService.create(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("NO-PRICE");
    }

    @Test
    void createAcceptsItemWithoutPriceWhenUnitPriceIsGiven() {
        UUID customerId = UUID.randomUUID();
        UUID repId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(new CustomerEntity()));
        when(salesRepresentativeRepository.findById(repId)).thenReturn(Optional.of(new SalesRepresentativeEntity()));
        ItemEntity item = new ItemEntity();
        item.setCode("NO-PRICE");
        when(itemRepository.findById(itemId)).thenReturn(Optional.of(item));
        when(quotationRepository.save(any(QuotationEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        QuotationRequest request = new QuotationRequest(customerId, null, null, repId, LocalDate.now(), null, null, null,
                List.of(new QuotationRequest.QuotationItemRequest(itemId, new BigDecimal("4"), new BigDecimal("5.00"), null)));
        quotationService.create(request);

        ArgumentCaptor<QuotationEntity> captor = ArgumentCaptor.forClass(QuotationEntity.class);
        verify(quotationRepository).save(captor.capture());
        assertThat(captor.getValue().getTotalPrice()).isEqualByComparingTo("20.00");
    }

    @Test
    void sendToCustomerMarksNewQuotationAsSent() {
        UUID quotationId = UUID.randomUUID();
        QuotationEntity quotation = new QuotationEntity();
        quotation.setQuotationNumber("QUO-1");
        quotation.setStatus(QuotationEntity.QuotationStatus.NEW);
        when(quotationRepository.findById(quotationId)).thenReturn(Optional.of(quotation));
        when(quotationRepository.save(quotation)).thenReturn(quotation);

        quotationService.sendToCustomer(quotationId);

        verify(quotationEmailService).sendQuotation(quotation);
        assertThat(quotation.getStatus()).isEqualTo(QuotationEntity.QuotationStatus.SENT);
    }

    @Test
    void createOrderConvertsQuotationAndMarksItAsInProgress() {
        UUID quotationId = UUID.randomUUID();
        QuotationEntity quotation = new QuotationEntity();
        quotation.setQuotationNumber("QUO-1");
        quotation.setStatus(QuotationEntity.QuotationStatus.ACCEPTED);
        when(quotationRepository.findById(quotationId)).thenReturn(Optional.of(quotation));
        when(quotationRepository.save(quotation)).thenReturn(quotation);
        OrderEntity order = new OrderEntity();
        order.setOrderNumber("ORD-1");
        when(orderService.createFromQuotation(quotation)).thenReturn(order);

        quotationService.createOrder(quotationId);

        assertThat(quotation.getOrder()).isSameAs(order);
        assertThat(quotation.getStatus()).isEqualTo(QuotationEntity.QuotationStatus.IN_PROGRESS);
    }

    @Test
    void createOrderFailsWhenQuotationIsAlreadyInProgress() {
        UUID quotationId = UUID.randomUUID();
        QuotationEntity quotation = new QuotationEntity();
        quotation.setQuotationNumber("QUO-1");
        quotation.setStatus(QuotationEntity.QuotationStatus.IN_PROGRESS);
        when(quotationRepository.findById(quotationId)).thenReturn(Optional.of(quotation));

        assertThatThrownBy(() -> quotationService.createOrder(quotationId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already been converted");
        verify(orderService, never()).createFromQuotation(any());
    }

    @Test
    void createOrderFailsWhenQuotationIsRejected() {
        UUID quotationId = UUID.randomUUID();
        QuotationEntity quotation = new QuotationEntity();
        quotation.setQuotationNumber("QUO-1");
        quotation.setStatus(QuotationEntity.QuotationStatus.REJECTED);
        when(quotationRepository.findById(quotationId)).thenReturn(Optional.of(quotation));

        assertThatThrownBy(() -> quotationService.createOrder(quotationId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cannot be converted");
        verify(orderService, never()).createFromQuotation(any());
    }

    @Test
    void updateFailsWhenQuotationIsInProgress() {
        UUID quotationId = UUID.randomUUID();
        QuotationEntity quotation = new QuotationEntity();
        quotation.setQuotationNumber("QUO-1");
        quotation.setStatus(QuotationEntity.QuotationStatus.IN_PROGRESS);
        when(quotationRepository.findById(quotationId)).thenReturn(Optional.of(quotation));

        QuotationRequest request = new QuotationRequest(UUID.randomUUID(), null, null, UUID.randomUUID(),
                LocalDate.now(), null, null, null,
                List.of(new QuotationRequest.QuotationItemRequest(UUID.randomUUID(), BigDecimal.ONE, null, null)));

        assertThatThrownBy(() -> quotationService.update(quotationId, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cannot be modified");
    }

    @Test
    void updateStatusFailsWhenQuotationIsInProgress() {
        UUID quotationId = UUID.randomUUID();
        QuotationEntity quotation = new QuotationEntity();
        quotation.setQuotationNumber("QUO-1");
        quotation.setStatus(QuotationEntity.QuotationStatus.IN_PROGRESS);
        when(quotationRepository.findById(quotationId)).thenReturn(Optional.of(quotation));

        assertThatThrownBy(() -> quotationService.updateStatus(quotationId, QuotationEntity.QuotationStatus.CANCELLED))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cannot be modified");
        assertThat(quotation.getStatus()).isEqualTo(QuotationEntity.QuotationStatus.IN_PROGRESS);
    }

    @Test
    void sendToCustomerFailsWhenQuotationIsInProgress() {
        UUID quotationId = UUID.randomUUID();
        QuotationEntity quotation = new QuotationEntity();
        quotation.setQuotationNumber("QUO-1");
        quotation.setStatus(QuotationEntity.QuotationStatus.IN_PROGRESS);
        when(quotationRepository.findById(quotationId)).thenReturn(Optional.of(quotation));

        assertThatThrownBy(() -> quotationService.sendToCustomer(quotationId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cannot be modified");
        verify(quotationEmailService, never()).sendQuotation(any());
    }

    private ItemEntity pricedItem(String code, String price) {
        ItemEntity item = new ItemEntity();
        item.setCode(code);
        ItemPriceEntity itemPrice = new ItemPriceEntity();
        itemPrice.setPrice(new BigDecimal(price));
        itemPrice.setCurrency("CZK");
        item.setPrice(itemPrice);
        return item;
    }

    private QuotationItemEntity item(String totalPrice) {
        QuotationItemEntity item = new QuotationItemEntity();
        item.setTotalPrice(new BigDecimal(totalPrice));
        return item;
    }
}
