package com.hakobtp.blog.common;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.kafka.annotation.KafkaListener;

import java.util.concurrent.CountDownLatch;

@Slf4j
@Getter
@TestComponent
public class TestKafkaConsumer {

    private JsonNode payload;
    private CountDownLatch latch = new CountDownLatch(1);

    @KafkaListener(
            topics = "order.events.v1",
            groupId = "test-consumer-group",
            autoStartup = "true"
    )
    public void receive(JsonNode message) {
        log.info("Test consumer received message: {}", message.toString());
        this.payload = message;
        latch.countDown();
    }

    public void resetLatch() {
        latch = new CountDownLatch(1);
    }
}