package cz.jbenak.ncrm_backend.model.entity.order;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-08-26
 * Utility computing the VAT amounts and totals of an order. The order lines store net prices
 * only, so the VAT rate is taken from the current item catalogue price (the same rule that is
 * applied when the order is invoiced). All VAT amounts are rounded to 2 decimal places
 * (HALF_UP), consistently with the invoicing service.
 */
public final class OrderTotals {

    private OrderTotals() {
    }

    /** VAT rate in percent of the order line taken from the current item price, {@code null} when unknown. */
    public static BigDecimal vatRate(OrderItemEntity item) {
        return item.getItem() == null || item.getItem().getPrice() == null
                ? null : item.getItem().getPrice().getVatRate();
    }

    /** Net total of the order line (falls back to quantity times unit price when not stored). */
    public static BigDecimal lineNet(OrderItemEntity item) {
        if (item.getTotalPrice() != null) {
            return item.getTotalPrice();
        }
        return item.getUnitPrice() == null || item.getQuantity() == null
                ? BigDecimal.ZERO : item.getUnitPrice().multiply(item.getQuantity());
    }

    /** VAT amount of the order line; zero when the item has no VAT rate defined. */
    public static BigDecimal lineVat(OrderItemEntity item) {
        BigDecimal rate = vatRate(item);
        return rate == null
                ? BigDecimal.ZERO
                : lineNet(item).multiply(rate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    /** Order total without VAT as the sum of the line net totals. */
    public static BigDecimal totalNet(OrderEntity order) {
        return order.getItems().stream()
                .map(OrderTotals::lineNet)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Total VAT of the order as the sum of the line VAT amounts. */
    public static BigDecimal totalVat(OrderEntity order) {
        return order.getItems().stream()
                .map(OrderTotals::lineVat)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Order total including VAT. */
    public static BigDecimal totalGross(OrderEntity order) {
        return totalNet(order).add(totalVat(order));
    }
}
