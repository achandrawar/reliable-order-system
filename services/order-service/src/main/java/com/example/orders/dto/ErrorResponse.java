package com.example.orders.dto;

import java.time.Instant;
import java.util.Map;

public record ErrorResponse(Instant timeStamp,
                            int status,
                            String error,
                            String message,
                            Map<String, String> fieldErrors) {
}
