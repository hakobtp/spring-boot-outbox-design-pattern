package com.hakobtp.blog.outbox.mapper;

import com.hakobtp.blog.outbox.dto.OutboxPayloadDto;
import com.hakobtp.blog.outbox.model.OutboxPayloadCapable;

import java.io.Serializable;

public interface OutboxPayloadMapper<T extends OutboxPayloadDto, E extends OutboxPayloadCapable<ID>, ID extends Serializable> {

    T toOutboxPayloadDto(E outboxPayloadCapable);
}
