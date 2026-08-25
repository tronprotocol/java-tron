#!/bin/bash
set -euo pipefail

TEST_DIR=$(cd -- "$(dirname -- "$0")" >/dev/null 2>&1 && pwd)
REPOSITORY_ROOT=$(cd -- "$TEST_DIR/../.." >/dev/null 2>&1 && pwd)
TEST_TMP=$(mktemp -d "$REPOSITORY_ROOT/.dockerignore-test.XXXXXX")
CONTEXT="$TEST_TMP/context"
ARCHIVE="$TEST_TMP/context.tar"
MANIFEST="$TEST_TMP/manifest"

cleanup() {
  rm -rf "$TEST_TMP"
}
trap cleanup EXIT

mkdir -p "$CONTEXT/java-tron/bin" "$CONTEXT/java-tron/lib/nested" \
  "$CONTEXT/java-tron/Wallet" "$CONTEXT/java-tron/wallet" \
  "$CONTEXT/java-tron/output-directory"
cp "$REPOSITORY_ROOT/.dockerignore" "$CONTEXT/.dockerignore"

printf 'launcher\n' > "$CONTEXT/java-tron/bin/FullNode"
printf 'windows-launcher\n' > "$CONTEXT/java-tron/bin/FullNode.bat"
printf 'vm-options\n' > "$CONTEXT/java-tron/bin/java-tron.vmoptions"
printf 'unexpected-launcher\n' > "$CONTEXT/java-tron/bin/helper"
printf 'jar\n' > "$CONTEXT/java-tron/lib/java-tron.jar"
printf 'nested-jar\n' > "$CONTEXT/java-tron/lib/nested/hidden.jar"
printf 'mainnet-config\n' > "$CONTEXT/java-tron/config.conf"
printf 'unexpected-runtime-file\n' > "$CONTEXT/java-tron/README.txt"

printf 'private-key\n' > "$CONTEXT/java-tron/witness.key"
printf 'private-key\n' > "$CONTEXT/java-tron/witness.KEY"
printf 'private-key\n' > "$CONTEXT/java-tron/witness.key.bak"
printf 'private-key\n' > "$CONTEXT/java-tron/witness.pem"
printf 'keystore\n' > "$CONTEXT/java-tron/localwitnesskeystore.json"
printf 'private-config\n' > "$CONTEXT/java-tron/private_net_config.conf"
printf 'wallet\n' > "$CONTEXT/java-tron/Wallet/account.json"
printf 'wallet\n' > "$CONTEXT/java-tron/wallet/account.json"
printf 'node-id\n' > "$CONTEXT/java-tron/nodeId.properties"
printf 'database\n' > "$CONTEXT/java-tron/output-directory/block.data"
printf 'unrelated-root-file\n' > "$CONTEXT/source.txt"

printf '%s\n' \
  'FROM scratch' \
  'COPY . /context' \
  > "$CONTEXT/Dockerfile"

DOCKER_BUILDKIT=1 docker build \
  --file "$CONTEXT/Dockerfile" \
  --output "type=tar,dest=$ARCHIVE" \
  "$CONTEXT" >/dev/null

LC_ALL=C tar -tf "$ARCHIVE" | sed 's#^\./##' | LC_ALL=C sort > "$MANIFEST"

for expected in \
  context/java-tron/bin/FullNode \
  context/java-tron/bin/FullNode.bat \
  context/java-tron/bin/java-tron.vmoptions \
  context/java-tron/config.conf \
  context/java-tron/lib/java-tron.jar; do
  if ! grep -Fqx -- "$expected" "$MANIFEST"; then
    echo "Allowed Docker context file is missing: $expected" >&2
    sed 's/^/  /' "$MANIFEST" >&2
    exit 1
  fi
done

for rejected in \
  context/java-tron/witness.key \
  context/java-tron/witness.KEY \
  context/java-tron/witness.key.bak \
  context/java-tron/witness.pem \
  context/java-tron/localwitnesskeystore.json \
  context/java-tron/private_net_config.conf \
  context/java-tron/Wallet/account.json \
  context/java-tron/wallet/account.json \
  context/java-tron/nodeId.properties \
  context/java-tron/output-directory/block.data \
  context/java-tron/bin/helper \
  context/java-tron/lib/nested/hidden.jar \
  context/java-tron/README.txt \
  context/source.txt; do
  if grep -Fqx -- "$rejected" "$MANIFEST"; then
    echo "Sensitive or unrelated file entered the Docker context: $rejected" >&2
    exit 1
  fi
done

echo "Root Docker context filtering tests passed"
