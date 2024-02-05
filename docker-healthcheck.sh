#!/bin/sh

set -e

HOST=localhost
STATUS=$(curl -sS -X POST  http://${HOST}:8090/wallet/getblock -H "Content-type: application/json" -d '{"detail":false}' | jq '.block_header.raw_data.number|type=="number"')

if [ ${STATUS} = true ]
then
  exit 0
else
  exit 1
fi
