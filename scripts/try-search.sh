#!/usr/bin/env bash
set -euo pipefail

curl --fail-with-body --request POST http://localhost:8080/api/developer-recordings \
  --header 'Content-Type: application/json' \
  --data '{"recordingId":"call-42","transcript":"Rollback followed the failed migration in the enrollment table","eventType":"release-operation","repository":"course-api","release":"2026.09.1","recordedAt":"2026-09-13T09:30:00Z"}'

curl --fail-with-body --request POST http://localhost:8080/api/developer-recordings/search \
  --header 'Content-Type: application/json' \
  --data '{"query":"Why did the course API roll back?","topK":3}'
