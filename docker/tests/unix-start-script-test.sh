#!/bin/bash
# Render gradle/unixStartScript.txt the way CreateStartScripts does and check
# that FullNode arguments containing spaces reach Java as a single argv entry.
set -euo pipefail

TEST_DIR=$(cd -- "$(dirname -- "$0")" >/dev/null 2>&1 && pwd)
REPOSITORY_ROOT=$(cd -- "$TEST_DIR/../.." >/dev/null 2>&1 && pwd)
TEMPLATE="$REPOSITORY_ROOT/gradle/unixStartScript.txt"
TEST_TMP=$(mktemp -d)
trap 'rm -rf "$TEST_TMP"' EXIT

# The template escapes $ for Groovy; look for that exact CreateStartScripts form.
# shellcheck disable=SC2016
if ! grep -Fq 'APP_ARGS=`save "\${array[@]}"`' "$TEMPLATE"; then
  echo "unixStartScript.txt must quote array elements when calling save:" >&2
  grep -n 'APP_ARGS=' "$TEMPLATE" >&2 || true
  exit 1
fi

python3 - "$TEMPLATE" "$TEST_TMP/FullNode" <<'PY'
import pathlib
import re
import sys

template = pathlib.Path(sys.argv[1]).read_text()
template = template.replace("${applicationName}", "FullNode")
template = template.replace("${appHomeRelativePath}", "..")
template = template.replace("${defaultJvmOpts}", '""')
template = template.replace("${optsEnvironmentVar}", "FULLNODE_OPTS")
template = template.replace("${mainClassName}", "org.tron.program.FullNode")
template = re.sub(
    r"<% if \( appNameSystemProperty \) \{ %>.*?<% \} %>",
    "",
    template,
    flags=re.S,
)

out = []
i = 0
while i < len(template):
    if template[i] == "\\" and i + 1 < len(template):
        nxt = template[i + 1]
        if nxt in {"$", "\\"}:
            out.append(nxt)
            i += 2
            continue
    out.append(template[i])
    i += 1

pathlib.Path(sys.argv[2]).write_text("".join(out))
PY

DIST="$TEST_TMP/java-tron"
JAVA_HOME="$TEST_TMP/java-home"
ARGV_LOG="$TEST_TMP/java-argv"
mkdir -p "$DIST/bin" "$DIST/lib" "$JAVA_HOME/bin"
mv "$TEST_TMP/FullNode" "$DIST/bin/FullNode"
# shellcheck disable=SC2016
if ! grep -Fq 'APP_ARGS=`save "${array[@]}"`' "$DIST/bin/FullNode"; then
  echo "Rendered FullNode script does not quote array elements:" >&2
  grep -n 'APP_ARGS=' "$DIST/bin/FullNode" >&2 || true
  exit 1
fi
chmod +x "$DIST/bin/FullNode"
printf '%s\n' "# fixture" > "$DIST/bin/java-tron.vmoptions"
touch "$DIST/lib/java-tron.jar"

cat > "$JAVA_HOME/bin/java" <<'MOCK_JAVA'
#!/bin/bash
set -euo pipefail
: > "$JAVA_ARGV_LOG"
for argument in "$@"; do
  printf '%s\0' "$argument" >> "$JAVA_ARGV_LOG"
done
MOCK_JAVA
chmod +x "$JAVA_HOME/bin/java"

run_fullnode() {
  : > "$ARGV_LOG"
  JAVA_HOME="$JAVA_HOME" JAVA_ARGV_LOG="$ARGV_LOG" \
    "$DIST/bin/FullNode" "$@"
}

assert_java_args_after_main() {
  python3 - "$ARGV_LOG" "$@" <<'PY'
import pathlib
import sys

payload = pathlib.Path(sys.argv[1]).read_bytes()
args = payload.split(b"\0")
if args and args[-1] == b"":
    args = args[:-1]
decoded = [item.decode() for item in args]
try:
    main_index = decoded.index("org.tron.program.FullNode")
except ValueError:
    raise SystemExit("Java argv did not include the FullNode main class:\n" + "\n".join(decoded))
actual = decoded[main_index + 1 :]
expected = sys.argv[2:]
if actual != expected:
    raise SystemExit(
        "Java argv after the main class did not match.\nExpected:\n  "
        + "\n  ".join(expected)
        + "\nActual:\n  "
        + "\n  ".join(actual)
    )
PY
}

run_fullnode \
  --p2p-disable true \
  --log-config "/java-tron/log configs/logback.xml"
assert_java_args_after_main \
  --p2p-disable true \
  --log-config "/java-tron/log configs/logback.xml"

run_fullnode \
  -c /java-tron/config.conf \
  -jvm "{-Xms256m}" \
  --log-config "/java-tron/log configs/logback.xml"
assert_java_args_after_main \
  -c /java-tron/config.conf \
  --log-config "/java-tron/log configs/logback.xml"

echo "unix start script argument quoting tests passed"
