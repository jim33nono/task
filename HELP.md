# Task Scheduling Service - 使用說明

這是一個基於 Spring Boot 和 RocketMQ 的異步任務調度服務。

---

## ⚠️ 重要：首次環境設定

在啟動服務之前，必須完成以下網路設定，否則應用程式將無法連接到 RocketMQ。

1.  **尋找本機區域網路 IP**
    - **Windows**: 打開 `cmd`，輸入 `ipconfig`，找到你的 "IPv4 位址" (例如：`192.168.0.26`)。
    - **macOS/Linux**: 打開終端機，輸入 `ifconfig` 或 `ip addr`，找到你的 IP 位址。

2.  **設定 Broker IP**
    - 打開專案根目錄下的 `broker.conf` 文件。
    - 在文件末尾，將 `brokerIP1` 的值設置為你剛剛找到的 IP 位址。

    ```conf
    # 檔案: broker.conf
    # ... 其他設定 ...
    brokerIP1 = 192.168.0.26  # <--- 請務必換成你自己的 IP
    ```

---

## ▶️ 如何運行

1.  **啟動基礎設施**
    - 在專案根目錄下，執行以下指令來啟動 MySQL, Redis, 和 RocketMQ 服務：
    ```bash
    docker-compose up -d
    ```

2.  **啟動應用程式**
    - 直接運行你的 Spring Boot 應用程式。

3.  **訪問 RocketMQ 管理介面**
    - 你可以透過瀏覽器訪問 `http://localhost:8088` 來查看 RocketMQ 的狀態。

---

## 🔄 任務狀態說明

任務在其生命週期中會經歷以下幾種狀態：

-   `PENDING`: **待處理**。任務已成功創建，正等待排程時間到達。
-   `PROCESSING`: **處理中**。排程器已選中此任務，正在將其發送到訊息隊列 (MQ) 的過程中。
-   `TRIGGERED`: **已觸發**。任務已成功發送到 MQ，等待消費者執行。
-   `CANCELLED`: **已取消**。任務在被處理前已被手動取消。
-   `FAILED`: **已失敗**。任務在多次重試後仍然處理失敗，已終止。

---

## 🚀 API 使用範例 (cURL)

你可以使用以下 `curl` 指令與服務進行互動。

### 1. 創建一個定時任務

-   **Endpoint**: `POST /tasks`
-   **功能**: 安排一個在未來特定時間執行的任務。

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

### 2. 根據 ID 查詢任務

-   **Endpoint**: `GET /tasks/{taskId}`
-   **功能**: 獲取特定任務的詳細資訊。

```bash
curl --location 'localhost:8080/tasks/abc-127'
```

### 3. 取消一個定時任務

-   **Endpoint**: `DELETE /tasks/{taskId}`
-   **功能**: 如果任務還處於 `PENDING` 狀態，可以將其取消。

```bash
curl --location --request DELETE 'localhost:8080/tasks/abc-128'
```

### 4. 列出特定狀態的任務

-   **Endpoint**: `GET /tasks`
-   **功能**: 分頁查詢特定狀態的任務列表。

```bash
curl --location 'localhost:8080/tasks?page=0&size=10&status=PENDING'
```

---

## ⚙️ 核心業務邏輯

關於任務調度和重試的核心邏輯，可以參考以下檔案和方法：

#### 任務的提取與觸發

-   **檔案**: `src/main/java/com/example/demo/scheduler/TaskExtractScheduler.java`
-   **方法**: `scheduleTasksForProcessing()`
-   **說明**: 此方法會定時從資料庫中撈取到期的 `PENDING` 任務，並將其發送到 RocketMQ 進行處理。

#### 異常任務的重試

-   **檔案**: `src/main/java/com/example/demo/scheduler/TaskExtractScheduler.java`
-   **方法**: `retryStuckTasks()`
-   **說明**: 此方法會定時處理那些長時間卡在 `PROCESSING` 狀態的任務，並進行重試。
