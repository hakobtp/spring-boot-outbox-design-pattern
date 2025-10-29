package com.hakobtp.blog.order.mapper;

import com.hakobtp.blog.order.controller.request.CreateOrUpdateOrderRequest;
import com.hakobtp.blog.order.controller.response.OrderResponse;
import com.hakobtp.blog.order.dto.OrderDto;
import com.hakobtp.blog.order.persistence.entity.OrderEntity;
import com.hakobtp.blog.outbox.mapper.OutboxPayloadMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", implementationName = "OrderOutboxPayloadMapper")
public interface OrderMapper extends OutboxPayloadMapper<OrderDto, OrderEntity, Long> {

    OrderEntity toEntity(OrderDto orderDto);

    OrderDto toDto(OrderEntity orderEntity);

    OrderDto toDto(CreateOrUpdateOrderRequest request);

    OrderResponse toResponse(OrderDto orderDto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "modifiedBy", ignore = true)
    @Mapping(target = "modifiedAt", ignore = true)
    OrderEntity margeForUpdate(OrderDto source, @MappingTarget OrderEntity target);
}
