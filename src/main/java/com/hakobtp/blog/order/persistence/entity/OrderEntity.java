package com.hakobtp.blog.order.persistence.entity;

import com.hakobtp.blog.common.persistence.AbstractModificationInfoBaseEntity;
import com.hakobtp.blog.outbox.model.OutboxPayloadCapable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Accessors(chain = true)
@Table(name = "orders")
public class OrderEntity extends AbstractModificationInfoBaseEntity implements OutboxPayloadCapable<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "orders_seq")
    @SequenceGenerator(name = "orders_seq", sequenceName = "sq_orders", allocationSize = 1)
    private Long id;

    @Column(nullable = false, unique = true)
    private String orderNumber;

    @Column(nullable = false)
    private String customerName;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private OffsetDateTime orderDate;
}