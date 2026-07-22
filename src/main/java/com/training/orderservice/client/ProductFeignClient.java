package com.training.orderservice.client;

import com.training.orderservice.client.dto.ProductResponse;
import com.training.orderservice.client.dto.StockAdjustmentRequest;
import com.training.orderservice.client.dto.StockReductionRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Declarative HTTP client for the Product Service, resolved through Eureka by the
 * name "product-service". Maps onto the product endpoints the order flow needs:
 * fetch a product, decrement stock at order time, and restore stock on cancellation.
 */
@FeignClient(name = "product-service")
public interface ProductFeignClient {

    @GetMapping("/api/v1/products/{id}")
    ProductResponse getProduct(@PathVariable("id") Long id);

    @PatchMapping("/api/v1/products/reduce/stock/{id}")
    ProductResponse reduceStock(@PathVariable("id") Long id, @RequestBody StockReductionRequest request);

    @PatchMapping("/api/v1/products/adjust/stock/{id}")
    ProductResponse adjustStock(@PathVariable("id") Long id, @RequestBody StockAdjustmentRequest request);
}
