package com.example.demo.service;

import com.example.demo.model.TaskStatus;
import com.example.demo.model.dto.CreateTaskRequest;
import com.example.demo.model.dto.TaskResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface TaskService {
    TaskResponse createTask(CreateTaskRequest request);
    Optional<TaskResponse> getTaskById(String taskId);
    boolean cancelTask(String taskId);
    Page<TaskResponse> listTasksByStatus(TaskStatus status, Pageable pageable);
}
