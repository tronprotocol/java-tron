#!/bin/bash

set -e

DATADIR=/app/node-data

SNAPSHOT_HOST_URL=http://3.219.199.168
SNAPSHOT_OUTPUT_FILE=snapshot.tgz

function install_packages() {
  echo "Installing required packages.."
  apk add curl aria2 tar pigz pv --no-cache
}

function custom_snapshot_url() {
  [ -z "$CUSTOM_SNAPSHOT_URL" ] && return 1

  echo "Using custom snapshot url"
  SNAPSHOT_FULL_URL="$CUSTOM_SNAPSHOT_URL"
  echo "Custom snapshot URL: $SNAPSHOT_FULL_URL"
}

function find_optimal_snapshot() {
  echo "Finding optimal snapshot.."
  latest_backup=$(curl -s $SNAPSHOT_HOST_URL | grep -o 'backup[0-9]\+' | tail -1)
  SNAPSHOT_FULL_URL="$SNAPSHOT_HOST_URL/$latest_backup/LiteFullNode_output-directory.tgz"
  echo "Optimal snapshot URL: $SNAPSHOT_FULL_URL"
}

function download_tar_snapshot() {
  echo "Downloading snapshot.."
  aria2c --auto-file-renaming=false -c -x 4 -s 4 -o $SNAPSHOT_OUTPUT_FILE $SNAPSHOT_FULL_URL
}

function extract_tar_snapshot() {
  echo "Decompressing snapshot.."
  mkdir -p snapshot
  pigz -d < $SNAPSHOT_OUTPUT_FILE | pv | tar --totals -xf - -C snapshot
  rm -r $SNAPSHOT_OUTPUT_FILE
}

# EXECUTION

if [ "${USE_SNAPSHOT}" != "true" ]; then
  echo "Not using snapshot"
  exit 0
fi

if [ -z "$SNAPSHOT_HOST_URL" ]; then
  echo "SNAPSHOT_HOST_URL has to be specified"
  exit 1
fi

if [ -d "${DATADIR}/snapshot" ]; then
  echo "Snapshot already exists"
  exit 0
fi

cd ${DATADIR}

install_packages

custom_snapshot_url || find_optimal_snapshot

download_tar_snapshot
extract_tar_snapshot
