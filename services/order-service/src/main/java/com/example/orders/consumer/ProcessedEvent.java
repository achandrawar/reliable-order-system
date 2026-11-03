package com.example.orders.consumer;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.Instant;

@Getter
@Entity
@Table(name = "processed_events")
public class ProcessedEvent {
    @Id
    @Column(nullable = false, updatable = false)
    private String eventId;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false, updatable = false)
    private Instant processedAt;
     protected  ProcessedEvent() {}

    public ProcessedEvent(String eventId, String eventType) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.processedAt = Instant.now();
    }
}
