package com.hakobtp.blog.outbox.service;

import com.hakobtp.blog.outbox.enums.OutboxStatus;
import com.hakobtp.blog.outbox.messaging.OutboxMessagePublisher;
import com.hakobtp.blog.outbox.persistence.entity.OutboxEventEntity;
import com.hakobtp.blog.outbox.persistence.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static com.hakobtp.blog.outbox.enums.OutboxStatus.COMPLETED;
import static com.hakobtp.blog.outbox.enums.OutboxStatus.PROCESSING;

/**
 * Core service for the Transactional Outbox pattern.
 * <p>
 * This class periodically polls the outbox table for new events, publishes them to a message broker,
 * and updates their status accordingly. It is designed to be resilient, with self-healing mechanisms
 * to handle application failures gracefully. It uses Spring Retry to handle transient database errors
 * during asynchronous status updates.
 *
 * @see com.hakobtp.blog.outbox.scheduler.OutboxScheduler for the scheduling mechanism that invokes methods in this class.
 */

@Slf4j
@Service
@RequiredArgsConstructor
class OutboxRelayServiceImpl implements OutboxRelayService {

    /**
     * The maximum number of times to retry the asynchronous status update upon failure.
     */
    private static final int MAX_CALLBACK_RETRIES = 4;

    @Value("${outbox.batch-size:50}")
    private int batchSize;

    @Value("${outbox.reaper.stuck-timeout-ms:300000}")
    private long reaperIntervalMs;

    private final OutboxRepository outboxRepository;
    private final OutboxMessagePublisher messagePublisher;

    /**
     * Polls for and relays a batch of outbox events using an "all or nothing" transactional approach.
     * <p>
     * This method performs the following steps within a single transaction:
     * 1. Fetches a batch of 'NEW' or 'FAILED' events using a pessimistic lock to prevent concurrent processing.
     * 2. Marks each event as 'PROCESSING' and increments its attempt count.
     * 3. Hands off the events to an asynchronous message publisher.
     * <p>
     * If any synchronous error occurs, the entire transaction is rolled back, leaving the events
     * in their original state to be retried on the next polling cycle.
     */
    @Override
    @Transactional
    public void relayBatch() {
        List<OutboxEventEntity> events = outboxRepository.findAndLockNewEvents(batchSize);
        if (events.isEmpty()) {
            return; // No work to do
        }

        log.info("Processing {} outbox events.", events.size());
        for (OutboxEventEntity event : events) {
            event.setStatus(PROCESSING);
            event.setAttemptCount(event.getAttemptCount() + 1);

            messagePublisher.publish(
                    event.getConfigurationKey(),
                    event.getEventId(),
                    event.getAggregateId(),
                    event.getPayload(),
                    status -> updateEventStatusWithRetry(event.getId(), status)
            );
        }
    }

    /**
     * Updates an event's status in a new, independent transaction.
     * <p>
     * This method is designed to be called from an asynchronous callback (e.g., after a message is published).
     * It is annotated with {@code @Retryable} to automatically handle transient database errors like connection
     * issues or deadlocks. If a {@link DataAccessException} is thrown, Spring Retry will automatically
     * re-invoke this method with an exponential backoff delay.
     *
     * @param eventId The ID of the event to update.
     * @param status  The final status from the publisher (e.g., COMPLETED or FAILED).
     * @see #recoverUpdateStatus(DataAccessException, Long, OutboxStatus) for the recovery handler.
     */
    @Retryable(
            retryFor = {DataAccessException.class},
            maxAttempts = MAX_CALLBACK_RETRIES,
            backoff = @Backoff(delay = 50, multiplier = 2) // Wait 50ms, then 100ms
    )
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateEventStatusWithRetry(Long eventId, OutboxStatus status) {
        outboxRepository.findById(eventId)
                .map(event -> {
                    event.setStatus(status);
                    if (status == COMPLETED) {
                        event.setProcessedAt(OffsetDateTime.now());
                    }
                    log.info("Outbox event {} updated to {}", eventId, status);
                    return event;
                }).ifPresent(outboxRepository::save);
    }

    /**
     * A recovery method called by Spring Retry if {@link #updateEventStatusWithRetry} fails all its attempts.
     * <p>
     * This method acts as a last resort, logging a critical error. The event's status is left unchanged,
     * allowing the "reaper" job to eventually find and reset it if it's stuck in the 'PROCESSING' state.
     *
     * @param exception The final exception that caused the failure.
     * @param eventId   The ID of the event that failed to be updated.
     * @param status    The status that could not be applied.
     */
    @Recover
    public void recoverUpdateStatus(DataAccessException exception, Long eventId, OutboxStatus status) {
        log.error("Final attempt failed for outbox event {}. Status remains unchanged. "
                + "The reaper job will handle cleanup.", eventId, exception);
    }

    /**
     * A scheduled "reaper" job to find and reset events stuck in the 'PROCESSING' state.
     * <p>
     * This is a critical self-healing mechanism. If the application crashes after {@link #relayBatch()}
     * commits but before the asynchronous callback completes, events can be left in 'PROCESSING' indefinitely.
     * This job runs periodically to find such events (based on a timeout) and reset their status to 'FAILED'
     * so they can be re-processed in a future poll.
     */
    @Override
    @Transactional
    public void reapStuckEvents() {
        log.debug("Running reaper job to find stuck outbox events...");
        var stuckTime = OffsetDateTime.now().minus(reaperIntervalMs, ChronoUnit.MILLIS);
        int resetCount = outboxRepository.resetStuckEvents(stuckTime);
        if (resetCount > 0) {
            log.warn("Reset {} stuck outbox events from PROCESSING to FAILED.", resetCount);
        }
    }
}