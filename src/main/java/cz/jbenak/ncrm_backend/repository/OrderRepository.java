package cz.jbenak.ncrm_backend.repository;

import cz.jbenak.ncrm_backend.model.entity.order.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * @author Jan BenĂˇk
 * @version 1.0
 * @since 2026-07-11
 * Repository for customer orders including aggregation queries used by the owner dashboards and reports.
 */
@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, UUID>, JpaSpecificationExecutor<OrderEntity> {

    Optional<OrderEntity> findByOrderNumber(String orderNumber);

    List<OrderEntity> findAllByCustomerId(UUID customerId);

    List<OrderEntity> findAllBySalesRepresentativeId(UUID salesRepresentativeId);

    List<OrderEntity> findAllByStatus(OrderEntity.OrderStatus status);

    long countByStatusIn(List<OrderEntity.OrderStatus> statuses);

    @Query("select coalesce(sum(o.totalPrice), 0) from OrderEntity o where o.status <> 'CANCELLED'")
    BigDecimal sumTotalRevenue();

    @Query("""
            select function('to_char', o.orderDate, 'YYYY-MM'), count(o), coalesce(sum(o.totalPrice), 0)
            from OrderEntity o where o.status <> 'CANCELLED'
            group by function('to_char', o.orderDate, 'YYYY-MM')
            order by function('to_char', o.orderDate, 'YYYY-MM')
            """)
    List<Object[]> aggregateOrdersByMonth();

    @Query("""
            select o.salesRepresentative.id, count(o), coalesce(sum(o.totalPrice), 0)
            from OrderEntity o where o.status <> 'CANCELLED'
            group by o.salesRepresentative.id
            """)
    List<Object[]> aggregateOrdersBySalesRepresentative();

    @Query("""
            select o.customer.id, o.customer.name, count(o), coalesce(sum(o.totalPrice), 0) as revenue
            from OrderEntity o where o.status <> 'CANCELLED'
            group by o.customer.id, o.customer.name order by revenue desc limit :limit
            """)
    List<Object[]> aggregateTopCustomers(@Param("limit") int limit);
}
