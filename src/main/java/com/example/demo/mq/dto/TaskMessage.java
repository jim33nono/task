package com.example.demo.mq.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskMessage {
    private String taskId;
    private String type;
    private String target;
    private String message;
}
