package com.example.url_shortener.billing;

import com.example.url_shortener.domain.ProcessedEvent;
import com.example.url_shortener.domain.UsageDaily;
import com.example.url_shortener.domain.UsageDailyId;
import com.example.url_shortener.repository.ProcessedEventRepository;
import com.example.url_shortener.repository.UsageDailyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;


@Component
public class VisitEventConsumer {
    private static final Logger LOG = LoggerFactory.getLogger(VisitEventConsumer.class);

    private final UsageDailyRepository usageDailyRepository;
    private final ProcessedEventRepository processedEventRepository;

    public VisitEventConsumer(UsageDailyRepository usageDailyRepository,
                              ProcessedEventRepository processedEventRepository) {
        this.usageDailyRepository = usageDailyRepository;
        this.processedEventRepository = processedEventRepository;
    }

    @KafkaListener(topics= VisitEventProducer.TOPIC, groupId = "billing-aggregator")
    @Transactional
    public void consume(VisitEvent event) {

        // 1. Idempotency check: have we already counted the exact visit?
        if (processedEventRepository.existsById(event.visitId())){
            LOG.debug("Duplicate visitId {} found - Skipping", event.visitId());
            return;  // redelivery -- no ops
        }

        // 2. Bucket the visit into its day , then increment the (shortCode, day) counter.

        LocalDate day = event.timestamp().atZone(ZoneOffset.UTC).toLocalDate();
        UsageDailyId id = new UsageDailyId(event.visitId(), day);

        UsageDaily usage = usageDailyRepository.findById(id)
                .orElseGet(() -> new UsageDaily(id, 0));
        usage.setClientId(event.clientId());     // <-- attribute to the owner
        usage.addVisits(1);
        usageDailyRepository.save(usage);

        // 3. Record this visitId as processed(same transaction as the increment)
        processedEventRepository.save(new ProcessedEvent(event.visitId()));

        LOG.debug("Counted visit {} for {} on {}", event.visitId(), event.shortCode(), day);
    }

}

