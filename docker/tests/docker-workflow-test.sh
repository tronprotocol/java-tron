#!/bin/bash
# GitHub Actions expressions are intentionally matched as literal text.
# shellcheck disable=SC2016
set -euo pipefail

test_dir=$(cd -- "$(dirname -- "$0")" >/dev/null 2>&1 && pwd)
repository_root=$(cd -- "$test_dir/../.." >/dev/null 2>&1 && pwd)
workflow="$repository_root/.github/workflows/docker.yml"
config_path="framework/src/main/resources/config.conf"

if [ "$(grep -Fxc -- "      - '$config_path'" "$workflow" || true)" -ne 1 ]; then
  echo "Docker CI push paths do not include $config_path exactly once." >&2
  exit 1
fi

if ! grep -Eq -- "^[[:space:]]+.*${config_path//./\\.}.*\\)$" "$workflow"; then
  echo "Docker CI's pull-request selector does not rebuild images for $config_path." >&2
  exit 1
fi

assert_job_needs_changes_only() {
  local job="$1"

  if ! awk -v job="$job" '
    $0 == "  " job ":" { in_job = 1; next }
    in_job && /^  [^[:space:]]/ { exit }
    in_job && $0 == "    needs: changes" { found = 1 }
    END { exit !found }
  ' "$workflow"; then
    echo "Docker CI job $job must depend only on change analysis." >&2
    exit 1
  fi
}

assert_job_needs_changes_only build-amd64
assert_job_needs_changes_only build-arm64
assert_job_needs_changes_only remote-build-amd64
assert_job_needs_changes_only remote-build-arm64

assert_selector_case_sets() {
  local case_label="$1"
  local assignment="$2"

  if ! awk -v label="$case_label)" -v assignment="$assignment" '
    {
      normalized = $0
      sub(/^[[:space:]]+/, "", normalized)
    }
    normalized == label {
      in_case = 1
      found_case = 1
      next
    }
    in_case && /^[[:space:]]+;;$/ {
      in_case = 0
      exit
    }
    in_case && normalized == assignment {
      found_assignment = 1
    }
    END { exit !(found_case && found_assignment) }
  ' "$workflow"; then
    echo "Docker CI selector case $case_label must set $assignment." >&2
    exit 1
  fi
}

assert_job_contains() {
  local job="$1"
  local expected="$2"

  if ! awk -v job="$job" -v expected="$expected" '
    $0 == "  " job ":" { in_job = 1; next }
    in_job && /^  [^[:space:]]/ { exit }
    in_job && index($0, expected) { found = 1 }
    END { exit !found }
  ' "$workflow"; then
    echo "Docker CI job $job does not contain: $expected" >&2
    exit 1
  fi
}

assert_selector_case_sets schedule 'source_mode=remote'
assert_selector_case_sets schedule 'remote_source_ref=master'
assert_selector_case_sets workflow_dispatch 'remote_source_ref="$REF_NAME"'
assert_selector_case_sets pull_request 'remote_source_ref=master'
assert_selector_case_sets push 'remote_source_ref=master'
assert_selector_case_sets docker/Dockerfile 'remote_amd64=true'
assert_selector_case_sets docker/arm64/Dockerfile 'remote_arm64=true'
assert_selector_case_sets 'docker/.dockerignore|.github/workflows/docker.yml' 'remote_amd64=true'
assert_selector_case_sets 'docker/.dockerignore|.github/workflows/docker.yml' 'remote_arm64=true'

if [ "$(grep -Fxc -- '                remote_amd64=true' "$workflow" || true)" -ne 2 ] \
  || [ "$(grep -Fxc -- '                remote_arm64=true' "$workflow" || true)" -ne 2 ]; then
  echo "Only Dockerfile and shared remote-context changes should request additional remote builds." >&2
  exit 1
fi

if [ "$(grep -Fxc -- '      remote_source_ref: ${{ steps.select.outputs.remote_source_ref }}' "$workflow" || true)" -ne 1 ]; then
  echo "Docker CI must expose the selected remote source ref exactly once." >&2
  exit 1
fi
if [ "$(grep -Fxc -- '      remote_amd64: ${{ steps.select.outputs.remote_amd64 }}' "$workflow" || true)" -ne 1 ] \
  || [ "$(grep -Fxc -- '      remote_arm64: ${{ steps.select.outputs.remote_arm64 }}' "$workflow" || true)" -ne 1 ]; then
  echo "Docker CI must expose both additional remote-build selectors." >&2
  exit 1
fi
if [ "$(grep -Fxc -- '          echo "remote_amd64=$remote_amd64" >> "$GITHUB_OUTPUT"' "$workflow" || true)" -ne 1 ] \
  || [ "$(grep -Fxc -- '          echo "remote_arm64=$remote_arm64" >> "$GITHUB_OUTPUT"' "$workflow" || true)" -ne 1 ] \
  || [ "$(grep -Fxc -- '          echo "remote_source_ref=$remote_source_ref" >> "$GITHUB_OUTPUT"' "$workflow" || true)" -ne 1 ]; then
  echo "Docker CI selector must publish all additional remote-build outputs." >&2
  exit 1
fi
if [ "$(grep -Fxc -- '          REF_NAME: ${{ github.ref_name }}' "$workflow" || true)" -ne 1 ]; then
  echo "Docker CI must pass the selected dispatch ref into the selector exactly once." >&2
  exit 1
fi
if [ "$(grep -Fxc -- '            SOURCE_REF=${{ needs.changes.outputs.remote_source_ref }}' "$workflow" || true)" -ne 4 ]; then
  echo "Every remote build must consume the selected remote source ref." >&2
  exit 1
fi
if grep -Fq -- 'SOURCE_REF=${{ github.ref_name }}' "$workflow"; then
  echo "Remote builds must not derive their source ref directly from the workflow trigger ref." >&2
  exit 1
fi

assert_job_contains remote-build-amd64 "if: needs.changes.outputs.remote_amd64 == 'true'"
assert_job_contains remote-build-amd64 'file: docker/Dockerfile'
assert_job_contains remote-build-amd64 'target: remote'
assert_job_contains remote-build-amd64 'no-cache-filters: remote-builder'
assert_job_contains remote-build-amd64 'run: docker run --rm --env JAVA_OPTS=-version "$IMAGE"'
assert_job_contains remote-build-amd64 'run: bash docker/tests/vmoptions-test.sh "$IMAGE"'
assert_job_contains remote-build-amd64 'run: bash docker/tests/docker-sh-run-smoke.sh "$IMAGE"'
assert_job_contains remote-build-arm64 "if: needs.changes.outputs.remote_arm64 == 'true'"
assert_job_contains remote-build-arm64 'file: docker/arm64/Dockerfile'
assert_job_contains remote-build-arm64 'target: remote'
assert_job_contains remote-build-arm64 'no-cache-filters: remote-builder'
assert_job_contains remote-build-arm64 'run: docker run --rm --env JAVA_OPTS=-version "$IMAGE"'
assert_job_contains remote-build-arm64 'run: bash docker/tests/vmoptions-test.sh "$IMAGE"'
assert_job_contains remote-build-arm64 'run: bash docker/tests/docker-sh-run-smoke.sh "$IMAGE"'
assert_job_contains gate 'REMOTE_AMD64_REQUIRED: ${{ needs.changes.outputs.remote_amd64 }}'
assert_job_contains gate 'REMOTE_AMD64_RESULT: ${{ needs.remote-build-amd64.result }}'
assert_job_contains gate 'REMOTE_ARM64_REQUIRED: ${{ needs.changes.outputs.remote_arm64 }}'
assert_job_contains gate 'REMOTE_ARM64_RESULT: ${{ needs.remote-build-arm64.result }}'
assert_job_contains gate 'require_success "$REMOTE_AMD64_REQUIRED" "$REMOTE_AMD64_RESULT"'
assert_job_contains gate 'require_success "$REMOTE_ARM64_REQUIRED" "$REMOTE_ARM64_RESULT"'
assert_job_contains gate 'if: always()'

if ! grep -Fq -- "group: docker-\${{ github.workflow }}-\${{ github.event_name == 'schedule' && 'schedule' || github.event.pull_request.number || github.ref }}" "$workflow"; then
  echo "Scheduled Docker CI must use a concurrency group independent of default-branch pushes." >&2
  exit 1
fi
if ! grep -Fq -- 'needs: [changes, script-check, build-amd64, build-arm64, remote-build-amd64, remote-build-arm64]' "$workflow"; then
  echo "Docker CI gate must wait for the additional remote builds." >&2
  exit 1
fi

if [ "$(grep -Fxc -- '        uses: docker/setup-buildx-action@v4' "$workflow" || true)" -ne 4 ]; then
  echo "All local/full and additional remote architecture jobs must set up the official Docker Buildx action." >&2
  exit 1
fi
if [ "$(grep -Fxc -- '        uses: docker/build-push-action@v7' "$workflow" || true)" -ne 6 ]; then
  echo "All local and remote builds must use build-push-action." >&2
  exit 1
fi
if [ "$(grep -Fc -- 'cache-from: type=gha,scope=java-tron-' "$workflow" || true)" -ne 6 ] \
  || [ "$(grep -Fc -- 'cache-to: type=gha,mode=max,scope=java-tron-' "$workflow" || true)" -ne 6 ]; then
  echo "Each local and remote architecture build must use the architecture/source-specific GHA cache scope." >&2
  exit 1
fi
if [ "$(grep -Fxc -- '          no-cache-filters: remote-builder' "$workflow" || true)" -ne 4 ]; then
  echo "Remote builds must bypass cache for the mutable source-builder stage." >&2
  exit 1
fi

echo "Docker CI selection, parallelism, and Buildx cache tests passed"
