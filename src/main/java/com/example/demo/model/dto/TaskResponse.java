package com.example.demo.model.dto;

import com.example.demo.model.TaskStatus;
import com.example.demo.model.entity.Task;

import java.time.Instant;
import java.util.Map;

public record TaskResponse(
    String taskId,
    TaskStatus status,
    Instant executeAt,
    Map<String, Object> payload,
    Instant createdAt,
    Instant updatedAt
) {
    public static TaskResponse fromEntity(Task task) {
        return new TaskResponse(
            task.getTaskId(),
            task.getStatus(),
            task.getExecuteAt(),
            task.getPayloadAsMap(),
            task.getCreatedAt(),
            task.getUpdatedAt()
        );
    }
}
