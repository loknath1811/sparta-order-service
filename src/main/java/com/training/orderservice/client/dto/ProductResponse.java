package com.training.orderservice.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

/**
 * Subset of the Product Service's product response we actually need. Extra fields
 * (description, category, timestamps…) are ignored so the contract stays loose.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductResponse(Long id, String productName, BigDecimal price, Integer stockQuantity) {
}
