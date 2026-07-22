package com.training.orderservice.client.impl;

import com.training.orderservice.client.ProductFeignClient;
import com.training.orderservice.client.dto.ProductResponse;
import com.training.orderservice.client.dto.ProductSnapshot;
import com.training.orderservice.client.dto.StockAdjustmentRequest;
import com.training.orderservice.client.dto.StockReductionRequest;
import com.training.orderservice.exception.InsufficientStockException;
import com.training.orderservice.exception.ProductNotFoundException;
import feign.FeignException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceRestClientTest {

    @Mock
    private ProductFeignClient productFeignClient;

    @InjectMocks
    private ProductServiceRestClient client;

    @Test
    void getProduct_mapsResponseToSnapshot() {
        when(productFeignClient.getProduct(55L))
                .thenReturn(new ProductResponse(55L, "Wireless Mouse", new BigDecimal("25.00"), 10));

        ProductSnapshot snapshot = client.getProduct(55L);

        assertThat(snapshot.productId()).isEqualTo(55L);
        assertThat(snapshot.name()).isEqualTo("Wireless Mouse");
        assertThat(snapshot.price()).isEqualByComparingTo("25.00");
        assertThat(snapshot.stockQuantity()).isEqualTo(10);
    }

    @Test
    void getProduct_translates404ToProductNotFound() {
        doThrow(mock(FeignException.NotFound.class)).when(productFeignClient).getProduct(55L);

        assertThatThrownBy(() -> client.getProduct(55L))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void reduceStock_sendsQuantityAndOrderReference() {
        client.reduceStock(55L, 3, 1001L);

        ArgumentCaptor<StockReductionRequest> captor = ArgumentCaptor.forClass(StockReductionRequest.class);
        verify(productFeignClient).reduceStock(eq(55L), captor.capture());
        assertThat(captor.getValue().quantity()).isEqualTo(3);
        assertThat(captor.getValue().orderReference()).isEqualTo("ORDER-1001");
    }

    @Test
    void reduceStock_translates409ToInsufficientStock() {
        doThrow(mock(FeignException.Conflict.class)).when(productFeignClient).reduceStock(eq(55L), any());

        assertThatThrownBy(() -> client.reduceStock(55L, 3, 1001L))
                .isInstanceOf(InsufficientStockException.class);
    }

    @Test
    void restoreStock_sendsIncreaseAdjustment() {
        client.restoreStock(55L, 2, 1001L);

        ArgumentCaptor<StockAdjustmentRequest> captor = ArgumentCaptor.forClass(StockAdjustmentRequest.class);
        verify(productFeignClient).adjustStock(eq(55L), captor.capture());
        assertThat(captor.getValue().quantity()).isEqualTo(2);
        assertThat(captor.getValue().operation()).isEqualTo("INCREASE");
    }
}
