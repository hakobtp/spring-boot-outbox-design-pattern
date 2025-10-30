package com.hakobtp.blog.outbox.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.hakobtp.blog.outbox.enums.OutboxStatus;

import java.util.UUID;
import java.util.function.Consumer;

public interface OutboxMessagePublisher {

    void publish(
            String configurationKey,
            UUID eventId,
            String aggregateId,
            JsonNode payload,
            Consumer<OutboxStatus> callback);
}

