#!/usr/bin/env bash
# Integration test for plugwerk-springboot-thymeleaf-example.
#
# The Spring host is pinned to a single namespace via plugwerk.namespace, so
# the application drives the catalog/install flow against public-ns; the
# private-ns is exercised purely through the auth-contract probe.
#
# Required env (exported by the workflow before this runs):
#   PLUGWERK_BOOTSTRAP_JWT     one-shot superadmin JWT for upload + approve
#   PUBLIC_NS_KEY              X-Api-Key for the public-ns namespace
#   PRIVATE_NS_KEY             X-Api-Key for the private-ns namespace
#   SPRINGBOOT_RUNTIME_DIR     dir holding the bootJar + plugins/ (with the SDK ZIP)
#   SPRINGBOOT_PLUGINS_DIR     dir holding the example plugin ZIPs to upload
#
# Visibility-matrix mapping (issue #12 acceptance criterion):
#   io.plugwerk.example.webapp.sysinfo -> public-ns   (host installs + renders)
#   io.plugwerk.example.webapp.env     -> private-ns  (auth contract only)

set -euo pipefail

: "${PLUGWERK_BOOTSTRAP_JWT:?must be set}"
: "${PUBLIC_NS_KEY:?must be set}"
: "${PRIVATE_NS_KEY:?must be set}"
: "${SPRINGBOOT_RUNTIME_DIR:?must be set}"
: "${SPRINGBOOT_PLUGINS_DIR:?must be set}"

PLUGWERK_BASE_URL="${PLUGWERK_SERVER_URL:-http://localhost:8080}"
APP_BASE_URL="${APP_BASE_URL:-http://localhost:8081}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# ---------------------------------------------------------------------------
# Locate artefacts.
# ---------------------------------------------------------------------------

BOOT_JAR=$(find "${SPRINGBOOT_RUNTIME_DIR}" -maxdepth 1 -name '*.jar' ! -name '*-plain.jar' | head -n1)
SYSINFO_ZIP=$(find "${SPRINGBOOT_PLUGINS_DIR}" -maxdepth 1 -name 'io.plugwerk.example.webapp.sysinfo-*.zip' | head -n1)
ENV_ZIP=$(find    "${SPRINGBOOT_PLUGINS_DIR}" -maxdepth 1 -name 'io.plugwerk.example.webapp.env-*.zip'     | head -n1)

[[ -f "${BOOT_JAR}"   ]] || { echo "[run-springboot-it] missing bootJar in ${SPRINGBOOT_RUNTIME_DIR}" >&2; exit 1; }
[[ -f "${SYSINFO_ZIP}" ]] || { echo "[run-springboot-it] missing sysinfo plugin ZIP" >&2; exit 1; }
[[ -f "${ENV_ZIP}"     ]] || { echo "[run-springboot-it] missing env plugin ZIP"     >&2; exit 1; }

SYSINFO_VERSION=$(basename "${SYSINFO_ZIP}" | sed -E 's/^io\.plugwerk\.example\.webapp\.sysinfo-(.+)\.zip$/\1/')
ENV_VERSION=$(basename     "${ENV_ZIP}"     | sed -E 's/^io\.plugwerk\.example\.webapp\.env-(.+)\.zip$/\1/')

echo "[run-springboot-it] bootJar:        ${BOOT_JAR}"
echo "[run-springboot-it] sysinfo plugin: ${SYSINFO_ZIP} (version ${SYSINFO_VERSION})"
echo "[run-springboot-it] env plugin:     ${ENV_ZIP}     (version ${ENV_VERSION})"

# ---------------------------------------------------------------------------
# 1. Upload plugins server-side.
# ---------------------------------------------------------------------------

echo
echo "=== 1. Uploading plugins to the server ==="
SYSINFO_RELEASE_ID=$(bash "${SCRIPT_DIR}/upload-plugin.sh" "${PLUGWERK_BOOTSTRAP_JWT}" public-ns  "${SYSINFO_ZIP}")
ENV_RELEASE_ID=$(bash     "${SCRIPT_DIR}/upload-plugin.sh" "${PLUGWERK_BOOTSTRAP_JWT}" private-ns "${ENV_ZIP}")

bash "${SCRIPT_DIR}/approve-release.sh" "${PLUGWERK_BOOTSTRAP_JWT}" public-ns  "${SYSINFO_RELEASE_ID}"
bash "${SCRIPT_DIR}/approve-release.sh" "${PLUGWERK_BOOTSTRAP_JWT}" private-ns "${ENV_RELEASE_ID}"

# ---------------------------------------------------------------------------
# 2. Working directory + plugins/ with the SDK ZIP next to the bootJar.
# ---------------------------------------------------------------------------

APP_WORK="$(pwd)/springboot-work"
rm -rf "${APP_WORK}"
mkdir -p "${APP_WORK}/plugins"
cp "${SPRINGBOOT_RUNTIME_DIR}/plugins/"plugwerk-client-plugin-*-pf4j.zip "${APP_WORK}/plugins/"

# ---------------------------------------------------------------------------
# 3. Start the Spring Boot app in the background, pinned to public-ns.
# ---------------------------------------------------------------------------

echo
echo "=== 2. Starting Spring Boot app on ${APP_BASE_URL} (namespace=public-ns) ==="

(
  cd "${APP_WORK}"
  PLUGWERK_SERVER_URL="${PLUGWERK_BASE_URL}" \
  PLUGWERK_NAMESPACE="public-ns" \
  PLUGWERK_API_KEY="${PUBLIC_NS_KEY}" \
  PLUGWERK_PLUGINS_DIR="${APP_WORK}/plugins" \
  SERVER_PORT="8081" \
    nohup java -jar "${BOOT_JAR}" >"${APP_WORK}/bootRun.log" 2>&1 &
  echo $! >"${APP_WORK}/app.pid"
)

APP_PID=$(cat "${APP_WORK}/app.pid")
echo "[run-springboot-it] app started, pid=${APP_PID}"

cleanup() {
  local exit_code=$?
  if [[ -f "${APP_WORK}/app.pid" ]]; then
    local pid
    pid=$(cat "${APP_WORK}/app.pid")
    if kill -0 "${pid}" 2>/dev/null; then
      echo "[run-springboot-it] stopping app pid=${pid}"
      kill "${pid}" 2>/dev/null || true
      # Wait up to 10s for graceful shutdown.
      for _ in {1..10}; do
        kill -0 "${pid}" 2>/dev/null || break
        sleep 1
      done
      kill -9 "${pid}" 2>/dev/null || true
    fi
  fi
  # Promote the bootRun.log to the workspace root so dump-logs.sh picks it up.
  cp "${APP_WORK}/bootRun.log" "$(pwd)/bootRun.log" 2>/dev/null || true
  exit "${exit_code}"
}
trap cleanup EXIT

# ---------------------------------------------------------------------------
# 4. Wait for the app to be reachable. The example app does not include the
#    Spring Boot Actuator starter, so we poll GET / (HomeController) instead.
# ---------------------------------------------------------------------------

echo
echo "=== 3. Waiting for app to become ready ==="
deadline=$(( $(date +%s) + 60 ))
while :; do
  if curl -fsS --max-time 3 -o /dev/null "${APP_BASE_URL}/" 2>/dev/null; then
    echo "[run-springboot-it] app is ready"
    break
  fi
  if (( $(date +%s) >= deadline )); then
    echo "[run-springboot-it] app did not become ready within 60s" >&2
    tail -n 100 "${APP_WORK}/bootRun.log" >&2 || true
    exit 1
  fi
  if ! kill -0 "${APP_PID}" 2>/dev/null; then
    echo "[run-springboot-it] app process died unexpectedly" >&2
    cat "${APP_WORK}/bootRun.log" >&2 || true
    exit 1
  fi
  sleep 2
done

# ---------------------------------------------------------------------------
# 5. Catalog -> install -> page render against public-ns.
# ---------------------------------------------------------------------------

echo
echo "=== 4. Catalog + install via /plugins/available + POST /plugins/install ==="

curl -fsS "${APP_BASE_URL}/plugins/available" >"available.html"
grep -q 'io.plugwerk.example.webapp.sysinfo' "available.html" || {
  echo "[run-springboot-it] FAIL: sysinfo plugin not visible in /plugins/available" >&2
  head -c 4000 "available.html" >&2
  exit 1
}
echo "[run-springboot-it] OK: sysinfo visible in catalog"

# The install endpoint redirects to /plugins/installed on success. We omit
# `-X POST` so that --data-urlencode triggers POST for the initial request
# and curl follows the 302 with a GET (as the Spring controller expects).
# `-X POST -L` would force POST on the redirect target and yield 405.
curl -fsS -L \
  "${APP_BASE_URL}/plugins/install" \
  --data-urlencode "pluginId=io.plugwerk.example.webapp.sysinfo" \
  --data-urlencode "version=${SYSINFO_VERSION}" \
  >"installed-after-install.html"

# Spring's flash attributes are consumed by the redirect target — the
# success message is rendered into /plugins/installed.
grep -q 'Successfully installed' "installed-after-install.html" || {
  echo "[run-springboot-it] FAIL: install did not produce success flash" >&2
  head -c 4000 "installed-after-install.html" >&2
  exit 1
}
echo "[run-springboot-it] OK: sysinfo installed via POST /plugins/install"

# Verify the dynamic page is rendered. The PageContribution is registered
# synchronously in the install handler, so /page/sysinfo must respond 200.
curl -fsS "${APP_BASE_URL}/page/sysinfo" >"page-sysinfo.html"
for needle in 'sysinfo' 'Java' 'OS'; do
  grep -qi "${needle}" "page-sysinfo.html" || {
    echo "[run-springboot-it] FAIL: /page/sysinfo missing '${needle}'" >&2
    head -c 4000 "page-sysinfo.html" >&2
    exit 1
  }
done
echo "[run-springboot-it] OK: /page/sysinfo renders the plugin contribution"

# ---------------------------------------------------------------------------
# 6. Auth contract on private-ns: anonymous catalog read must be 401, with key 200.
# ---------------------------------------------------------------------------

echo
echo "=== 5. Auth contract: private-ns ==="

bash "${SCRIPT_DIR}/assert-http.sh" 401 "${PLUGWERK_BASE_URL}/api/v1/namespaces/private-ns/plugins"
bash "${SCRIPT_DIR}/assert-http.sh" 200 "${PLUGWERK_BASE_URL}/api/v1/namespaces/private-ns/plugins" \
  -H "X-Api-Key: ${PRIVATE_NS_KEY}"

# Sanity: env plugin is actually in the private-ns catalog (proves the upload
# matrix worked, even though the host app doesn't reach it).
curl -fsS -H "X-Api-Key: ${PRIVATE_NS_KEY}" \
  "${PLUGWERK_BASE_URL}/api/v1/namespaces/private-ns/plugins" >"private-catalog.json"
grep -q 'io.plugwerk.example.webapp.env' "private-catalog.json" || {
  echo "[run-springboot-it] FAIL: env plugin not in private-ns catalog" >&2
  cat "private-catalog.json" >&2
  exit 1
}

echo
echo "[run-springboot-it] all assertions passed"
