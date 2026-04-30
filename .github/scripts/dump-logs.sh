#!/usr/bin/env bash
# Dump diagnostics into ./ci-logs/ on test failure. Called from the workflow's
# `if: failure()` step before actions/upload-artifact.
#
# Usage: dump-logs.sh [output-dir]
#   output-dir defaults to ./ci-logs

set -uo pipefail

OUT_DIR="${1:-./ci-logs}"
mkdir -p "${OUT_DIR}"

echo "[dump-logs] writing to ${OUT_DIR}"

# Plugwerk server + Postgres logs from docker compose. Run from the repo root
# where docker-compose.yml lives; tolerate the case where compose isn't running.
if command -v docker >/dev/null 2>&1; then
  docker compose ps                  >"${OUT_DIR}/compose-ps.txt"      2>&1 || true
  docker compose logs --no-color     >"${OUT_DIR}/compose-all.log"     2>&1 || true
  docker compose logs --no-color plugwerk-server >"${OUT_DIR}/plugwerk-server.log" 2>&1 || true
  docker compose logs --no-color postgres        >"${OUT_DIR}/postgres.log"        2>&1 || true
fi

# App logs written by the IT scripts (CLI stdout, Spring bootRun.log).
for f in cli-stdout.log cli-stderr.log bootRun.log app.pid; do
  if [[ -f "${f}" ]]; then
    cp "${f}" "${OUT_DIR}/" || true
  fi
done

# Environment dump (no secrets — only what the test assumed about the box).
{
  echo "## uname"
  uname -a
  echo "## java"
  java -version 2>&1 || true
  echo "## docker"
  docker --version 2>&1 || true
  docker compose version 2>&1 || true
  echo "## listening ports (8080/8081)"
  (ss -tln 2>/dev/null || netstat -tln 2>/dev/null) | grep -E ':(8080|8081)\s' || true
} >"${OUT_DIR}/env.txt" 2>&1

echo "[dump-logs] done"
