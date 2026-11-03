package com.example.orders.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;

import java.time.Instant;

@Entity
@Table(name = "orders")
@Data
@Getter
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String productId;

    @Column(nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    // Required by JPA when loading entities from the database
    protected Order() {
    }

    public Order(String productId, Integer quantity) {
        this.productId = productId;
        this.quantity = quantity;
        this.status = OrderStatus.PENDING;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    public void updateStatus(OrderStatus status) {
        this.status = status;
    }

    public void confirm() {
        if (this.status != OrderStatus.PENDING) {
            throw new IllegalStateException(
                    "Only pending orders can be confirmed"
            );
        }

        this.status = OrderStatus.CONFIRMED;
    }

    public void cancel() {
        if (this.status != OrderStatus.PENDING) {
            throw new IllegalStateException(
                    "Only pending orders can be cancelled"
            );
        }

        this.status = OrderStatus.CANCELLED;
    }

    public void markFailed() {
        if (this.status != OrderStatus.PENDING) {
            throw new IllegalStateException(
                    "Only pending orders can be marked failed"
            );
        }

        this.status = OrderStatus.FAILED;
    }
}