#!/bin/bash
#############################################################################
#
#                    GNU LESSER GENERAL PUBLIC LICENSE
#                        Version 3, 29 June 2007
#
#  Copyright (C) [2007] [TRON Foundation], Inc. <https://fsf.org/>
#  Everyone is permitted to copy and distribute verbatim copies
#  of this license document, but changing it is not allowed.
#
#
#   This version of the GNU Lesser General Public License incorporates
# the terms and conditions of version 3 of the GNU General Public
# License, supplemented by the additional permissions listed below.
#
# You can find java-tron at https://github.com/tronprotocol/java-tron/
#
##############################################################################

usage() {
  cat <<'EOF'
Usage: docker.sh COMMAND [OPTIONS]

Commands:
  --pull                         Pull an explicitly selected java-tron image
  --build [OPTIONS]              Build an image for the host architecture
  --run [OPTIONS]                Create and start a FullNode container
  --start                        Start the existing container
  --stop                         Stop the existing container
  --log                          Follow the java-tron log
  --rm                           Remove the existing container
  -h, --help                     Show this help message

Build options:
  --source local|remote          Build a host distribution or remote source
  --source-ref REF               Select a remote branch or tag
  --source-repository URL        Select a remote Git repository
  --export-context PATH          Prepare a new local build context without
                                 building an image

Common options:
  --image NAME[:TAG]             Image for --pull, --build, or --run.
                                 JAVA_TRON_IMAGE can set the same value.
                                 Build and run default to the local image;
                                 pull requires an explicit image.
  --container-name NAME          Container name for --run, --start, --stop,
                                 --log, and --rm. Default: tronprotocol-java-tron.

Run options:
  --net main|private             Select the network configuration
  --update-config true|false     Refresh the private configuration
  --data-dir PATH                Set the host runtime-data directory
  --memory LIMIT                 Set the container memory limit
  --jvm-opts "OPTIONS"           Replace docker.sh default JVM options
  -p MAPPING                     Publish a container port; repeatable
  -v MAPPING                     Add or replace a bind mount; repeatable
  -e NAME=VALUE, --env NAME=VALUE
                                  Set an environment variable; repeatable.
                                  JVM option environment variables are not allowed;
                                  use --jvm-opts instead.
  -c CONTAINER_PATH              Use a custom configuration file
  -- [FULLNODE_ARGS...]          Pass remaining arguments to FullNode
EOF
}

if [ $# -eq 0 ]; then
  usage >&2
  exit 1
fi
if [[ "$1" = "-h" || "$1" = "--help" ]]; then
  usage
  exit 0
fi

BASE_DIR="/java-tron"
DOCKER_REPOSITORY="tronprotocol"
DOCKER_IMAGES="java-tron"
CONTAINER_NAME="$DOCKER_REPOSITORY-$DOCKER_IMAGES"
BUILD_IMAGE_DEFAULT="$DOCKER_REPOSITORY/$DOCKER_IMAGES:local"
RUN_IMAGE_DEFAULT="$BUILD_IMAGE_DEFAULT"
IMAGE_OVERRIDE="${JAVA_TRON_IMAGE:-}"

HOST_HTTP_PORT=8090
HOST_RPC_PORT=50051
HOST_LISTEN_PORT=18888
HOST_HTTP_BIND_ADDRESS="127.0.0.1"
HOST_RPC_BIND_ADDRESS="127.0.0.1"

DOCKER_HTTP_PORT=8090
DOCKER_RPC_PORT=50051
DOCKER_LISTEN_PORT=18888

DOCKER_MEMORY="16g"
JAVA_TRON_UID=10001
JAVA_TRON_GID=10001
# Do not use the selected workload image as a privileged host-mount helper.
# This multi-architecture Docker Official Image is pinned by OCI index digest.
RUNTIME_INIT_IMAGE="busybox:1.37.0-musl@sha256:fc6dddc4c44b1bfe37f41cae8e67d1693828e8f42a91862816d7953e2c9d3f23"
# Helper-only defaults. The image and packaged vmoptions do not set heap size.
# JDK 8 images also receive -XX:MaxDirectMemorySize=1g at --run time.
JVM_OPTS="-Xms2g -XX:MaxRAMPercentage=60.0"

DEFAULT_DATA_DIR=$(pwd)

CONFIG_PATH="/java-tron/config/"
MAIN_NET_CONFIG_PATH="$BASE_DIR/config.conf"
CONFIG_FILE=""
PRIVATE_NET_CONFIG_FILE="private_net_config.conf"

# Preserve an existing private configuration by default. A missing or
# empty file is downloaded; use --update-config true to refresh it.
UPDATE_CONFIG=false

LOG_FILE="logs/tron.log"

JAVA_TRON_DOCKER_REPOSITORY="https://raw.githubusercontent.com/tronprotocol/java-tron/master/docker"
JAVA_TRON_SOURCE_REPOSITORY="https://github.com/tronprotocol/java-tron.git"
JAVA_TRON_SOURCE_REF="master"
PRIVATE_NET_CONFIG_URL="https://raw.githubusercontent.com/tronprotocol/tron-deployment/master/private_net_config.conf"
DOCKER_SCRIPT_DIR=$(cd -- "$(dirname -- "$0")" >/dev/null 2>&1 && pwd)

if ! command -v docker >/dev/null 2>&1; then
  echo "warning: docker must be installed, please install docker first."
  exit 1
fi
docker_version_output=$(docker --version) || exit 1
echo "$docker_version_output"
if [[ ! "$docker_version_output" =~ [Vv]ersion[[:space:]]+([0-9]+)\. ]]; then
  echo "Unable to determine the Docker version from: $docker_version_output" >&2
  exit 1
fi
if [ "${BASH_REMATCH[1]}" -lt 23 ]; then
  echo "Docker 23.0 or later is required for BuildKit target builds." >&2
  exit 1
fi

docker_ps() {
  local inspected_container
  local inspected_id
  local inspected_name

  if inspected_container=$(docker container inspect \
    --format '{{.Id}} {{.Name}}' "$CONTAINER_NAME" 2>/dev/null); then
    inspected_id=${inspected_container%% *}
    inspected_name=${inspected_container#* }
    if [ -n "$inspected_id" ] && [ "$inspected_name" = "/$CONTAINER_NAME" ]; then
      containerID=$inspected_id
      cid=$containerID
      return 0
    fi

    # Docker also accepts container-ID prefixes as inspect targets. Treat an
    # inspect result with a different name as no exact name match.
    containerID=""
    cid=""
    return 0
  fi

  # A failed inspect normally means that the exact name is absent. Verify that
  # the daemon is still queryable so lifecycle commands do not hide API errors.
  if ! docker container ls -aq >/dev/null 2>&1; then
    echo "failed to query the java-tron container" >&2
    containerID=""
    cid=""
    return 1
  fi

  containerID=""
  cid=""
}

valid_container_name() {
  [[ "$1" =~ ^[a-zA-Z0-9][a-zA-Z0-9_.-]*$ ]]
}

set_container_name() {
  local command_name="$1"
  local value="$2"

  if ! valid_container_name "$value"; then
    echo "$command_name: invalid container name: $value" >&2
    return 1
  fi
  CONTAINER_NAME=$value
}

apply_container_name_args() {
  local command_name="$1"
  shift

  while [ $# -gt 0 ]; do
    case "$1" in
      --container-name)
        if [ $# -lt 2 ]; then
          echo "$command_name: arg $1 requires a value"
          return 1
        fi
        set_container_name "$command_name" "$2" || return 1
        shift 2
        ;;
      *)
        echo "$command_name: arg $1 is not a valid parameter"
        return 1
        ;;
    esac
  done
}

image_exists() {
  docker image inspect "$1" >/dev/null 2>&1
}

selected_image() {
  if [ -n "$IMAGE_OVERRIDE" ]; then
    printf '%s\n' "$IMAGE_OVERRIDE"
    return
  fi

  case "$1" in
    build)
      printf '%s\n' "$BUILD_IMAGE_DEFAULT"
      ;;
    run)
      printf '%s\n' "$RUN_IMAGE_DEFAULT"
      ;;
    pull)
      echo "pull: no compatible default published image is configured; specify --image NAME[:TAG]" >&2
      return 1
      ;;
    *)
      echo "selected_image: unknown command $1" >&2
      return 1
      ;;
  esac
}

docker_image() {
  local ref

  ref=$(selected_image run) || return 1
  if [ -n "$ref" ] && image_exists "$ref"; then
    image=$ref
  else
    image=""
  fi
}

file_is_usable() {
  [ -f "$1" ] && [ -s "$1" ]
}

non_symlink_file_is_usable() {
  [ ! -L "$1" ] && file_is_usable "$1"
}

download_destination_is_replaceable() {
  local output="$1"

  if [ -L "$output" ] || { [ -e "$output" ] && [ ! -f "$output" ]; }; then
    echo "Download destination exists but is not a non-symbolic-link regular file: $output" >&2
    return 1
  fi
}

download_file() {
  local url="$1"
  local output="$2"
  local output_mode="${3:-}"
  local output_dir
  local temporary

  output_dir=$(dirname "$output")
  mkdir -p "$output_dir" || return 1
  download_destination_is_replaceable "$output" || return 1
  temporary=$(mktemp "${output}.tmp.XXXXXX") || return 1

  if command -v curl >/dev/null 2>&1; then
    if ! curl -fsSL -o "$temporary" "$url"; then
      rm -f "$temporary"
      return 1
    fi
  elif command -v wget >/dev/null 2>&1; then
    if ! wget -q -O "$temporary" "$url"; then
      rm -f "$temporary"
      return 1
    fi
  else
    echo "Unable to download $url: install curl or wget first."
    rm -f "$temporary"
    return 1
  fi

  if [ ! -s "$temporary" ]; then
    echo "Downloaded file is empty: $url" >&2
    rm -f "$temporary"
    return 1
  fi

  if [ -n "$output_mode" ] && ! chmod "$output_mode" "$temporary"; then
    echo "Unable to set mode $output_mode on downloaded file: $output" >&2
    rm -f "$temporary"
    return 1
  fi

  if ! download_destination_is_replaceable "$output"; then
    rm -f "$temporary"
    return 1
  fi
  if ! mv -f "$temporary" "$output"; then
    rm -f "$temporary"
    return 1
  fi
  if ! non_symlink_file_is_usable "$output"; then
    echo "Downloaded file did not produce a non-empty regular destination: $output" >&2
    return 1
  fi
}

download_config() {
  local config_directory="$1"
  local config_file="$2"
  local config_url

  case "$config_file" in
    "$PRIVATE_NET_CONFIG_FILE")
      config_url=$PRIVATE_NET_CONFIG_URL
      ;;
    *)
      echo "Unsupported configuration file: $config_file" >&2
      return 1
      ;;
  esac

  echo "Downloading $config_file"
  download_file "$config_url" "$config_directory/$config_file" 0644
}

check_download_config() {
  local config_directory="$1"
  local config_file="$2"
  local config_path="$config_directory/$config_file"

  if ! non_symlink_file_is_usable "$config_path"; then
    download_destination_is_replaceable "$config_path" || return 1
    echo "$config_path is missing or empty; downloading it for the initial run."
    download_config "$config_directory" "$config_file"
  fi
}

normalize_data_directory() {
  local requested_directory="$1"
  local requested_parent
  local normalized_directory

  if [[ "$requested_directory" != /* ]]; then
    requested_directory="$(pwd)/$requested_directory"
  fi
  while [ "$requested_directory" != / ] && [[ "$requested_directory" == */ ]]; do
    requested_directory=${requested_directory%/}
  done

  if [ -e "$requested_directory" ] && [ ! -d "$requested_directory" ]; then
    echo "run: data directory is not a directory: $requested_directory" >&2
    return 1
  fi
  if [ -L "$requested_directory" ] && [ ! -d "$requested_directory" ]; then
    echo "run: data directory link does not resolve to a directory: $requested_directory" >&2
    return 1
  fi

  requested_parent=${requested_directory%/*}
  [ -n "$requested_parent" ] || requested_parent=/
  assert_trusted_path_prefixes "$requested_parent" || return 1

  secure_mkdir_p "$requested_directory" || return 1
  if [ ! -d "$requested_directory" ]; then
    echo "run: data directory is not a directory: $requested_directory" >&2
    return 1
  fi

  if ! normalized_directory=$(cd -P -- "$requested_directory" >/dev/null 2>&1 && pwd -P); then
    echo "run: failed to resolve data directory: $requested_directory" >&2
    return 1
  fi
  printf '%s\n' "$normalized_directory"
}

secure_mkdir_p() (
  local current_umask
  local secure_umask

  current_umask=$(umask)
  if [[ ! "$current_umask" =~ ^[0-7]{3,4}$ ]]; then
    echo "run: could not determine a safe directory-creation mask" >&2
    return 1
  fi
  secure_umask=$((8#$current_umask | 0077))
  printf -v secure_umask '%04o' "$secure_umask"
  umask "$secure_umask"
  mkdir -p "$1"
)

assert_trusted_path_prefixes() {
  local path="$1"
  local prefix=/
  local remainder=${path#/}
  local component
  local normalized_prefix

  while [ -n "$remainder" ]; do
    component=${remainder%%/*}
    if [ "$component" = "$remainder" ]; then
      remainder=
    else
      remainder=${remainder#*/}
    fi
    [ -n "$component" ] || continue

    if [ "$prefix" = / ]; then
      prefix="/$component"
    else
      prefix="$prefix/$component"
    fi
    if [ -d "$prefix" ]; then
      if ! normalized_prefix=$(cd -P -- "$prefix" >/dev/null 2>&1 && pwd -P); then
        echo "run: failed to resolve data directory path prefix: $prefix" >&2
        return 1
      fi
      assert_trusted_data_directory "$normalized_prefix" || return 1
    fi
  done
}

directory_owner_and_mode() {
  local directory="$1"
  local metadata

  if metadata=$(stat -f '%u %Lp' "$directory" 2>/dev/null); then
    printf '%s\n' "$metadata"
    return 0
  fi
  stat -c '%u %a' "$directory" 2>/dev/null
}

assert_trusted_data_directory() {
  local directory="$1"
  local trusted_uid
  local metadata
  local owner_uid
  local mode
  local numeric_mode

  trusted_uid=$(id -u) || {
    echo "run: failed to determine the current user ID" >&2
    return 1
  }
  if [ "$trusted_uid" = 0 ] && [[ "${SUDO_UID:-}" =~ ^[0-9]+$ ]]; then
    trusted_uid=$SUDO_UID
  fi

  while :; do
    if ! metadata=$(directory_owner_and_mode "$directory"); then
      echo "run: failed to inspect data directory path: $directory" >&2
      return 1
    fi
    owner_uid=${metadata%% *}
    mode=${metadata#* }
    if [[ ! "$owner_uid" =~ ^[0-9]+$ || ! "$mode" =~ ^[0-7]{3,4}$ ]]; then
      echo "run: received invalid ownership metadata for data directory path: $directory" >&2
      return 1
    fi
    if [ "$owner_uid" != 0 ] && [ "$owner_uid" != "$trusted_uid" ]; then
      echo "run: data directory path must be owned by root or UID $trusted_uid: $directory (owner UID $owner_uid)" >&2
      return 1
    fi
    numeric_mode=$((8#$mode))
    if ((numeric_mode & 0022)); then
      echo "run: data directory path must not be group- or other-writable: $directory (mode $mode)" >&2
      return 1
    fi

    [ "$directory" = / ] && break
    directory=${directory%/*}
    [ -n "$directory" ] || directory=/
  done
}

assert_managed_directory() {
  local directory="$1"

  if [ -L "$directory" ]; then
    echo "run: managed path must not be a symbolic link: $directory" >&2
    return 1
  fi
  if [ ! -d "$directory" ]; then
    echo "run: managed path is not a directory: $directory" >&2
    return 1
  fi
}

prepare_managed_directory() {
  local directory="$1"

  if [ -L "$directory" ]; then
    echo "run: managed path must not be a symbolic link: $directory" >&2
    return 1
  fi
  if [ -e "$directory" ] && [ ! -d "$directory" ]; then
    echo "run: managed path is not a directory: $directory" >&2
    return 1
  fi

  secure_mkdir_p "$directory" || return 1
  assert_managed_directory "$directory"
}

prepare_managed_config_directory() {
  local directory="$1"

  prepare_managed_directory "$directory" || return 1
  if ! chmod 0755 "$directory"; then
    echo "run: failed to set configuration-directory mode 0755: $directory" >&2
    return 1
  fi
  assert_managed_directory "$directory"
}

has_port_mapping() {
  local expected_port="$1"
  local expected_protocol="$2"
  local mapping
  local container_spec
  local container_port
  local protocol
  shift 2

  for mapping in "$@"; do
    [ "$mapping" = "-p" ] && continue
    container_spec="${mapping##*:}"
    protocol="tcp"
    if [[ "$container_spec" == */* ]]; then
      protocol="${container_spec##*/}"
      container_spec="${container_spec%/*}"
    fi
    container_port="$container_spec"
    if [ "$container_port" = "$expected_port" ] && [ "$protocol" = "$expected_protocol" ]; then
      return 0
    fi
  done
  return 1
}

has_volume_mount() {
  local expected_target="$1"
  local mapping
  shift

  for mapping in "$@"; do
    [ "$mapping" = "-v" ] && continue
    if [[ "$mapping" == *":$expected_target" || "$mapping" == *":$expected_target:"* ]]; then
      return 0
    fi
  done
  return 1
}

image_architecture() {
  local image_ref="$1"

  docker image inspect -f '{{.Architecture}}' "$image_ref"
}

validate_image_user() {
  local image_ref="$1"
  local image_user

  if ! image_user=$(docker image inspect -f '{{.Config.User}}' "$image_ref"); then
    echo "run: failed to inspect image user: $image_ref" >&2
    return 1
  fi
  if [ "$image_user" != "$JAVA_TRON_UID:$JAVA_TRON_GID" ]; then
    echo "run: image $image_ref must run as UID:GID $JAVA_TRON_UID:$JAVA_TRON_GID; found '${image_user:-root}'" >&2
    echo "Pull or build an updated non-root java-tron image before retrying." >&2
    return 1
  fi
}

verify_private_config_readable() {
  local image_ref="$1"
  local config_directory="$2"
  local config_file="$3"
  local container_config="$CONFIG_PATH$config_file"

  assert_managed_directory "$config_directory" || return 1
  if docker run --rm \
    --user "$JAVA_TRON_UID:$JAVA_TRON_GID" \
    --security-opt no-new-privileges \
    --network none \
    --read-only \
    --cap-drop ALL \
    --entrypoint sh \
    -v "$config_directory:/java-tron/config:ro" \
    "$image_ref" \
    -ec 'test ! -L "$1" && test -f "$1" && test -s "$1" && test -r "$1"' \
    sh "$container_config"; then
    assert_managed_directory "$config_directory"
    return
  fi

  echo "run: private configuration must be readable by java-tron UID:GID $JAVA_TRON_UID:$JAVA_TRON_GID: $config_directory/$config_file" >&2
  echo "Set the directory mode to 0755 and the public configuration-template mode to 0644, then retry." >&2
  return 1
}

append_jdk8_direct_memory() {
  local image_ref="$1"
  local architecture

  if ! architecture=$(image_architecture "$image_ref"); then
    echo "run: failed to inspect image architecture: $image_ref" >&2
    return 1
  fi

  case "$architecture" in
    amd64|386)
      jvm_opts="$jvm_opts -XX:MaxDirectMemorySize=1g"
      ;;
  esac
}

docker_namespace_mode() {
  local security_options

  if ! security_options=$(docker info --format '{{json .SecurityOptions}}'); then
    echo "run: failed to inspect Docker user-namespace configuration" >&2
    return 1
  fi

  if [[ "$security_options" == *'name=rootless'* ]]; then
    printf '%s\n' rootless
  elif [[ "$security_options" == *'name=userns'* ]]; then
    printf '%s\n' rootful-userns-remap
  else
    printf '%s\n' rootful
  fi
}

prepare_runtime_directories() {
  local image_ref="$1"
  local host_directory
  local container_directory
  local first_entry
  local host_inspection_succeeded
  local metadata
  local owner_uid
  local mode
  local numeric_mode
  local namespace_mode
  local rootful_userns_remap=false
  local directory_existed
  local -a mount_args=()
  local -a host_directories=()
  local -a container_directories=()
  local -a initialize_mount_args=()
  local -a initialize_host_directories=()
  local -a initialize_directories=()
  shift

  while [ $# -gt 0 ]; do
    host_directory="$1"
    container_directory="$2"
    shift 2

    directory_existed=false
    if [ -d "$host_directory" ] && [ ! -L "$host_directory" ]; then
      directory_existed=true
    fi
    prepare_managed_directory "$host_directory" || return 1
    host_inspection_succeeded=false
    first_entry=""
    if first_entry=$(find "$host_directory" -mindepth 1 -maxdepth 1 -print -quit 2>/dev/null); then
      host_inspection_succeeded=true
    else
      if ! metadata=$(directory_owner_and_mode "$host_directory"); then
        echo "run: failed to inspect runtime directory: $host_directory" >&2
        return 1
      fi
      owner_uid=${metadata%% *}
      mode=${metadata#* }
      if [[ ! "$owner_uid" =~ ^[0-9]+$ || ! "$mode" =~ ^[0-7]{3,4}$ ]]; then
        echo "run: received invalid metadata for runtime directory: $host_directory" >&2
        return 1
      fi
      numeric_mode=$((8#$mode))
      if ((numeric_mode & 0077)); then
        echo "run: host-unreadable runtime directory must use mode 0700: $host_directory (mode $mode)" >&2
        printf 'Stop the node and restrict it before retrying: chmod 0700 %q\n' \
          "$host_directory" >&2
        return 1
      fi

      # A previous initialization can leave a private (0700) directory owned
      # by either the runtime UID or its user-namespace-mapped host UID. The
      # host cannot enumerate it, so validate effective access from inside the
      # restricted container below without widening its mode.
      assert_managed_directory "$host_directory" || return 1
    fi

    if [ "$host_inspection_succeeded" = true ] && [ -z "$first_entry" ]; then
      if ! chmod 0700 "$host_directory"; then
        echo "run: failed to set empty runtime-directory mode 0700: $host_directory" >&2
        return 1
      fi
    elif [ "$host_inspection_succeeded" = true ] \
      && [ "$directory_existed" = true ] && [ -n "$first_entry" ]; then
      if ! metadata=$(directory_owner_and_mode "$host_directory"); then
        echo "run: failed to inspect runtime-directory mode: $host_directory" >&2
        return 1
      fi
      mode=${metadata#* }
      if [[ ! "$mode" =~ ^[0-7]{3,4}$ ]]; then
        echo "run: received invalid mode metadata for runtime directory: $host_directory" >&2
        return 1
      fi
      numeric_mode=$((8#$mode))
      if ((numeric_mode & 0077)); then
        echo "run: warning: existing non-empty runtime directory is accessible by group or other users; preserving mode $mode: $host_directory" >&2
        printf 'Restrict it while the node is stopped: chmod 0700 %q\n' "$host_directory" >&2
      fi
    fi

    mount_args+=("-v" "$host_directory:$container_directory")
    host_directories+=("$host_directory")
    container_directories+=("$container_directory")
    if [ "$host_inspection_succeeded" = true ] && [ -z "$first_entry" ]; then
      initialize_mount_args+=("-v" "$host_directory:$container_directory")
      initialize_host_directories+=("$host_directory")
      initialize_directories+=("$container_directory")
    fi
  done

  if [ ${#initialize_directories[@]} -gt 0 ]; then
    for host_directory in "${initialize_host_directories[@]}"; do
      assert_managed_directory "$host_directory" || return 1
    done

    namespace_mode=$(docker_namespace_mode) || return 1
    if [ "$namespace_mode" = rootful-userns-remap ]; then
      # Remapped root generally cannot chown a directory created by the host
      # user. Skip the privileged attempt and let the runtime-identity
      # preflight below accept correctly pre-provisioned mapped ownership.
      rootful_userns_remap=true
    else
      if ! docker run --rm \
        --pull missing \
        --user 0:0 \
        --security-opt no-new-privileges \
        --network none \
        --read-only \
        --cap-drop ALL \
        --cap-add CHOWN \
        --entrypoint chown \
        "${initialize_mount_args[@]}" \
        "$RUNTIME_INIT_IMAGE" \
        "$JAVA_TRON_UID:$JAVA_TRON_GID" \
        "${initialize_directories[@]}"; then
        echo "run: failed to initialize runtime-directory ownership" >&2
        return 1
      fi
    fi
  fi

  for host_directory in "${host_directories[@]}"; do
    assert_managed_directory "$host_directory" || return 1
  done

  if docker run --rm \
    --user "$JAVA_TRON_UID:$JAVA_TRON_GID" \
    --security-opt no-new-privileges \
    --network none \
    --read-only \
    --cap-drop ALL \
    --entrypoint sh \
    "${mount_args[@]}" \
    "$image_ref" \
    -ec '
      for path do
        test -w "$path" || exit 1
        if ! first_unwritable=$(find "$path" -mindepth 1 -maxdepth 1 ! -writable -print -quit); then
          exit 1
        fi
        test -z "$first_unwritable" || exit 1
      done
    ' sh "${container_directories[@]}"; then
    return 0
  fi

  if [ "$rootful_userns_remap" = true ]; then
    echo "run: rootful Docker userns-remap cannot automatically initialize host-user-owned runtime directories." >&2
    echo "Pre-create empty directories with the host UID:GID mapped from container $JAVA_TRON_UID:$JAVA_TRON_GID, then retry." >&2
    printf 'Affected directories:' >&2
    for host_directory in "${initialize_host_directories[@]}"; do
      printf ' %q' "$host_directory" >&2
    done
    printf '\n' >&2
    echo "See docker.md for a mapped-ID provisioning example." >&2
  fi
  echo "run: runtime directories must be writable by java-tron UID:GID $JAVA_TRON_UID:$JAVA_TRON_GID." >&2
  echo "Stop the node and migrate existing data with the same Docker daemon and user namespace before retrying." >&2
  echo "For rootless Docker or userns-remap, use the host UID:GID mapped from container $JAVA_TRON_UID:$JAVA_TRON_GID by that daemon." >&2
  echo "For rootful Docker without user-namespace remapping:" >&2
  printf '  sudo chown -R %s:%s' "$JAVA_TRON_UID" "$JAVA_TRON_GID" >&2
  for host_directory in "${host_directories[@]}"; do
    printf ' %q' "$host_directory" >&2
  done
  printf '\n' >&2
  return 1
}

run() {
  local docker_memory="$DOCKER_MEMORY"
  local jvm_opts="$JVM_OPTS"
  local jvm_opts_replaced=false
  local data_dir="$DEFAULT_DATA_DIR"
  local config_directory
  local output_directory
  local logs_directory
  local env_name
  local -a volume_args=()
  local -a port_args=()
  local -a environment_args=()
  local -a tron_args=()
  local -a fullnode_args=()
  local -a default_runtime_directories=()
  local custom_config=false
  local default_config_mount=false
  local manages_data_directory=false
  local runtime_directory_index

  while [ $# -gt 0 ]; do
    case "$1" in
      -v)
        if [ $# -lt 2 ]; then
          echo "run: arg $1 requires a value"
          return 1
        fi
        volume_args+=("-v" "$2")
        shift 2
        ;;
      -p)
        if [ $# -lt 2 ]; then
          echo "run: arg $1 requires a value"
          return 1
        fi
        port_args+=("-p" "$2")
        shift 2
        ;;
      -e|--env)
        if [ $# -lt 2 ]; then
          echo "run: arg $1 requires a value"
          return 1
        fi
        env_name="${2%%=*}"
        case "$env_name" in
          JAVA_OPTS|FULL_NODE_OPTS|JAVA_TOOL_OPTIONS|_JAVA_OPTIONS|JDK_JAVA_OPTIONS)
            echo "run: $1 $env_name is not supported; use --jvm-opts to set JVM options" >&2
            return 1
            ;;
        esac
        environment_args+=("--env" "$2")
        shift 2
        ;;
      -c)
        if [ $# -lt 2 ]; then
          echo "run: arg $1 requires a value"
          return 1
        fi
        tron_args=("-c" "$2")
        UPDATE_CONFIG=false
        custom_config=true
        shift 2
        ;;
      --net)
        if [ $# -lt 2 ]; then
          echo "run: arg $1 requires a value"
          return 1
        fi
        if [[ "$2" = "main" ]]; then
          CONFIG_FILE=""
        elif [[ "$2" = "private" ]]; then
          CONFIG_FILE=$PRIVATE_NET_CONFIG_FILE
        else
          echo "run: network $2 is not valid; expected main or private"
          return 1
        fi
        shift 2
        ;;
      --update-config)
        if [ $# -lt 2 ]; then
          echo "run: arg $1 requires a value"
          return 1
        fi
        if [[ "$2" != "true" && "$2" != "false" ]]; then
          echo "run: arg $1 must be true or false"
          return 1
        fi
        UPDATE_CONFIG=$2
        shift 2
        ;;
      --memory)
        if [ $# -lt 2 ]; then
          echo "run: arg $1 requires a value"
          return 1
        fi
        docker_memory=$2
        shift 2
        ;;
      --data-dir)
        if [ $# -lt 2 ]; then
          echo "run: arg $1 requires a value"
          return 1
        fi
        data_dir=$2
        shift 2
        ;;
      --jvm-opts)
        if [ $# -lt 2 ]; then
          echo "run: arg $1 requires a value"
          return 1
        fi
        jvm_opts=$2
        jvm_opts_replaced=true
        shift 2
        ;;
      --image)
        if [ $# -lt 2 ]; then
          echo "run: arg $1 requires a value"
          return 1
        fi
        IMAGE_OVERRIDE=$2
        shift 2
        ;;
      --container-name)
        if [ $# -lt 2 ]; then
          echo "run: arg $1 requires a value"
          return 1
        fi
        set_container_name run "$2" || return 1
        shift 2
        ;;
      --)
        shift
        fullnode_args=("$@")
        break
        ;;
      *)
        echo "run: arg $1 is not a valid parameter"
        return 1
        ;;
    esac
  done

  docker_ps || return 1
  if [ -n "$cid" ]; then
    echo "container $CONTAINER_NAME already exists (ID: $cid)." >&2
    echo "Use --start to reuse it, or --rm before creating a new container." >&2
    return 1
  fi

  docker_image || return 1

  if [ -z "$image" ]; then
    if [ -n "$IMAGE_OVERRIDE" ]; then
      echo "run: image not found: $IMAGE_OVERRIDE" >&2
      return 1
    fi
    echo "run: compatible local image not found: $RUN_IMAGE_DEFAULT" >&2
    echo "Build it with: bash docker.sh --build" >&2
    echo "Or select a compatible image with: bash docker.sh --run --image NAME[:TAG]" >&2
    return 1
  fi

  validate_image_user "$image" || return 1

  if [ "$custom_config" = false ] && [ -n "$CONFIG_FILE" ] \
    && ! has_volume_mount "/java-tron/config" "${volume_args[@]}"; then
    default_config_mount=true
  fi

  if [ "$default_config_mount" = true ] \
    || ! has_volume_mount "/java-tron/output-directory" "${volume_args[@]}" \
    || ! has_volume_mount "/java-tron/logs" "${volume_args[@]}"; then
    manages_data_directory=true
  fi

  if [ "$manages_data_directory" = true ]; then
    data_dir=$(normalize_data_directory "$data_dir") || return 1
    assert_trusted_data_directory "$data_dir" || return 1
    config_directory="$data_dir/config"
    output_directory="$data_dir/output-directory"
    logs_directory="$data_dir/logs"
  fi

  if [ "$default_config_mount" = true ]; then
    prepare_managed_config_directory "$config_directory" || return 1
  fi

  if [ "$default_config_mount" = true ]; then
    if [ "$UPDATE_CONFIG" = true ]; then
      download_config "$config_directory" "$CONFIG_FILE" || return 1
    else
      check_download_config "$config_directory" "$CONFIG_FILE" || return 1
    fi
  fi

  if [ "$default_config_mount" = true ]; then
    volume_args+=("-v" "$config_directory:/java-tron/config:ro")
    verify_private_config_readable "$image" "$config_directory" "$CONFIG_FILE" || return 1
  fi
  if ! has_volume_mount "/java-tron/output-directory" "${volume_args[@]}"; then
    default_runtime_directories+=("$output_directory" "/java-tron/output-directory")
    volume_args+=("-v" "$output_directory:/java-tron/output-directory")
  fi
  if ! has_volume_mount "/java-tron/logs" "${volume_args[@]}"; then
    default_runtime_directories+=("$logs_directory" "/java-tron/logs")
    volume_args+=("-v" "$logs_directory:/java-tron/logs")
  fi

  if [ ${#default_runtime_directories[@]} -gt 0 ]; then
    prepare_runtime_directories "$image" "${default_runtime_directories[@]}" || return 1
  fi

  if ! has_port_mapping "$DOCKER_HTTP_PORT" "tcp" "${port_args[@]}"; then
    port_args+=("-p" "$HOST_HTTP_BIND_ADDRESS:$HOST_HTTP_PORT:$DOCKER_HTTP_PORT")
  fi
  if ! has_port_mapping "$DOCKER_RPC_PORT" "tcp" "${port_args[@]}"; then
    port_args+=("-p" "$HOST_RPC_BIND_ADDRESS:$HOST_RPC_PORT:$DOCKER_RPC_PORT")
  fi
  if ! has_port_mapping "$DOCKER_LISTEN_PORT" "tcp" "${port_args[@]}"; then
    port_args+=("-p" "$HOST_LISTEN_PORT:$DOCKER_LISTEN_PORT")
  fi
  if ! has_port_mapping "$DOCKER_LISTEN_PORT" "udp" "${port_args[@]}"; then
    port_args+=("-p" "$HOST_LISTEN_PORT:$DOCKER_LISTEN_PORT/udp")
  fi

  if [ ${#tron_args[@]} -eq 0 ]; then
    if [ -n "$CONFIG_FILE" ]; then
      tron_args=("-c" "$CONFIG_PATH$CONFIG_FILE")
    else
      tron_args=("-c" "$MAIN_NET_CONFIG_PATH")
    fi
  fi

  if [ "$jvm_opts_replaced" = false ]; then
    append_jdk8_direct_memory "$image" || return 1
  fi

  if [ "$default_config_mount" = true ]; then
    assert_managed_directory "$config_directory" || return 1
  fi
  for ((runtime_directory_index=0;
       runtime_directory_index<${#default_runtime_directories[@]};
       runtime_directory_index+=2)); do
    assert_managed_directory "${default_runtime_directories[$runtime_directory_index]}" || return 1
  done

  docker run -d --name "$CONTAINER_NAME" \
    --user "$JAVA_TRON_UID:$JAVA_TRON_GID" \
    "${volume_args[@]}" \
    "${port_args[@]}" \
    --memory "$docker_memory" \
    --env "JAVA_OPTS=$jvm_opts" \
    "${environment_args[@]}" \
    --security-opt no-new-privileges \
    --restart always \
    "$image" \
    "${tron_args[@]}" \
    "${fullnode_args[@]}"
}

validate_local_image_config() {
  local config_path="$1"
  local sensitive_setting

  # config.conf is copied verbatim into the image. Recognize the supported
  # HOCON forms for settings that can directly contain signing keys or
  # service credentials, while ignoring line comments outside quoted strings.
  if ! sensitive_setting=$(awk '
    function uncomment(value, output, position, character, next_character, quoted, escaped) {
      output = ""
      quoted = 0
      escaped = 0
      for (position = 1; position <= length(value); position++) {
        character = substr(value, position, 1)
        next_character = substr(value, position + 1, 1)
        if (quoted) {
          output = output character
          if (escaped) {
            escaped = 0
          } else if (character == "\\") {
            escaped = 1
          } else if (character == "\"") {
            quoted = 0
          }
        } else if (character == "\"") {
          quoted = 1
          output = output character
        } else if (character == "#" || (character == "/" && next_character == "/")) {
          return output
        } else {
          output = output character
        }
      }
      return output
    }

    function witness_fragment_has_value(value, compacted) {
      compacted = value
      gsub(/[[:space:],]/, "", compacted)
      while (sub(/""/, "", compacted)) {
      }
      return compacted != ""
    }

    function scalar_secret_value_is_nonempty(value, remainder) {
      sub(/^[[:space:]]*/, "", value)
      if (substr(value, 1, 2) != "\"\"") {
        return 1
      }
      remainder = substr(value, 3)
      sub(/^[[:space:]]*/, "", remainder)
      return remainder != "" && substr(remainder, 1, 1) !~ /[,}]/
    }

    {
      line = uncomment($0)

      if (in_witness_list) {
        closing_bracket = index(line, "]")
        fragment = closing_bracket ? substr(line, 1, closing_bracket - 1) : line
        if (witness_fragment_has_value(fragment)) {
          sensitive_setting = "localwitness"
          exit
        }
        if (!closing_bracket) {
          next
        }
        in_witness_list = 0
        line = substr(line, closing_bracket + 1)
        remainder = line
        sub(/^[[:space:]]*/, "", remainder)
        if (remainder != "" && substr(remainder, 1, 1) !~ /[,}]/) {
          sensitive_setting = "localwitness"
          exit
        }
      }

      database_line = line
      dns_private_line = line
      dns_access_secret_line = line
      while (match(line, /(^|[[:space:]{,.])("localwitness"|localwitness)[[:space:]]*([+]?=|:)/)) {
        value = substr(line, RSTART + RLENGTH)
        sub(/^[[:space:]]*/, "", value)
        if (substr(value, 1, 1) != "[") {
          sensitive_setting = "localwitness"
          exit
        }
        value = substr(value, 2)
        closing_bracket = index(value, "]")
        fragment = closing_bracket ? substr(value, 1, closing_bracket - 1) : value
        if (witness_fragment_has_value(fragment)) {
          sensitive_setting = "localwitness"
          exit
        }
        if (!closing_bracket) {
          in_witness_list = 1
          line = ""
        } else {
          line = substr(value, closing_bracket + 1)
          remainder = line
          sub(/^[[:space:]]*/, "", remainder)
          if (remainder != "" && substr(remainder, 1, 1) !~ /[,}]/) {
            sensitive_setting = "localwitness"
            exit
          }
        }
      }

      while (match(database_line, /(^|[[:space:]{,.])("dbconfig"|dbconfig)[[:space:]]*([+]?=|:)/)) {
        value = substr(database_line, RSTART + RLENGTH)
        if (scalar_secret_value_is_nonempty(value)) {
          sensitive_setting = "event.subscribe.dbconfig"
          exit
        }
        database_line = substr(value, 3)
      }

      while (match(dns_private_line, /(^|[[:space:]{,.])("dnsPrivate"|dnsPrivate)[[:space:]]*([+]?=|:)/)) {
        value = substr(dns_private_line, RSTART + RLENGTH)
        if (scalar_secret_value_is_nonempty(value)) {
          sensitive_setting = "node.dns.dnsPrivate"
          exit
        }
        dns_private_line = substr(value, 3)
      }

      while (match(dns_access_secret_line, /(^|[[:space:]{,.])("accessKeySecret"|accessKeySecret)[[:space:]]*([+]?=|:)/)) {
        value = substr(dns_access_secret_line, RSTART + RLENGTH)
        if (scalar_secret_value_is_nonempty(value)) {
          sensitive_setting = "node.dns.accessKeySecret"
          exit
        }
        dns_access_secret_line = substr(value, 3)
      }
    }

    END {
      if (sensitive_setting != "") {
        print sensitive_setting
      }
    }
  ' "$config_path"); then
    echo "build: failed to inspect local configuration: $config_path" >&2
    return 1
  fi

  if [ -n "$sensitive_setting" ]; then
    echo "build: refusing to bake non-empty plaintext $sensitive_setting into the image: $config_path" >&2
    echo "Clear the setting and provide sensitive signing, database, or DNS credentials through a protected runtime mount." >&2
    return 1
  fi
}

# shellcheck disable=SC2329  # Called by functions that are invoked from EXIT traps.
remove_local_build_tree() {
  local target_path="$1"
  local find_root="$target_path"

  if [ ! -e "$target_path" ] && [ ! -L "$target_path" ]; then
    return 0
  fi

  # Prefix relative paths so find cannot interpret a leading dash as an
  # expression. -P prevents an archive-created symlink from redirecting chmod.
  case "$find_root" in
    /*)
      ;;
    *)
      find_root="./$find_root"
      ;;
  esac

  # ZIP directory modes are preserved by unzip. Restore owner access one
  # directory at a time, before descent, so mode 000/0500 entries can be
  # removed without following symbolic links outside the private tree.
  find -P "$find_root" -type d -exec chmod u+rwx {} \; || true

  if ! rm -rf -- "$target_path"; then
    echo "build: failed to remove private build tree: $target_path" >&2
    return 1
  fi
  if [ -e "$target_path" ] || [ -L "$target_path" ]; then
    echo "build: private build tree still exists after cleanup: $target_path" >&2
    return 1
  fi
}

write_local_build_dockerignore() {
  local output_path="$1"

  cat > "$output_path" <<'EOF'
# Remote targets do not read from the build context. Local targets accept only
# the runtime files emitted by the supported Gradle distribution plus the
# Mainnet configuration staged by docker.sh. Everything else stays excluded.
**
!java-tron/
java-tron/**
!java-tron/bin/
java-tron/bin/**
!java-tron/bin/FullNode
!java-tron/bin/FullNode.bat
!java-tron/bin/java-tron.vmoptions
!java-tron/lib/
java-tron/lib/**
!java-tron/lib/*.jar
!java-tron/config.conf
EOF
}

validate_local_distribution_tree() {
  local staging_root="$1"
  local manifest_path="$2"
  local distribution_name="java-tron-1.0.0"
  local distribution_root="$staging_root/$distribution_name"
  local entry
  local relative
  local library_name

  if [ ! -d "$distribution_root" ] || [ -L "$distribution_root" ]; then
    echo "build: the distribution does not contain a regular $distribution_name directory" >&2
    return 1
  fi

  if ! find -P "$staging_root" -mindepth 1 -print0 > "$manifest_path"; then
    echo "build: failed to inspect the extracted local distribution" >&2
    return 1
  fi

  while IFS= read -r -d '' entry; do
    relative=${entry#"$staging_root"/}

    if [ -L "$entry" ]; then
      printf 'build: refusing symbolic link in local distribution: %q\n' \
        "$relative" >&2
      return 1
    fi

    if [ -d "$entry" ]; then
      case "$relative" in
        "$distribution_name"|"$distribution_name/bin"|"$distribution_name/lib")
          ;;
        *)
          printf 'build: refusing unexpected directory in local distribution: %q\n' \
            "$relative" >&2
          return 1
          ;;
      esac
      continue
    fi

    if [ ! -f "$entry" ]; then
      printf 'build: refusing non-regular file in local distribution: %q\n' \
        "$relative" >&2
      return 1
    fi

    case "$relative" in
      "$distribution_name/bin/FullNode"|\
      "$distribution_name/bin/FullNode.bat"|\
      "$distribution_name/bin/java-tron.vmoptions")
        ;;
      "$distribution_name/lib/"*.jar)
        library_name=${relative#"$distribution_name/lib/"}
        if [ -z "$library_name" ] || [[ "$library_name" = */* ]]; then
          printf 'build: refusing unexpected library path in local distribution: %q\n' \
            "$relative" >&2
          return 1
        fi
        ;;
      *)
        printf 'build: refusing unexpected or sensitive file in local distribution: %q\n' \
          "$relative" >&2
        return 1
        ;;
    esac
  done < "$manifest_path"
}

prepare_local_build_context() (
  local source_root="$1"
  local dockerfile_path="$2"
  local context_root="$3"
  local distribution="$source_root/framework/build/distributions/java-tron-1.0.0.zip"
  local config_path="$source_root/framework/src/main/resources/config.conf"
  local distribution_staging=""
  local distribution_manifest=""
  local distribution_root

  # shellcheck disable=SC2329  # Invoked indirectly by the EXIT trap.
  cleanup_local_distribution_staging() {
    local original_status=$?
    local cleanup_status=0

    trap - EXIT
    if [ -n "$distribution_staging" ]; then
      remove_local_build_tree "$distribution_staging" || cleanup_status=$?
    fi
    if [ -n "$distribution_manifest" ]; then
      if ! rm -f -- "$distribution_manifest"; then
        echo "build: failed to remove distribution manifest: $distribution_manifest" >&2
        cleanup_status=1
      fi
    fi

    if [ "$original_status" -ne 0 ]; then
      exit "$original_status"
    fi
    if [ "$cleanup_status" -ne 0 ]; then
      exit "$cleanup_status"
    fi
    exit 0
  }
  trap cleanup_local_distribution_staging EXIT

  if ! command -v unzip >/dev/null 2>&1; then
    echo "build: unzip is required for --source local" >&2
    return 1
  fi

  if [ ! -f "$config_path" ]; then
    echo "build: local configuration does not exist: $config_path" >&2
    return 1
  fi
  if ! validate_local_image_config "$config_path"; then
    return 1
  fi

  echo "Building the java-tron distribution from local source: $source_root"
  if ! (cd -- "$source_root" && ./gradlew :framework:distZip -x test -x check --no-daemon); then
    echo "build: failed to create the local java-tron distribution" >&2
    return 1
  fi
  if [ ! -f "$distribution" ]; then
    echo "build: expected distribution does not exist: $distribution" >&2
    return 1
  fi

  distribution_staging=$(mktemp -d "$context_root/.java-tron-dist.XXXXXX") \
    || return 1
  distribution_manifest=$(mktemp "$context_root/.java-tron-dist-manifest.XXXXXX") \
    || return 1

  if ! unzip -q -o "$distribution" -d "$distribution_staging"; then
    echo "build: failed to extract $distribution" >&2
    return 1
  fi
  if ! validate_local_distribution_tree \
    "$distribution_staging" "$distribution_manifest"; then
    return 1
  fi
  distribution_root="$distribution_staging/java-tron-1.0.0"
  mv "$distribution_root" "$context_root/java-tron" || return 1

  if [ ! -x "$context_root/java-tron/bin/FullNode" ] \
    || [ ! -f "$context_root/java-tron/bin/java-tron.vmoptions" ]; then
    echo "build: the staged distribution is missing FullNode or java-tron.vmoptions" >&2
    return 1
  fi

  cp "$config_path" "$context_root/java-tron/config.conf" || return 1
  cp "$dockerfile_path" "$context_root/Dockerfile" || return 1
  write_local_build_dockerignore "$context_root/.dockerignore" || return 1
)

build_local_image() (
  local source_root="$1"
  local dockerfile_path="$2"
  local build_context

  # shellcheck disable=SC2329  # Invoked indirectly by the EXIT trap.
  cleanup_temporary_build_context() {
    local original_status=$?
    local cleanup_status=0

    trap - EXIT
    remove_local_build_tree "$build_context" || cleanup_status=$?
    if [ "$original_status" -ne 0 ]; then
      exit "$original_status"
    fi
    if [ "$cleanup_status" -ne 0 ]; then
      exit "$cleanup_status"
    fi
    exit 0
  }

  build_context=$(mktemp -d) || return 1
  trap cleanup_temporary_build_context EXIT

  prepare_local_build_context "$source_root" "$dockerfile_path" "$build_context" \
    || return 1

  echo "Building the local image from a temporary distribution-only context."
  DOCKER_BUILDKIT=1 docker build \
    --target local \
    --file "$build_context/Dockerfile" \
    -t "$(selected_image build)" \
    "$build_context"
)

export_local_build_context() (
  local source_root="$1"
  local dockerfile_path="$2"
  local build_context="$3"
  local context_created=false

  # shellcheck disable=SC2329  # Invoked indirectly by the EXIT trap.
  cleanup_failed_export() {
    local status=$?
    local cleanup_status=0

    trap - EXIT
    if [ "$status" -ne 0 ] && [ "$context_created" = true ]; then
      remove_local_build_tree "$build_context" || cleanup_status=$?
    fi
    if [ "$status" -ne 0 ]; then
      exit "$status"
    fi
    if [ "$cleanup_status" -ne 0 ]; then
      exit "$cleanup_status"
    fi
    exit 0
  }
  trap cleanup_failed_export EXIT

  if [ -e "$build_context" ] || [ -L "$build_context" ]; then
    echo "build: export context already exists: $build_context" >&2
    return 1
  fi
  if ! mkdir -m 700 -- "$build_context"; then
    echo "build: failed to create export context: $build_context" >&2
    return 1
  fi
  context_created=true

  prepare_local_build_context "$source_root" "$dockerfile_path" "$build_context" \
    || return 1

  echo "Prepared local Docker build context: $build_context"
)

build_remote_image() (
  local dockerfile_path="$1"
  local source_repository="$2"
  local source_ref="$3"
  local build_context

  build_context=$(mktemp -d) || return 1
  trap 'rm -rf "$build_context"' EXIT
  cp "$dockerfile_path" "$build_context/Dockerfile" || return 1

  echo "Building remote java-tron source '$source_ref' from $source_repository."
  echo "Local working-tree changes are not included; use --source local to include them."
  DOCKER_BUILDKIT=1 docker build \
    --pull \
    --no-cache-filter remote-builder \
    --target remote \
    --file "$build_context/Dockerfile" \
    --build-arg "SOURCE_REPOSITORY=$source_repository" \
    --build-arg "SOURCE_REF=$source_ref" \
    -t "$(selected_image build)" \
    "$build_context"
)

build() {
  local architecture
  local dockerfile_path
  local dockerfile_relative
  local script_dir
  local source_root
  local source_mode="remote"
  local source_ref="$JAVA_TRON_SOURCE_REF"
  local source_repository="$JAVA_TRON_SOURCE_REPOSITORY"
  local source_ref_set=false
  local source_repository_set=false
  local export_context=""

  while [ $# -gt 0 ]; do
    case "$1" in
      --source)
        if [ $# -lt 2 ]; then
          echo "build: arg $1 requires a value"
          return 1
        fi
        if [[ "$2" != "local" && "$2" != "remote" ]]; then
          echo "build: source $2 is not valid; expected local or remote"
          return 1
        fi
        source_mode=$2
        shift 2
        ;;
      --source-ref)
        if [ $# -lt 2 ]; then
          echo "build: arg $1 requires a value"
          return 1
        fi
        source_ref=$2
        source_ref_set=true
        shift 2
        ;;
      --source-repository)
        if [ $# -lt 2 ]; then
          echo "build: arg $1 requires a value"
          return 1
        fi
        source_repository=$2
        source_repository_set=true
        shift 2
        ;;
      --export-context)
        if [ $# -lt 2 ]; then
          echo "build: arg $1 requires a value"
          return 1
        fi
        export_context=$2
        shift 2
        ;;
      --image)
        if [ $# -lt 2 ]; then
          echo "build: arg $1 requires a value"
          return 1
        fi
        IMAGE_OVERRIDE=$2
        shift 2
        ;;
      *)
        echo "build: arg $1 is not a valid parameter"
        return 1
        ;;
    esac
  done

  if [ "$source_mode" = "local" ] && { [ "$source_ref_set" = true ] || [ "$source_repository_set" = true ]; }; then
    echo "build: --source-ref and --source-repository can only be used with --source remote"
    return 1
  fi
  if [ -n "$export_context" ] && [ "$source_mode" != "local" ]; then
    echo "build: --export-context can only be used with --source local"
    return 1
  fi

  script_dir=$DOCKER_SCRIPT_DIR
  architecture=$(uname -m)
  case "$architecture" in
    x86_64|amd64)
      dockerfile_relative="Dockerfile"
      ;;
    arm64|aarch64)
      dockerfile_relative="arm64/Dockerfile"
      ;;
    *)
      echo "Unsupported architecture: $architecture; expected x86_64, amd64, arm64, or aarch64."
      return 1
      ;;
  esac

  if [ "$source_mode" = "local" ]; then
    if [ -x "$(pwd)/gradlew" ]; then
      source_root=$(pwd)
    elif [ -x "$script_dir/gradlew" ]; then
      source_root=$script_dir
    elif [ -x "$script_dir/../gradlew" ]; then
      source_root=$(cd -- "$script_dir/.." >/dev/null 2>&1 && pwd)
    else
      echo "build: unable to find a java-tron checkout for local source"
      echo "Run this command from the repository root or use docker/docker.sh from a checkout."
      return 1
    fi

    dockerfile_path="$source_root/docker/$dockerfile_relative"
    if ! file_is_usable "$dockerfile_path"; then
      echo "build: local Dockerfile does not exist: $dockerfile_path" >&2
      return 1
    fi
    if [ -n "$export_context" ]; then
      export_local_build_context "$source_root" "$dockerfile_path" "$export_context"
    else
      build_local_image "$source_root" "$dockerfile_path"
    fi
  else
    dockerfile_path="$script_dir/$dockerfile_relative"
    if ! file_is_usable "$dockerfile_path"; then
      echo "$dockerfile_relative does not exist; downloading it."
      download_file "$JAVA_TRON_DOCKER_REPOSITORY/$dockerfile_relative" "$dockerfile_path" || return 1
    fi
    build_remote_image "$dockerfile_path" "$source_repository" "$source_ref"
  fi
}

pull() {
  local image_name

  while [ $# -gt 0 ]; do
    case "$1" in
      --image)
        if [ $# -lt 2 ]; then
          echo "pull: arg $1 requires a value"
          return 1
        fi
        IMAGE_OVERRIDE=$2
        shift 2
        ;;
      *)
        echo "pull: arg $1 is not a valid parameter"
        return 1
        ;;
    esac
  done

  image_name=$(selected_image pull) || return 1
  echo "docker pull $image_name"
  docker pull "$image_name" || return 1
  validate_image_user "$image_name"
}

start() {
  apply_container_name_args start "$@" || return 1
  docker_ps || return 1
  if [ -n "$cid" ]; then
    echo "containerID: $cid"
    echo "docker start $cid"
    docker start "$cid" || return 1
    docker ps || return 1
  else
    echo "container does not exist!" >&2
    return 1
  fi
}

stop() {
  apply_container_name_args stop "$@" || return 1
  docker_ps || return 1
  if [ -n "$cid" ]; then
    echo "containerID: $cid"
    echo "docker stop $cid"
    docker stop "$cid" || return 1
    docker ps || return 1
  else
    echo "container does not exist!" >&2
    return 1
  fi
}

rm_container() {
  apply_container_name_args rm "$@" || return 1
  stop || return 1
  echo "containerID: $cid"
  echo "docker rm $cid"
  docker rm "$cid" || return 1
  docker_ps || return 1
}

log() {
  apply_container_name_args log "$@" || return 1
  docker_ps || return 1

  if [ -n "$cid" ]; then
    echo "containerID: $cid"
    docker exec "$cid" tail -100f "$BASE_DIR/$LOG_FILE" || return 1
  else
    echo "container does not exist!" >&2
    return 1
  fi

}

case "$1" in
  --pull)
    pull "${@:2}"
    exit
    ;;
  --start)
    start "${@:2}"
    exit
    ;;
  --stop)
    stop "${@:2}"
    exit
    ;;
  --build)
    build "${@:2}"
    exit
    ;;
  --run)
    run "${@:2}"
    exit
    ;;
  --rm)
    rm_container "${@:2}"
    exit
    ;;
  --log)
    log "${@:2}"
    exit
    ;;
  *)
    echo "arg: $1 is not a valid parameter"
    exit 1
    ;;
esac
