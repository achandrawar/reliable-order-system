package com.example.orders.consumer;

import com.example.orders.entity.OrderStatus;
import com.example.orders.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderEventConsumerService {
    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumerService.class);

    private final ProcessedEventRepository processedEventRepository;
    private final OrderRepository orderRepository;

    public OrderEventConsumerService(ProcessedEventRepository processedEventRepository, OrderRepository orderRepository) {
        this.processedEventRepository = processedEventRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional
    public boolean processEvent(String eventId, String eventType, Long orderId) {
        int inserted = processedEventRepository.insertIfNotExists(
                eventId, eventType
        );

        if (inserted == 0) {
            log.info("Duplicate event ignored: {}", eventId);
            return false;
        }

        if (!"ORDER_CREATED".equals(eventType)) {
            throw new IllegalArgumentException("Unsupported event type: " + eventType);
        }

        int updated = orderRepository.updateStatus(orderId, OrderStatus.PENDING, OrderStatus.CONFIRMED);

        if (updated == 0) {
            throw new IllegalStateException("Order does not exist or is not in PENDING status: " + orderId);
        }

        log.info("Order {} confirmed successfully for event {}", orderId, eventId);
        return true;
    }
}