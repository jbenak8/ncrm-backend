package cz.jbenak.ncrm_backend.model.entity.order;

import cz.jbenak.ncrm_backend.model.entity.store.ItemEntity;
import cz.jbenak.ncrm_backend.model.entity.store.ItemPriceEntity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-08-26
 * Unit tests of the {@link OrderTotals} VAT computation used by the order API and the printed order.
 */
class OrderTotalsTest {

    @Test
    void computesGrossTotalFromItemVatRates() {
        OrderEntity order = new OrderEntity();
        order.addItem(item("100.00", "21"));
        order.addItem(item("200.00", "12"));

        assertThat(OrderTotals.totalNet(order)).isEqualByComparingTo("300.00");
        assertThat(OrderTotals.totalVat(order)).isEqualByComparingTo("45.00");
        assertThat(OrderTotals.totalGross(order)).isEqualByComparingTo("345.00");
    }

    @Test
    void missingVatRateIsTreatedAsZero() {
        OrderEntity order = new OrderEntity();
        order.addItem(item("150.00", null));

        assertThat(OrderTotals.totalVat(order)).isEqualByComparingTo("0");
        assertThat(OrderTotals.totalGross(order)).isEqualByComparingTo("150.00");
    }

    @Test
    void fallsBackToQuantityTimesUnitPriceWhenLineTotalMissing() {
        OrderItemEntity line = item(null, "21");
        line.setQuantity(new BigDecimal("3"));
        line.setUnitPrice(new BigDecimal("10.00"));
        OrderEntity order = new OrderEntity();
        order.addItem(line);

        assertThat(OrderTotals.totalNet(order)).isEqualByComparingTo("30.00");
        assertThat(OrderTotals.totalGross(order)).isEqualByComparingTo("36.30");
    }

    private OrderItemEntity item(String lineTotal, String vatRate) {
        ItemPriceEntity price = new ItemPriceEntity();
        price.setVatRate(vatRate == null ? null : new BigDecimal(vatRate));
        ItemEntity item = new ItemEntity();
        item.setPrice(price);
        OrderItemEntity line = new OrderItemEntity();
        line.setItem(item);
        line.setTotalPrice(lineTotal == null ? null : new BigDecimal(lineTotal));
        return line;
    }
}
