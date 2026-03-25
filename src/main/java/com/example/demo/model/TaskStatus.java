package com.example.demo.model;

public enum TaskStatus {
    PENDING,
    PROCESSING, // Task has been claimed and is being processed
    TRIGGERED,
    CANCELLED,
    FAILED      // Task processing failed after retries
}
