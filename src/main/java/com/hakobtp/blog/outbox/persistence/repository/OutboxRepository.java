package com.hakobtp.blog.outbox.persistence.repository;

import com.hakobtp.blog.outbox.enums.OutboxEventType;
import com.hakobtp.blog.outbox.persistence.entity.OutboxEventEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface OutboxRepository extends JpaRepository<OutboxEventEntity, Long> {

    @Transactional(readOnly = true)
    Page<OutboxEventEntity> findAllByEventType(OutboxEventType eventType, Pageable pageable);

    /**
     * Finds a batch of unprocessed ('NEW' or 'FAILED') outbox events and applies a pessimistic lock.
     * <p>
     * This native query uses {@code FOR UPDATE SKIP LOCKED} to ensure that multiple instances
     * of the application can poll for events concurrently without processing the same batch.
     * When one instance selects and locks a set of rows, other instances will skip those
     * locked rows and select the next available ones. This is crucial for horizontal scalability.
     *
     * @param batchSize The maximum number of events to retrieve.
     * @return A list of {@link OutboxEventEntity} ready for processing.
     */
    @Query(value = """
            SELECT * FROM outbox_events
            WHERE status IN ('NEW', 'FAILED')
            ORDER BY created_at
            FOR UPDATE SKIP LOCKED
            LIMIT :batchSize
            """, nativeQuery = true)
    List<OutboxEventEntity> findAndLockNewEvents(@Param("batchSize") int batchSize);

    /**
     * Resets the status of events that have been in the 'PROCESSING' state for too long.
     * <p>
     * This "reaper" query is a self-healing mechanism. It handles cases where an application
     * instance crashes after marking an event as 'PROCESSING' but before completing its publication.
     * Such events would otherwise be stuck. This method finds them and resets their status to 'FAILED'
     * so they can be picked up again by the next relay batch.
     *
     * @param stuckTime A timestamp before which any 'PROCESSING' event is considered stuck.
     * @return The number of events whose status was reset.
     */
    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE OutboxEventEntity event
            SET event.status = 'FAILED'
            WHERE event.status = 'PROCESSING' AND  event.modifiedAt < :stuckTime
            """)
    int resetStuckEvents(@Param("stuckTime") OffsetDateTime stuckTime);
}
