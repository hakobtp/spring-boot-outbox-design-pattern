package com.hakobtp.blog.order.controller;


import com.hakobtp.blog.common.BaseIT;
import com.hakobtp.blog.order.controller.request.CreateOrUpdateOrderRequest;
import com.hakobtp.blog.order.controller.response.OrderResponse;
import com.hakobtp.blog.outbox.enums.OutboxEventType;
import com.hakobtp.blog.outbox.enums.OutboxStatus;
import com.hakobtp.blog.outbox.persistence.repository.OutboxRepository;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;

import java.math.BigDecimal;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.SqlConfig.TransactionMode.ISOLATED;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderControllerTest extends BaseIT {

    private static final String ORDERS_ENDPOINT = "/api/v1/orders";
    private static final String CLEAN_SCRIPT = "/scripts/order/order_clean_script.sql";

    @Autowired
    private OutboxRepository outboxService;

    @Test
    @SneakyThrows
    @DisplayName("POST: " + ORDERS_ENDPOINT + " → Create new order successfully and verify outbox event")
    @Sql(scripts = CLEAN_SCRIPT, executionPhase = AFTER_TEST_METHOD, config = @SqlConfig(transactionMode = ISOLATED))
    void createOrder_success() {
        // Arrange
        CreateOrUpdateOrderRequest request = new CreateOrUpdateOrderRequest(
                "ORD-" + System.currentTimeMillis(),
                "John Doe",
                BigDecimal.valueOf(150.50)
        );

        // Act: create order
        String responseContent = mockMvc.perform(post(ORDERS_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Deserialize response
        var orderResponse = jsonStringToObject(responseContent, OrderResponse.class);

        // Assert order is created
        assertThat(orderResponse).isNotNull();
        assertThat(orderResponse.id()).isNotNull();

        // Act & Assert: get order by ID
        mockMvc.perform(get(ORDERS_ENDPOINT + "/" + orderResponse.id())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderResponse.id()))
                .andExpect(jsonPath("$.orderNumber").value(request.orderNumber()))
                .andExpect(jsonPath("$.customerName").value(request.customerName()))
                .andExpect(jsonPath("$.amount").value(request.amount().doubleValue()))
                .andExpect(jsonPath("$.orderDate").isNotEmpty());

        // Assert Outbox event
        var outboxEvents = outboxService.findAllByEventType(OutboxEventType.INSERT, Pageable.unpaged()).getContent();
        assertThat(outboxEvents.size()).isEqualTo(1);

        var outboxEvent = outboxEvents.get(0);

        assertThat(outboxEvent.getId()).isNotNull();
        assertThat(outboxEvent.getEventId()).isNotNull();
        assertThat(outboxEvent.getAggregateType()).isEqualTo("OrderEntity");
        assertThat(outboxEvent.getEventType()).isEqualTo(OutboxEventType.INSERT);
        assertThat(outboxEvent.getStatus()).isEqualTo(OutboxStatus.NEW);
        assertThat(outboxEvent.getAttemptCount()).isEqualTo(0);
        assertThat(outboxEvent.getConfigurationKey()).isEqualTo("order");
        assertThat(outboxEvent.getCustomHeaders().size()).isEqualTo(0);
        assertThat(outboxEvent.getCreatedAt()).isNotNull();
        assertThat(outboxEvent.getCreatedBy()).isEqualTo("system");
        assertThat(outboxEvent.getModifiedAt()).isNotNull();
        assertThat(outboxEvent.getModifiedBy()).isEqualTo("system");

        assertThat(outboxEvent.getPayload().get("id").asLong()).isEqualTo(orderResponse.id());
        assertThat(outboxEvent.getPayload().get("amount").decimalValue()).isEqualTo(request.amount());
        assertThat(outboxEvent.getPayload().get("createdAt")).isNotNull();
        assertThat(outboxEvent.getPayload().get("createdBy").textValue()).isEqualTo("system");
        assertThat(outboxEvent.getPayload().get("modifiedAt")).isNotNull();
        assertThat(outboxEvent.getPayload().get("modifiedBy").textValue()).isEqualTo("system");
        assertThat(outboxEvent.getPayload().get("orderNumber").textValue()).isEqualTo(orderResponse.orderNumber());
        assertThat(outboxEvent.getPayload().get("customerName").textValue()).isEqualTo(request.customerName());
    }
}