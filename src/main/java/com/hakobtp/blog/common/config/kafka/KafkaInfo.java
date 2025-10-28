package com.hakobtp.blog.common.config.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@ConfigurationProperties(prefix = "kafka")
public record KafkaInfo(Set<KafkaTopicInfo> topicInfos) {

    public KafkaInfo {
        Objects.requireNonNull(topicInfos, "kafkaTopicInfos must not be null");
    }

    public Optional<KafkaTopicInfo> findByConfigurationKey(String configurationKey) {
        if (configurationKey == null || configurationKey.isBlank()) {
            return Optional.empty();
        }

        return topicInfos.stream()
                .filter(info -> info.configurationKey().equals(configurationKey))
                .findFirst();
    }
}
