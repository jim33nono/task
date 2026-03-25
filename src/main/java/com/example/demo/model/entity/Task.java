package com.example.demo.model.entity;

import com.example.demo.model.TaskStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

@Data
public class Task {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private String taskId;
    private TaskStatus status;
    private Instant executeAt;
    private String payload;
    private int retryCount;
    private Instant createdAt;
    private Instant updatedAt;

    public Map<String, Object> getPayloadAsMap() {
        try {
            return objectMapper.readValue(payload, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to parse payload JSON", e);
        }
    }

    public void setPayloadFromMap(Map<String, Object> map) {
        try {
            this.payload = objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize payload to JSON", e);
        }
    }
}
