package com.example.demo.service;

import com.example.demo.mapper.TaskMapper;
import com.example.demo.model.TaskStatus;
import com.example.demo.model.dto.CreateTaskRequest;
import com.example.demo.model.dto.TaskResponse;
import com.example.demo.model.entity.Task;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final TaskMapper taskMapper;
    private final RedisDelayQueueService redisDelayQueueService;

    @Override
    @Transactional
    public TaskResponse createTask(CreateTaskRequest request) {
        if (taskMapper.findByTaskId(request.taskId()).isPresent()) {
            throw new IllegalArgumentException("Task with ID " + request.taskId() + " already exists.");
        }
        
        Task task = new Task();
        task.setTaskId(request.taskId());
        task.setExecuteAt(request.executeAt());
        task.setPayloadFromMap(request.payload());
        task.setStatus(TaskStatus.PENDING);

        taskMapper.insert(task);

        try {
            redisDelayQueueService.addTask(task.getTaskId(), task.getExecuteAt());
        } catch (Exception e) {
            throw new RuntimeException("Failed to add task to Redis delay queue", e);
        }

        return TaskResponse.fromEntity(task);
    }

    @Override
    public Optional<TaskResponse> getTaskById(String taskId) {
        return taskMapper.findByTaskId(taskId)
                .map(TaskResponse::fromEntity);
    }

    @Override
    @Transactional
    public boolean cancelTask(String taskId) {
        Optional<Task> taskOptional = taskMapper.findByTaskId(taskId);
        if (taskOptional.isEmpty()) {
            return false;
        }

        Task task = taskOptional.get();

        if (task.getStatus() != TaskStatus.PENDING) {
            throw new IllegalStateException("Only PENDING tasks can be cancelled. Current status: " + task.getStatus());
        }

        task.setStatus(TaskStatus.CANCELLED);
        taskMapper.update(task);

        redisDelayQueueService.removeTask(taskId);
        
        return true;
    }

    @Override
    public Page<TaskResponse> listTasksByStatus(TaskStatus status, Pageable pageable) {
        long offset = pageable.getOffset();
        int limit = pageable.getPageSize();

        List<Task> tasks = taskMapper.findByStatusWithPagination(status, limit, offset);
        long total = taskMapper.countByStatus(status);

        List<TaskResponse> responses = tasks.stream()
                .map(TaskResponse::fromEntity)
                .collect(Collectors.toList());

        return new PageImpl<>(responses, pageable, total);
    }
}
