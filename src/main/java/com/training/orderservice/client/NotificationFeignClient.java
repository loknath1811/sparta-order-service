package com.training.orderservice.client;

import com.training.orderservice.client.dto.EmailNotificationRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Declarative HTTP client for the Notification Service. The name "notification-service"
 * is resolved through Eureka (the same registry Product uses to reach us), so no host
 * or port is hardcoded. Kept separate from {@link NotificationServiceClient}: this is the
 * raw transport, while NotificationServiceClient is the async, failure-safe port that the
 * service layer depends on.
 */
@FeignClient(name = "notification-service")
public interface NotificationFeignClient {

    @PostMapping("/api/v1/notifications/email")
    void sendEmail(@RequestBody EmailNotificationRequest request);
}
