package com.example.orders.repository;

import com.example.orders.entity.Order;
import com.example.orders.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Order o
            SET o.status = :newStatus
            WHERE o.id = :orderId
              AND o.status = :currentStatus
            """)
    int updateStatus(
            @Param("orderId") Long orderId,
            @Param("currentStatus") OrderStatus currentStatus,
            @Param("newStatus") OrderStatus newStatus
    );
}