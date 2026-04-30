#!/usr/bin/env bash
# Assert that an HTTP request returns an expected status code. Used for the
# auth-contract checks (private namespace must return 401 without key, 200 with key).
#
# Usage: assert-http.sh <expected-status> <url> [curl-args...]
#
# Example:
#   assert-http.sh 401 http://localhost:8080/api/v1/namespaces/private-ns/plugins
#   assert-http.sh 200 http://localhost:8080/api/v1/namespaces/private-ns/plugins -H "X-Api-Key: ${KEY}"

set -euo pipefail

if [[ $# -lt 2 ]]; then
  echo "Usage: $0 <expected-status> <url> [curl-args...]" >&2
  exit 64
fi

EXPECTED="$1"
URL="$2"
shift 2

body_file=$(mktemp)
trap 'rm -f "${body_file}"' EXIT

actual=$(curl -sS -o "${body_file}" -w '%{http_code}' "${URL}" "$@")

if [[ "${actual}" == "${EXPECTED}" ]]; then
  echo "[assert-http] OK ${actual} ${URL}"
  exit 0
fi

echo "[assert-http] FAIL expected=${EXPECTED} actual=${actual} url=${URL}" >&2
echo "[assert-http] response body:" >&2
cat "${body_file}" >&2 || true
echo >&2
exit 1
