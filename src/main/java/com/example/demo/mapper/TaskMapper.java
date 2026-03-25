package com.example.demo.mapper;

import com.example.demo.model.TaskStatus;
import com.example.demo.model.entity.Task;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Mapper
public interface TaskMapper {

    void insert(Task task);

    Optional<Task> findByTaskId(@Param("taskId") String taskId);

    // The return type is changed from int to String to match the <selectKey> in the XML
    int update(Task task);

    List<Task> findByStatusWithPagination(
            @Param("status") TaskStatus status,
            @Param("limit") int limit,
            @Param("offset") long offset
    );

    long countByStatus(@Param("status") TaskStatus status);

    List<Task> findStuckProcessingTasks(@Param("timeout") Instant timeout);
}
