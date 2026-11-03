
package com.example.orders.service;

import com.example.orders.dto.CreateOrderRequest;
import com.example.orders.dto.OrderResponse;
import com.example.orders.repository.OrderRepository;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
class OrderServiceIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16")
                    .withDatabaseName("order_test")
                    .withUsername("test_user")
                    .withPassword("test_password");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @BeforeAll
    static void setTestTimeZone() {
        java.util.TimeZone.setDefault(
                java.util.TimeZone.getTimeZone("Asia/Kolkata")
        );
    }

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Test
    void createAndRetrieveOrder_shouldPersistInPostgres() {
        CreateOrderRequest request =
                new CreateOrderRequest("LAPTOP-001", 2);

        OrderResponse created = orderService.createOrder(request);

        assertNotNull(created.id());
        assertEquals("LAPTOP-001", created.productId());
        assertEquals(2, created.quantity());
        assertEquals("PENDING", created.status());

        OrderResponse retrieved =
                orderService.getOrderById(created.id());

        assertEquals(created.id(), retrieved.id());
        assertEquals("LAPTOP-001", retrieved.productId());
        assertEquals(2, retrieved.quantity());
        assertEquals("PENDING", retrieved.status());

        assertTrue(orderRepository.existsById(created.id()));
    }


}
