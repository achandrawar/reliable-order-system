package com.example.orders;

import com.example.orders.consumer.OrderEventConsumerService;
import com.example.orders.consumer.ProcessedEventRepository;
import com.example.orders.entity.Order;
import com.example.orders.entity.OrderStatus;
import com.example.orders.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest

class OrderEventProcessingIntegrationTest {

    @Autowired
    private OrderEventConsumerService consumerService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @Test
    void shouldConfirmOrderWhenNewEventIsReceived() {

        Order order = orderRepository.save(
                new Order("PRODUCT-101", 2)
        );

        String eventId = "test-event-101";

        boolean result = consumerService.processEvent(
                eventId,
                "ORDER_CREATED",
                order.getId()
        );

        assertTrue(result);

        Order updatedOrder = orderRepository.findById(order.getId())
                .orElseThrow();

        assertEquals(OrderStatus.CONFIRMED, updatedOrder.getStatus());
        assertTrue(processedEventRepository.existsById(eventId));
    }

    @Test
    void shouldIgnoreDuplicateEvent() {

        Order order = orderRepository.save(
                new Order("PRODUCT-102", 1)
        );

        String eventId = "test-event-102";

        boolean first = consumerService.processEvent(
                eventId, "ORDER_CREATED", order.getId()
        );

        boolean second = consumerService.processEvent(
                eventId, "ORDER_CREATED", order.getId()
        );

        assertTrue(first);
        assertFalse(second);

        assertEquals(
                OrderStatus.CONFIRMED,
                orderRepository.findById(order.getId())
                        .orElseThrow()
                        .getStatus()
        );
    }

    @Test
    void shouldRollbackProcessedEventWhenOrderDoesNotExist() {
        String eventId = "test-event-invalid-order";
        Long invalidOrderId = 999999L;

        assertThrows(
                IllegalStateException.class,
                () -> consumerService.processEvent(
                        eventId,
                        "ORDER_CREATED",
                        invalidOrderId
                )
        );

        assertFalse(
                processedEventRepository.existsById(eventId),
                "Processed event should be rolled back when order update fails"
        );
    }
}