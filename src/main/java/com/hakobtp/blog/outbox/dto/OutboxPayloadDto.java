package com.hakobtp.blog.outbox.dto;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.OffsetDateTime;

@Getter
@Setter
@ToString
@EqualsAndHashCode
public class OutboxPayloadDto {

    private String createdBy;
    private OffsetDateTime createdAt;
    private String modifiedBy;
    private OffsetDateTime modifiedAt;
}
