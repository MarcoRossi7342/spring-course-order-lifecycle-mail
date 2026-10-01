#!/usr/bin/env sh
set -eu

: "${INFRAI_API_KEY:?Set INFRAI_API_KEY first}"

mvn -q spring-boot:run &
service_pid=$!
trap 'kill "$service_pid" 2>/dev/null || true' EXIT
sleep 5

curl --fail-with-body -X POST http://localhost:8080/order-mails \
  -H 'Content-Type: application/json' \
  -d '{"eventId":"lesson-order-42-fulfilled","orderId":"ORDER-42","learnerEmail":"chenhua@changba.com","learnerName":"Mina","courseName":"Practical Geometry","stage":"FULFILLMENT","detail":"https://learn.example/courses/geometry"}'
