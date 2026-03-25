package com.example.demo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisDelayQueueService {

    private static final String TASK_SCHEDULE_ZSET_KEY = "task:schedule:zset";

    private final StringRedisTemplate redisTemplate;
    private final RedisScript<List> fetchAndRemoveScript;

    public void addTask(String taskId, Instant executeAt) {
        long executeAtTimestamp = executeAt.toEpochMilli();
        redisTemplate.opsForZSet().add(TASK_SCHEDULE_ZSET_KEY, taskId, executeAtTimestamp);
    }

    public void removeTask(String taskId) {
        redisTemplate.opsForZSet().remove(TASK_SCHEDULE_ZSET_KEY, taskId);
    }

    /**
     * Atomically fetches and removes due tasks using a Lua script.
     * @param batchSize The maximum number of tasks to claim.
     * @return A list of task IDs that were successfully claimed.
     */
    public List<String> fetchAndClaimDueTasks(int batchSize) {
        long now = Instant.now().toEpochMilli();
        List<String> claimedTaskIds = null;
        try {
            claimedTaskIds = redisTemplate.execute(
                    fetchAndRemoveScript,
                    Collections.singletonList(TASK_SCHEDULE_ZSET_KEY),
                    String.valueOf(now),
                    String.valueOf(batchSize)
            );
        } catch (Exception e) {
            log.error("執行 Redis 腳本後失敗！", e);
            return Collections.emptyList();
        }
        return claimedTaskIds != null ? claimedTaskIds : Collections.emptyList();
    }
}
