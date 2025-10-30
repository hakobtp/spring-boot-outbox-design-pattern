package com.hakobtp.blog.outbox.service;

import com.hakobtp.blog.outbox.enums.OutboxStatus;

public interface OutboxRelayService {
    void relayBatch();

    void reapStuckEvents();

    void updateEventStatusWithRetry(Long eventId, OutboxStatus status);
}
