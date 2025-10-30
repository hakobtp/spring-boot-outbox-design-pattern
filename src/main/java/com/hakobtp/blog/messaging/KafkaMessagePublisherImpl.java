package com.hakobtp.blog.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.hakobtp.blog.common.config.kafka.KafkaInfo;
import com.hakobtp.blog.common.config.kafka.KafkaTopicInfo;
import com.hakobtp.blog.outbox.enums.OutboxStatus;
import com.hakobtp.blog.outbox.messaging.OutboxMessagePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * A Kafka-specific implementation of the {@link OutboxMessagePublisher}.
 * This class is responsible for sending event payloads to the appropriate Kafka topics.
 */

@Slf4j
@Component
@RequiredArgsConstructor
class KafkaMessagePublisherImpl implements OutboxMessagePublisher {

    private final KafkaInfo kafkaInfo;
    private final KafkaTemplate<String, Object> kafkaTemplate;


    @Override
    public void publish(
            String configurationKey,
            UUID eventId,
            String aggregateId,
            JsonNode payload,
            Map<String, String> customHeaders,
            Consumer<OutboxStatus> callback
    ) {
        var topic = kafkaInfo.findByConfigurationKey(configurationKey)
                .map(KafkaTopicInfo::topicName)
                .orElseThrow(() -> new IllegalArgumentException("Unknown topic name: " + configurationKey));

        ProducerRecord<String, Object> producerRecord = new ProducerRecord<>(topic, aggregateId, payload);

        producerRecord.headers().add(createRecordHeader("eventId", eventId.toString()));
        createRecordHeader(customHeaders).forEach(c -> producerRecord.headers().add(c));

        kafkaTemplate.send(producerRecord)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Kafka send failed for event {}", eventId, ex);
                        callback.accept(OutboxStatus.FAILED);
                    } else {
                        callback.accept(OutboxStatus.COMPLETED);
                    }
                });
    }

    private List<RecordHeader> createRecordHeader(Map<String, String> headerMap) {
        return Objects.requireNonNullElseGet(headerMap, Map::<String, String>of).entrySet().stream()
                .map(entry -> createRecordHeader(entry.getKey(), entry.getValue()))
                .toList();
    }

    private RecordHeader createRecordHeader(String key, String value) {
        byte[] valueBytes = value.getBytes(StandardCharsets.UTF_8);
        return new RecordHeader(key, valueBytes);
    }
}

