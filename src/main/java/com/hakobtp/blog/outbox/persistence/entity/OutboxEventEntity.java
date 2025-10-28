package com.hakobtp.blog.outbox.persistence.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.hakobtp.blog.common.persistence.AbstractModificationInfoBaseEntity;
import com.hakobtp.blog.outbox.enums.OutboxEventType;
import com.hakobtp.blog.outbox.enums.OutboxStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;
import java.util.UUID;

/**
 * Represents a single event stored in the outbox table for reliable, asynchronous processing.
 * <p>
 * This entity captures all the necessary information to process an event that occurred
 * within a domain transaction. It ensures that the event is eventually published to an
 * external system (like a message broker) even if failures occur, following the
 * Transactional Outbox Pattern.
 */
@Getter
@Setter
@Entity
@Accessors(chain = true)
@Table(name = "outbox_events")
public class OutboxEventEntity extends AbstractModificationInfoBaseEntity {

    /**
     * The unique primary key for the database record.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "outbox_events_generator")
    @SequenceGenerator(name = "outbox_events_generator", sequenceName = "sq_outbox_events", allocationSize = 1)
    private Long id;

    /**
     * A universally unique identifier (UUID) for the event itself.
     * This ensures that each event has a unique business ID, which can be used for
     * idempotency checks by downstream consumers. It is set automatically before persistence.
     */
    @Setter(AccessLevel.NONE)
    @Column(name = "event_id", nullable = false, unique = true, updatable = false)
    private UUID eventId;

    /**
     * The name of the domain entity (aggregate root) this event is related to.
     * For example, for an event about an 'Order' entity, this field would be "ORDER".
     */
    @Column(name = "aggregate_type", nullable = false)
    private String aggregateType;

    /**
     * The unique identifier of the specific entity instance this event pertains to.
     * For example, the ID of the specific order that was created or updated.
     */
    @Column(name = "aggregate_id", nullable = false)
    private String aggregateId;

    /**
     * The specific type of operation that triggered the event (e.g., CREATED, UPDATED, DELETED).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 10) // Adjusted length for clarity
    private OutboxEventType eventType;

    /**
     * The actual data of the event, stored as a JSON object.
     * This payload contains the details needed by consumers to react to the event.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private JsonNode payload;

    /**
     * The current processing status of the event (e.g., NEW, PROCESSING, COMPLETED, FAILED).
     * This is used by the event processor to track the lifecycle of the outbox message.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private OutboxStatus status = OutboxStatus.NEW;

    /**
     * A counter for the number of times processing has been attempted for this event.
     * This helps in implementing retry strategies like exponential backoff or a max retry limit.
     */
    @Column(name = "attempt_count", nullable = false)
    private int attemptCount = 0;

    /**
     * An optional key to specify a particular configuration or processor for this event.
     * This can be used to route the event to a specific message topic or handler.
     */
    @Column(name = "configuration_key")
    private String configurationKey;

    /**
     * Custom headers for the event, often used to pass metadata to message brokers
     * (e.g., Kafka headers for tracing, tenancy, or routing).
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "custom_headers", columnDefinition = "jsonb")
    private Map<String, String> customHeaders;

    /**
     * JPA lifecycle callback method that runs before the entity is first persisted.
     * It ensures that the {@code eventId} is initialized with a new UUID if it hasn't been set.
     */
    @PrePersist
    public void prePersist() {
        if (eventId == null) {
            eventId = UUID.randomUUID();
        }
    }
}
