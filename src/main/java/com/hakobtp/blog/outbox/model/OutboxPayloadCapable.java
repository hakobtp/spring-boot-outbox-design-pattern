package com.hakobtp.blog.outbox.model;

import java.io.Serializable;
import java.util.Objects;

public interface OutboxPayloadCapable<ID extends Serializable> {

    ID getId();

    default String getAggregateId() {
        var id = getId();
        Objects.requireNonNull(id, "id is null");
        return id.toString();
    }
}
