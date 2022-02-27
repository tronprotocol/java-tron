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
#DOCKER_TARGET="latest"

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
LOG_FILE="/logs/tron.log"

if test docker; then
    docker -v
fi

docker_ps() {
  containerID=`docker ps -a | grep "$DOCKER_REPOSITORY-$DOCKER_IMAGES" | awk '{print $1}'`
  cid=$containerID
}

docker_image() {
  image_name=`docker images |grep "$DOCKER_REPOSITORY/$DOCKER_IMAGES" |awk {'print $1'}| awk 'NR==1'`
  image=$image_name
}

run() {
  docker_image

  if [ ! $image ] ; then
    echo 'warning: no java-tron mirror image, do you need to get the mirror image?'
    read need
    if [ $need ]; then
      pull
    fi
  fi

  if [[ ! -d 'config' || ! -f "config/$CONFIG_FILE" ]]; then
    mkdir -p config
    if test curl; then
      curl -o config/$CONFIG_FILE -LO https://raw.githubusercontent.com/tronprotocol/tron-deployment/master/main_net_config.conf
    elif test wget; then
      wget -P config/ https://raw.githubusercontent.com/tronprotocol/tron-deployment/master/main_net_config.conf
    fi
  fi
  docker run -d -it --name "$DOCKER_REPOSITORY-$DOCKER_IMAGES" \
      -v $CONFIG:/java-tron/config \
      -v $OUTPUT_DIRECTORY:/java-tron/output-directory \
      -p $HOST_HTTP_PORT:$DOCKER_HTTP_PORT \
      -p $HOST_RPC_PORT:$DOCKER_RPC_PORT \
      -p $HOST_LISTEN_PORT:$DOCKER_LISTEN_PORT \
      --restart always \
      "$DOCKER_REPOSITORY/$DOCKER_IMAGES:$DOCKER_TARGET" \
      -c "$CONFIG_PATH$CONFIG_FILE"
}

build() {
  echo 'docker build'
  if [ ! -f "Dockerfile" ]; then
    echo 'warning: Dockerfile not exists.'
    exit
  fi
  docker build -t "$DOCKER_REPOSITORY/$DOCKER_IMAGES:$DOCKER_TARGET" .
}

pull() {
  echo 'docker pull'
  docker pull "$DOCKER_REPOSITORY/$DOCKER_IMAGES:$DOCKER_TARGET"
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
    echo "container not exists!"
  fi
}

log() {
  docker_ps
  if [ $cid ]; then
    echo "containerID: $cid"
    echo "docker rm $cid"
    docker exec -it 1e98eb9695b0  tail -222f /java-tron/logs/tron.log
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
