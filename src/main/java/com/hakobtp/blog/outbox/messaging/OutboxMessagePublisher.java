package com.hakobtp.blog.outbox.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.hakobtp.blog.outbox.enums.OutboxStatus;

import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * An interface that defines a contract for publishing outbox event messages.
 * <p>
 * This acts as an abstraction over the specific message broker technology (e.g., Kafka, RabbitMQ).
 * Implementations are responsible for sending the event payload to the appropriate destination
 * and invoking a callback with the final status of the operation.
 */
public interface OutboxMessagePublisher {

    /**
     * Asynchronously publishes a message to a Kafka topic without custom headers.
     * <p>
     * This is a convenience method that delegates to the main publish method with a null headers map.
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
    default void publish(
            String configurationKey,
            UUID eventId,
            String aggregateId,
            JsonNode payload,
            Consumer<OutboxStatus> callback) {
        publish(configurationKey, eventId, aggregateId, payload, null, callback);
    }


    /**
     * Asynchronously publishes a message with custom headers to a Kafka topic.
     * <p>
     * The method sends the message and uses a {@code CompletableFuture} callback
     * ({@code whenComplete}) to handle the result. It does not block. Upon completion,
     * it invokes the provided callback function with either {@code OutboxStatus.COMPLETED}
     * or {@code OutboxStatus.FAILED}, allowing the caller to update the event's status
     * in a separate transaction. This version allows for custom headers to be added to the
     * Kafka record, which is useful for metadata like trace IDs.
     *
     * @param configurationKey A key used to look up the target Kafka topic name.
     * @param eventId          The unique ID of the event, used for logging.
     * @param aggregateId      The aggregate ID, used as the Kafka message key for partitioning.
     * @param payload          The JSON payload of the event.
     * @param customHeaders    A map of custom headers to add to the Kafka message. Can be {@code null} if no headers are needed.
     * @param callback         A {@link Consumer} function to be executed upon completion,
     *                         accepting the final {@link OutboxStatus}.
     */
    void publish(
            String configurationKey,
            UUID eventId,
            String aggregateId,
            JsonNode payload,
            Map<String, String> customHeaders,
            Consumer<OutboxStatus> callback);
}

