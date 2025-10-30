package com.hakobtp.blog.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.hakobtp.blog.common.config.kafka.KafkaInfo;
import com.hakobtp.blog.common.config.kafka.KafkaTopicInfo;
import com.hakobtp.blog.outbox.enums.OutboxStatus;
import com.hakobtp.blog.outbox.messaging.OutboxMessagePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

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

    /**
     * Asynchronously publishes a message to a Kafka topic.
     * <p>
     * The method sends the message and uses a {@code CompletableFuture} callback
     * ({@code whenComplete}) to handle the result. It does not block. Upon completion,
     * it invokes the provided callback function with either {@code OutboxStatus.COMPLETED}
     * or {@code OutboxStatus.FAILED}, allowing the caller to update the event's status
     * in a separate transaction.
     *
     * @param configurationKey A key used to look up the target Kafka topic name.
     * @param eventId          The unique ID of the event, used for logging.
     * @param aggregateId      The aggregate ID, used as the Kafka message key for partitioning.
     * @param payload          The JSON payload of the event.
     * @param callback         A {@link Consumer} function to be executed upon completion,
     *                         accepting the final {@link OutboxStatus}.
     */
    @Override
    public void publish(
            String configurationKey,
            UUID eventId,
            String aggregateId,
            JsonNode payload,
            Consumer<OutboxStatus> callback
    ) {
        var topic = kafkaInfo.findByConfigurationKey(configurationKey)
                .map(KafkaTopicInfo::topicName)
                .orElseThrow(() -> new IllegalArgumentException("Unknown topic name: " + configurationKey));

        kafkaTemplate.send(topic, aggregateId, payload)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Kafka send failed for event {}", eventId, ex);
                        callback.accept(OutboxStatus.FAILED);
                    } else {
                        callback.accept(OutboxStatus.COMPLETED);
                    }
                });
    }
}
