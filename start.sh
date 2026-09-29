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

# Build FullNode config
FULL_NODE_DIR="FullNode"
FULL_NODE_CONFIG_DIR="config"
# config file
FULL_NODE_CONFIG_TEST_NET="test_net_config.conf"
FULL_NODE_CONFIG_PRIVATE_NET="private_net_config.conf"
DEFAULT_FULL_NODE_CONFIG='config.conf'
JAR_NAME="FullNode.jar"
FULL_START_OPT=''

# Github
GITHUB_BRANCH='master'
GITHUB_CLONE_TYPE='HTTPS'
GITHUB_REPOSITORY=''
GITHUB_REPOSITORY_HTTPS_URL='https://github.com/tronprotocol/java-tron.git'
GITHUB_REPOSITORY_SSH_URL='git@github.com:tronprotocol/java-tron.git'

# Shell option
ALL_OPT_LENGTH=$#
# Start service option
MAX_STOP_TIME=60
# Modify this option to allow the minimum memory to be started, unit MB
ALLOW_MIN_MEMORY=8192

# JVM option, adjust as needed
MAX_DIRECT_MEMORY=1g
JVM_MS=4g
JVM_MX=12g
IS_BACKUP_GC_LOG=true

SPECIFY_MEMORY=0
UPGRADE=false

# Rebuild manifest
REBUILD_MANIFEST=true
REBUILD_DIR="$PWD/output-directory/database"
REBUILD_MANIFEST_SIZE=128
REBUILD_BATCH_SIZE=80000

# Download and upgrade
DOWNLOAD=false
RELEASE_URL='https://github.com/tronprotocol/java-tron/releases'
# Release jars are signed with this key, see "Integrity Check" in README.md
RELEASE_KEY_FINGERPRINT='1254F859D2B1BD9F66E7107DF859BCB44A28290B'
RELEASE_KEY_SERVERS='hkps://keys.openpgp.org hkps://keyserver.ubuntu.com'
MAIN_NET_CONFIG_URL='https://raw.githubusercontent.com/tronprotocol/java-tron/master/framework/src/main/resources/config.conf'
TEST_NET_CONFIG_URL='https://raw.githubusercontent.com/tron-nile-testnet/nile-testnet/master/framework/src/main/resources/config-nile.conf'
QUICK_START=false
CLONE_BUILD=false

if [[ $GITHUB_CLONE_TYPE == 'HTTPS' ]]; then
  GITHUB_REPOSITORY=$GITHUB_REPOSITORY_HTTPS_URL
else
  GITHUB_REPOSITORY=$GITHUB_REPOSITORY_SSH_URL
fi

darwin=false
case "`uname`" in
  Darwin*) darwin=true ;;
esac

# Determine the Java command to use to start the JVM.
if [ -z "$JAVA_HOME" ]; then
  javaExecutable="`which javac`"
  if [ -n "$javaExecutable" ] && ! [ "`expr \"$javaExecutable\" : '\([^ ]*\)'`" = "no" ]; then
    # readlink(1) is not available as standard on Solaris 10.
    readLink=`which readlink`
    if [ ! `expr "$readLink" : '\([^ ]*\)'` = "no" ]; then
      if $darwin ; then
        javaHome="`dirname \"$javaExecutable\"`"
        javaExecutable="`cd \"$javaHome\" && pwd -P`/javac"
      else
        javaExecutable="`readlink -f \"$javaExecutable\"`"
      fi
      javaHome="`dirname \"$javaExecutable\"`"
      javaHome=`expr "$javaHome" : '\(.*\)/bin'`
      JAVA_HOME="$javaHome"
      export JAVA_HOME
    fi
  fi
fi

if [ -z "$JAVACMD" ] ; then
  if [ -n "$JAVA_HOME"  ] ; then
    if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
      # IBM's JDK on AIX uses strange locations for the executables
      JAVACMD="$JAVA_HOME/jre/sh/java"
    else
      JAVACMD="$JAVA_HOME/bin/java"
    fi
  else
    JAVACMD="`which java`"
  fi
fi

if [ ! -x "$JAVACMD" ] ; then
  echo "Error: JAVA_HOME is not defined correctly." >&2
  echo "  We cannot execute $JAVACMD" >&2
  exit 1
fi

if [ -z "$JAVA_HOME" ] ; then
  echo "Warning: JAVA_HOME environment variable is not set."
fi

JAVA_PROPERTIES=$("$JAVACMD" -XshowSettings:properties -version 2>&1)
javaProperty() {
  echo "$JAVA_PROPERTIES" | awk -F ' = ' -v key="$1" '$1 ~ "^ *" key "$" {print $2; exit}'
}

# x86_64 runs on JDK 8 and ARM64 on JDK 17, each with its own release jars
JAVA_SPEC_VERSION=$(javaProperty java.specification.version)
case "$(javaProperty os.arch)" in
  aarch64|arm64) RELEASE_ARCH_SUFFIX='-aarch64' ;;
  *) RELEASE_ARCH_SUFFIX='' ;;
esac
RELEASE_JAR="FullNode$RELEASE_ARCH_SUFFIX.jar"
RELEASE_ARCHIVE_JAR="ArchiveManifest$RELEASE_ARCH_SUFFIX.jar"

backupGCLog() {
  local maxFile=5
  local gcLogDir=logs/gc_logs/
  if [ ! -d "$gcLogDir" ];then
    mkdir -p 'logs/gc_logs'
  fi

  if [ -f 'gc.log' ]; then
    echo '[info] backup gc.log'
    local dateformat=`date "+%Y-%m-%d_%H-%M-%S"`
    tar -czvf gc.log_$dateformat'.tar.gz' gc.log
    mv gc.log_$dateformat'.tar.gz' $gcLogDir
    rm -rf gc.log

    # checking the number of backups
    local currentDirCount=`ls -l $gcLogDir | grep "gc.log*" | wc -l`
    if [ $currentDirCount -gt $maxFile ]; then
      local oldFileSize=`expr $currentDirCount - $maxFile`
      local oldGcLogFiles=(`ls -1 $gcLogDir |head -n $oldFileSize`)
    fi

    for fileName in ${oldGcLogFiles[@]}; do
      rm -rf $gcLogDir$fileName
    done
  fi
}

getLatestReleaseVersion() {
  git ls-remote --tags --refs $GITHUB_REPOSITORY 2>/dev/null | awk -F '/' '{print $3}' \
    | grep -E '^GreatVoyage-v[0-9]+(\.[0-9]+)*$' | sort -V | tail -1
}

upgrade() {
  latest_version=$(getLatestReleaseVersion)
  echo "info: latest version: $latest_version"
  if [[ -n $latest_version ]]; then
    if downloadRelease $latest_version $RELEASE_JAR $JAR_NAME.download; then
      if [[ -f $JAR_NAME ]]; then
        echo "info: backup $JAR_NAME"
        mv $JAR_NAME $JAR_NAME'_bak'
      fi
      mv $JAR_NAME.download $JAR_NAME
      echo "info: download version $latest_version success"
    else
      echo "warn: upgrade aborted, $JAR_NAME is unchanged"
      exit 1
    fi
  else
    echo 'info: nothing to upgrade'
  fi
}

download() {
  local url=$1
  local file_name=$2
  if type wget >/dev/null 2>&1; then
    wget -q -O "$file_name" "$url" || { rm -f "$file_name"; return 1; }
  elif type curl >/dev/null 2>&1; then
    echo "curl -fsSL -o $file_name $url"
    curl -fsSL -o "$file_name" "$url" || { rm -f "$file_name"; return 1; }
  else
    echo 'info: no exists wget or curl, make sure the system can use the "wget" or "curl" command'
    return 1
  fi
}

# Downloads asset $2 of release $1 to file $3 and verifies its signature.
downloadRelease() {
  download "$RELEASE_URL/download/$1/$2" "$3" && checkSign "$1" "$2" "$3"
}

mkdirFullNode() {
  if [ ! -d $FULL_NODE_DIR ]; then
    echo "info: create $FULL_NODE_DIR"
    mkdir $FULL_NODE_DIR
    $(cp $0 $FULL_NODE_DIR)
    cd $FULL_NODE_DIR
  elif [ -d $FULL_NODE_DIR ]; then
    cd $FULL_NODE_DIR
  fi
}

quickStart() {
  full_node_version=$(getLatestReleaseVersion)
  if [[ -n $full_node_version ]]; then
    mkdirFullNode
    echo "info: check latest version: $full_node_version"
    echo 'info: download config'
    download $MAIN_NET_CONFIG_URL config.conf || exit 1

    echo "info: download $full_node_version"
    downloadRelease $full_node_version $RELEASE_JAR $JAR_NAME || exit 1
  else
    echo 'info: not getting the latest version'
    exit 1
  fi
}

cloneCode() {
  if type git >/dev/null 2>&1; then
    if git clone -b $GITHUB_BRANCH $GITHUB_REPOSITORY; then
      echo 'info: git clone java-tron success'
    else
      echo 'warn: git clone java-tron failed'
      return 1
    fi
  else
    echo 'info: no exists git, make sure the system can use the "git" command'
    return 1
  fi
}

cloneBuild() {
  local currentPwd=$PWD
  echo 'info: clone java-tron'
  cloneCode || exit 1

  echo 'info: build java-tron'
  cd java-tron || exit 1
  sh gradlew clean build -x test
  if [[ $? == 0 ]];then
    cd $currentPwd
    mkdirFullNode
    cp '../java-tron/build/libs/FullNode.jar' $PWD
    cp '../java-tron/framework/src/main/resources/config.conf' $PWD
  else
    exit
  fi
}

checkPid() {
  pid=$(ps -ef | grep -v start | grep "${JAR_NAME##*/}" | grep -v grep | awk '{print $2}')
}

stopService() {
  count=1
  while [ $count -le $MAX_STOP_TIME ]; do
    checkPid
    if [ -n "$pid" ]; then
      kill -15 $pid
      sleep 1
    else
      echo "info: java-tron stop"
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
  os=`uname`
  totalMemory=$(`echo getTotalMemory`)
  total=`expr $totalMemory / 1024`
  if [[ $os == 'Darwin' ]]; then
    return
  fi

  if [[ $total -lt $ALLOW_MIN_MEMORY ]]; then
    echo "warn: the memory $total MB cannot be smaller than the minimum memory $ALLOW_MIN_MEMORY MB"
    exit 1
  elif [[ $SPECIFY_MEMORY -gt 0 ]] &&
   [[ $SPECIFY_MEMORY -lt $ALLOW_MIN_MEMORY ]]; then
    echo "warn: the specified memory $SPECIFY_MEMORY MB cannot be smaller than the minimum memory $ALLOW_MIN_MEMORY MB"
    echo 'warn: start abort'
    exit 1
  fi
}

setTCMalloc() {
  os=`uname`
  if [[ $os == 'Linux' ]] || [[ $os == 'linux' ]] ; then
    lib_tc_malloc="/usr/lib64/libtcmalloc.so"
    if [[ -f $lib_tc_malloc ]]; then
      export LD_PRELOAD="$lib_tc_malloc"
      export TCMALLOC_RELEASE_RATE=10
    else
      echo 'info: recommended for linux systems using tcmalloc as the default memory management tool'
    fi
  fi
}

getTotalMemory() {
  os=`uname`
  if [[ $os == 'Linux' ]] || [[ $os == 'linux' ]] ; then
    total=$(cat /proc/meminfo | grep MemTotal | awk -F ' ' '{print $2}')
    echo $total
    return
  elif [[  $os == 'Darwin' ]]; then
    total=$(sysctl -n hw.memsize)
    echo $((total / 1024))
  fi
}

setJVMMemory() {
  os=`uname`
  if [[ $os == 'Linux' ]] || [[ $os == 'linux' ]] ; then
    local total_gb
    if [[ $SPECIFY_MEMORY -gt 0 ]]; then
      total_gb=$((SPECIFY_MEMORY / 1024))
    else
      total_gb=$(($(getTotalMemory) / 1024 / 1024))
    fi
    if [[ $((total_gb / 10)) -gt 0 ]]; then
      MAX_DIRECT_MEMORY="$((total_gb / 10))g"
    fi
    if [[ $((total_gb * 6 / 10)) -gt 0 ]]; then
      JVM_MX="$((total_gb * 6 / 10))g"
      JVM_MS=$JVM_MX
    fi

  elif [[ $os == 'Darwin' ]]; then
    MAX_DIRECT_MEMORY='1g'
  fi
}

startService() {
  echo $(date) >>start.log
  if [[ ! -f $JAR_NAME ]]; then
    echo "warn: jar file $JAR_NAME not exist"
    exit 1
  fi

  local gc_opts='-XX:+UseZGC -Xlog:gc,gc+heap:file=gc.log:time,tags,level:filecount=10,filesize=100M'
  local tail_opts=''
  if [[ $JAVA_SPEC_VERSION == '1.8' ]]; then
    gc_opts='-XX:+UseConcMarkSweepGC -XX:+PrintGCDetails -Xloggc:./gc.log -XX:+PrintGCDateStamps -XX:+CMSParallelRemarkEnabled'
    tail_opts='-XX:NewRatio=2'
  fi

  ulimit -n 65535 2>/dev/null || echo 'warn: failed to set ulimit -n 65535'
  nohup $JAVACMD -Xms$JVM_MS -Xmx$JVM_MX $gc_opts -XX:ReservedCodeCacheSize=256m -XX:+UseCodeCacheFlushing \
    -XX:MetaspaceSize=256m -XX:MaxMetaspaceSize=512m \
    -XX:MaxDirectMemorySize=$MAX_DIRECT_MEMORY -Dio.netty.allocator.type=pooled \
    -XX:+HeapDumpOnOutOfMemoryError \
    $tail_opts -jar \
    $JAR_NAME $FULL_START_OPT -c $DEFAULT_FULL_NODE_CONFIG >>start.log 2>&1 &
  checkPid
  echo "info: start java-tron with pid $pid on $HOSTNAME"
  echo "info: if you need to stop the service, execute: sh start.sh --stop"
}

rebuildManifest() {
  if [[ $REBUILD_MANIFEST = false ]]; then
    echo 'info: disable rebuild manifest!'
    return
  fi

  if [[ -n $RELEASE_ARCH_SUFFIX ]]; then
    echo 'info: ARM64 only supports RocksDB, skip rebuild manifest'
    return
  fi

  if [[ ! -d $REBUILD_DIR ]]; then
    echo "info: database not exists, skip rebuild manifest"
    return
  fi

  ARCHIVE_JAR='ArchiveManifest.jar'
  if [[ ! -f $ARCHIVE_JAR ]]; then
    echo 'info: download the rebuild manifest plugin from the github'
    if ! downloadRelease "$(getLatestReleaseVersion)" $RELEASE_ARCHIVE_JAR $ARCHIVE_JAR; then
      echo 'warn: skip rebuild manifest'
      return
    fi
  fi
  echo 'info: execute rebuild manifest.'
  if $JAVACMD -jar $ARCHIVE_JAR -d $REBUILD_DIR -m $REBUILD_MANIFEST_SIZE -b $REBUILD_BATCH_SIZE; then
    echo 'info: rebuild manifest success'
  else
    echo 'info: rebuild manifest fail, log in logs/archive.log'
  fi
}

specifyConfig(){
  echo "info: specify the net: $1"
  local netType=$1
  local configName;
  local configUrl;
  if [[ "$netType" = 'test' ]]; then
    configName=$FULL_NODE_CONFIG_TEST_NET
    configUrl=$TEST_NET_CONFIG_URL
  elif [[ "$netType" = 'private' ]]; then
    configName=$FULL_NODE_CONFIG_PRIVATE_NET
    configUrl=https://raw.githubusercontent.com/tronprotocol/tron-deployment/$GITHUB_BRANCH/$configName
  else
    echo "warn: no support config $netType"
    exit 1
  fi

  if [[ ! -d $FULL_NODE_CONFIG_DIR ]]; then
    mkdir -p $FULL_NODE_CONFIG_DIR
  fi

  if [[ ! -f $FULL_NODE_CONFIG_DIR/$configName ]]; then
    download $configUrl $FULL_NODE_CONFIG_DIR/$configName || exit 1
  fi
  DEFAULT_FULL_NODE_CONFIG=$FULL_NODE_CONFIG_DIR/$configName
}

# Verifies file $3 against the signature of asset $2 in release $1, which must be made
# by the release key. Removes $3 if the signature is missing or invalid.
checkSign() {
  echo 'info: verify signature'
  local version=$1
  local asset=$2
  local file=$3
  local gnupg_home
  local server
  local verified=false
  if type gpg >/dev/null 2>&1; then
    gnupg_home=$(mktemp -d)
    for server in $RELEASE_KEY_SERVERS; do
      gpg --homedir "$gnupg_home" --batch --quiet --keyserver "$server" \
        --recv-keys "$RELEASE_KEY_FINGERPRINT" >/dev/null 2>&1 && break
    done
    if download "$RELEASE_URL/download/$version/$asset.sig" "$file.sig" \
        && gpg --homedir "$gnupg_home" --batch --status-fd 1 --verify "$file.sig" "$file" 2>/dev/null \
          | grep '^\[GNUPG:\] VALIDSIG ' | grep -q -w "$RELEASE_KEY_FINGERPRINT"; then
      verified=true
    fi
    gpgconf --homedir "$gnupg_home" --kill all >/dev/null 2>&1
    rm -rf "$gnupg_home" "$file.sig"
  else
    echo 'warn: gpg is required to verify the release signature'
  fi
  if [[ $verified == true ]]; then
    echo 'info: signature verified'
    return 0
  fi
  echo "warn: signature verification failed, remove $file"
  rm -f "$file"
  return 1
}

restart() {
  stopService
  checkAllowMemory
  rebuildManifest
  setTCMalloc
  setJVMMemory
  startService
}

while [ -n "$1" ]; do
  case "$1" in
  -c)
    DEFAULT_FULL_NODE_CONFIG=$2
    shift 2
    ;;
  -d)
    REBUILD_DIR=$2/database
    FULL_START_OPT="$FULL_START_OPT $1 $2"
    shift 2
    ;;
  -j)
    JAR_NAME=$2
    shift 2
    ;;
  -p)
    FULL_START_OPT="$FULL_START_OPT $1 $2"
    shift 2
    ;;
  -w)
    FULL_START_OPT="$FULL_START_OPT $1"
    shift 1
    ;;
  --witness)
    FULL_START_OPT="$FULL_START_OPT $1"
    shift 1
    ;;
  --net)
    specifyConfig $2
    shift 2
    ;;
  -m)
    REBUILD_MANIFEST_SIZE=$2
    shift 2
    ;;
  -n)
    JAR_NAME=$2
    shift 2
    ;;
  -b)
    REBUILD_BATCH_SIZE=$2
    shift 2
    ;;
  -cb)
    CLONE_BUILD=true
    shift 1
    ;;
  --download)
    DOWNLOAD=true
    shift 1
    ;;
  --deploy)
    QUICK_START=true
    shift 1
    ;;
  --release)
    QUICK_START=true
    shift 1
    ;;
  --clone)
    cloneCode
    exit
    ;;
  -mem)
    SPECIFY_MEMORY=$2
    shift 2
    ;;
  --disable-rewrite-manifes)
    REBUILD_MANIFEST=false
    shift 1
    ;;
  -dr)
    REBUILD_MANIFEST=false
    shift 1
    ;;
  --upgrade)
    UPGRADE=true
    shift 1
    ;;
  --run)
    shift 1
    ;;
  --stop|-s)
    stopService
    exit 0
    ;;
  FullNode)
    shift 1
    ;;
  FullNode.jar)
    shift 1
    ;;
  *.jar)
    JAR_NAME=$1
    shift 1
    ;;
  *)
    if [[ $ALL_OPT_LENGTH -eq 1 ]]; then
      if [[ ! "$1" =~ "-" ]] && [[ ! "$1" =~ "--" ]]; then
        if [[ $1 =~ '.jar' ]]; then
          JAR_NAME=$1
        else
          JAR_NAME="$1.jar"
        fi
        restart
        exit
      fi
    fi
    FULL_START_OPT="$FULL_START_OPT $@"
    break
    ;;
  esac
done

if [[ $IS_BACKUP_GC_LOG = true ]]; then
  backupGCLog
fi

if [[ $CLONE_BUILD == true ]];then
  cloneBuild
fi

if [[ $QUICK_START == true ]]; then
  quickStart
fi

if [[ $UPGRADE == true ]]; then
  upgrade
fi

if [[ $DOWNLOAD == true ]]; then
  latest=$(getLatestReleaseVersion)
  if [[ -n $latest ]]; then
    downloadRelease $latest $RELEASE_JAR $JAR_NAME.download && mv $JAR_NAME.download $JAR_NAME
    exit
  else
    echo 'info: not getting the latest version'
    exit 1
  fi
fi

restart

