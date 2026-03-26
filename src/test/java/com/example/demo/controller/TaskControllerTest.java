package com.example.demo.controller;

import com.example.demo.model.TaskStatus;
import com.example.demo.model.dto.CreateTaskRequest;
import com.example.demo.model.dto.TaskResponse;
import com.example.demo.service.TaskService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TaskController.class)
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TaskService taskService;

    private TaskResponse taskResponse;
    private CreateTaskRequest createTaskRequest;

    @BeforeEach
    void setUp() {
        // 準備一個通用的 TaskResponse 物件供測試使用
        Map<String, Object> payload = Collections.singletonMap("data", "test");
        taskResponse = new TaskResponse("task-123", TaskStatus.PENDING, Instant.now(), payload, Instant.now(),Instant.now());

        // 準備一個通用的 CreateTaskRequest 物件供測試使用
        createTaskRequest = new CreateTaskRequest("task-123", Instant.now().plusSeconds(20), payload);

    }

    /**
     * Tests the successful creation of a task.
     * Expects HTTP 201 (Created) and the returned task data.
     */
    @Test
    void createTask_whenSuccess_shouldReturnCreated() throws Exception {
        // 模擬 Service 層成功創建任務
        when(taskService.createTask(any(CreateTaskRequest.class))).thenReturn(taskResponse);

        mockMvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createTaskRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.taskId").value("task-123"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    /**
     * Tests task creation failure when a task with the same ID already exists.
     * Expects HTTP 409 (Conflict).
     */
    @Test
    void createTask_whenTaskIdExists_shouldReturnConflict() throws Exception {
        // 模擬 Service 層因任務 ID 重複而拋出異常
        when(taskService.createTask(any(CreateTaskRequest.class))).thenThrow(new IllegalArgumentException("Task ID exists"));

        mockMvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createTaskRequest)))
                .andExpect(status().isConflict());
    }

    /**
     * Tests retrieving a task by its ID when the task exists.
     * Expects HTTP 200 (OK) and the task data.
     */
    @Test
    void getTaskById_whenTaskExists_shouldReturnTask() throws Exception {
        // 模擬 Service 層成功找到任務
        when(taskService.getTaskById("task-123")).thenReturn(Optional.of(taskResponse));

        mockMvc.perform(get("/tasks/task-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskId").value("task-123"));
    }

    /**
     * Tests retrieving a task by its ID when the task does not exist.
     * Expects HTTP 404 (Not Found).
     */
    @Test
    void getTaskById_whenTaskNotFound_shouldReturnNotFound() throws Exception {
        // 模擬 Service 層找不到任務
        when(taskService.getTaskById("task-non-existent")).thenReturn(Optional.empty());

        mockMvc.perform(get("/tasks/task-non-existent"))
                .andExpect(status().isNotFound());
    }

    /**
     * Tests the successful cancellation of a pending task.
     * Expects HTTP 204 (No Content).
     */
    @Test
    void cancelTask_whenSuccess_shouldReturnNoContent() throws Exception {
        // 模擬 Service 層成功取消任務
        when(taskService.cancelTask("task-123")).thenReturn(true);

        mockMvc.perform(delete("/tasks/task-123"))
                .andExpect(status().isNoContent());
    }

    /**
     * Tests cancelling a task that does not exist.
     * Expects HTTP 404 (Not Found).
     */
    @Test
    void cancelTask_whenTaskNotFound_shouldReturnNotFound() throws Exception {
        // 模擬 Service 層因找不到任務而取消失敗
        when(taskService.cancelTask("task-non-existent")).thenReturn(false);

        mockMvc.perform(delete("/tasks/task-non-existent"))
                .andExpect(status().isNotFound());
    }

    /**
     * Tests cancelling a task that is not in a cancellable state (e.g., already PROCESSING).
     * Expects HTTP 409 (Conflict).
     */
    @Test
    void cancelTask_whenTaskNotCancellable_shouldReturnConflict() throws Exception {
        // 模擬 Service 層因任務狀態不合法 (例如已在處理中) 而拋出異常
        when(taskService.cancelTask("task-processing")).thenThrow(new IllegalStateException("Task is not in PENDING state"));

        mockMvc.perform(delete("/tasks/task-processing"))
                .andExpect(status().isConflict());
    }

    /**
     * Tests listing tasks by status with pagination.
     * Expects HTTP 200 (OK) and a paginated list of tasks.
     */
    @Test
    void listTasks_shouldReturnPageOfTasks() throws Exception {
        // 準備分頁的返回結果
        Pageable pageable = PageRequest.of(0, 10);
        Page<TaskResponse> taskPage = new PageImpl<>(Collections.singletonList(taskResponse), pageable, 1);

        // 模擬 Service 層返回分頁數據
        when(taskService.listTasksByStatus(eq(TaskStatus.PENDING), any(Pageable.class))).thenReturn(taskPage);

        mockMvc.perform(get("/tasks")
                        .param("status", "PENDING")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].taskId").value("task-123"))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));
    }
}
