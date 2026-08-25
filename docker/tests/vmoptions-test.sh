#!/bin/bash
set -euo pipefail

if [ "$#" -ne 1 ]; then
  echo "Usage: $0 IMAGE" >&2
  exit 1
fi

image="$1"
fixture_dir=$(mktemp -d)
trap 'rm -rf "$fixture_dir"' EXIT
vm_options_file="$fixture_dir/java-tron.vmoptions"

# Cover comments, blank lines, CRLF endings, and a quoted value containing
# spaces. Append the final option without a trailing newline. Mount the
# fixture over the image vmoptions so the test does not need a writable
# application directory.
printf "%s\r\n" \
  "# This comment must not be passed to the JVM." \
  "" \
  "-Djava.tron.vmoptions.spaced=\"value with spaces\"" \
  > "$vm_options_file"
printf "%s" \
  "-Djava.tron.vmoptions.final=\"last line without newline\"" \
  >> "$vm_options_file"

if ! output=$(docker run --rm \
  --entrypoint bash \
  -v "$vm_options_file:/java-tron/bin/java-tron.vmoptions:ro" \
  "$image" \
  -c 'JAVA_OPTS="-XshowSettings:properties -version" exec /java-tron/bin/FullNode' \
  2>&1); then
  echo "$output" >&2
  echo "FullNode failed while parsing the JVM options fixture." >&2
  exit 1
fi

assert_output() {
  local expected="$1"

  if ! grep -Fq -- "$expected" <<< "$output"; then
    echo "Missing expected JVM property: $expected" >&2
    echo "$output" >&2
    exit 1
  fi
}

assert_output "java.tron.vmoptions.spaced = value with spaces"
assert_output "java.tron.vmoptions.final = last line without newline"

echo "JVM options parsing test passed for $image"
