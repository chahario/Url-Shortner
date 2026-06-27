package com.example.url_shortener.billing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class VisitEventProducer {

    private static final Logger log = LoggerFactory.getLogger(VisitEventProducer.class);
    public static final String TOPIC = "visits";

    private final KafkaTemplate<String, VisitEvent> kafkaTemplate;

    public VisitEventProducer(KafkaTemplate<String, VisitEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(VisitEvent event) {
        try {
            kafkaTemplate.send(TOPIC, event.shortCode(), event);
        } catch (Exception e) {
            log.warn("Failed to publish visit event for {}", event.shortCode(), e);
        }
    }
}