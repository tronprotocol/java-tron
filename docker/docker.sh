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

BASE_DIR="/java-tron"
DOCKER_REPOSITORY="tronprotocol"
DOCKER_IMAGES="java-tron"
# latest or version
DOCKER_TARGET="latest"

HOST_HTTP_PORT=8090
HOST_RPC_PORT=50051
HOST_LISTEN_PORT=18888

DOCKER_HTTP_PORT=8090
DOCKER_RPC_PORT=50051
DOCKER_LISTEN_PORT=18888

VOLUME=`pwd`
CONFIG="$VOLUME/config"
OUTPUT_DIRECTORY="$VOLUME/output-directory"

CONFIG_PATH="/java-tron/config/"
CONFIG_FILE="main_net_config.conf"
MAIN_NET_CONFIG_FILE="main_net_config.conf"
TEST_NET_CONFIG_FILE="test_net_config.conf"
PRIVATE_NET_CONFIG_FILE="private_net_config.conf"

# update the configuration file, if true, the configuration file will be fetched from the network every time you start
UPDATE_CONFIG=true

LOG_FILE="/logs/tron.log"

JAVA_TRON_REPOSITORY="https://raw.githubusercontent.com/tronprotocol/java-tron/develop/"
DOCKER_FILE="Dockerfile"
ENDPOINT_SHELL="docker-entrypoint.sh"

if test docker; then
  docker -v
else
  echo "warning: docker must be installed, please install docker first."
  exit
fi

docker_ps() {
  containerID=`docker ps -a | grep "$DOCKER_REPOSITORY-$DOCKER_IMAGES" | awk '{print $1}'`
  cid=$containerID
}

docker_image() {
  image_name=`docker images |grep "$DOCKER_REPOSITORY/$DOCKER_IMAGES" |awk {'print $1'}| awk 'NR==1'`
  image=$image_name
}

download_config() {
  mkdir -p config
  if test curl; then
    curl -o config/$CONFIG_FILE -LO https://raw.githubusercontent.com/tronprotocol/tron-deployment/master/$CONFIG_FILE -s
  elif test wget; then
    wget -P -q config/ https://raw.githubusercontent.com/tronprotocol/tron-deployment/master/$CONFIG_FILE
  fi
}


check_download_config() {
  if [[ ! -d 'config' || ! -f "config/$CONFIG_FILE" ]]; then
    mkdir -p config
    if test curl; then
      curl -o config/$CONFIG_FILE -LO https://raw.githubusercontent.com/tronprotocol/tron-deployment/master/$CONFIG_FILE -s
    elif test wget; then
      wget -P -q config/ https://raw.githubusercontent.com/tronprotocol/tron-deployment/master/$CONFIG_FILE
    fi
  fi
}

run() {
  docker_image

  if [ ! $image ] ; then
    echo 'warning: no java-tron mirror image, do you need to get the mirror image?[y/n]'
    read need

    if [[ $need == 'y' || $need == 'yes' ]]; then
      pull
    else
      echo "warning: no mirror image found, go ahead and download a mirror."
      exit
    fi
  fi

  volume=""
  parameter=""
  tron_parameter=""
  if [ $# -gt 0 ]; then
    while [ -n "$1" ]; do
      case "$1" in
        -v)
          volume="$volume -v $2"
          shift 2
          ;;
        -p)
          parameter="$parameter -p $2"
          shift 2
          ;;
        -c)
          tron_parameter="$tron_parameter -c $2"
          UPDATE_CONFIG=false
          shift 2
          ;;
        --net)
          if [[ "$2" = "main" ]]; then
            CONFIG_FILE=$MAIN_NET_CONFIG_FILE
          elif [[ "$2" = "test" ]]; then
            CONFIG_FILE=$TEST_NET_CONFIG_FILE
          elif [[ "$2" = "private" ]]; then
            CONFIG_FILE=$PRIVATE_NET_CONFIG_FILE
          fi
          shift 2
          ;;
        --update-config)
          UPDATE_CONFIG=$2
          shift 2
          ;;
        *)
          echo "run: arg $1 is not a valid parameter"
          exit
          ;;
      esac
    done
    if [ $UPDATE_CONFIG = true ]; then
      download_config
    fi

    if [ -z "$volume" ]; then
      volume=" -v $CONFIG:/java-tron/config -v $OUTPUT_DIRECTORY:/java-tron/output-directory"
    fi

    if [ -z "$parameter" ]; then
      parameter=" -p $HOST_HTTP_PORT:$DOCKER_HTTP_PORT -p $HOST_RPC_PORT:$DOCKER_RPC_PORT -p $HOST_LISTEN_PORT:$DOCKER_LISTEN_PORT"
    fi

    if [ -z "$tron_parameter" ]; then
      tron_parameter=" -c $CONFIG_PATH$CONFIG_FILE"
    fi

    # Using custom parameters
    docker run -d -it --name "$DOCKER_REPOSITORY-$DOCKER_IMAGES" \
        $volume \
        $parameter \
        --restart always \
        "$DOCKER_REPOSITORY/$DOCKER_IMAGES:$DOCKER_TARGET" \
        $tron_parameter
  else
    if [ $UPDATE_CONFIG = true ]; then
      download_config
    fi
    # Default parameters
    docker run -d -it --name "$DOCKER_REPOSITORY-$DOCKER_IMAGES" \
      -v $CONFIG:/java-tron/config \
      -v $OUTPUT_DIRECTORY:/java-tron/output-directory \
      -p $HOST_HTTP_PORT:$DOCKER_HTTP_PORT \
      -p $HOST_RPC_PORT:$DOCKER_RPC_PORT \
      -p $HOST_LISTEN_PORT:$DOCKER_LISTEN_PORT \
      --restart always \
      "$DOCKER_REPOSITORY/$DOCKER_IMAGES:$DOCKER_TARGET" \
      -c "$CONFIG_PATH$CONFIG_FILE"
  fi
}

build() {
  echo 'docker build'
  if [ ! -f "Dockerfile" ]; then
    echo 'warning: Dockerfile not exists.'
    if test curl; then
      DOWNLOAD_CMD="curl -LJO "
    elif test wget; then
      DOWNLOAD_CMD="wget "
    else
      echo "Dockerfile cannot be downloaded, you need to install 'curl' or 'wget'!"
      exit
    fi
    # download Dockerfile
   `$DOWNLOAD_CMD "$JAVA_TRON_REPOSITORY$DOCKER_FILE"`
   `$DOWNLOAD_CMD "$JAVA_TRON_REPOSITORY$ENDPOINT_SHELL"`
   chmod u+rwx $ENDPOINT_SHELL
  fi
  docker build -t "$DOCKER_REPOSITORY/$DOCKER_IMAGES:$DOCKER_TARGET" .
}

pull() {
  echo "docker pull $DOCKER_REPOSITORY/$DOCKER_IMAGES:$DOCKER_TARGET"
  docker pull "$DOCKER_REPOSITORY/$DOCKER_IMAGES:$DOCKER_TARGET"
}

start() {
  docker_ps
  if [ $cid ]; then
    echo "containerID: $cid"
    echo "docker stop $cid"
    docker start $cid
    docker ps
  else
    echo "container not running!"
  fi
}

stop() {
  docker_ps
  if [ $cid ]; then
    echo "containerID: $cid"
    echo "docker stop $cid"
    docker stop $cid
    docker ps
  else
    echo "container not running!"
  fi
}

rm_container() {
  stop
  if [ $cid ]; then
    echo "containerID: $cid"
    echo "docker rm $cid"
    docker rm $cid
    docker_ps
  else
    echo "image not exists!"
  fi
}

log() {
  docker_ps

  if [ $cid ]; then
    echo "containerID: $cid"
    docker exec -it $cid tail -100f $BASE_DIR/$LOG_FILE
  else
    echo "container not exists!"
  fi

}

case "$1" in
  --pull)
    pull ${@: 2}
    exit
    ;;
  --start)
    start ${@: 2}
    exit
    ;;
  --stop)
    stop ${@: 2}
    exit
    ;;
  --build)
    build ${@: 2}
    exit
    ;;
  --run)
    run ${@: 2}
    exit
    ;;
  --rm)
    rm_container ${@: 2}
    exit
    ;;
  --log)
    log ${@: 2}
    exit
    ;;
  *)
    echo "arg: $1 is not a valid parameter"
    exit
    ;;
esac
