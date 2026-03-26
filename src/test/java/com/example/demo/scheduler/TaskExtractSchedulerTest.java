package com.example.demo.scheduler;

import com.example.demo.mapper.TaskMapper;
import com.example.demo.model.entity.Task;
import com.example.demo.service.RedisDelayQueueService;
import com.example.demo.service.TaskProcessor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskExtractSchedulerTest {

    @Mock
    private RedisDelayQueueService redisDelayQueueService;

    @Mock
    private TaskProcessor taskProcessor;

    @Mock
    private TaskMapper taskMapper;

    @InjectMocks
    private TaskExtractScheduler taskExtractScheduler;

    // --- Tests for scheduleTasksForProcessing method ---

    /**
     * Tests that the scheduler processes all task IDs claimed from Redis.
     */
    @Test
    void whenDueTasksExist_shouldProcessAllOfThem() {
        // Arrange: 模擬 Redis 返回兩個待處理的任務 ID
        List<String> taskIds = Arrays.asList("task-1", "task-2");
        when(redisDelayQueueService.fetchAndClaimDueTasks(anyInt())).thenReturn(taskIds);

        // Act: 執行排程方法
        taskExtractScheduler.scheduleTasksForProcessing();

        // Assert: 驗證 taskProcessor 的 process 方法被每個任務 ID 調用了一次
        verify(taskProcessor, times(1)).process("task-1");
        verify(taskProcessor, times(1)).process("task-2");
        verify(taskProcessor, times(2)).process(anyString()); // 總共調用了兩次
    }

    /**
     * Tests that the scheduler does nothing when no due tasks are claimed from Redis.
     */
    @Test
    void whenNoDueTasks_shouldNotCallProcessor() {
        // Arrange: 模擬 Redis 返回一個空列表
        when(redisDelayQueueService.fetchAndClaimDueTasks(anyInt())).thenReturn(Collections.emptyList());

        // Act: 執行排程方法
        taskExtractScheduler.scheduleTasksForProcessing();

        // Assert: 驗證 taskProcessor 的 process 方法從未被調用
        verify(taskProcessor, never()).process(anyString());
    }

    // --- Tests for retryStuckTasks method ---

    /**
     * Tests the compensation job for a stuck task that has not exceeded its retry limit.
     * Expects the task to be retried.
     */
    @Test
    void whenStuckTasksFoundAndRetriesNotExceeded_shouldRetryThem() {
        // Arrange: 創建一個未達到重試上限的卡住任務
        Task stuckTask = new Task();
        stuckTask.setTaskId("stuck-task-1");
        stuckTask.setRetryCount(1); // MAX_RETRIES 是 3

        List<Task> stuckTasks = Collections.singletonList(stuckTask);
        when(taskMapper.findStuckProcessingTasks(any(Instant.class))).thenReturn(stuckTasks);

        // Act: 執行補償排程
        taskExtractScheduler.retryStuckTasks();

        // Assert: 驗證 taskProcessor 的 retry 方法被調用，而 markAsFailed 方法未被調用
        verify(taskProcessor, times(1)).retry(stuckTask);
        verify(taskProcessor, never()).markAsFailed(any(Task.class));
    }

    /**
     * Tests the compensation job for a stuck task that has reached its maximum retry limit.
     * Expects the task to be marked as FAILED.
     */
    @Test
    void whenStuckTasksFoundAndRetriesExceeded_shouldMarkAsFailed() {
        // Arrange: 創建一個已達到重試上限的卡住任務
        Task failedTask = new Task();
        failedTask.setTaskId("failed-task-1");
        failedTask.setRetryCount(3); // MAX_RETRIES 是 3

        List<Task> stuckTasks = Collections.singletonList(failedTask);
        when(taskMapper.findStuckProcessingTasks(any(Instant.class))).thenReturn(stuckTasks);

        // Act: 執行補償排程
        taskExtractScheduler.retryStuckTasks();

        // Assert: 驗證 taskProcessor 的 markAsFailed 方法被調用，而 retry 方法未被調用
        verify(taskProcessor, times(1)).markAsFailed(failedTask);
        verify(taskProcessor, never()).retry(any(Task.class));
    }

    /**
     * Tests the compensation job when no stuck tasks are found.
     * Expects no action to be taken.
     */
    @Test
    void whenNoStuckTasksFound_shouldDoNothing() {
        // Arrange: 模擬資料庫沒有返回任何卡住的任務
        when(taskMapper.findStuckProcessingTasks(any(Instant.class))).thenReturn(Collections.emptyList());

        // Act: 執行補償排程
        taskExtractScheduler.retryStuckTasks();

        // Assert: 驗證 taskProcessor 的任何方法都未被調用
        verify(taskProcessor, never()).retry(any(Task.class));
        verify(taskProcessor, never()).markAsFailed(any(Task.class));
    }

    /**
     * Tests the compensation job with a mix of tasks to be retried and tasks to be marked as failed.
     * Expects each task to be handled correctly according to its retry count.
     */
    @Test
    void whenMultipleStuckTasks_shouldHandleEachCorrectly() {
        // Arrange: 準備一個需要重試的任務和一個需要標記為失敗的任務
        Task taskToRetry = new Task();
        taskToRetry.setTaskId("task-to-retry");
        taskToRetry.setRetryCount(1);

        Task taskToFail = new Task();
        taskToFail.setTaskId("task-to-fail");
        taskToFail.setRetryCount(3);

        List<Task> stuckTasks = Arrays.asList(taskToRetry, taskToFail);
        when(taskMapper.findStuckProcessingTasks(any(Instant.class))).thenReturn(stuckTasks);

        // Act: 執行補償排程
        taskExtractScheduler.retryStuckTasks();

        // Assert: 分別驗證兩個任務的處理邏輯是否正確
        verify(taskProcessor, times(1)).retry(taskToRetry);
        verify(taskProcessor, times(1)).markAsFailed(taskToFail);
    }
}
