package com.training.orderservice.controller;

import com.training.orderservice.dto.request.CreateOrderRequest;
import com.training.orderservice.dto.request.UpdateOrderStatusRequest;
import com.training.orderservice.dto.response.OrderResponse;
import com.training.orderservice.entity.Order;
import com.training.orderservice.entity.OrderStatus;
import com.training.orderservice.mapper.OrderMapper;
import com.training.orderservice.security.CallerContext;
import com.training.orderservice.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private static final int MAX_PAGE_SIZE = 100;

    private final OrderService orderService;
    private final OrderMapper orderMapper;

    public OrderController(OrderService orderService, OrderMapper orderMapper) {
        this.orderService = orderService;
        this.orderMapper = orderMapper;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        OrderResponse response = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{orderId}")
    public OrderResponse getOrder(
            @PathVariable Long orderId,
            @RequestHeader("X-Customer-Id") Long customerId,
            @RequestHeader(value = "X-User-Role", required = false, defaultValue = "CUSTOMER") String role) {
        CallerContext caller = new CallerContext(customerId, role);
        Order order = orderService.getOrderById(orderId, caller);
        return orderMapper.toResponse(order);
    }

    @GetMapping
    public ResponseEntity<Page<OrderResponse>> listOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) Long customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int clampedSize = Math.min(size, MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(page, clampedSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(orderService.getOrders(status, customerId, pageable));
    }

    @PatchMapping("/{orderId}/status")
    public ResponseEntity<OrderResponse> updateStatus(
            @PathVariable Long orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request) {

        OrderResponse response = orderService.updateStatus(orderId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(
            @PathVariable Long orderId,
            @RequestHeader("X-Customer-Id") Long customerId,
            @RequestHeader(value = "X-User-Role", required = false, defaultValue = "CUSTOMER") String role) {
        CallerContext caller = new CallerContext(customerId, role);
        return ResponseEntity.ok(orderService.cancelOrder(orderId, caller));
    }

    // Called by the Product Service (Feign, resolved via Eureka: name "order-service") before it
    // discontinues a product. Returns true if any open order still references the product, in which
    // case Product refuses the delete. See OrderServiceImpl.OPEN_STATUSES for what counts as "open".
    @GetMapping("/product/{productId}/has-open")
    public boolean hasOpenOrders(@PathVariable Long productId) {
        return orderService.hasOpenOrders(productId);
    }

    @DeleteMapping("/{orderId}")
    public ResponseEntity<Void> deleteOrder(
            @PathVariable Long orderId,
            @RequestHeader(value = "X-Customer-Id", required = false) Long customerId,
            @RequestHeader(value = "X-User-Role", required = false, defaultValue = "CUSTOMER") String role) {
        // Hard-delete is admin-only (BR-11) and independent of ownership, so
        // X-Customer-Id is optional here; the admin-role check happens in the service.
        CallerContext caller = new CallerContext(customerId, role);
        orderService.deleteOrder(orderId, caller);
        return ResponseEntity.noContent().build();
    }
}
