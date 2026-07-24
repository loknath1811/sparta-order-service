package com.training.orderservice.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String topic;

    public OrderEventPublisher(KafkaTemplate<String, Object> kafkaTemplate,
                                @Value("${kafka.topic.order-events}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void publish(OrderEvent event) {
        // Keyed by orderId so all events for the same order land on the same partition
        // and are consumed in order.
        kafkaTemplate.send(topic, String.valueOf(event.orderId()), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.warn("Failed to publish OrderEvent for order {} (status={}): {}",
                                event.orderId(), event.status(), ex.getMessage());
                    } else {
                        log.debug("Published OrderEvent for order {} (status={}) to partition {}",
                                event.orderId(), event.status(), result.getRecordMetadata().partition());
                    }
                });
    }
}
