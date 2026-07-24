package com.training.orderservice.event;

import com.training.orderservice.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Payload published to Kafka whenever an order is created or its status changes.
 * Consumers (notification-service, analytics, audit, etc.) react to these independently
 * of the synchronous request that triggered them.
 */
public record OrderEvent(
        Long orderId,
        Long customerId,
        OrderStatus status,
        BigDecimal totalAmount,
        LocalDateTime occurredAt) {

    public static OrderEvent of(Long orderId, Long customerId, OrderStatus status, BigDecimal totalAmount) {
        return new OrderEvent(orderId, customerId, status, totalAmount, LocalDateTime.now());
    }
}
