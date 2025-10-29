DELETE FROM outbox_events;
DELETE FROM orders;

ALTER SEQUENCE sq_orders RESTART;
ALTER SEQUENCE sq_outbox_events RESTART;