package com.example.orders.event;

import java.time.Instant;

public record OrderCreatedEvent(String eventId,
                                String eventType,
                                Long orderId,
                                String productId,
                                Integer quantity,
                                Instant occurredAt) {
}
