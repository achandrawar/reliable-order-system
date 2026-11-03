package com.example.orders.service;

import com.example.orders.dto.CreateOrderRequest;
import com.example.orders.dto.OrderResponse;
import com.example.orders.entity.Order;
import com.example.orders.event.OrderCreatedEvent;
import com.example.orders.exception.OrderNotFoundException;
import com.example.orders.outbox.OutboxEvent;
import com.example.orders.outbox.OutboxEventRepository;
import com.example.orders.repository.OrderRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.time.Instant;import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public OrderService(OrderRepository orderRepository, OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        Order order = new Order(request.productId(), request.quantity());
        Order savedOrder = orderRepository.save(order);
        OrderCreatedEvent event = new OrderCreatedEvent(UUID.randomUUID().toString(),
                "ORDER_CREATED",
                savedOrder.getId(),
                savedOrder.getProductId(),
                savedOrder.getQuantity(),
                Instant.now());
        try {
            String payload = objectMapper.writeValueAsString(event);

            OutboxEvent outboxEvent = new OutboxEvent(
                    event.eventType(),
                    savedOrder.getId().toString(),
                    payload
            );
            outboxEventRepository.save(outboxEvent);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Failed to serialize order-created event",
                    exception
            );
        }
        return toResponse(savedOrder);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        return toResponse(order);
    }

    private OrderResponse toResponse(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getProductId(),
                order.getQuantity(),
                order.getStatus().name(),
                order.getCreatedAt()
        );
    }
}