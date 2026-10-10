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

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
BASE_DIR="/java-tron"
DOCKER_REPOSITORY="tronprotocol"
DOCKER_IMAGES="java-tron"
# latest or version
DOCKER_TARGET="latest"
CONTAINER_NAME="$DOCKER_REPOSITORY-$DOCKER_IMAGES"
IMAGE_REFERENCE="$DOCKER_REPOSITORY/$DOCKER_IMAGES:$DOCKER_TARGET"

HOST_API_BIND_ADDRESS="127.0.0.1"
HOST_HTTP_PORT=8090
HOST_RPC_PORT=50051
HOST_LISTEN_PORT=18888

DOCKER_HTTP_PORT=8090
DOCKER_RPC_PORT=50051
DOCKER_LISTEN_PORT=18888

PRIVATE_HTTP_PORT=16667

VOLUME=$(pwd)
CONFIG_DIR="$VOLUME/config"
OUTPUT_DIRECTORY="$VOLUME/output-directory"

BUNDLED_CONFIG_FILE="$BASE_DIR/config.conf"
PRIVATE_NET_CONFIG_FILE="private_net_config.conf"
PRIVATE_NET_CONFIG_URL="https://raw.githubusercontent.com/tronprotocol/tron-deployment/master/$PRIVATE_NET_CONFIG_FILE"

LOG_FILE="$BASE_DIR/logs/tron.log"

JAVA_TRON_DOCKER_URL="https://raw.githubusercontent.com/tronprotocol/java-tron/develop/docker"

if ! command -v docker >/dev/null 2>&1; then
  echo "docker is required but was not found" >&2
  exit 1
fi

if ! docker info >/dev/null 2>&1; then
  echo "unable to connect to the Docker daemon" >&2
  exit 1
fi

docker_container_exists() {
  docker container inspect "$CONTAINER_NAME" >/dev/null 2>&1
}

docker_image_exists() {
  docker image inspect "$IMAGE_REFERENCE" >/dev/null 2>&1
}

download_file() {
  local source_url=$1
  local destination=$2
  local failure_message=$3
  local missing_tool_message=$4
  local empty_file_message=$5
  local error_fd=$6
  local -a download_command

  if command -v curl >/dev/null 2>&1; then
    download_command=(curl --fail --silent --show-error --location
      --output "$destination" "$source_url")
  elif command -v wget >/dev/null 2>&1; then
    download_command=(wget --quiet --output-document="$destination" "$source_url")
  else
    echo "$missing_tool_message" >&"$error_fd"
    return 1
  fi

  if ! "${download_command[@]}"; then
    echo "$failure_message" >&"$error_fd"
    return 1
  fi

  if [[ ! -s "$destination" ]]; then
    echo "$empty_file_message" >&"$error_fd"
    return 1
  fi
}

download_private_config() {
  local config_file=$1
  local temp_file

  if ! mkdir -p "$CONFIG_DIR"; then
    echo "run: failed to create configuration directory: $CONFIG_DIR" >&2
    return 1
  fi

  if ! temp_file=$(mktemp "$CONFIG_DIR/.private_net_config.conf.XXXXXX"); then
    echo "run: failed to create a temporary configuration file" >&2
    return 1
  fi

  if ! download_file "$PRIVATE_NET_CONFIG_URL" "$temp_file" \
      "run: failed to download private network configuration" \
      "run: curl or wget is required to download the private network configuration" \
      "run: downloaded private network configuration is empty" 2; then
    rm -f "$temp_file"
    return 1
  fi

  if ! chmod 644 "$temp_file" || ! mv -f "$temp_file" "$config_file"; then
    rm -f "$temp_file"
    echo "run: failed to save private network configuration: $config_file" >&2
    return 1
  fi

  echo "private network configuration saved to $config_file"
}

require_run_option_value() {
  local option=$1

  if [[ $# -lt 2 || -z "$2" ]]; then
    echo "run: $option requires a value" >&2
    return 1
  fi
}

require_no_args() {
  local command=$1
  shift

  if [[ $# -gt 0 ]]; then
    echo "$command: does not accept arguments: $*" >&2
    return 1
  fi
}

is_output_volume() {
  local destination=$1
  local segment
  local normalized=""

  # Docker accepts destination, source:destination, or source:destination:options.
  if [[ "$destination" == *:* ]]; then
    destination=${destination#*:}
    destination=${destination%%:*}
  fi
  [[ "$destination" == /* ]] || return 1

  # Normalize the container path without resolving it on the host filesystem.
  destination="$destination/"
  while [[ -n "$destination" ]]; do
    segment=${destination%%/*}
    destination=${destination#*/}
    case "$segment" in
      "" | .) ;;
      ..) normalized=${normalized%/*} ;;
      *) normalized="$normalized/$segment" ;;
    esac
  done

  [[ "$normalized" == "/java-tron/output-directory" ]]
}

run() {
  local -a volume_args=()
  local -a port_args=()
  local -a tron_args=()
  local network="main"
  local network_config=""
  local update_config=false
  local has_output_volume=false

  while [[ $# -gt 0 ]]; do
    case "$1" in
      -v)
        require_run_option_value "$@" || return 1
        volume_args+=(-v "$2")
        if is_output_volume "$2"; then
          has_output_volume=true
        fi
        shift 2
        ;;
      -p)
        require_run_option_value "$@" || return 1
        port_args+=(-p "$2")
        shift 2
        ;;
      -c)
        require_run_option_value "$@" || return 1
        tron_args+=(-c "$2")
        shift 2
        ;;
      --net)
        require_run_option_value "$@" || return 1
        network=$2
        shift 2
        ;;
      --update-config)
        require_run_option_value "$@" || return 1
        if [[ "$2" != "true" && "$2" != "false" ]]; then
          echo "run: --update-config expects true or false" >&2
          return 1
        fi
        update_config=$2
        shift 2
        ;;
      *)
        echo "run: arg $1 is not a valid parameter" >&2
        return 1
        ;;
    esac
  done

  if [[ "$network" = "private" ]]; then
    network_config="$CONFIG_DIR/$PRIVATE_NET_CONFIG_FILE"
  elif [[ "$network" != "main" ]]; then
    echo "run: unsupported network '$network'; expected main or private" >&2
    return 1
  fi

  if [[ ${#tron_args[@]} -gt 0 && -n "$network_config" ]]; then
    echo "run: -c cannot be combined with --net private" >&2
    return 1
  fi

  if [[ "$update_config" = true && "$network" != "private" ]]; then
    echo "run: --update-config true is only supported with --net private" >&2
    return 1
  fi

  if docker_container_exists; then
    echo "container already exists: $CONTAINER_NAME" >&2
    echo "use --start if it is stopped, or --rm before rerunning --run" >&2
    return 1
  fi

  if ! docker_image_exists; then
    echo 'warning: no java-tron mirror image, do you need to get the mirror image?[y/n]'
    IFS= read -r need

    if [[ $need == 'y' || $need == 'yes' ]]; then
      pull || return $?
    else
      echo "warning: no mirror image found, go ahead and download a mirror."
      return 1
    fi
  fi

  if [[ -n "$network_config" ]]; then
    if [[ "$update_config" = true ]]; then
      echo "updating private network configuration from tron-deployment"
      download_private_config "$network_config" || return 1
    elif [[ ! -f "$network_config" ]]; then
      echo "private network configuration not found; downloading it from tron-deployment"
      download_private_config "$network_config" || return 1
    fi
    volume_args+=(-v "$network_config:$BUNDLED_CONFIG_FILE:ro")
  fi

  if [[ "$has_output_volume" = false ]]; then
    volume_args=(-v "$OUTPUT_DIRECTORY:/java-tron/output-directory" "${volume_args[@]}")
  fi

  if [[ ${#port_args[@]} -eq 0 ]]; then
    if [[ "$network" = "private" ]]; then
      port_args=(
        -p "$HOST_API_BIND_ADDRESS:$PRIVATE_HTTP_PORT:$PRIVATE_HTTP_PORT"
        -p "$HOST_API_BIND_ADDRESS:$HOST_RPC_PORT:$DOCKER_RPC_PORT"
      )
    else
      port_args=(
        -p "$HOST_API_BIND_ADDRESS:$HOST_HTTP_PORT:$DOCKER_HTTP_PORT"
        -p "$HOST_API_BIND_ADDRESS:$HOST_RPC_PORT:$DOCKER_RPC_PORT"
        -p "$HOST_LISTEN_PORT:$DOCKER_LISTEN_PORT"
        -p "$HOST_LISTEN_PORT:$DOCKER_LISTEN_PORT/udp"
      )
    fi
  fi

  if [[ ${#tron_args[@]} -eq 0 ]]; then
    tron_args=(-c "$BUNDLED_CONFIG_FILE")
  fi

  if [[ "$network" = "private" ]]; then
    tron_args+=(--witness)
  fi

  docker run -d --name "$CONTAINER_NAME" \
    "${volume_args[@]}" \
    "${port_args[@]}" \
    --restart always \
    "$IMAGE_REFERENCE" \
    "${tron_args[@]}"
}

build() {
  local arch="${1:-}"
  local platform
  local dockerfile_path
  local dockerfile_source
  local build_context="$SCRIPT_DIR"
  local temporary_context=""
  local build_status

  if [[ $# -gt 1 ]]; then
    echo "build: expected at most one architecture argument" >&2
    return 1
  fi

  if [[ -z "$arch" ]]; then
    if ! arch=$(docker info --format '{{.Architecture}}'); then
      echo "build: failed to determine the Docker daemon architecture" >&2
      return 1
    fi
  fi

  case "$arch" in
    amd64 | x86_64)
      platform="linux/amd64"
      dockerfile_path="$SCRIPT_DIR/Dockerfile"
      dockerfile_source="$JAVA_TRON_DOCKER_URL/Dockerfile"
      ;;
    arm64 | aarch64)
      platform="linux/arm64"
      dockerfile_path="$SCRIPT_DIR/arm64/Dockerfile"
      dockerfile_source="$JAVA_TRON_DOCKER_URL/arm64/Dockerfile"
      ;;
    *)
      echo "build: unsupported architecture: $arch" >&2
      return 1
      ;;
  esac

  if [[ ! -f "$dockerfile_path" ]]; then
    if ! temporary_context=$(mktemp -d "${TMPDIR:-/tmp}/java-tron-docker.XXXXXX"); then
      echo "build: failed to create a temporary build context" >&2
      return 1
    fi

    build_context="$temporary_context"
    dockerfile_path="$temporary_context/Dockerfile"

    echo "build files not found next to docker.sh; downloading a temporary build context"
    if ! download_file "$dockerfile_source" "$dockerfile_path" \
        "build: failed to download: $dockerfile_source" \
        "build: curl or wget is required to download build files" \
        "build: downloaded file is empty: $dockerfile_source" 2; then
      rm -rf "$temporary_context"
      return 1
    fi
  fi

  echo "docker build --platform $platform --file $dockerfile_path"
  docker build \
    --platform "$platform" \
    --file "$dockerfile_path" \
    --tag "$IMAGE_REFERENCE" \
    "$build_context"
  build_status=$?

  if [[ -n "$temporary_context" ]]; then
    rm -rf "$temporary_context"
  fi

  return "$build_status"
}

pull() {
  require_no_args pull "$@" || return 1

  echo "docker pull $IMAGE_REFERENCE"
  docker pull "$IMAGE_REFERENCE"
}

change_container_state() {
  local command_name=$1
  shift

  require_no_args "$command_name" "$@" || return 1
  if ! docker_container_exists; then
    echo "container not found: $CONTAINER_NAME" >&2
    return 1
  fi

  echo "container: $CONTAINER_NAME"
  echo "docker $command_name $CONTAINER_NAME"
  docker "$command_name" "$CONTAINER_NAME" || return $?
  docker ps
}

start() {
  change_container_state start "$@"
}

stop() {
  change_container_state stop "$@"
}

rm_container() {
  require_no_args rm "$@" || return 1

  if ! docker_container_exists; then
    echo "container not found: $CONTAINER_NAME" >&2
    return 1
  fi

  echo "container: $CONTAINER_NAME"
  echo "docker stop $CONTAINER_NAME"
  docker stop "$CONTAINER_NAME" || return $?
  echo "docker rm $CONTAINER_NAME"
  docker rm "$CONTAINER_NAME"
}

log() {
  require_no_args log "$@" || return 1

  if docker_container_exists; then
    echo "container: $CONTAINER_NAME"
    docker exec "$CONTAINER_NAME" tail -100f "$LOG_FILE"
  else
    echo "container not found: $CONTAINER_NAME" >&2
    return 1
  fi
}

command_name=${1:-}
[[ $# -eq 0 ]] || shift

case "$command_name" in
  --pull) pull "$@" ;;
  --start) start "$@" ;;
  --stop) stop "$@" ;;
  --build) build "$@" ;;
  --run) run "$@" ;;
  --rm) rm_container "$@" ;;
  --log) log "$@" ;;
  *)
    echo "arg: $command_name is not a valid parameter" >&2
    exit 1
    ;;
esac

exit $?
