package com.training.orderservice.client.impl;

import com.training.orderservice.client.NotificationFeignClient;
import com.training.orderservice.client.dto.EmailNotificationRequest;
import com.training.orderservice.client.dto.OrderConfirmationRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationServiceRestClientTest {

    @Mock
    private NotificationFeignClient notificationFeignClient;

    @InjectMocks
    private NotificationServiceRestClient client;

    @Captor
    private ArgumentCaptor<EmailNotificationRequest> emailCaptor;

    private OrderConfirmationRequest sampleOrder() {
        return new OrderConfirmationRequest(
                123L, 101L, "Jane Doe", "jane@example.com", new BigDecimal("50.00"),
                List.of(new OrderConfirmationRequest.Item("Wireless Mouse", 2)),
                LocalDateTime.now());
    }

    @Test
    void sendOrderConfirmation_flattensOrderIntoEmail() {
        client.sendOrderConfirmation(sampleOrder());

        verify(notificationFeignClient).sendEmail(emailCaptor.capture());
        EmailNotificationRequest sent = emailCaptor.getValue();

        assertThat(sent.recipient()).isEqualTo("jane@example.com");
        assertThat(sent.subject()).isEqualTo("Order #123 confirmed");
        assertThat(sent.message())
                .contains("Jane Doe")
                .contains("2 x Wireless Mouse")
                .contains("50.00");
    }

    @Test
    void sendOrderConfirmation_swallowsFailureSoOrderIsNeverAffected() {
        doThrow(new RuntimeException("notification-service down"))
                .when(notificationFeignClient).sendEmail(org.mockito.ArgumentMatchers.any());

        // Must not propagate — the order is already confirmed.
        assertThatCode(() -> client.sendOrderConfirmation(sampleOrder())).doesNotThrowAnyException();
    }
}
