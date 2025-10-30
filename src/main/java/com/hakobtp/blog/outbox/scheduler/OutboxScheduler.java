package com.hakobtp.blog.outbox.scheduler;

import com.hakobtp.blog.outbox.service.OutboxRelayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxScheduler {

    private final OutboxRelayService relayService;

    @Scheduled(fixedDelayString = "${outbox.poll-interval-ms:5000}")
    public void processOutbox() {
        try {
            relayService.relayBatch();
        } catch (Exception e) {
            log.error("Outbox relay failed with an unexpected exception", e);
        }
    }

    @Scheduled(fixedDelayString = "${outbox.reaper.run-interval-ms:60000}")
    public void reapStuckEvents() {
        relayService.reapStuckEvents();
    }
}
