package com.training.orderservice.client.dto;

/**
 * Body for PATCH /api/v1/products/reduce/stock/{id}. orderReference lets the
 * Product Service trace which order caused the decrement.
 */
public record StockReductionRequest(Integer quantity, String orderReference) {
}
