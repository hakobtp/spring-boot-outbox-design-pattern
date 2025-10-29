package com.hakobtp.blog.order.controller.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record OrderResponse(
        Long id,
        String orderNumber,
        String customerName,
        BigDecimal amount,
        OffsetDateTime orderDate) {
}
