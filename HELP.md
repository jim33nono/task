#  To create task – curl

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

#  To query task – curl
curl --location 'localhost:8080/tasks/abc-127'

#  To delete task – curl
curl --location --request DELETE 'localhost:8080/tasks/abc-128'

#  List Pending Tasks – curl
curl --location 'localhost:8080/tasks?page=0&size=1&status=PENDING'