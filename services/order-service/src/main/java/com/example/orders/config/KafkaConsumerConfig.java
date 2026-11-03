
package com.example.orders.config;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConsumerConfig {

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(
            KafkaTemplate<String, String> kafkaTemplate
    ) {

        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
                        (ConsumerRecord<?, ?> record, Exception exception) ->
                                new TopicPartition(
                                        record.topic() + ".DLQ",
                                        record.partition()
                                )
                );

        FixedBackOff backOff = new FixedBackOff(
                2000L,  // 2 seconds between retries
                3L      // 3 retries after initial attempt
        );

        return new DefaultErrorHandler(recoverer, backOff);
    }
}