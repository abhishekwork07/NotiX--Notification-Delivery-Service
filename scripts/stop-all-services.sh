#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RUN_DIR="${ROOT_DIR}/.notix-run"
PID_DIR="${RUN_DIR}/pids"

SERVICES=(
  "retry-scheduler-service"
  "sms-sender-service"
  "email-sender-service"
  "dispatcher-service"
  "api-service"
)

is_pid_running() {
  local pid="$1"
  [[ -n "$pid" ]] && kill -0 "$pid" >/dev/null 2>&1
}

stop_service() {
  local service="$1"
  local pid_file="${PID_DIR}/${service}.pid"

  if [[ ! -f "$pid_file" ]]; then
    echo "${service} is not managed by this launcher."
    return
  fi

  local pid
  pid="$(cat "$pid_file")"
  if ! is_pid_running "$pid"; then
    echo "${service} PID ${pid} is not running."
    rm -f "$pid_file"
    return
  fi

  echo "Stopping ${service} with PID ${pid}..."
  kill "$pid" >/dev/null 2>&1 || true

  for _ in {1..20}; do
    if ! is_pid_running "$pid"; then
      rm -f "$pid_file"
      echo "${service} stopped."
      return
    fi
    sleep 1
  done

  echo "${service} did not stop gracefully; forcing stop."
  kill -9 "$pid" >/dev/null 2>&1 || true
  rm -f "$pid_file"
}

for service in "${SERVICES[@]}"; do
  stop_service "$service"
done

echo "Managed NotiX services stopped."
