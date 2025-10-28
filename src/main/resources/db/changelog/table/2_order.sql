CREATE SEQUENCE IF NOT EXISTS sq_orders START WITH 1 INCREMENT BY 1;

CREATE TABLE orders
(
    id            BIGINT PRIMARY KEY       NOT NULL DEFAULT nextval('sq_orders'::regclass),
    order_number  VARCHAR(50)              NOT NULL UNIQUE,
    customer_name VARCHAR(100)             NOT NULL,
    amount        NUMERIC(12, 2)           NOT NULL,
    order_date    TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    created_by    VARCHAR(64)              NOT NULL DEFAULT 'system',
    modified_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    modified_by   VARCHAR(64)              NOT NULL DEFAULT 'system'
);
