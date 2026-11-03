package com.example.orders.consumer;

import com.example.orders.event.OrderCreatedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderEventKafkaListener {
    private static final Logger log = LoggerFactory.getLogger(OrderEventKafkaListener.class);

    private final ObjectMapper objectMapper;
    private final OrderEventConsumerService consumerService;

    public OrderEventKafkaListener(ObjectMapper objectMapper, OrderEventConsumerService consumerService) {
        this.objectMapper = objectMapper;
        this.consumerService = consumerService;
    }

    @KafkaListener(
            topics = "order-events",
            groupId = "order-service-consumer"
    )
    public void consume(String message) {
        try {
            OrderCreatedEvent event = objectMapper.readValue(message, OrderCreatedEvent.class);

            if (event.eventId() == null || event.eventId().isBlank()) {
                throw new IllegalArgumentException("Event ID is missing");
            }

            if (event.orderId() == null) {
                throw new IllegalArgumentException("Order ID is missing");
            }

            consumerService.processEvent(event.eventId(), event.eventType(), event.orderId());

        } catch (Exception exception) {
            log.error("Failed to consume order event: {}", message, exception);
            throw new RuntimeException(
                    "Order event processing failed", exception
            );
        }
    }
}