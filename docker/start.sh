#!/bin/bash

set -e

echo "---> JVM_ARGUMENTS: $JVM_ARGUMENTS"
export JAVA_OPTS="$JAVA_OPTS $JVM_ARGUMENTS"
echo "---> Starting Tron Full Node - Running application from jar ${HOME}/FullNode.jar ..."

echo "java ${JAVA_OPTS} -jar ${HOME}/FullNode.jar -c ${HOME}/main_net_config.conf"
java $JAVA_OPTS -jar ${HOME}/FullNode.jar -c ${HOME}/main_net_config.conf