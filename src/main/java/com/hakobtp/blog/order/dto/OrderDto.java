package com.hakobtp.blog.order.dto;

import com.hakobtp.blog.outbox.dto.OutboxPayloadDto;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Setter
@ToString
@EqualsAndHashCode(callSuper = true)
public class OrderDto extends OutboxPayloadDto {

    private Long id;
    private String orderNumber;
    private String customerName;
    private BigDecimal amount;
    private OffsetDateTime orderDate;
}
