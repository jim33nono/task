package com.example.demo.service;

import com.example.demo.mapper.TaskMapper;
import com.example.demo.model.TaskStatus;
import com.example.demo.model.entity.Task;
import com.example.demo.mq.TaskProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskProcessor {

    private final TaskMapper taskMapper;
    private final TaskProducer taskProducer;

    @Transactional(rollbackFor = Exception.class)
    public void process(String taskId) {
        boolean claimed = updateTaskStatus(taskId, TaskStatus.PENDING, TaskStatus.PROCESSING);

        if (!claimed) {
            log.warn("Task {} could not be claimed (was not in PENDING state).", taskId);
            return;
        }

        Task task = taskMapper.findByTaskId(taskId).orElseThrow();
        executeAndFinalize(task);
    }

    @Transactional(rollbackFor = Exception.class)
    public void retry(Task task) {
        task.setRetryCount(task.getRetryCount() + 1);
        taskMapper.update(task);
        
        executeAndFinalize(task);
    }

    @Transactional(rollbackFor = Exception.class)
    public void executeAndFinalize(Task task) {
        try {

            boolean is_send = taskProducer.sendTask(task.getTaskId(), task.getPayloadAsMap());
            // TODO To write some business code in the future
            if (is_send) {
                log.info("Task {} processed and sent to MQ successfully.", task.getTaskId());
                updateTaskStatus(task.getTaskId(), TaskStatus.PROCESSING, TaskStatus.TRIGGERED);
            } else {   // TODO Stay PROCESSING status while something wrong
                log.warn("Task {} was sent to MQ, but its status was not PROCESSING. Manual check may be needed.  It will be retried by the compensation job.", task.getTaskId());
            }
        } catch (Exception e) { // TODO Stay PROCESSING status while something wrong
            log.error("Failed to send task {} to MQ. It will be retried by the compensation job.", task.getTaskId(), e);
        }
    }

    @Transactional
    public void markAsFailed(Task task) {
        task.setStatus(TaskStatus.FAILED);
        taskMapper.update(task);
    }
    
    @Transactional(propagation = Propagation.REQUIRED)
    public boolean updateTaskStatus(String taskId, TaskStatus expectedStatus, TaskStatus newStatus) {
        Task task = taskMapper.findByTaskId(taskId).orElse(null);
        if (task != null && task.getStatus() == expectedStatus) {
            task.setStatus(newStatus);
            taskMapper.update(task);
            return true;
        }
        return false;
    }
}
