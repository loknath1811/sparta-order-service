package com.training.orderservice.client.dto;

/**
 * Body for PATCH /api/v1/products/adjust/stock/{id}. operation is the Product
 * Service's StockOperation ("INCREASE" / "DECREASE" / "SET") as a string; order
 * cancellation uses "INCREASE" to restore previously-reduced stock (BR-6).
 */
public record StockAdjustmentRequest(Integer quantity, String operation) {
}
