#!/usr/bin/env bash
# Helper script for manually regenerating Java sources from .proto files.
# The recommended approach is `./gradlew build`, which compiles protos via the
# Gradle protobuf plugin automatically. Use this script only if you need to run
# protoc directly (e.g., for a single proto file during development).
#
# Requirements: protoc v3.x installed and available on PATH.
# Proto sources live under protocol/src/main/protos/.

set -e

PROTO_SRC="protocol/src/main/protos"
JAVA_OUT="protocol/src/main/java"

# Compile core protos (Tron.proto + all contract protos)
protoc \
  -I="$PROTO_SRC" \
  -I="$PROTO_SRC/core" \
  -I="$PROTO_SRC/core/contract" \
  --java_out="$JAVA_OUT" \
  "$PROTO_SRC/core/Tron.proto" \
  "$PROTO_SRC/core/contract/"*.proto

# Compile API proto
protoc \
  -I="$PROTO_SRC" \
  -I="$PROTO_SRC/core" \
  -I="$PROTO_SRC/api" \
  --java_out="$JAVA_OUT" \
  "$PROTO_SRC/api/api.proto"

echo "Proto compilation complete. Output: $JAVA_OUT"
