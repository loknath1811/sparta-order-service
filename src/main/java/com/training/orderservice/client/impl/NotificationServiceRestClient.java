package com.training.orderservice.client.impl;

import com.training.orderservice.client.NotificationFeignClient;
import com.training.orderservice.client.NotificationServiceClient;
import com.training.orderservice.client.dto.EmailNotificationRequest;
import com.training.orderservice.client.dto.OrderConfirmationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Real adapter over {@link NotificationFeignClient}. Flattens the rich order-confirmation
 * payload into the email endpoint's {recipient, subject, message} contract and dispatches it.
 *
 * <p>Runs on the {@code notification-executor} pool (Section 15) so the caller's transaction
 * is never blocked on the HTTP call. The order is already CONFIRMED and stock already reduced
 * by the time we get here, so a notification failure must NOT propagate — it is caught and
 * logged. The endpoint is not idempotent (no order/idempotency key), so we deliberately do
 * NOT retry: a retry would send the customer a duplicate email.
 */
@Component
public class NotificationServiceRestClient implements NotificationServiceClient {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceRestClient.class);

    private final NotificationFeignClient notificationFeignClient;

    public NotificationServiceRestClient(NotificationFeignClient notificationFeignClient) {
        this.notificationFeignClient = notificationFeignClient;
    }

    @Override
    @Async("notification-executor")
    public void sendOrderConfirmation(OrderConfirmationRequest request) {
        try {
            notificationFeignClient.sendEmail(toEmail(request));
            log.info("Order-confirmation email dispatched for order {}", request.orderId());
        } catch (Exception ex) {
            // Best-effort: a confirmation email must never fail an already-confirmed order.
            log.warn("Order-confirmation email failed for order {} (not retried, endpoint is non-idempotent): {}",
                    request.orderId(), ex.getMessage());
        }
    }

    private EmailNotificationRequest toEmail(OrderConfirmationRequest request) {
        String subject = "Order #" + request.orderId() + " confirmed";
        return new EmailNotificationRequest(request.customerEmail(), subject, buildBody(request));
    }

    private String buildBody(OrderConfirmationRequest request) {
        StringBuilder body = new StringBuilder();
        body.append("Hi ").append(request.customerName()).append(",\n\n")
                .append("Your order #").append(request.orderId()).append(" has been confirmed.\n\n")
                .append("Items:\n");
        for (OrderConfirmationRequest.Item item : request.items()) {
            body.append("  - ").append(item.quantity()).append(" x ").append(item.productName()).append("\n");
        }
        body.append("\nTotal: ").append(request.totalAmount())
                .append("\n\nThank you for shopping with us.");
        return body.toString();
    }
}
