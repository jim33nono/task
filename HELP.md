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
curl --location 'localhost:8080/tasks/bb-113888'
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

---

## 🤔 Alternative Design: Using RocketMQ's Native Delayed Queues

The current implementation uses a database-polling scheduler (`TaskExtractScheduler`) to find due tasks and send them to an immediate-delivery RocketMQ queue. This approach offers great flexibility for managing and cancelling tasks directly in the database.

However, a more common and efficient approach for delayed execution is to use RocketMQ's native delayed queue feature.

### How It Works

1.  **On Task Creation**: Instead of just saving the task to the database, the service would also send a **delayed message** directly to RocketMQ. The delay level would be chosen to match the desired `executeAt` time as closely as possible.
2.  **Waiting in Broker**: The message resides within the RocketMQ broker until its delay time expires. This offloads the entire waiting and scheduling responsibility from our application to the message queue system.
3.  **Automatic Delivery**: Once the delay time is met, the broker automatically delivers the message to the consumer for immediate processing.

### Pros & Cons

*   **Pros**:
    *   **Simplified Architecture**: Removes the need for a custom polling scheduler (`TaskExtractScheduler`), reducing application complexity and potential points of failure.
    *   **Higher Efficiency & Scalability**: Eliminates the database polling load. This approach is more performant and scales better as the number of scheduled tasks grows.
    *   **Increased Reliability**: Relies on RocketMQ's battle-tested, highly available infrastructure for scheduling, which is generally more robust than a self-managed application scheduler.

*   **Cons**:
    *   **Fixed Delay Levels**: Open-source RocketMQ supports a set of fixed delay levels (e.g., 1s, 5s, 10m, 2h). It does not support arbitrary delay times, so you must choose the level closest to your requirement.
    *   **Cancellation is Difficult**: Once a delayed message is sent, it is very difficult to cancel it before it's delivered. The common workaround is for the consumer to check the task's status in the database upon receiving the message and discard it if it has been marked as `CANCELLED`.
