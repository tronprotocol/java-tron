#!/bin/bash
set -eo pipefail
shopt -s nullglob

exec "./bin/FullNode" "$@"