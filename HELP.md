# Task Scheduling Service - Usage Guide

This is an asynchronous task scheduling service built with Spring Boot and RocketMQ.

---

## ⚠️ Important: First-Time Environment Setup

Before starting the service, you must complete the following network configuration to ensure the application can connect to RocketMQ.

1.  **Find Your Local Network IP**
    *   **Windows**: Open `cmd`, run `ipconfig`, and find your "IPv4 Address" (e.g., `192.168.0.26`).
    *   **macOS/Linux**: Open a terminal, run `ifconfig` or `ip addr`, and find your IP address.

2.  **Configure the Broker IP**
    *   Open the `broker.conf` file in the project root directory.
    *   At the end of the file, set the value of `brokerIP1` to the IP address you just found.

    ```conf
    # File: broker.conf
    # ... other settings ...
    brokerIP1 = 192.168.0.26  # <--- IMPORTANT: Replace with your own IP
    ```

---

## ▶️ How to Run

1.  **Start the Infrastructure**
    *   In the project root directory, run the following command to start the MySQL, Redis, and RocketMQ services:
    ```bash
    docker-compose up -d
    ```

2.  **Start the Application**
    *   Run the Spring Boot application directly from your IDE or via the command line.

3.  **Access the RocketMQ Console**
    *   You can monitor the status of RocketMQ by visiting `http://localhost:8088` in your browser.

---

## 🔄 Task Status Explained

A task goes through the following statuses in its lifecycle:

*   `PENDING`: The task has been successfully created and is waiting for its scheduled execution time.
*   `PROCESSING`: The scheduler has picked up the task and is in the process of sending it to the message queue (MQ).
*   `TRIGGERED`: The task has been successfully sent to the MQ and is awaiting consumption.
*   `CANCELLED`: The task was manually cancelled before it was processed.
*   `FAILED`: The task has failed after multiple retry attempts and will not be processed again.

---

## 🧪 Running Unit Tests

This project includes a comprehensive suite of unit tests for controllers and services.

You can run the tests in two ways:

1.  **Via Your IDE**
    *   Navigate to the `src/test/java` directory.
    *   Right-click on a specific test class (e.g., `TaskControllerTest.java`) or the entire directory and select "Run Tests".

2.  **Via Maven Command Line**
    *   Open a terminal in the project root directory and run the following command:
    ```bash
    ./mvnw test
    ```

---

## 🚀 API Usage Examples (cURL)

You can interact with the service using the following cURL commands.

### 1. Create a Scheduled Task

*   **Endpoint**: `POST /tasks`
*   **Description**: Schedules a new task to be executed at a future time.

```bash
curl --location 'localhost:8080/tasks' \
--header 'Content-Type: application/json' \
--data-raw '{
    "taskId": "bb-1123",
    "executeAt": "2026-03-25T09:34:00Z",
    "payload": {
        "type": "email",
        "target": "hello@example.com",
        "message": "This is a scheduled task!"
    }
}'
```

### 2. Get Task by ID

*   **Endpoint**: `GET /tasks/{taskId}`
*   **Description**: Retrieves the details of a specific task.

```bash
curl --location 'localhost:8080/tasks/abc-127'
```

### 3. Cancel a Scheduled Task

*   **Endpoint**: `DELETE /tasks/{taskId}`
*   **Description**: Cancels a task if it is still in the `PENDING` state.

```bash
curl --location --request DELETE 'localhost:8080/tasks/abc-128'
```

### 4. List Tasks by Status

*   **Endpoint**: `GET /tasks`
*   **Description**: Retrieves a paginated list of tasks filtered by their status.

```bash
curl --location 'localhost:8080/tasks?page=0&size=10&status=PENDING'
```

---

## ⚙️ Core Business Logic

For the core logic regarding task scheduling and retries, refer to the following files and methods:

#### Task Polling and Triggering

*   **File**: `src/main/java/com/example/demo/scheduler/TaskExtractScheduler.java`
*   **Method**: `scheduleTasksForProcessing()`
*   **Description**: This method periodically polls for due tasks from the database and sends them to RocketMQ for processing.

#### Stuck Task Retries (Compensation)

*   **File**: `src/main/java/com/example/demo/scheduler/TaskExtractScheduler.java`
*   **Method**: `retryStuckTasks()`
*   **Description**: This method periodically handles tasks that have been stuck in the `PROCESSING` state for too long and retries them.
