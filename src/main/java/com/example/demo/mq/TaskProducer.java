package com.example.demo.mq;

import com.example.demo.mq.dto.TaskMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskProducer {

    private final RocketMQTemplate rocketMQTemplate;
    private final ObjectMapper objectMapper;

    @Value("${task.scheduling.topic}")
    private String topic;

    public boolean sendTask(String taskId, Map<String, Object> payload) {
        try {
            // Extract data from the payload to create a structured message
            String type = (String) payload.get("type");
            String target = (String) payload.get("target");
            String message = (String) payload.get("message");
            TaskMessage taskMessage = new TaskMessage(taskId, type, target, message);

            // Serialize the entire DTO object into JSON
            String messagePayload = objectMapper.writeValueAsString(taskMessage);

            rocketMQTemplate.syncSend(topic, MessageBuilder.withPayload(messagePayload).setHeader("KEYS", taskId).build());
            log.info("Successfully sent task to RocketMQ. TaskId: {}, Topic: {}, messagePayload:{}", taskId, topic, messagePayload);
            return true;
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize task payload for MQ. TaskId: {}", taskId, e);
            return false;
        } catch (Exception e) {
            log.error("Failed to send task to RocketMQ. TaskId: {}", taskId, e);
            return false;
        }
    }
}
