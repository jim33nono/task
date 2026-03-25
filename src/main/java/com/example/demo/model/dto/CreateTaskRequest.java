package com.example.demo.model.dto;

import com.example.demo.validation.FutureWithBuffer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Map;

public record CreateTaskRequest(
    @NotBlank(message = "taskId cannot be blank")
    String taskId,

    @NotNull(message = "executeAt cannot be null")
    @FutureWithBuffer(seconds = 10, message = "executeAt must be at least 10 seconds in the future")
    Instant executeAt,

    @NotNull(message = "payload cannot be null")
    Map<String, Object> payload
) {}
