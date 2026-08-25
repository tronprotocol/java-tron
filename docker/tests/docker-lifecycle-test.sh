#!/bin/bash
set -euo pipefail

TEST_DIR=$(cd -- "$(dirname -- "$0")" >/dev/null 2>&1 && pwd)
REPOSITORY_ROOT=$(cd -- "$TEST_DIR/../.." >/dev/null 2>&1 && pwd)
DOCKER_SCRIPT="$REPOSITORY_ROOT/docker/docker.sh"
TEST_TMP=$(mktemp -d)
MOCK_BIN="$TEST_TMP/bin"
DOCKER_LOG="$TEST_TMP/docker-args"

cleanup() {
  rm -rf "$TEST_TMP"
}
trap cleanup EXIT

mkdir -p "$MOCK_BIN"

cat > "$MOCK_BIN/docker" <<'MOCK_DOCKER'
#!/bin/bash
set -euo pipefail

printf '%s\n' "$*" >> "$DOCKER_MOCK_LOG"

case "${1:-}" in
  --version)
    echo "Docker version 23.0.0, build mock"
    ;;
  container)
    case "${2:-}" in
      inspect)
        if [ "${MOCK_QUERY_STATUS:-0}" -ne 0 ]; then
          exit "$MOCK_QUERY_STATUS"
        fi

        requested_name="${@: -1}"
        if [ "${MOCK_CONTAINER_EXISTS:-false}" = true ] &&
          [ "$requested_name" = "${MOCK_CONTAINER_NAME:-tronprotocol-java-tron}" ]; then
          printf 'deadbeef /%s\n' "$requested_name"
        else
          exit 1
        fi
        ;;
      ls)
        if [ "${3:-}" != "-aq" ]; then
          echo "Unexpected docker container ls command: $*" >&2
          exit 99
        fi
        if [ "${MOCK_QUERY_STATUS:-0}" -ne 0 ]; then
          exit "$MOCK_QUERY_STATUS"
        fi
        if [ "${MOCK_CONTAINER_EXISTS:-false}" = true ]; then
          echo "deadbeef"
        fi
        ;;
      *)
        echo "Unexpected docker container command: $*" >&2
        exit 99
        ;;
    esac
    ;;
  ps)
    if [ "${2:-}" = "-aq" ]; then
      if [ "${MOCK_QUERY_STATUS:-0}" -ne 0 ]; then
        exit "$MOCK_QUERY_STATUS"
      fi
      name_filter=""
      previous=""
      for argument in "$@"; do
        if [ "$previous" = "--filter" ]; then
          name_filter=${argument#name=}
        fi
        previous=$argument
      done
      if [ "${MOCK_CONTAINER_EXISTS:-false}" = true ] \
        && { [ -z "$name_filter" ] \
          || [[ "/${MOCK_CONTAINER_NAME:-tronprotocol-java-tron}" =~ $name_filter ]]; }; then
        echo "deadbeef"
      fi
    else
      exit "${MOCK_PS_STATUS:-0}"
    fi
    ;;
  start)
    exit "${MOCK_START_STATUS:-0}"
    ;;
  stop)
    exit "${MOCK_STOP_STATUS:-0}"
    ;;
  rm)
    exit "${MOCK_RM_STATUS:-0}"
    ;;
  exec)
    exit "${MOCK_EXEC_STATUS:-0}"
    ;;
  *)
    echo "Unexpected docker command: $*" >&2
    exit 99
    ;;
esac
MOCK_DOCKER

chmod +x "$MOCK_BIN/docker"

run_lifecycle() {
  local operation="$1"
  shift

  : > "$DOCKER_LOG"
  env \
    PATH="$MOCK_BIN:$PATH" \
    DOCKER_MOCK_LOG="$DOCKER_LOG" \
    "$@" \
    bash "$DOCKER_SCRIPT" "$operation"
}

run_named_lifecycle() {
  local operation="$1"
  local container_name="$2"
  shift 2

  : > "$DOCKER_LOG"
  env \
    PATH="$MOCK_BIN:$PATH" \
    DOCKER_MOCK_LOG="$DOCKER_LOG" \
    "$@" \
    bash "$DOCKER_SCRIPT" "$operation" --container-name "$container_name"
}

expect_status() {
  local expected="$1"
  shift
  local actual

  set +e
  "$@" >/dev/null 2>&1
  actual=$?
  set -e

  if [ "$actual" -ne "$expected" ]; then
    echo "Expected exit status $expected, got $actual: $*" >&2
    sed 's/^/  docker /' "$DOCKER_LOG" >&2
    exit 1
  fi
}

assert_call_count() {
  local pattern="$1"
  local expected="$2"
  local actual

  actual=$(grep -Ec -- "$pattern" "$DOCKER_LOG" || true)
  if [ "$actual" -ne "$expected" ]; then
    echo "Expected $expected calls matching '$pattern', got $actual" >&2
    sed 's/^/  docker /' "$DOCKER_LOG" >&2
    exit 1
  fi
}

expect_status 1 run_lifecycle --start \
  MOCK_CONTAINER_EXISTS=true MOCK_START_STATUS=42
assert_call_count '^container inspect --format .* tronprotocol-java-tron$' 1
assert_call_count '^ps$' 0

expect_status 1 run_lifecycle --stop \
  MOCK_CONTAINER_EXISTS=true MOCK_STOP_STATUS=43
assert_call_count '^container inspect --format .* tronprotocol-java-tron$' 1
assert_call_count '^ps$' 0

expect_status 1 run_lifecycle --start MOCK_CONTAINER_EXISTS=false
expect_status 1 run_lifecycle --stop MOCK_CONTAINER_EXISTS=false
expect_status 1 run_lifecycle --log MOCK_CONTAINER_EXISTS=false
assert_call_count '^container inspect --format .* tronprotocol-java-tron$' 1
assert_call_count '^container ls -aq$' 1

expect_status 1 run_lifecycle --start MOCK_QUERY_STATUS=51
assert_call_count '^container inspect --format .* tronprotocol-java-tron$' 1
assert_call_count '^container ls -aq$' 1
assert_call_count '^start ' 0

expect_status 1 run_lifecycle --start \
  MOCK_CONTAINER_EXISTS=true MOCK_PS_STATUS=52
expect_status 1 run_lifecycle --stop \
  MOCK_CONTAINER_EXISTS=true MOCK_PS_STATUS=53

expect_status 1 run_lifecycle --log \
  MOCK_CONTAINER_EXISTS=true MOCK_EXEC_STATUS=44

expect_status 1 run_lifecycle --rm \
  MOCK_CONTAINER_EXISTS=true MOCK_RM_STATUS=45
expect_status 1 run_lifecycle --rm MOCK_CONTAINER_EXISTS=false

# Docker's name filter treats '.' as a regular-expression wildcard. Exact-name
# lookup must not operate on nodeXone when the requested container is node.one.
expect_status 0 run_named_lifecycle --stop node.one \
  MOCK_CONTAINER_EXISTS=true MOCK_CONTAINER_NAME=node.one
assert_call_count '^stop deadbeef$' 1

expect_status 1 run_named_lifecycle --stop node.one \
  MOCK_CONTAINER_EXISTS=true MOCK_CONTAINER_NAME=nodeXone
assert_call_count '^container inspect --format .* node[.]one$' 1
assert_call_count '^container ls -aq$' 1
assert_call_count '^stop ' 0

expect_status 1 run_named_lifecycle --log node.one \
  MOCK_CONTAINER_EXISTS=true MOCK_CONTAINER_NAME=nodeXone
assert_call_count '^exec ' 0

expect_status 1 run_named_lifecycle --rm node.one \
  MOCK_CONTAINER_EXISTS=true MOCK_CONTAINER_NAME=nodeXone
assert_call_count '^stop ' 0
assert_call_count '^rm ' 0

expect_status 0 run_lifecycle --start MOCK_CONTAINER_EXISTS=true
assert_call_count '^start deadbeef$' 1
assert_call_count '^ps$' 1

expect_status 0 run_lifecycle --stop MOCK_CONTAINER_EXISTS=true
assert_call_count '^stop deadbeef$' 1
assert_call_count '^ps$' 1

expect_status 0 run_lifecycle --log MOCK_CONTAINER_EXISTS=true
assert_call_count '^exec deadbeef tail -100f /java-tron/logs/tron.log$' 1
assert_call_count '^exec -' 0

expect_status 0 run_lifecycle --rm MOCK_CONTAINER_EXISTS=true
assert_call_count '^stop deadbeef$' 1
assert_call_count '^rm deadbeef$' 1

echo "docker.sh lifecycle tests passed"
