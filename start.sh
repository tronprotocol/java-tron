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

# The script needs bash. Rerun under bash when it was started with sh and sh is another shell,
# such as dash on Debian and Ubuntu.
if [ -z "$BASH_VERSION" ]; then
  exec bash "$0" "$@"
fi

# Build FullNode config
FULL_NODE_DIR="FullNode"
FULL_NODE_CONFIG_DIR="config"
# config file
FULL_NODE_CONFIG_TEST_NET="test_net_config.conf"
FULL_NODE_CONFIG_PRIVATE_NET="private_net_config.conf"
DEFAULT_FULL_NODE_CONFIG='config.conf'
JAR_NAME="FullNode.jar"
# FullNode options given on the command line, passed on as they are.
FULL_START_OPT=()

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
JVM_MX=9g
IS_BACKUP_GC_LOG=true

SPECIFY_MEMORY=0
UPGRADE=false

# Rebuild manifest
# REBUILD_DIR is relative to the directory the node is started from, like the -d option of
# FullNode, so it stays right after --release or -cb change into $FULL_NODE_DIR.
REBUILD_MANIFEST=true
REBUILD_DIR="output-directory/database"
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

# macOS derives JAVA_HOME in its own way, see below.
darwin=false
case "`uname`" in
  Darwin*) darwin=true ;;
esac

# Determine the Java command to use to start the JVM.
# A JAVA_HOME from the environment is used as is. Otherwise it is derived from the installed JDK.
if [ -z "$JAVA_HOME" ]; then
  if $darwin ; then
    # /usr/bin/java and /usr/bin/javac are stubs that run the JDK selected by
    # /usr/libexec/java_home. Deriving JAVA_HOME from their path gives /usr, and the
    # stub then runs itself forever.
    JAVA_HOME="`/usr/libexec/java_home 2>/dev/null`"
  else
    javaExecutable="`which javac`"
    if [ -n "$javaExecutable" ] && ! [ "`expr \"$javaExecutable\" : '\([^ ]*\)'`" = "no" ]; then
      # readlink(1) is not available as standard on Solaris 10.
      readLink=`which readlink`
      if [ ! `expr "$readLink" : '\([^ ]*\)'` = "no" ]; then
        javaExecutable="`readlink -f \"$javaExecutable\"`"
        javaHome="`dirname \"$javaExecutable\"`"
        JAVA_HOME=`expr "$javaHome" : '\(.*\)/bin'`
      fi
    fi
  fi
  # Export it so that child processes, such as the gradle build of -cb, use the same JDK.
  if [ -n "$JAVA_HOME" ]; then
    export JAVA_HOME
  fi
fi

# A JAVACMD from the environment is used as is. Otherwise it is java under JAVA_HOME when JAVA_HOME
# is set, else java on PATH.
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

# JVM system properties of $JAVACMD, read once and looked up by javaProperty. JAVA_ERROR holds
# the first line of output when java cannot run, for example a JDK built for another CPU.
JAVA_ERROR=''
if ! JAVA_PROPERTIES=$("$JAVACMD" -XshowSettings:properties -version 2>&1); then
  JAVA_ERROR=$(echo "$JAVA_PROPERTIES" | head -n 1)
  JAVA_PROPERTIES=''
fi
# Prints one JVM system property from the -XshowSettings output above.
javaProperty() {
  echo "$JAVA_PROPERTIES" | awk -F ' = ' -v key="$1" '$1 ~ "^ *" key "$" {print $2; exit}'
}

# x86_64 runs on JDK 8 and ARM64 on JDK 17, each with its own release jars
# JAVA_SPEC_VERSION selects the GC options in startService. ARM64 release assets carry the
# -aarch64 suffix, and x86_64 assets have none.
JAVA_SPEC_VERSION=$(javaProperty java.specification.version)
case "$(javaProperty os.arch)" in
  aarch64|arm64) RELEASE_ARCH_SUFFIX='-aarch64' ;;
  *) RELEASE_ARCH_SUFFIX='' ;;
esac
# Release asset names of the node jar and of the manifest rebuild tool for this architecture.
RELEASE_JAR="FullNode$RELEASE_ARCH_SUFFIX.jar"
RELEASE_ARCHIVE_JAR="ArchiveManifest$RELEASE_ARCH_SUFFIX.jar"

# Exits when $JAVACMD cannot run or its version is unknown. Called before anything that needs
# java, so that --stop still works with a broken JDK.
checkJava() {
  if [ -n "$JAVA_ERROR" ]; then
    echo "Error: $JAVACMD cannot run: $JAVA_ERROR" >&2
    exit 1
  fi
  if [ -z "$JAVA_SPEC_VERSION" ]; then
    echo "Error: cannot determine the Java version of $JAVACMD" >&2
    exit 1
  fi
}

# Archives the gc.log of the stopped node into logs/gc_logs/ and keeps the newest 5 archives.
backupGCLog() {
  local maxFile=5
  local gcLogDir=logs/gc_logs
  local dateformat
  local archives
  local count
  mkdir -p "$gcLogDir"

  if [ -f 'gc.log' ]; then
    echo '[info] backup gc.log'
    dateformat=$(date "+%Y-%m-%d_%H-%M-%S")
    tar -czf "$gcLogDir/gc.log_$dateformat.tar.gz" gc.log && rm -f gc.log

    # Archive names sort by their timestamp, so the first ones listed are the oldest. Other
    # files in the directory are left alone.
    archives=$(ls -1 "$gcLogDir" | grep '^gc\.log_.*\.tar\.gz$')
    count=$(echo "$archives" | grep -c .)
    if [ "$count" -gt "$maxFile" ]; then
      echo "$archives" | head -n $((count - maxFile)) | while read -r fileName; do
        rm -f "$gcLogDir/$fileName"
      done
    fi
  fi
}

# Prints the newest GreatVoyage release tag of the java-tron repository; prints nothing when
# the tags cannot be listed.
getLatestReleaseVersion() {
  # List the remote tags, keep the GreatVoyage-vX.Y.Z ones and take the highest version.
  git ls-remote --tags --refs $GITHUB_REPOSITORY 2>/dev/null | awk -F '/' '{print $3}' \
    | grep -E '^GreatVoyage-v[0-9]+(\.[0-9]+)*$' | sort -V | tail -1
}

# --upgrade: downloads and verifies the latest release jar, then replaces $JAR_NAME with it and
# keeps the previous file as ${JAR_NAME}_bak. Exits without touching $JAR_NAME if that fails.
upgrade() {
  latest_version=$(getLatestReleaseVersion)
  echo "info: latest version: $latest_version"
  if [[ -n $latest_version ]]; then
    # Download to a temporary name first, so that a failed download or signature check leaves
    # $JAR_NAME untouched.
    if downloadRelease "$latest_version" "$RELEASE_JAR" "$JAR_NAME.download"; then
      # Verified: keep the previous jar as ${JAR_NAME}_bak and move the new one into place.
      if [[ -f $JAR_NAME ]]; then
        echo "info: backup $JAR_NAME"
        mv "$JAR_NAME" "${JAR_NAME}_bak"
      fi
      mv "$JAR_NAME.download" "$JAR_NAME"
      echo "info: download version $latest_version success"
    else
      # download or checkSign has already removed the temporary file.
      echo "warn: upgrade aborted, $JAR_NAME is unchanged"
      exit 1
    fi
  else
    # No release tag could be listed. The current jar is kept.
    echo 'info: nothing to upgrade'
  fi
}

# Downloads $1 to file $2 with wget or curl, verifying the TLS certificate. Returns 1 and
# leaves no file behind when the download fails.
download() {
  local url=$1
  local file_name=$2
  if type wget >/dev/null 2>&1; then
    # wget follows redirects by default.
    wget -q -O "$file_name" "$url" || { rm -f "$file_name"; return 1; }
  elif type curl >/dev/null 2>&1; then
    # -f fails on HTTP errors instead of saving the error page, -L follows redirects.
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

# Creates the $FULL_NODE_DIR directory with a copy of this script and changes into it.
mkdirFullNode() {
  if [ ! -d "$FULL_NODE_DIR" ]; then
    echo "info: create $FULL_NODE_DIR"
    mkdir "$FULL_NODE_DIR"
    cp "$0" "$FULL_NODE_DIR"
  fi
  cd "$FULL_NODE_DIR" || exit 1
}

# --release / --deploy: sets up $FULL_NODE_DIR with the mainnet config and the latest verified
# release jar. Exits when the version, the config or the jar cannot be fetched.
quickStart() {
  full_node_version=$(getLatestReleaseVersion)
  if [[ -n $full_node_version ]]; then
    # Enter $FULL_NODE_DIR, then download the mainnet config and the signed release jar for
    # this architecture.
    mkdirFullNode
    echo "info: check latest version: $full_node_version"
    echo 'info: download config'
    download "$MAIN_NET_CONFIG_URL" config.conf || exit 1

    # Download to a temporary name first, so that a failed download or signature check leaves
    # an existing $JAR_NAME untouched.
    echo "info: download $full_node_version"
    downloadRelease "$full_node_version" "$RELEASE_JAR" "$JAR_NAME.download" || exit 1
    mv "$JAR_NAME.download" "$JAR_NAME"
  else
    # Without a release tag there is nothing to download.
    echo 'info: not getting the latest version'
    exit 1
  fi
}

# --clone: clones branch $GITHUB_BRANCH of java-tron into the current directory.
cloneCode() {
  if type git >/dev/null 2>&1; then
    # The checkout goes to ./java-tron.
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

# -cb: clones and builds java-tron, then copies FullNode.jar and config.conf into
# $FULL_NODE_DIR. Exits when the clone or the build fails.
cloneBuild() {
  local currentPwd=$PWD
  echo 'info: clone java-tron'
  cloneCode || exit 1

  echo 'info: build java-tron'
  cd java-tron || exit 1
  sh gradlew clean build -x test
  if [[ $? == 0 ]];then
    cd "$currentPwd" || exit 1
    mkdirFullNode
    cp '../java-tron/build/libs/FullNode.jar' "$PWD"
    cp '../java-tron/framework/src/main/resources/config.conf' "$PWD"
  else
    exit 1
  fi
}

# Prints the process ids of the "java ... -jar <JAR_NAME>" lines in the ps output on stdin.
# The jar may be given with a directory, only its file name is compared.
matchNodePid() {
  awk -v name="${JAR_NAME##*/}" '{
    for (i = 3; i <= NF; i++) {
      if ($(i - 1) == "-jar" && ($i == name || substr($i, length($i) - length(name)) == "/" name)) {
        print $1
        break
      }
    }
  }'
}

# Sets $pid to the process id recorded in <jar name>.pid; empty when that process is gone. A
# directory that has a start.log but no pid file was used by an earlier version of this script,
# which kept no pid file; there every process on the machine that runs the jar name is taken,
# as that version did, so $pid may hold several ids.
checkPid() {
  local pidFile="${JAR_NAME##*/}.pid"
  local saved
  pid=''
  if [ -f "$pidFile" ]; then
    # Only the recorded process counts, and only while it still runs the jar. Once it is found
    # gone the file is emptied, so its id is not looked up again.
    saved=$(cat "$pidFile" 2>/dev/null)
    if [ -n "$saved" ]; then
      pid=$(ps -o pid=,args= -p "$saved" 2>/dev/null | matchNodePid)
      if [ -z "$pid" ]; then
        : > "$pidFile"
      fi
    fi
  elif [ -f start.log ]; then
    pid=$(ps -A -o pid=,args= 2>/dev/null | matchNodePid)
  fi
}

# Stops the running node: sends SIGTERM once per second for up to MAX_STOP_TIME seconds,
# then SIGKILL. Returns 1 when no node was ever started from this directory.
stopService() {
  local pidFile="${JAR_NAME##*/}.pid"
  # A node set up by --release or -cb runs in $FULL_NODE_DIR. Handle it from the parent
  # directory as well, as long as no node was started from the parent itself.
  if [ ! -f "$pidFile" ] && [ ! -f start.log ] && [ -f "$FULL_NODE_DIR/$pidFile" ]; then
    cd "$FULL_NODE_DIR" || exit 1
  fi
  if [ ! -f "$pidFile" ] && [ ! -f start.log ]; then
    echo "info: no node was started from $PWD"
    return 1
  fi
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

# Linux only: refuses to start with less than ALLOW_MIN_MEMORY MB of memory, or when -mem
# asks for less than that.
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

# Linux only: preloads tcmalloc when it is installed at /usr/lib64/libtcmalloc.so.
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

# Prints the total memory of the machine in KB.
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

# Linux only: sizes the heap to 60% and the direct memory to 10% of the total memory, or of
# the -mem value when given. macOS keeps JVM_MS / JVM_MX and uses 1g of direct memory.
setJVMMemory() {
  os=`uname`
  if [[ $os == 'Linux' ]] || [[ $os == 'linux' ]] ; then
    local total_gb
    # The base is -mem when given, otherwise the total memory, in whole GB.
    if [[ $SPECIFY_MEMORY -gt 0 ]]; then
      total_gb=$((SPECIFY_MEMORY / 1024))
    else
      total_gb=$(($(getTotalMemory) / 1024 / 1024))
    fi
    # Direct memory gets 10% and the heap 60%, with -Xms equal to -Xmx. A share that rounds
    # down to 0 GB keeps its default.
    if [[ $((total_gb / 10)) -gt 0 ]]; then
      MAX_DIRECT_MEMORY="$((total_gb / 10))g"
    fi
    if [[ $((total_gb * 6 / 10)) -gt 0 ]]; then
      JVM_MX="$((total_gb * 6 / 10))g"
      JVM_MS=$JVM_MX
    fi

  elif [[ $os == 'Darwin' ]]; then
    # macOS keeps JVM_MS / JVM_MX and uses 1g of direct memory.
    MAX_DIRECT_MEMORY='1g'
  fi
}

# Starts $JAR_NAME in the background with the JVM options for the detected Java version
# (CMS on JDK 8, ZGC on JDK 17), passing the FullNode options and the config file. Output goes
# to start.log, the process id to <jar name>.pid.
startService() {
  if [[ ! -f $JAR_NAME ]]; then
    echo "warn: jar file $JAR_NAME not exist"
    exit 1
  fi
  echo $(date) >>start.log

  # ZGC with unified GC logging by default. JDK 8 has no ZGC, so it uses CMS with the JDK 8
  # GC log flags and NewRatio=2.
  local gc_opts='-XX:+UseZGC -Xlog:gc,gc+heap:file=gc.log:time,tags,level:filecount=10,filesize=100M'
  local tail_opts=''
  if [[ $JAVA_SPEC_VERSION == '1.8' ]]; then
    gc_opts='-XX:+UseConcMarkSweepGC -XX:+PrintGCDetails -Xloggc:./gc.log -XX:+PrintGCDateStamps -XX:+CMSParallelRemarkEnabled'
    tail_opts='-XX:NewRatio=2'
  fi

  # The node keeps many database and network files open. Raise the soft open file limit to
  # 65535 when it is lower; a higher soft limit and the hard limit are kept.
  local openFiles=$(ulimit -S -n)
  if [[ $openFiles != unlimited && $openFiles -lt 65535 ]]; then
    ulimit -S -n 65535 2>/dev/null || echo 'warn: failed to set ulimit -n 65535'
  fi
  # Run in the background, immune to hangups, appending all output to start.log.
  nohup "$JAVACMD" -Xms$JVM_MS -Xmx$JVM_MX $gc_opts -XX:ReservedCodeCacheSize=256m -XX:+UseCodeCacheFlushing \
    -XX:MetaspaceSize=256m -XX:MaxMetaspaceSize=512m \
    -XX:MaxDirectMemorySize=$MAX_DIRECT_MEMORY -Dio.netty.allocator.type=pooled \
    -XX:+HeapDumpOnOutOfMemoryError \
    $tail_opts -jar \
    "$JAR_NAME" "${FULL_START_OPT[@]}" -c "$DEFAULT_FULL_NODE_CONFIG" >>start.log 2>&1 &
  pid=$!
  echo "$pid" > "${JAR_NAME##*/}.pid"
  # A JVM that cannot start, for example on a bad option or a locked database, exits within
  # moments. Report that instead of a successful start.
  sleep 3
  if ! kill -0 "$pid" 2>/dev/null; then
    echo "warn: java-tron exited right after the start, see start.log"
    : > "${JAR_NAME##*/}.pid"
    exit 1
  fi
  echo "info: start java-tron with pid $pid on $HOSTNAME"
  echo "info: if you need to stop the service, execute in this directory: sh start.sh --stop"
}

# Rewrites LevelDB manifests of at least REBUILD_MANIFEST_SIZE MB under $REBUILD_DIR before
# a start, so the databases open faster (TIP-298). Downloads and verifies ArchiveManifest.jar
# when it is missing. Skipped with -dr, on ARM64 (RocksDB only) and without a database.
rebuildManifest() {
  if [[ $REBUILD_MANIFEST = false ]]; then
    echo 'info: disable rebuild manifest!'
    return
  fi

  # The tool handles LevelDB only, and ARM64 runs RocksDB only.
  if [[ -n $RELEASE_ARCH_SUFFIX ]]; then
    echo 'info: ARM64 only supports RocksDB, skip rebuild manifest'
    return
  fi

  if [[ ! -d $REBUILD_DIR ]]; then
    echo "info: database not exists, skip rebuild manifest"
    return
  fi

  ARCHIVE_JAR='ArchiveManifest.jar'
  # Download and verify the tool on first use. If that fails, the rebuild is skipped and the
  # start goes on.
  if [[ ! -f $ARCHIVE_JAR ]]; then
    echo 'info: download the rebuild manifest plugin from the github'
    if ! downloadRelease "$(getLatestReleaseVersion)" $RELEASE_ARCHIVE_JAR $ARCHIVE_JAR; then
      echo 'warn: skip rebuild manifest'
      return
    fi
  fi
  echo 'info: execute rebuild manifest.'
  # A failed rebuild does not block the start either. The tool writes its log to logs/toolkit.log.
  if "$JAVACMD" -jar "$ARCHIVE_JAR" -d "$REBUILD_DIR" -m "$REBUILD_MANIFEST_SIZE" -b "$REBUILD_BATCH_SIZE"; then
    echo 'info: rebuild manifest success'
  else
    echo 'info: rebuild manifest fail, log in logs/toolkit.log'
  fi
}

# --net: selects the test (Nile) or private network config under $FULL_NODE_CONFIG_DIR,
# downloading it when the file does not exist yet. Exits on an unknown network or a failed
# download.
specifyConfig(){
  echo "info: specify the net: $1"
  local netType=$1
  local configName;
  local configUrl;
  if [[ "$netType" = 'test' ]]; then
    # Nile testnet config from the nile-testnet repository.
    configName=$FULL_NODE_CONFIG_TEST_NET
    configUrl=$TEST_NET_CONFIG_URL
  elif [[ "$netType" = 'private' ]]; then
    # Private network config from the tron-deployment repository.
    configName=$FULL_NODE_CONFIG_PRIVATE_NET
    configUrl=https://raw.githubusercontent.com/tronprotocol/tron-deployment/$GITHUB_BRANCH/$configName
  else
    echo "warn: no support config $netType"
    exit 1
  fi

  if [[ ! -d $FULL_NODE_CONFIG_DIR ]]; then
    mkdir -p "$FULL_NODE_CONFIG_DIR"
  fi

  # Download only when the file is missing. An existing file is used as is.
  if [[ ! -f $FULL_NODE_CONFIG_DIR/$configName ]]; then
    download "$configUrl" "$FULL_NODE_CONFIG_DIR/$configName" || exit 1
  fi
  # The selected config is passed to FullNode when it is started. The path is absolute, so it
  # stays valid after --release or -cb change into $FULL_NODE_DIR.
  DEFAULT_FULL_NODE_CONFIG=$PWD/$FULL_NODE_CONFIG_DIR/$configName
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
    # Use a temporary keyring, so that the user's keyring is neither read nor changed.
    gnupg_home=$(mktemp -d)
    # Fetch the release key from the first key server that answers.
    for server in $RELEASE_KEY_SERVERS; do
      gpg --homedir "$gnupg_home" --batch --quiet --keyserver "$server" \
        --recv-keys "$RELEASE_KEY_FINGERPRINT" >/dev/null 2>&1 && break
    done
    # Download the detached signature. Only a VALIDSIG status line that carries the release
    # key fingerprint counts as verified.
    if download "$RELEASE_URL/download/$version/$asset.sig" "$file.sig" \
        && gpg --homedir "$gnupg_home" --batch --status-fd 1 --verify "$file.sig" "$file" 2>/dev/null \
          | grep '^\[GNUPG:\] VALIDSIG ' | grep -q -w "$RELEASE_KEY_FINGERPRINT"; then
      verified=true
    fi
    # Stop the gpg daemons of the temporary keyring, then remove the keyring and the signature.
    gpgconf --homedir "$gnupg_home" --kill all >/dev/null 2>&1
    rm -rf "$gnupg_home" "$file.sig"
  else
    # Without gpg the file cannot be verified and is removed below.
    echo 'warn: gpg is required to verify the release signature'
  fi
  # Anything other than a verified signature removes the file.
  if [[ $verified == true ]]; then
    echo 'info: signature verified'
    return 0
  fi
  echo "warn: signature verification failed, remove $file"
  rm -f "$file"
  return 1
}

# Stops a running node and starts it again with the current settings.
restart() {
  checkJava
  stopService
  if [[ $IS_BACKUP_GC_LOG = true ]]; then
    backupGCLog
  fi
  checkAllowMemory
  rebuildManifest
  setTCMalloc
  setJVMMemory
  startService
}

# Exits when option $1 was given without a value.
needValue() {
  if [ -z "$2" ]; then
    echo "error: option $1 needs a value" >&2
    exit 1
  fi
}

# Command line options. Anything else is passed to FullNode as is, except a single word given
# as the only argument, which names the jar to start. Everything after -- goes to FullNode
# unchanged.
while [ -n "$1" ]; do
  case "$1" in
  --)
    shift 1
    FULL_START_OPT+=("$@")
    break
    ;;
  -c)
    needValue "$1" "$2"
    DEFAULT_FULL_NODE_CONFIG=$2
    shift 2
    ;;
  -d)
    needValue "$1" "$2"
    REBUILD_DIR=$2/database
    FULL_START_OPT+=("$1" "$2")
    shift 2
    ;;
  -j|-n)
    needValue "$1" "$2"
    JAR_NAME=$2
    shift 2
    ;;
  -p)
    needValue "$1" "$2"
    FULL_START_OPT+=("$1" "$2")
    shift 2
    ;;
  -w|--witness)
    FULL_START_OPT+=("$1")
    shift 1
    ;;
  --net)
    needValue "$1" "$2"
    specifyConfig "$2"
    shift 2
    ;;
  -m)
    needValue "$1" "$2"
    REBUILD_MANIFEST_SIZE=$2
    shift 2
    ;;
  -b)
    needValue "$1" "$2"
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
  --deploy|--release)
    QUICK_START=true
    shift 1
    ;;
  --clone)
    cloneCode
    exit
    ;;
  -mem)
    needValue "$1" "$2"
    SPECIFY_MEMORY=$2
    shift 2
    ;;
  # --disable-rewrite-manifes is the spelling of earlier versions and stays accepted.
  --disable-rewrite-manifest|--disable-rewrite-manifes|-dr)
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
    exit $?
    ;;
  FullNode|FullNode.jar)
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
    # A FullNode option, passed on as is. Parsing goes on, so script options may follow it.
    FULL_START_OPT+=("$1")
    shift 1
    ;;
  esac
done

# Main flow: optional clone / download steps, then one start of the node. Everything from here
# on runs java or picks release assets by the architecture that java reports, so a broken JDK
# is reported first.
checkJava

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
    downloadRelease "$latest" "$RELEASE_JAR" "$JAR_NAME.download" && mv "$JAR_NAME.download" "$JAR_NAME"
    exit
  else
    echo 'info: not getting the latest version'
    exit 1
  fi
fi

restart

