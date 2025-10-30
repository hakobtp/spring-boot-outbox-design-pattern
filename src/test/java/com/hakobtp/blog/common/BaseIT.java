package com.hakobtp.blog.common;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hakobtp.blog.common.testcontainer.KafkaContainerInitializer;
import com.hakobtp.blog.common.testcontainer.PostgresqlContainerInitializer;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestKafkaConsumer.class)
@ContextConfiguration(initializers = {PostgresqlContainerInitializer.class, KafkaContainerInitializer.class})
public class BaseIT {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;


    protected <T> JavaType getListJavaType(Class<T> clazz) {
        return objectMapper.getTypeFactory().constructCollectionType(List.class, clazz);
    }

    @SneakyThrows
    protected <T> T jsonStringToObject(String json, Class<T> clazz) {
        return objectMapper.readValue(json, clazz);
    }

    @SneakyThrows
    protected <T> T jsonStringToObject(String json, JavaType javaType) {
        return objectMapper.readValue(json, javaType);
    }

    @SneakyThrows
    protected <T> T jsonNodeToObject(JsonNode node, Class<T> clazz) {
        return objectMapper.treeToValue(node, clazz);
    }
}
