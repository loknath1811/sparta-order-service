package com.training.orderservice.client.impl;

import com.training.orderservice.client.ProductFeignClient;
import com.training.orderservice.client.ProductServiceClient;
import com.training.orderservice.client.dto.ProductResponse;
import com.training.orderservice.client.dto.ProductSnapshot;
import com.training.orderservice.client.dto.StockAdjustmentRequest;
import com.training.orderservice.client.dto.StockReductionRequest;
import com.training.orderservice.exception.InsufficientStockException;
import com.training.orderservice.exception.ProductNotFoundException;
import com.training.orderservice.exception.ProductServiceUnavailableException;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Real adapter over {@link ProductFeignClient}. Wraps every call with the "productService"
 * circuit breaker (Section 12 / resilience4j config): when the Product Service is repeatedly
 * failing or unreachable the breaker opens and the fallback fails fast with a 503-mapped
 * {@link ProductServiceUnavailableException} instead of hanging the order flow.
 *
 * <p>Business outcomes are translated to domain exceptions and are configured as
 * ignore-exceptions on the breaker so they never trip it: a missing product -> 404
 * {@link ProductNotFoundException}, insufficient stock at decrement time -> 409
 * {@link InsufficientStockException}.
 */
@Component
public class ProductServiceRestClient implements ProductServiceClient {

    private static final Logger log = LoggerFactory.getLogger(ProductServiceRestClient.class);

    private final ProductFeignClient productFeignClient;

    public ProductServiceRestClient(ProductFeignClient productFeignClient) {
        this.productFeignClient = productFeignClient;
    }

    @Override
    @CircuitBreaker(name = "productService", fallbackMethod = "getProductFallback")
    public ProductSnapshot getProduct(Long productId) {
        try {
            ProductResponse product = productFeignClient.getProduct(productId);
            return new ProductSnapshot(product.id(), product.productName(), product.price(), product.stockQuantity());
        } catch (FeignException.NotFound ex) {
            throw new ProductNotFoundException("Product " + productId + " does not exist");
        }
    }

    @Override
    @CircuitBreaker(name = "productService", fallbackMethod = "reduceStockFallback")
    public void reduceStock(Long productId, int quantity, Long orderId) {
        try {
            productFeignClient.reduceStock(productId, new StockReductionRequest(quantity, "ORDER-" + orderId));
        } catch (FeignException.NotFound ex) {
            throw new ProductNotFoundException("Product " + productId + " does not exist");
        } catch (FeignException.Conflict ex) {
            throw new InsufficientStockException(
                    "Product " + productId + " no longer has " + quantity + " units available");
        }
    }

    @Override
    @CircuitBreaker(name = "productService", fallbackMethod = "restoreStockFallback")
    public void restoreStock(Long productId, int quantity, Long orderId) {
        // Compensating call for cancellation (BR-6): put the reserved units back.
        productFeignClient.adjustStock(productId, new StockAdjustmentRequest(quantity, "INCREASE"));
    }

    // ----- fallbacks: invoked on any thrown exception (incl. an open breaker) -----

    @SuppressWarnings("unused")
    private ProductSnapshot getProductFallback(Long productId, Throwable t) {
        rethrowBusiness(t);
        log.warn("Product Service unavailable while fetching product {}: {}", productId, t.getMessage());
        throw new ProductServiceUnavailableException("Product Service unavailable while fetching product " + productId, t);
    }

    @SuppressWarnings("unused")
    private void reduceStockFallback(Long productId, int quantity, Long orderId, Throwable t) {
        rethrowBusiness(t);
        log.warn("Product Service unavailable while reducing stock for product {}: {}", productId, t.getMessage());
        throw new ProductServiceUnavailableException("Product Service unavailable while reducing stock for product " + productId, t);
    }

    @SuppressWarnings("unused")
    private void restoreStockFallback(Long productId, int quantity, Long orderId, Throwable t) {
        // cancelOrder() already catches this and records a reconciliation log (Section 31) so the
        // sweep can retry the restore later — cancellation itself still succeeds. So it is safe
        // (and correct) to surface the failure here rather than swallow it silently.
        log.warn("Product Service unavailable while restoring stock for product {}: {}", productId, t.getMessage());
        throw new ProductServiceUnavailableException("Product Service unavailable while restoring stock for product " + productId, t);
    }

    // Business exceptions are not Product-Service failures — surface them unchanged.
    private void rethrowBusiness(Throwable t) {
        if (t instanceof ProductNotFoundException pnfe) {
            throw pnfe;
        }
        if (t instanceof InsufficientStockException ise) {
            throw ise;
        }
    }
}
