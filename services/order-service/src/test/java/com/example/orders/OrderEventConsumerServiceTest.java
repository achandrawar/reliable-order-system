package com.example.orders;

import com.example.orders.consumer.OrderEventConsumerService;
import com.example.orders.consumer.ProcessedEventRepository;
import com.example.orders.repository.OrderRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class OrderEventConsumerServiceTest {

    @Test
    void shouldReturnTrueWhenEventIsRegistered() {

        ProcessedEventRepository processedEventRepository =
                mock(ProcessedEventRepository.class);

        OrderRepository orderRepository = mock(OrderRepository.class);

        when(processedEventRepository.insertIfNotExists(
                "event-123", "ORDER_CREATED"
        )).thenReturn(1);

        when(orderRepository.updateStatus(
                101L,
                com.example.orders.entity.OrderStatus.PENDING,
                com.example.orders.entity.OrderStatus.CONFIRMED
        )).thenReturn(1);

        OrderEventConsumerService service =
                new OrderEventConsumerService(
                        processedEventRepository,
                        orderRepository
                );

        boolean result = service.processEvent(
                "event-123", "ORDER_CREATED", 101L
        );

        assertTrue(result);

        verify(processedEventRepository)
                .insertIfNotExists("event-123", "ORDER_CREATED");

        verify(orderRepository).updateStatus(
                101L,
                com.example.orders.entity.OrderStatus.PENDING,
                com.example.orders.entity.OrderStatus.CONFIRMED
        );
    }

    @Test
    void shouldReturnFalseWhenEventAlreadyExists() {

        ProcessedEventRepository processedEventRepository =
                mock(ProcessedEventRepository.class);

        OrderRepository orderRepository = mock(OrderRepository.class);

        when(processedEventRepository.insertIfNotExists(
                "event-123", "ORDER_CREATED"
        )).thenReturn(0);

        OrderEventConsumerService service =
                new OrderEventConsumerService(
                        processedEventRepository,
                        orderRepository
                );

        boolean result = service.processEvent(
                "event-123", "ORDER_CREATED", 101L
        );

        assertFalse(result);

        verify(processedEventRepository)
                .insertIfNotExists("event-123", "ORDER_CREATED");

        verifyNoInteractions(orderRepository);
    }
}