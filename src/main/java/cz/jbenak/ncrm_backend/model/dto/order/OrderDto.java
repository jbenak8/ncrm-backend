package cz.jbenak.ncrm_backend.model.dto.order;

import cz.jbenak.ncrm_backend.model.entity.order.OrderEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-11
 * DTO representing a customer order with its items, exposed to the React frontend.
 */
public record OrderDto(
        UUID id,
        String orderNumber,
        UUID customerId,
        String customerName,
        UUID contactPersonId,
        String contactPersonName,
        UUID salesRepresentativeId,
        String salesRepresentativeName,
        LocalDate orderDate,
        OrderEntity.OrderStatus status,
        BigDecimal totalPrice,
        String currency,
        String note,
        List<OrderItemDto> items
) {
}
