package com.hakobtp.blog.outbox.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hakobtp.blog.common.config.kafka.KafkaInfo;
import com.hakobtp.blog.common.config.kafka.KafkaTopicInfo;
import com.hakobtp.blog.common.exception.EntityWithGivenIdNotFoundException;
import com.hakobtp.blog.common.exception.OutboxConfigurationException;
import com.hakobtp.blog.common.persistence.AbstractModificationInfoBaseEntity;
import com.hakobtp.blog.outbox.dto.OutboxPayloadDto;
import com.hakobtp.blog.outbox.enums.OutboxEventType;
import com.hakobtp.blog.outbox.enums.OutboxStatus;
import com.hakobtp.blog.outbox.mapper.OutboxPayloadMapper;
import com.hakobtp.blog.outbox.model.OutboxEvent;
import com.hakobtp.blog.outbox.model.OutboxPayloadCapable;
import com.hakobtp.blog.outbox.persistence.entity.OutboxEventEntity;
import com.hakobtp.blog.outbox.persistence.repository.OutboxRepository;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
class OutboxServiceImpl implements OutboxService {

    private final KafkaInfo kafkaInfo;
    private final ObjectMapper objectMapper;
    private final OutboxRepository outboxRepository;
    private final Map<String,
            OutboxPayloadMapper<? extends OutboxPayloadDto,
                    ? extends OutboxPayloadCapable<?>,
                    ? extends Serializable>> outboxPayloadMapperMap;

    OutboxServiceImpl(
            KafkaInfo kafkaInfo,
            ObjectMapper objectMapper,
            OutboxRepository outboxRepository,
            List<OutboxPayloadMapper<
                    ? extends OutboxPayloadDto,
                    ? extends OutboxPayloadCapable<?>,
                    ? extends Serializable>> outboxPayloadMappers
    ) {
        this.kafkaInfo = kafkaInfo;
        this.objectMapper = objectMapper;
        this.outboxRepository = outboxRepository;
        this.outboxPayloadMapperMap = buildOutboxPayloadMapperMap(outboxPayloadMappers);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OutboxEventEntity> findAllByEventType(OutboxEventType eventType, Pageable pageable) {
        return outboxRepository.findAllByEventType(eventType, pageable);
    }

    @Override
    public <T extends AbstractModificationInfoBaseEntity> OutboxEventEntity save(
            OutboxPayloadCapable<? extends Serializable> payload,
            OutboxEventType eventType,
            String configurationKey,
            Map<String, String> customHeaders
    ) {
        var entity = createEntity(payload, eventType, configurationKey, customHeaders);
        return outboxRepository.save(entity);
    }

    @Override
    public void saveAll(List<OutboxEvent> events, String configurationKey) {
        final Map<String, String> customHeaders = Map.of();
        var entities = Objects.requireNonNullElseGet(events, List::<OutboxEvent>of).stream()
                .map(event -> createEntity(event.payload(), event.eventType(), configurationKey, customHeaders))
                .toList();
        outboxRepository.saveAll(entities);
    }

    @Override
    public void delete(@NotNull OutboxEventEntity outboxEvent) {
        outboxRepository.delete(outboxEvent);
    }

    @Override
    public void deleteById(@NotNull Long outboxEventId) {
        outboxRepository.deleteById(outboxEventId);

    }

    @Override
    public OutboxEventEntity updateStatus(Long outboxEventId, OutboxStatus newStatus) {
        return outboxRepository.findById(outboxEventId)
                .map(entity -> entity.setStatus(newStatus))
                .orElseThrow(() -> EntityWithGivenIdNotFoundException.of(outboxEventId));
    }

    @SuppressWarnings("unchecked")
    private OutboxPayloadDto preparePayload(
            String configurationKey,
            OutboxPayloadCapable<? extends Serializable> payload
    ) {
        return kafkaInfo.findByConfigurationKey(configurationKey)
                .map(KafkaTopicInfo::mapperName)
                .map(String::toLowerCase)
                .map(outboxPayloadMapperMap::get)
                .map(mapper -> ((OutboxPayloadMapper<OutboxPayloadDto, OutboxPayloadCapable<?>, ?>) mapper)
                        .toOutboxPayloadDto(payload))
                .orElseThrow(() -> OutboxConfigurationException.configurationKeyNotFound(configurationKey));
    }


    private OutboxEventEntity createEntity(
            OutboxPayloadCapable<? extends Serializable> payload,
            OutboxEventType eventType,
            String configurationKey,
            Map<String, String> customHeaders
    ) {
        var aggregateId = payload.getAggregateId();
        var aggregateType = payload.getClass().getSimpleName();
        var preparedPayload = preparePayload(configurationKey, payload);
        return new OutboxEventEntity()
                .setAggregateType(aggregateType)
                .setAggregateId(aggregateId)
                .setEventType(eventType)
                .setPayload(objectMapper.valueToTree(preparedPayload))
                .setConfigurationKey(configurationKey)
                .setCustomHeaders(customHeaders);
    }

    private Map<String,
            OutboxPayloadMapper<
                    ? extends OutboxPayloadDto,
                    ? extends OutboxPayloadCapable<?>,
                    ? extends Serializable>> buildOutboxPayloadMapperMap(
            List<OutboxPayloadMapper<
                    ? extends OutboxPayloadDto,
                    ? extends OutboxPayloadCapable<?>,
                    ? extends Serializable>> outboxPayloadMappers
    ) {
        return outboxPayloadMappers.stream()
                .collect(Collectors.toMap(
                        mapper -> mapper.getClass().getSimpleName().toLowerCase(),
                        Function.identity(),
                        (existing, duplicate) -> {
                            throw new OutboxConfigurationException(
                                    "Duplicate OutboxPayloadMapper found: " + duplicate.getClass().getSimpleName()
                            );
                        }
                ));
    }
}
