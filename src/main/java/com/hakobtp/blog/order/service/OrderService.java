package com.hakobtp.blog.order.service;

import com.hakobtp.blog.common.exception.EntityWithGivenIdNotFoundException;
import com.hakobtp.blog.common.exception.InvalidRequestException;
import com.hakobtp.blog.order.dto.OrderDto;
import com.hakobtp.blog.order.mapper.OrderMapper;
import com.hakobtp.blog.order.persistence.repository.OrderRepository;
import com.hakobtp.blog.outbox.enums.OutboxEventType;
import com.hakobtp.blog.outbox.service.OutboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class OrderService {

    private static final String CONFIGURATION_KEY = "order";

    private final OrderMapper orderMapper;
    private final OutboxService outboxService;
    private final OrderRepository orderRepository;

    @Transactional(readOnly = true)
    public Optional<OrderDto> findById(Long orderId) {
        return orderRepository.findById(orderId)
                .map(orderMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<OrderDto> search(Pageable pageable) {
        return orderRepository.findAll(pageable)
                .map(orderMapper::toDto);
    }

    public OrderDto create(OrderDto orderDto) {
        var entity = orderMapper.toEntity(orderDto)
                .setOrderDate(OffsetDateTime.now());
        entity = orderRepository.save(entity);
        outboxService.save(entity, OutboxEventType.INSERT, CONFIGURATION_KEY);
        return orderMapper.toDto(entity);
    }

    public OrderDto update(Long orderId, OrderDto orderDto) {
        if (orderId.equals(orderDto.getId())) {
            throw new InvalidRequestException("Order id is invalid");
        }
        var entity = orderRepository.findById(orderId)
                .orElseThrow(() -> EntityWithGivenIdNotFoundException.of(orderId));
        entity = orderMapper.margeForUpdate(orderDto, entity);
        entity = orderRepository.save(entity);
        outboxService.save(entity, OutboxEventType.UPDATE, CONFIGURATION_KEY);
        return orderMapper.toDto(entity);
    }

    public void deleteById(Long orderId) {
        var entity = orderRepository.findById(orderId)
                .orElseThrow(() -> EntityWithGivenIdNotFoundException.of(orderId));
        outboxService.save(entity, OutboxEventType.DELETE, CONFIGURATION_KEY);
        orderRepository.delete(entity);
    }
}
