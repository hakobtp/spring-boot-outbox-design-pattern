CREATE SEQUENCE IF NOT EXISTS sq_outbox_events START WITH 1 INCREMENT BY 1;

CREATE TABLE outbox_events
(
    id                BIGINT PRIMARY KEY       NOT NULL DEFAULT nextval('sq_outbox_events'::regclass),
    event_id          UUID                     NOT NULL UNIQUE,
    aggregate_type    VARCHAR(128)             NOT NULL,
    aggregate_id      VARCHAR(255)             NOT NULL,
    event_type        VARCHAR(6)               NOT NULL CHECK (event_type IN ('INSERT', 'UPDATE', 'DELETE') ),
    payload           JSONB                    NOT NULL,
    status            VARCHAR(30)              NOT NULL DEFAULT 'NEW' CHECK ( status IN ('NEW', 'PROCESSING', 'COMPLETED', 'FAILED') ),
    attempt_count     INT                      NOT NULL DEFAULT 0,
    configuration_key VARCHAR(255),
    custom_headers    JSONB,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    created_by        VARCHAR(64)              NOT NULL DEFAULT 'system',
    modified_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    modified_by       VARCHAR(64)              NOT NULL DEFAULT 'system',
    processed_at      TIMESTAMP WITH TIME ZONE          DEFAULT NULL
);

CREATE INDEX IF NOT EXISTS idx_outbox_status ON outbox_events (status);
CREATE INDEX IF NOT EXISTS idx_outbox_aggregate_id ON outbox_events (aggregate_id);


