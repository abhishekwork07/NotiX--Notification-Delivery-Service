#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RUN_DIR="${ROOT_DIR}/.notix-run"
LOG_DIR="${RUN_DIR}/logs"
PID_DIR="${RUN_DIR}/pids"
MVNW="${ROOT_DIR}/api-service/mvnw"
MAVEN_ARGS="${NOTIX_MAVEN_ARGS:-}"
JAVA_OPTS="${NOTIX_JAVA_OPTS:-}"
API_WARMUP_SECONDS="${NOTIX_API_WARMUP_SECONDS:-8}"

SERVICES=(
  "api-service:7070"
  "dispatcher-service:7071"
  "email-sender-service:7072"
  "sms-sender-service:7073"
  "retry-scheduler-service:7074"
)

SKIP_BUILD=false
START_INFRA=false
TAIL_LOGS=false

for arg in "$@"; do
  case "$arg" in
    --no-build)
      SKIP_BUILD=true
      ;;
    --with-infra)
      START_INFRA=true
      ;;
    --tail)
      TAIL_LOGS=true
      ;;
    *)
      echo "Unknown argument: $arg" >&2
      echo "Usage: $0 [--no-build] [--with-infra] [--tail]" >&2
      exit 1
      ;;
  esac
done

mkdir -p "$LOG_DIR" "$PID_DIR"

is_pid_running() {
  local pid="$1"
  [[ -n "$pid" ]] && kill -0 "$pid" >/dev/null 2>&1
}

is_port_open() {
  local port="$1"
  nc -z localhost "$port" >/dev/null 2>&1
}

start_infra() {
  echo "Starting Docker infrastructure..."
  docker compose -f "${ROOT_DIR}/infrastructure/docker/docker-compose.yml" up -d
}

build_apps() {
  echo "Packaging all NotiX services..."
  # shellcheck disable=SC2086
  "$MVNW" -f "${ROOT_DIR}/pom.xml" -DskipTests $MAVEN_ARGS package
}

start_service() {
  local service="$1"
  local port="$2"
  local pid_file="${PID_DIR}/${service}.pid"
  local log_file="${LOG_DIR}/${service}.log"
  local jar_file="${ROOT_DIR}/${service}/target/${service}-0.0.1-SNAPSHOT.jar"

  if [[ -f "$pid_file" ]] && is_pid_running "$(cat "$pid_file")"; then
    echo "${service} is already managed and running with PID $(cat "$pid_file")."
    return
  fi

  if is_port_open "$port"; then
    echo "${service} was not started because port ${port} is already in use."
    echo "If that is an old manual run, stop it first or use scripts/stop-all-services.sh for managed runs."
    return
  fi

  if [[ ! -f "$jar_file" ]]; then
    echo "Missing jar for ${service}: ${jar_file}" >&2
    echo "Run without --no-build so the launcher can package services first." >&2
    exit 1
  fi

  echo "Starting ${service} on port ${port}..."
  # shellcheck disable=SC2086
  nohup java $JAVA_OPTS -jar "$jar_file" >"$log_file" 2>&1 &
  echo "$!" > "$pid_file"
}

if [[ "$START_INFRA" == true ]]; then
  start_infra
fi

if [[ "$SKIP_BUILD" == false ]]; then
  build_apps
fi

for service_entry in "${SERVICES[@]}"; do
  service="${service_entry%%:*}"
  port="${service_entry##*:}"
  start_service "$service" "$port"

  if [[ "$service" == "api-service" ]]; then
    echo "Waiting ${API_WARMUP_SECONDS}s for api-service and Eureka to warm up..."
    sleep "$API_WARMUP_SECONDS"
  fi
done

echo
echo "NotiX service launch complete."
echo "Logs: ${LOG_DIR}"
echo "Eureka dashboard: http://localhost:7070/eureka-dashboard"
echo "Swagger dashboard: http://localhost:7070/swagger-ui.html"
echo "Infrastructure health: http://localhost:7070/monitoring/infra/health"
echo
echo "To stop managed services: scripts/stop-all-services.sh"

if [[ "$TAIL_LOGS" == true ]]; then
  echo
  echo "Tailing service logs. Press Ctrl+C to stop tailing; services will keep running."
  tail -n 80 -f "${LOG_DIR}"/*.log
fi
