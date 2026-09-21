#!/bin/bash
set -eo pipefail
shopt -s nullglob

echo "./bin/FullNode $@" > command.txt
exec "./bin/FullNode" "$@"