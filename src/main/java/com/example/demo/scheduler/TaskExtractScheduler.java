package com.example.demo.scheduler;

import com.example.demo.mapper.TaskMapper;
import com.example.demo.model.entity.Task;
import com.example.demo.service.RedisDelayQueueService;
import com.example.demo.service.TaskProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskExtractScheduler {

    private final RedisDelayQueueService redisDelayQueueService;
    private final TaskProcessor taskProcessor;
    private final TaskMapper taskMapper;

    private static final int BATCH_SIZE = 100;
    private static final int MAX_RETRIES = 3;

    /**
     * Main scheduler: claims tasks from Redis and dispatches them to the processor.
     */
    @Scheduled(fixedRate = 1000)
    public void scheduleTasksForProcessing() {
        // T○ run lua script. Redis
        List<String> claimedTaskIds = redisDelayQueueService.fetchAndClaimDueTasks(BATCH_SIZE);
        if (claimedTaskIds.isEmpty()) {
            return;
        }
        log.info("Claimed {} tasks from Redis. Dispatching for processing.", claimedTaskIds.size());
        // TODO batch sql update
        for (String taskId : claimedTaskIds) {
            taskProcessor.process(taskId);
        }
    }

    /**
     * Compensation scheduler: handles tasks stuck in the PROCESSING state.
     */
    @Scheduled(fixedRate = 300000, initialDelay = 60000) // Runs every 5 minutes, starts after 1 min
    public void retryStuckTasks() {
        log.info("Running compensation job for stuck tasks...");
        Instant fiveMinutesAgo = Instant.now().minus(6, ChronoUnit.MINUTES);
        List<Task> stuckTasks = taskMapper.findStuckProcessingTasks(fiveMinutesAgo);

        if (stuckTasks.isEmpty()) {
            log.info("No stuck tasks found.");
            return;
        }

        log.warn("Found {} stuck tasks. Retrying...", stuckTasks.size());
        for (Task task : stuckTasks) {
            if (task.getRetryCount() >= MAX_RETRIES) {
                log.error("Task {} has exceeded max retries ({}). Marking as FAILED.", task.getTaskId(), MAX_RETRIES);
                taskProcessor.markAsFailed(task);
            } else {
                log.warn("Retrying task {} (current retries: {}).", task.getTaskId(), task.getRetryCount());
                taskProcessor.retry(task);
            }
        }
    }
}