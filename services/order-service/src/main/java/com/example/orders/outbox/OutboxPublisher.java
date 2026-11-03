package com.example.orders.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;import java.util.concurrent.TimeUnit;

@Component
public class OutboxPublisher {
    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private static final String TOPIC = "order-events";

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxPublisher(OutboxEventRepository outboxEventRepository,
                           KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelay = 5000)
    public void publishPendingEvents() {
        List<OutboxEvent> events = outboxEventRepository.findByStatusOrderByIdAsc("PENDING");

        for(OutboxEvent event:events) {
            try {
                kafkaTemplate.send(TOPIC, event.getAggregateId(), event.getPayload()).get(10, TimeUnit.SECONDS);
                event.markPublished();
                outboxEventRepository.save(event);

                log.info(
                        "Published outbox event id={} orderId={}",
                        event.getId(),
                        event.getAggregateId()
                );
            } catch (Exception exception) {
                log.error(
                        "Failed to publish outbox event id={}; it remains pending",
                        event.getId(),
                        exception
                );
            }
        }
    }
}
