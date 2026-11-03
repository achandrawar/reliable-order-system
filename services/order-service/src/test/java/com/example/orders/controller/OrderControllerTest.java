package com.example.orders.controller;

import com.example.orders.dto.CreateOrderRequest;
import com.example.orders.dto.OrderResponse;
import com.example.orders.exception.OrderNotFoundException;
import com.example.orders.service.OrderService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @Test
    void createOrder_shouldReturn201() throws Exception {
        when(orderService.createOrder(any(CreateOrderRequest.class)))
                .thenReturn(new OrderResponse(
                        1L,
                        "LAPTOP-001",
                        2,
                        "PENDING",
                        Instant.parse("2026-10-18T00:00:00Z")
                ));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "productId": "LAPTOP-001",
                                    "quantity": 2
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.productId").value("LAPTOP-001"))
                .andExpect(jsonPath("$.quantity").value(2))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void getOrderById_shouldReturn200() throws Exception {
        when(orderService.getOrderById(1L))
                .thenReturn(new OrderResponse(
                        1L,
                        "LAPTOP-001",
                        2,
                        "PENDING",
                        Instant.parse("2026-10-18T00:00:00Z")
                ));

        mockMvc.perform(get("/api/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.productId").value("LAPTOP-001"))
                .andExpect(jsonPath("$.quantity").value(2))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void createOrder_shouldReturn400ForInvalidRequest() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "productId": "",
                                "quantity": 0
                            }
                            """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.productId")
                        .value("Product ID is required"))
                .andExpect(jsonPath("$.fieldErrors.quantity")
                        .value("Quantity must be at least 1"));
    }

    @Test
    void getOrderById_shouldReturn404WhenOrderNotFound() throws Exception {
        when(orderService.getOrderById(99999L))
                .thenThrow(new OrderNotFoundException(99999L));

        mockMvc.perform(get("/api/orders/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message")
                        .value("Order not found with ID: 99999"));
    }
}