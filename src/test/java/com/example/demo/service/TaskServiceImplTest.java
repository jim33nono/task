package com.example.demo.service;

import com.example.demo.mapper.TaskMapper;
import com.example.demo.model.TaskStatus;
import com.example.demo.model.dto.CreateTaskRequest;
import com.example.demo.model.dto.TaskResponse;
import com.example.demo.model.entity.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceImplTest {

    @Mock
    private TaskMapper taskMapper;

    @Mock
    private RedisDelayQueueService redisDelayQueueService;

    @InjectMocks
    private TaskServiceImpl taskService;

    private CreateTaskRequest createTaskRequest;
    private Task task;

    @BeforeEach
    void setUp() {
        createTaskRequest = new CreateTaskRequest(
                "test-task-1",
                Instant.now().plusSeconds(20),
                Map.of("type", "email", "target", "test@example.com")
        );

        task = new Task();
        task.setTaskId(createTaskRequest.taskId());
        task.setExecuteAt(createTaskRequest.executeAt());
        task.setPayloadFromMap(createTaskRequest.payload());
        task.setStatus(TaskStatus.PENDING);
    }

    @Test
    void createTask_shouldSucceed_whenTaskIdIsUnique() {
        // Given
        when(taskMapper.findByTaskId(createTaskRequest.taskId())).thenReturn(Optional.empty());
        doNothing().when(taskMapper).insert(any(Task.class));
        doNothing().when(redisDelayQueueService).addTask(anyString(), any(Instant.class));

        // When
        TaskResponse response = taskService.createTask(createTaskRequest);

        // Then
        assertNotNull(response);
        assertEquals(createTaskRequest.taskId(), response.taskId());
        assertEquals(TaskStatus.PENDING, response.status());
        verify(taskMapper, times(1)).findByTaskId(createTaskRequest.taskId());
        verify(taskMapper, times(1)).insert(any(Task.class));
        verify(redisDelayQueueService, times(1)).addTask(task.getTaskId(), task.getExecuteAt());
    }

    @Test
    void createTask_shouldThrowException_whenTaskIdAlreadyExists() {
        // Given
        when(taskMapper.findByTaskId(createTaskRequest.taskId())).thenReturn(Optional.of(task));

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            taskService.createTask(createTaskRequest);
        });

        assertEquals("Task with ID " + createTaskRequest.taskId() + " already exists.", exception.getMessage());
        verify(taskMapper, times(1)).findByTaskId(createTaskRequest.taskId());
        verify(taskMapper, never()).insert(any(Task.class));
        verify(redisDelayQueueService, never()).addTask(anyString(), any(Instant.class));
    }
    
    @Test
    void createTask_shouldThrowRuntimeException_whenRedisFails() {
        // Given
        when(taskMapper.findByTaskId(createTaskRequest.taskId())).thenReturn(Optional.empty());
        doNothing().when(taskMapper).insert(any(Task.class));
        doThrow(new RuntimeException("Redis connection failed")).when(redisDelayQueueService).addTask(anyString(), any(Instant.class));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            taskService.createTask(createTaskRequest);
        });

        assertEquals("Failed to add task to Redis delay queue", exception.getMessage());
        assertTrue(exception.getCause() instanceof RuntimeException);
        assertEquals("Redis connection failed", exception.getCause().getMessage());
        
        verify(taskMapper, times(1)).insert(any(Task.class));
        verify(redisDelayQueueService, times(1)).addTask(task.getTaskId(), task.getExecuteAt());
    }

}
