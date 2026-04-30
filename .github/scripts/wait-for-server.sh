#!/usr/bin/env bash
# Poll the Plugwerk server's /actuator/health endpoint until status=UP.
# /actuator/health is publicly reachable by design (ADR-0025): docker-compose,
# uptime probes and CI all rely on it being unauthenticated.
#
# Usage: wait-for-server.sh [base-url] [timeout-seconds]
#   base-url        defaults to http://localhost:8080
#   timeout-seconds defaults to 90

set -euo pipefail

BASE_URL="${1:-http://localhost:8080}"
TIMEOUT="${2:-90}"
INTERVAL=2

deadline=$(( $(date +%s) + TIMEOUT ))

echo "[wait-for-server] polling ${BASE_URL}/actuator/health (timeout ${TIMEOUT}s)"

while :; do
  if status=$(curl -fsS --max-time 3 "${BASE_URL}/actuator/health" 2>/dev/null | jq -r '.status // "DOWN"'); then
    if [[ "${status}" == "UP" ]]; then
      echo "[wait-for-server] server is UP"
      exit 0
    fi
    echo "[wait-for-server] status=${status}, waiting…"
  else
    echo "[wait-for-server] not reachable yet, waiting…"
  fi

  if (( $(date +%s) >= deadline )); then
    echo "[wait-for-server] timed out after ${TIMEOUT}s" >&2
    exit 1
  fi

  sleep "${INTERVAL}"
done
