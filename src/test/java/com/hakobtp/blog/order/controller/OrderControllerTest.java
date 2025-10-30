package com.hakobtp.blog.order.controller;


import com.fasterxml.jackson.databind.JsonNode;
import com.hakobtp.blog.common.BaseIT;
import com.hakobtp.blog.common.TestKafkaConsumer;
import com.hakobtp.blog.order.controller.request.CreateOrUpdateOrderRequest;
import com.hakobtp.blog.order.controller.response.OrderResponse;
import com.hakobtp.blog.order.persistence.entity.OrderEntity;
import com.hakobtp.blog.outbox.enums.OutboxEventType;
import com.hakobtp.blog.outbox.enums.OutboxStatus;
import com.hakobtp.blog.outbox.persistence.repository.OutboxRepository;
import com.hakobtp.blog.outbox.service.OutboxRelayService;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

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
    private OutboxRepository outboxRepository;
    @Autowired
    private TestKafkaConsumer testKafkaConsumer;
    @Autowired
    private OutboxRelayService outboxRelayService;

    @BeforeEach
    void setUp() {
        testKafkaConsumer.resetLatch();
    }

    @Test
    @SneakyThrows
    @DisplayName("POST: " + ORDERS_ENDPOINT + "Create order, trigger relay, and verify Kafka message publication")
    @Sql(scripts = CLEAN_SCRIPT, executionPhase = AFTER_TEST_METHOD, config = @SqlConfig(transactionMode = ISOLATED))
    void createOrder_success() {
        //region Arrange

        CreateOrUpdateOrderRequest request = new CreateOrUpdateOrderRequest(
                "ORD-" + System.currentTimeMillis(),
                "John Doe",
                BigDecimal.valueOf(150.50)
        );

        //endregion

        //region Act: create order

        String responseContent = mockMvc.perform(post(ORDERS_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        //endregion

        // region  Deserialize response

        var orderResponse = jsonStringToObject(responseContent, OrderResponse.class);
        assertThat(orderResponse).isNotNull();
        assertThat(orderResponse.id()).isNotNull();

        //endregion

        // region  Act & Assert: get order by ID

        mockMvc.perform(get(ORDERS_ENDPOINT + "/" + orderResponse.id())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderResponse.id()))
                .andExpect(jsonPath("$.orderNumber").value(request.orderNumber()))
                .andExpect(jsonPath("$.customerName").value(request.customerName()))
                .andExpect(jsonPath("$.amount").value(request.amount().doubleValue()))
                .andExpect(jsonPath("$.orderDate").isNotEmpty());

        //endregion

        //region  Assert Outbox event

        var outboxEvents = outboxRepository.findAllByEventType(OutboxEventType.INSERT, Pageable.unpaged()).getContent();
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

        //endregion

        // Act: Manually trigger the outbox relay to publish the message
        outboxRelayService.relayBatch();

        // Wait for the asynchronous Kafka consumer to receive the message
        boolean messageReceived = testKafkaConsumer.getLatch().await(10, TimeUnit.SECONDS);
        assertThat(messageReceived).isTrue(); // Fails if the consumer times out

        // Assert: Verify the content of the published Kafka message
        JsonNode kafkaPayload = testKafkaConsumer.getPayload();
        assertThat(kafkaPayload).isNotNull();
        var payload = jsonNodeToObject(kafkaPayload, OrderEntity.class);

        assertThat(payload.getOrderNumber()).isEqualTo(request.orderNumber());
        assertThat(payload.getCustomerName()).isEqualTo(request.customerName());
        assertThat(payload.getAmount()).isEqualByComparingTo(request.amount());
        assertThat(payload.getAggregateId()).isEqualTo(outboxEvent.getAggregateId());

        // Assert: Verify the outbox event in the DB was updated to 'COMPLETED'
        var completedEvent = outboxRepository.findById(outboxEvent.getId()).orElseThrow();
        assertThat(completedEvent.getStatus()).isEqualTo(OutboxStatus.COMPLETED);
    }
}