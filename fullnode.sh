#!/usr/bin/env bash

#
# Copyright 2015 the original author or authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

##############################################################################
##
##  FullNode start up script for UN*X
##
##############################################################################

# Attempt to set APP_HOME
# Resolve links: $0 may be a link
PRG="$0"
# Need this for relative symlinks.
while [ -h "$PRG" ] ; do
    ls=`ls -ld "$PRG"`
    link=`expr "$ls" : '.*-> \(.*\)$'`
    if expr "$link" : '/.*' > /dev/null; then
        PRG="$link"
    else
        PRG=`dirname "$PRG"`"/$link"
    fi
done
SAVED="`pwd`"
cd "`dirname \"$PRG\"`/.." >/dev/null
APP_HOME="`pwd -P`"
cd "$SAVED" >/dev/null

APP_NAME="FullNode"
APP_BASE_NAME=`basename "$0"`

#UNAME=$(uname -s)
#if [ x"$UNAME" == x"Linux" ];then
#    TOTAL=$(cat /proc/meminfo  |grep MemTotal |awk -F ' ' '{print $2}')
#    MEM=$(echo "$TOTAL/1024/1024*0.8" | bc |awk -F. '{print $1"g"}')
#    JAVA_OPTS='"-Xmx$MEM" "-Xms$MEM"'
#fi

# Add default JVM options here. You can also use JAVA_OPTS and FULL_NODE_OPTS to pass JVM options to this script.
DEFAULT_JVM_OPTS=""
for line in $(cat $APP_HOME/bin/java-tron.vmoptions)
do
    DEFAULT_JVM_OPTS="$DEFAULT_JVM_OPTS $line"
done

# Use the maximum available, or set MAX_FD != -1 to use that value.
MAX_FD="maximum"

warn () {
    echo "$*"
}

die () {
    echo
    echo "$*"
    echo
    exit 1
}

# OS specific support (must be 'true' or 'false').
cygwin=false
msys=false
darwin=false
nonstop=false
case "`uname`" in
  CYGWIN* )
    cygwin=true
    ;;
  Darwin* )
    darwin=true
    ;;
  MINGW* )
    msys=true
    ;;
  NONSTOP* )
    nonstop=true
    ;;
esac

for file in `ls $APP_HOME/lib`
do
    CLASSPATH='$APP_HOME'/lib/$file:$CLASSPATH
done

#CLASSPATH=$APP_HOME/lib/framework-1.0.0.jar:$APP_HOME/lib/actuator-1.0.0.jar:$APP_HOME/lib/consensus-1.0.0.jar:$APP_HOME/lib/chainbase-1.0.0.jar:$APP_HOME/lib/crypto-1.0.0.jar:$APP_HOME/lib/common-1.0.0.jar:$APP_HOME/lib/protocol-1.0.0.jar:$APP_HOME/lib/platform-1.0.0.jar:$APP_HOME/lib/jcl-over-slf4j-1.7.25.jar:$APP_HOME/lib/libp2p-2.2.5.jar:$APP_HOME/lib/logback-classic-1.2.13.jar:$APP_HOME/lib/metrics-core-3.1.2.jar:$APP_HOME/lib/metrics-influxdb-0.8.2.jar:$APP_HOME/lib/jsonrpc4j-1.6.jar:$APP_HOME/lib/dnsjava-3.6.2.jar:$APP_HOME/lib/crypto-5.0.0.jar:$APP_HOME/lib/route53-2.18.41.jar:$APP_HOME/lib/aws-xml-protocol-2.18.41.jar:$APP_HOME/lib/aws-query-protocol-2.18.41.jar:$APP_HOME/lib/protocol-core-2.18.41.jar:$APP_HOME/lib/aws-core-2.18.41.jar:$APP_HOME/lib/auth-2.18.41.jar:$APP_HOME/lib/regions-2.18.41.jar:$APP_HOME/lib/sdk-core-2.18.41.jar:$APP_HOME/lib/apache-client-2.18.41.jar:$APP_HOME/lib/netty-nio-client-2.18.41.jar:$APP_HOME/lib/http-client-spi-2.18.41.jar:$APP_HOME/lib/metrics-spi-2.18.41.jar:$APP_HOME/lib/json-utils-2.18.41.jar:$APP_HOME/lib/profiles-2.18.41.jar:$APP_HOME/lib/utils-2.18.41.jar:$APP_HOME/lib/alidns20150109-3.0.1.jar:$APP_HOME/lib/tea-openapi-0.2.8.jar:$APP_HOME/lib/alibabacloud-gateway-spi-0.0.1.jar:$APP_HOME/lib/tea-rpc-0.1.2.jar:$APP_HOME/lib/credentials-java-0.2.4.jar:$APP_HOME/lib/tea-1.2.0.jar:$APP_HOME/lib/slf4j-api-1.7.36.jar:$APP_HOME/lib/grpc-services-1.60.0.jar:$APP_HOME/lib/protobuf-java-util-3.25.5.jar:$APP_HOME/lib/grpc-protobuf-1.60.0.jar:$APP_HOME/lib/guice-4.1.0.jar:$APP_HOME/lib/reflections-0.9.11.jar:$APP_HOME/lib/grpc-netty-1.60.0.jar:$APP_HOME/lib/grpc-stub-1.60.0.jar:$APP_HOME/lib/grpc-core-1.60.0.jar:$APP_HOME/lib/grpc-protobuf-lite-1.60.0.jar:$APP_HOME/lib/grpc-context-1.60.0.jar:$APP_HOME/lib/grpc-api-1.60.0.jar:$APP_HOME/lib/grpc-util-1.60.0.jar:$APP_HOME/lib/guava-32.0.1-jre.jar:$APP_HOME/lib/jsr305-3.0.2.jar:$APP_HOME/lib/spring-context-5.3.18.jar:$APP_HOME/lib/spring-tx-5.3.18.jar:$APP_HOME/lib/commons-lang3-3.4.jar:$APP_HOME/lib/commons-math-2.2.jar:$APP_HOME/lib/commons-collections4-4.1.jar:$APP_HOME/lib/joda-time-2.3.jar:$APP_HOME/lib/bcprov-jdk15on-1.69.jar:$APP_HOME/lib/javax.annotation-api-1.3.2.jar:$APP_HOME/lib/javax.jws-api-1.1.jar:$APP_HOME/lib/jetty-servlet-9.4.53.v20231009.jar:$APP_HOME/lib/jetty-security-9.4.53.v20231009.jar:$APP_HOME/lib/jetty-server-9.4.53.v20231009.jar:$APP_HOME/lib/fastjson-1.2.83.jar:$APP_HOME/lib/vavr-0.9.2.jar:$APP_HOME/lib/pf4j-3.10.0.jar:$APP_HOME/lib/jeromq-0.5.3.jar:$APP_HOME/lib/jansi-1.16.jar:$APP_HOME/lib/zksnark-java-sdk-1.0.0.jar:$APP_HOME/lib/proto-google-common-protos-2.22.0.jar:$APP_HOME/lib/protobuf-java-3.25.5.jar:$APP_HOME/lib/jcip-annotations-1.0.jar:$APP_HOME/lib/logback-core-1.2.13.jar:$APP_HOME/lib/spring-aop-5.3.18.jar:$APP_HOME/lib/spring-beans-5.3.18.jar:$APP_HOME/lib/spring-expression-5.3.18.jar:$APP_HOME/lib/spring-core-5.3.18.jar:$APP_HOME/lib/javax.inject-1.jar:$APP_HOME/lib/aopalliance-1.0.jar:$APP_HOME/lib/javax.servlet-api-3.1.0.jar:$APP_HOME/lib/jetty-http-9.4.53.v20231009.jar:$APP_HOME/lib/jetty-io-9.4.53.v20231009.jar:$APP_HOME/lib/jetty-util-ajax-9.4.53.v20231009.jar:$APP_HOME/lib/base64-2.3.9.jar:$APP_HOME/lib/jackson-annotations-2.13.4.jar:$APP_HOME/lib/jackson-databind-2.13.4.2.jar:$APP_HOME/lib/jackson-core-2.13.4.jar:$APP_HOME/lib/openapiutil-0.2.0.jar:$APP_HOME/lib/httpasyncclient-4.1.1.jar:$APP_HOME/lib/httpclient-4.5.13.jar:$APP_HOME/lib/commons-codec-1.15.jar:$APP_HOME/lib/httpcore-nio-4.4.5.jar:$APP_HOME/lib/vavr-match-0.9.2.jar:$APP_HOME/lib/java-semver-0.9.0.jar:$APP_HOME/lib/jnacl-1.0.0.jar:$APP_HOME/lib/java-util-1.8.0.jar:$APP_HOME/lib/jcommander-1.78.jar:$APP_HOME/lib/config-1.3.2.jar:$APP_HOME/lib/simpleclient_httpserver-0.15.0.jar:$APP_HOME/lib/simpleclient_hotspot-0.15.0.jar:$APP_HOME/lib/simpleclient_common-0.15.0.jar:$APP_HOME/lib/simpleclient-0.15.0.jar:$APP_HOME/lib/aspectjrt-1.9.8.jar:$APP_HOME/lib/aspectjweaver-1.9.8.jar:$APP_HOME/lib/aspectjtools-1.9.8.jar:$APP_HOME/lib/hawtjni-runtime-1.18.jar:$APP_HOME/lib/commons-io-2.11.0.jar:$APP_HOME/lib/javassist-3.21.0-GA.jar:$APP_HOME/lib/tea-util-0.2.16.jar:$APP_HOME/lib/tea-rpc-util-0.1.3.jar:$APP_HOME/lib/gson-2.10.1.jar:$APP_HOME/lib/error_prone_annotations-2.20.0.jar:$APP_HOME/lib/j2objc-annotations-2.8.jar:$APP_HOME/lib/netty-codec-http2-4.1.100.Final.jar:$APP_HOME/lib/netty-handler-proxy-4.1.100.Final.jar:$APP_HOME/lib/perfmark-api-0.26.0.jar:$APP_HOME/lib/netty-codec-http-4.1.100.Final.jar:$APP_HOME/lib/netty-handler-4.1.100.Final.jar:$APP_HOME/lib/netty-transport-native-unix-common-4.1.100.Final.jar:$APP_HOME/lib/spring-jcl-5.3.18.jar:$APP_HOME/lib/jetty-util-9.4.53.v20231009.jar:$APP_HOME/lib/httpcore-4.4.13.jar:$APP_HOME/lib/commons-logging-1.2.jar:$APP_HOME/lib/json-io-2.4.1.jar:$APP_HOME/lib/simpleclient_tracer_otel-0.15.0.jar:$APP_HOME/lib/simpleclient_tracer_otel_agent-0.15.0.jar:$APP_HOME/lib/snappy-java-1.1.10.5.jar:$APP_HOME/lib/bcpkix-jdk18on-1.79.jar:$APP_HOME/lib/commons-cli-1.5.0.jar:$APP_HOME/lib/leveldbjni-all-1.8.jar:$APP_HOME/lib/rocksdbjni-7.7.3.jar:$APP_HOME/lib/failureaccess-1.0.1.jar:$APP_HOME/lib/listenablefuture-9999.0-empty-to-avoid-conflict-with-guava.jar:$APP_HOME/lib/checker-qual-3.33.0.jar:$APP_HOME/lib/annotations-4.1.1.4.jar:$APP_HOME/lib/animal-sniffer-annotations-1.23.jar:$APP_HOME/lib/netty-codec-socks-4.1.100.Final.jar:$APP_HOME/lib/netty-codec-4.1.100.Final.jar:$APP_HOME/lib/netty-transport-4.1.100.Final.jar:$APP_HOME/lib/netty-buffer-4.1.100.Final.jar:$APP_HOME/lib/netty-resolver-4.1.100.Final.jar:$APP_HOME/lib/netty-common-4.1.100.Final.jar:$APP_HOME/lib/simpleclient_tracer_common-0.15.0.jar:$APP_HOME/lib/abi-5.0.0.jar:$APP_HOME/lib/rlp-5.0.0.jar:$APP_HOME/lib/utils-5.0.0.jar:$APP_HOME/lib/endpoints-spi-2.18.41.jar:$APP_HOME/lib/annotations-2.18.41.jar:$APP_HOME/lib/endpoint-util-0.0.7.jar:$APP_HOME/lib/reactive-streams-1.0.3.jar:$APP_HOME/lib/eventstream-1.0.1.jar:$APP_HOME/lib/third-party-jackson-core-2.18.41.jar:$APP_HOME/lib/okhttp-3.12.13.jar:$APP_HOME/lib/org.jacoco.agent-0.8.4-runtime.jar:$APP_HOME/lib/tea-xml-0.1.5.jar:$APP_HOME/lib/dom4j-2.1.3.jar:$APP_HOME/lib/jaxb-api-2.3.0.jar:$APP_HOME/lib/jaxb-core-2.3.0.jar:$APP_HOME/lib/jaxb-impl-2.3.0.jar:$APP_HOME/lib/ini4j-0.5.4.jar:$APP_HOME/lib/okio-1.15.0.jar

# Determine the Java command to use to start the JVM.
if [ -n "$JAVA_HOME" ] ; then
    if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
        # IBM's JDK on AIX uses strange locations for the executables
        JAVACMD="$JAVA_HOME/jre/sh/java"
    else
        JAVACMD="$JAVA_HOME/bin/java"
    fi
    if [ ! -x "$JAVACMD" ] ; then
        die "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME

Please set the JAVA_HOME variable in your environment to match the
location of your Java installation."
    fi
else
    JAVACMD="java"
    which java >/dev/null 2>&1 || die "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.

Please set the JAVA_HOME variable in your environment to match the
location of your Java installation."
fi

# Increase the maximum file descriptors if we can.
if [ "$cygwin" = "false" -a "$darwin" = "false" -a "$nonstop" = "false" ] ; then
    MAX_FD_LIMIT=`ulimit -H -n`
    if [ $? -eq 0 ] ; then
        if [ "$MAX_FD" = "maximum" -o "$MAX_FD" = "max" ] ; then
            MAX_FD="$MAX_FD_LIMIT"
        fi
        ulimit -n $MAX_FD
        if [ $? -ne 0 ] ; then
            warn "Could not set maximum file descriptor limit: $MAX_FD"
        fi
    else
        warn "Could not query maximum file descriptor limit: $MAX_FD_LIMIT"
    fi
fi

# For Darwin, add options to specify how the application appears in the dock
if $darwin; then
    GRADLE_OPTS="$GRADLE_OPTS \"-Xdock:name=$APP_NAME\" \"-Xdock:icon=$APP_HOME/media/gradle.icns\""
fi

# For Cygwin or MSYS, switch paths to Windows format before running java
if [ "$cygwin" = "true" -o "$msys" = "true" ] ; then
    APP_HOME=`cygpath --path --mixed "$APP_HOME"`
    CLASSPATH=`cygpath --path --mixed "$CLASSPATH"`
    JAVACMD=`cygpath --unix "$JAVACMD"`

    # We build the pattern for arguments to be converted via cygpath
    ROOTDIRSRAW=`find -L / -maxdepth 1 -mindepth 1 -type d 2>/dev/null`
    SEP=""
    for dir in $ROOTDIRSRAW ; do
        ROOTDIRS="$ROOTDIRS$SEP$dir"
        SEP="|"
    done
    OURCYGPATTERN="(^($ROOTDIRS))"
    # Add a user-defined pattern to the cygpath arguments
    if [ "$GRADLE_CYGPATTERN" != "" ] ; then
        OURCYGPATTERN="$OURCYGPATTERN|($GRADLE_CYGPATTERN)"
    fi
    # Now convert the arguments - kludge to limit ourselves to /bin/sh
    i=0
    for arg in "$@" ; do
        CHECK=`echo "$arg"|egrep -c "$OURCYGPATTERN" -`
        CHECK2=`echo "$arg"|egrep -c "^-"`                                 ### Determine if an option

        if [ $CHECK -ne 0 ] && [ $CHECK2 -eq 0 ] ; then                    ### Added a condition
            eval `echo args$i`=`cygpath --path --ignore --mixed "$arg"`
        else
            eval `echo args$i`="\"$arg\""
        fi
        i=`expr $i + 1`
    done
    case $i in
        0) set -- ;;
        1) set -- "$args0" ;;
        2) set -- "$args0" "$args1" ;;
        3) set -- "$args0" "$args1" "$args2" ;;
        4) set -- "$args0" "$args1" "$args2" "$args3" ;;
        5) set -- "$args0" "$args1" "$args2" "$args3" "$args4" ;;
        6) set -- "$args0" "$args1" "$args2" "$args3" "$args4" "$args5" ;;
        7) set -- "$args0" "$args1" "$args2" "$args3" "$args4" "$args5" "$args6" ;;
        8) set -- "$args0" "$args1" "$args2" "$args3" "$args4" "$args5" "$args6" "$args7" ;;
        9) set -- "$args0" "$args1" "$args2" "$args3" "$args4" "$args5" "$args6" "$args7" "$args8" ;;
    esac
fi

# Parse the jvm paramter from console
array=("$@")
jvm_index=-1
for ((i=0;i<${#array[@]};i++))
do
    if [[ "-jvm" == ${array[$i]} ]]; then
        if [[ ${array[$i+1]} =~ ^\{.*\}$ ]]; then
            jvm_args=${array[$i+1]}
            len=${#jvm_args}
            jvm_args=${jvm_args:1:$len-2}
            JAVA_OPTS="$JAVA_OPTS $jvm_args"
            jvm_index=$i
        else
            echo "jvm param format is not right"
            exit -1
        fi
    fi
done

if [[ $jvm_index -ge 0 ]]; then
    unset array[jvm_index]
    unset array[jvm_index+1]
fi

# Escape application args
save () {
    for i do printf %s\\n "$i" | sed "s/'/'\\\\''/g;1s/^/'/;\$s/\$/' \\\\/" ; done
    echo " "
}
APP_ARGS=`save ${array[*]}`

# Collect all arguments for the java command, following the shell quoting and substitution rules
eval set -- $DEFAULT_JVM_OPTS $JAVA_OPTS $FULL_NODE_OPTS -classpath "\"$CLASSPATH\"" org.tron.program.FullNode "$APP_ARGS"

exec "$JAVACMD" "$@"