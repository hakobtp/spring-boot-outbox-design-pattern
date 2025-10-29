package com.hakobtp.blog.order.controller.request;


import java.math.BigDecimal;

public record CreateOrUpdateOrderRequest(
        String orderNumber,
        String customerName,
        BigDecimal amount) {
}
