
package com.example.orders.dto;

import java.time.Instant;

public record OrderResponse(
        Long id,
        String productId,
        Integer quantity,
        String status,
        Instant createdAt
) {
}
