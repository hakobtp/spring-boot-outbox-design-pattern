package com.hakobtp.blog.outbox.model;

import com.hakobtp.blog.outbox.enums.OutboxEventType;

import java.io.Serializable;

public record OutboxEvent(
        OutboxEventType eventType,
        OutboxPayloadCapable<? extends Serializable> payload
) {
}
