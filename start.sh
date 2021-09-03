#!/bin/bash
# build FullNode config
FULL_NODE_DIR="FullNode"
FULL_NODE_CONFIG="main_net_config.conf"
FULL_NODE_SHELL="start.sh"
JAR_NAME="FullNode.jar"
FULL_START_OPT=$(echo ${@:2})

# start service option
MAX_STOP_TIME=60
# modify this option to allow the minimum memory to be started, unit MB
ALLOW_MIN_MEMORY=8000
SPECIFY_MEMORY=0
RUN=false
UPGRADE=false

# rebuild manifest
REBUILD_MANIFEST=true
REBUILD_DIR="$PWD/output-directory/database"
REBUILD_MANIFEST_SIZE=0
REBUILD_BATCH_SIZE=80000

getLatestReleaseVersion() {
  default_version='GreatVoyage-v4.3.0'
  full_node_version=`git ls-remote --tags git@github.com:tronprotocol/java-tron.git |grep GreatVoyage- | awk -F '/' 'END{print $3}'`
  if [[ -n $full_node_version ]]; then
   echo "latest release version: $full_node_version"
   return $full_node_version
  else
   echo "default version: " $default_version
   return $default_version
  fi
}

checkVersion() {
 local_full_node_version=''
 github_release_version=getLatestReleaseVersion
 if [[ $local_full_node_version -ne $github_release_version ]]; then
  echo "local version: $local_full_node_version, remote latest version: $github_release_version"
  return $github_release_version
 else
   return $local_full_node_version
 fi
}

upgrade(latest_version) {
  if [[ -n $latest_version ]]; then
    //备份
    old_jar=$PWD/$JAR_NAME
    if [[ -f $old_jar ]];then
      mv $PWD/$JAR_NAME $PWD/$JAR_NAME'_bak'
    if

    is_download=$(wget -q https://github.com/tronprotocol/java-tron/releases/download/$latest_version/$JAR_NAME)
    if [[ $is_download == 0 ]]; then
      echo "download version $latest_version success"
    fi
  fi
}

download() {
  if type wget >/dev/null 2>&1; then
    full_node_version=getLatestReleaseVersion
    if [ ! -d "$FULL_NODE_DIR" ]; then
      echo "mkdir $FULL_NODE_DIR"
      mkdir $FULL_NODE_DIR
      cd $FULL_NODE_DIR
    elif [ -d "$FULL_NODE_DIR" ]; then
      cd $FULL_NODE_DIR
    fi
    echo 'execute download config'
    config_file=$(wget -q https://raw.githubusercontent.com/tronprotocol/tron-deployment/master/$FULL_NODE_CONFIG)
    sh_file=$(wget -q https://raw.githubusercontent.com/tronprotocol/java-tron/master/$FULL_NODE_SHELL)
    full_node=$(wget -q https://github.com/tronprotocol/java-tron/releases/download/$full_node_version/$JAR_NAME)

    if [[ $full_node == 0 ]]; then
      echo "download $JAR_NAME success"
    fi

    if [[ $sh_file == 0 ]]; then
      chmod u+rwx start.sh
      echo "download $FULL_NODE_SHELL success"
    fi

    if [[ $config_file == 0 ]]; then
      echo "download $FULL_NODE_CONFIG success"
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
  clone=$(cloneCode)
  if [[ $clone == 0 ]]; then
    cd 'java-tron'
    echo "build java-tron"
    sh gradlew clean build -x test
  fi
}

checkPid() {
  pid=$(ps -ef | grep $JAR_NAME | grep -v grep | awk '{print $2}')
  return $pid
}

stopService() {
  count=1
  while [ $count -le $MAX_STOP_TIME ]; do
    checkPid
    if [ $pid ]; then
      kill -15 $pid
      sleep 1
    else
      echo "java-tron stop"
      return
    fi
    count=$(($count + 1))
    if [ $count -eq $MAX_STOP_TIME ]; then
      kill -9 $pid
      sleep 1
    fi
  done
  sleep 5
}

checkAllowMemory() {
  if [[ $SPECIFY_MEMORY -gt 0 ]] &&
   [[ $SPECIFY_MEMORY -lt $ALLOW_MIN_MEMORY ]]; then
    echo "warn: the specified memory $SPECIFY_MEMORY MB cannot be smaller than the minimum memory $ALLOW_MIN_MEMORY MB"
    echo 'start abort'
    exit
  fi
}

setTCMalloc() {
  libtcmalloc="/usr/lib64/libtcmalloc.so"
  if [[ -f $libtcmalloc ]]; then
    export LD_PRELOAD="$libtcmalloc"
    export TCMALLOC_RELEASE_RATE=10
  else
    echo 'recommended for linux systems using tcmalloc as the default memory management tool'
  fi
}

startService() {
  setTCMalloc
  echo $(date) >>start.log
  total=$(cat /proc/meminfo | grep MemTotal | awk -F ' ' '{print $2}')
  xmx=$(echo "$total/1024/1024*0.6" | bc | awk -F. '{print $1"g"}')
  directmem=$(echo "$total/1024/1024*0.1" | bc | awk -F. '{print $1"g"}')
  logtime=$(date +%Y-%m-%d_%H-%M-%S)
  nohup java -Xms$xmx -Xmx$xmx -XX:+UseConcMarkSweepGC -XX:+PrintGCDetails -Xloggc:./gc.log \
    -XX:+PrintGCDateStamps -XX:+CMSParallelRemarkEnabled -XX:ReservedCodeCacheSize=256m -XX:+UseCodeCacheFlushing \
    -XX:MetaspaceSize=256m -XX:MaxMetaspaceSize=512m \
    -XX:MaxDirectMemorySize=$directmem -XX:+HeapDumpOnOutOfMemoryError \
    -XX:NewRatio=2 -jar \
    $JAR_NAME $FULL_START_OPT -c config.conf >>start.log 2>&1 &
  pid=$(ps -ef | grep $JAR_NAME | grep -v grep | awk '{print $2}')
  echo "start java-tron with pid $pid on $HOSTNAME"
}

rebuildManifest() {
  if [[ "$REBUILD_MANIFEST" = false ]]; then
    echo 'disable rebuild manifest!'
    return
  fi

  if [[ ! -d $REBUILD_DIR ]]; then
    echo "$REBUILD_DIR not exists, skip rebuild manifest"
    return
  fi

  ARCHIVE_JAR='ArchiveManifest.jar'
  if [[ -f $ARCHIVE_JAR ]]; then
    echo 'execute rebuild manifest.'
    java -jar $ARCHIVE_JAR -d $REBUILD_DIR -m $REBUILD_MANIFEST_SIZE -b $REBUILD_BATCH_SIZE
  else
    echo 'download the rebuild manifest plugin from the github'
    download=$(wget -q https://github.com/tronprotocol/java-tron/releases/download/GreatVoyage-v4.3.0/$ARCHIVE_JAR)
    if [[ $download == 0 ]]; then
      echo 'download success, rebuild manifest'
      java -jar $ARCHIVE_JAR $REBUILD_DIR -m $REBUILD_MANIFEST_SIZE -b $REBUILD_BATCH_SIZE
    fi
  fi
  if [[ $? == 0 ]]; then
    echo 'rebuild manifest success'
  else
    echo 'rebuild manifest fail, log in logs/archive.log'
  fi
}

restart() {
  stopService
  checkAllowMemory
  rebuildManifest
  startService
}

while [ -n "$1" ]; do
  case "$1" in
  -d)
    REBUILD_DIR=$2/database
    shift 2
    ;;
  -m)
    REBUILD_MANIFEST_SIZE=$2
    shift 2
    ;;
  -b)
    REBUILD_BATCH_SIZE=$2
    shift 2
    ;;
  --download)
    download
    exit
    ;;
  --clone)
    cloneCode
    exit
    ;;
  --cb)
    cloneBuild
    exit
    ;;
  --mem)
    SPECIFY_MEMORY=$2
    shift 2
    ;;
  --disable-rewrite-manifes)
    REBUILD_MANIFEST=false
    shift 1
    ;;
  --dr)
    REBUILD_MANIFEST=false
    shift 1
    ;;
  --upgrade)
    UPGRADE=true
    shift 1
    ;;
  --run)
    RUN=true
    shift 1
    ;;
  *)
    echo "warn: option $1 does not exist"
    exit
    ;;
  esac
done

if [[ $UPGRADE = true ]]; then
  latest_version=checkVersion
  upgrade($latest_version)
fi

if [[ $RUN = true ]]; then
  restart
  exit
fi

restart