#!/bin/bash
START_OPT=$1
FULL_NODE_DIR="FullNode"
FULL_NODE_VERSION="GreatVoyage-v4.3.0"

download() {
  if [ ! -d "$FULL_NODE_DIR" ]; then
    mkdir $FULL_NODE_DIR
    cd $FULL_NODE_DIR
  elif [ -d "$FULL_NODE_DIR" ]; then
    cd $FULL_NODE_DIR
  fi

  if type wget >/dev/null 2>&1; then
    config_file=$(wget https://raw.githubusercontent.com/tronprotocol/java-tron/develop/framework/src/main/resources/config.conf)
    sh_file=$(wget https://raw.githubusercontent.com/tronprotocol/java-tron/develop/start.sh)
    full_node=$(wget https://github.com/tronprotocol/java-tron/releases/download/$FULL_NODE_VERSION/FullNode.jar)

    if [[ $full_node == 0 ]]; then
      echo 'download FullNode.jar success'
    fi

    if [[ $sh_file == 0 ]]; then
      chmod u+rwx start.sh
      echo 'download start.sh success'
    fi

    if [[ $config_file == 0 ]]; then
      echo 'download config success'
    fi
  else
    echo 'no exists wget, make sure the system can use the "wget" command'
  fi
}

cloneCode() {
  if type git >/dev/null 2>&1; then
    git_clone=$(git clone -b master git@github.com:tronprotocol/java-tron.git)
    if [[ git_clone == 0 ]]; then
      echo 'git clone java-tron success'
    fi
  else
    echo 'no exists git, make sure the system can use the "git" command'
  fi
}

cloneBuild() {
  clone=`cloneCode`
  if [[ $clone == 0 ]]; then
    cd 'java-tron'
    echo "build java-tron"
    sh gradlew clean build -x test
  fi
}

if [[ $START_OPT == '-download' ]]; then
  download
  sh start.sh
elif [[ $START_OPT == '-clone' ]]; then
  cloneCode
elif [[ $START_OPT == '-cb' ]]; then
  cloneBuild
else
  download
fi