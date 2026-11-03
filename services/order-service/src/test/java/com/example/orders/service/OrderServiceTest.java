
package com.example.orders.service;

import com.example.orders.consumer.OrderEventConsumerService;
import com.example.orders.consumer.ProcessedEventRepository;
import com.example.orders.dto.OrderResponse;
import com.example.orders.entity.Order;
import com.example.orders.entity.OrderStatus;
import com.example.orders.exception.OrderNotFoundException;
import com.example.orders.outbox.OutboxEventRepository;
import com.example.orders.repository.OrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    private OrderService orderService;
    private OrderEventConsumerService orderEventConsumerService;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();

        orderService = new OrderService(
                orderRepository,
                outboxEventRepository,
                objectMapper
        );

        orderEventConsumerService = new OrderEventConsumerService(
                processedEventRepository,
                orderRepository
        );
    }

    @Test
    void getOrderById_shouldReturnOrderWhenFound() {

        Order order = new Order("LAPTOP-001", 2);

        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(order));

        OrderResponse response = orderService.getOrderById(1L);

        assertEquals("LAPTOP-001", response.productId());
        assertEquals(2, response.quantity());
        assertEquals("PENDING", response.status());

        verify(orderRepository).findById(1L);
    }

    @Test
    void getOrderById_shouldThrowExceptionWhenNotFound() {

        when(orderRepository.findById(99999L))
                .thenReturn(Optional.empty());

        assertThrows(
                OrderNotFoundException.class,
                () -> orderService.getOrderById(99999L)
        );

        verify(orderRepository).findById(99999L);
    }

    @Test
    void shouldProcessNewEventSuccessfully() {

        String eventId = "test-event-123";
        Long orderId = 101L;

        when(processedEventRepository.insertIfNotExists(
                eventId, "ORDER_CREATED"
        )).thenReturn(1);

        when(orderRepository.updateStatus(
                orderId,
                OrderStatus.PENDING,
                OrderStatus.CONFIRMED
        )).thenReturn(1);

        boolean result = orderEventConsumerService.processEvent(
                eventId, "ORDER_CREATED", orderId
        );

        assertTrue(result);

        verify(processedEventRepository)
                .insertIfNotExists(eventId, "ORDER_CREATED");

        verify(orderRepository).updateStatus(
                orderId,
                OrderStatus.PENDING,
                OrderStatus.CONFIRMED
        );
    }

    @Test
    void shouldIgnoreDuplicateEvent() {

        String eventId = "test-event-123";
        Long orderId = 101L;

        when(processedEventRepository.insertIfNotExists(
                eventId, "ORDER_CREATED"
        )).thenReturn(0);

        boolean result = orderEventConsumerService.processEvent(
                eventId, "ORDER_CREATED", orderId
        );

        assertFalse(result);

        verify(processedEventRepository)
                .insertIfNotExists(eventId, "ORDER_CREATED");

        verifyNoInteractions(orderRepository);
    }
}
