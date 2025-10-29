package com.hakobtp.blog.order.facade;

import com.hakobtp.blog.common.exception.EntityWithGivenIdNotFoundException;
import com.hakobtp.blog.order.controller.request.CreateOrUpdateOrderRequest;
import com.hakobtp.blog.order.controller.response.OrderResponse;
import com.hakobtp.blog.order.mapper.OrderMapper;
import com.hakobtp.blog.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
@RequiredArgsConstructor
public class OrderFacade {

    private final OrderMapper orderMapper;
    private final OrderService orderService;

    @Transactional(readOnly = true)
    public OrderResponse getById(Long orderId) {
        return orderService.findById(orderId)
                .map(orderMapper::toResponse)
                .orElseThrow(() -> EntityWithGivenIdNotFoundException.of(orderId));
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> search(Pageable pageable) {
        return orderService.search(pageable)
                .map(orderMapper::toResponse);
    }

    public OrderResponse create(CreateOrUpdateOrderRequest request) {
        var orderDto = orderMapper.toDto(request);
        orderDto = orderService.create(orderDto);
        return orderMapper.toResponse(orderDto);
    }

    public OrderResponse update(Long orderId, CreateOrUpdateOrderRequest request) {
        var orderDto = orderMapper.toDto(request);
        orderDto = orderService.update(orderId, orderDto);
        return orderMapper.toResponse(orderDto);
    }

    public void delete(Long orderId) {
        orderService.deleteById(orderId);
    }
}
