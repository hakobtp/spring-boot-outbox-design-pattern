package com.hakobtp.blog.common.config.kafka;


import java.util.Objects;

public record KafkaTopicInfo(String configurationKey, String topicName, String mapperName) {
    public KafkaTopicInfo {
        Objects.requireNonNull(configurationKey, "configurationKey must not be null");
        Objects.requireNonNull(topicName, "topicName must not be null");
        Objects.requireNonNull(topicName, "mapperName must not be null");
    }
}
