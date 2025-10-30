package com.hakobtp.blog.outbox.service;

import com.hakobtp.blog.common.persistence.AbstractModificationInfoBaseEntity;
import com.hakobtp.blog.outbox.enums.OutboxEventType;
import com.hakobtp.blog.outbox.enums.OutboxStatus;
import com.hakobtp.blog.outbox.model.OutboxEvent;
import com.hakobtp.blog.outbox.model.OutboxPayloadCapable;
import com.hakobtp.blog.outbox.persistence.entity.OutboxEventEntity;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public interface OutboxService {

    <T extends AbstractModificationInfoBaseEntity> OutboxEventEntity save(
            @NotNull OutboxPayloadCapable<? extends Serializable> payload,
            @NotNull OutboxEventType eventType,
            @NotNull String configurationKey,
            @NotNull Map<String, String> customHeaders);

    default <T extends AbstractModificationInfoBaseEntity> OutboxEventEntity save(
            @NotNull OutboxPayloadCapable<? extends Serializable> payload,
            @NotNull OutboxEventType eventType,
            @NotNull String configurationKey) {
        return save(payload, eventType, configurationKey, Map.of());
    }

    void saveAll(List<OutboxEvent> events, String configurationKey);


    void delete(@NotNull OutboxEventEntity outboxEvent);

    void deleteById(@NotNull Long outboxEventId);

    OutboxEventEntity updateStatus(@NotNull Long outboxEventId, @NotNull OutboxStatus newStatus);
}
