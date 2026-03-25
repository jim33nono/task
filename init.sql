CREATE TABLE IF NOT EXISTS tasks (
    task_id VARCHAR(255) NOT NULL PRIMARY KEY,
    status VARCHAR(20) NOT NULL COMMENT 'PENDING, PROCESSING(Task has been claimed and is being processed), TRIGGERED, CANCELLED, FAILED(Task processing failed after retries)',
    execute_at TIMESTAMP NOT NULL,
    payload JSON NOT NULL,
    retry_count INT NOT NULL DEFAULT 0  COMMENT 'times of retry',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_task_id (task_id),
    INDEX idx_status_execute_at (status, execute_at),
    INDEX idx_status_updated_at (status, updated_at)
);




