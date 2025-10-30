package com.hakobtp.blog.common.testcontainer;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

public class KafkaContainerInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    // Use a singleton Kafka container instance
    private static final KafkaContainer KAFKA_CONTAINER =
            new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.0.1"))
                    .withReuse(true);

    static {
        // Start the container once for all tests
        KAFKA_CONTAINER.start();
    }

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        // Apply the Kafka bootstrap server property to the Spring environment
        TestPropertyValues.of(
                "spring.kafka.bootstrap-servers=" + KAFKA_CONTAINER.getBootstrapServers()
        ).applyTo(applicationContext.getEnvironment());
    }
}