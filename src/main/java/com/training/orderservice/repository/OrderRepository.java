package com.training.orderservice.repository;

import com.training.orderservice.entity.Order;
import com.training.orderservice.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items WHERE o.id = :orderId")
    Optional<Order> findByIdWithItems(@Param("orderId") Long orderId);

    // Product Service calls this (via GET /api/v1/orders/product/{productId}/has-open) before
    // discontinuing a product: true if any order item for the product belongs to an order in one
    // of the given (open/non-terminal) statuses.
    @Query("SELECT CASE WHEN COUNT(oi) > 0 THEN true ELSE false END "
            + "FROM OrderItem oi WHERE oi.productId = :productId AND oi.order.status IN :statuses")
    boolean existsOpenOrderForProduct(@Param("productId") Long productId,
                                      @Param("statuses") Collection<OrderStatus> statuses);

    @Query("SELECT o FROM Order o WHERE (:status IS NULL OR o.status = :status) "
            + "AND (:customerId IS NULL OR o.customerId = :customerId)")
    Page<Order> findByOptionalFilters(@Param("status") OrderStatus status,
                                       @Param("customerId") Long customerId,
                                       Pageable pageable);

    Page<Order> findByStatusAndUpdatedAtBefore(OrderStatus status, LocalDateTime threshold, Pageable pageable);
}
